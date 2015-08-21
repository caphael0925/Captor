package com.captor.actor

import akka.actor._
import akka.routing._
import akka.util.Timeout
import akka.pattern._

import com.captor.actor.keeper.{PureKeeper, QueueKeeper}
import com.captor.actor.spider.RetryEntry
import com.captor.message._
import com.captor.serializer.DataSerializer
import com.captor.utils.{ProxyUtils, FetchProxyListFromXiCi}

import scala.collection.mutable.Queue
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.net.{Proxy=>JProxy}

/**
 * Created by caphael on 15/8/17.
 */
abstract class AbstractMaster extends Actor with ActorLogging{

  implicit var timeout = Timeout(10 seconds)

  /** =========================================
   * Master Control
   */
  //Whether the target queue is empty
  var TARGET_EMPTY = false
  //How many targets were started
  var STARTED_COUNT = 0
  //How many targets were successful
  var SUCCEED_COUNT = 0
  //How many targets were failed
  var FAILED_COUNT = 0
  //How many targets were discarded
  var DISCARD_COUNT = 0

  //Maxmium times to retry for each failed target
  var MAX_RETRY = 5

  //Proxy Control
  //When size of proxy pool below PORXIES_LOW_MARK, refresh the proxy pool
  var PORXIES_LOW_MARK = 10
  var PROXIES_REFRESHING = false

  //Stopping Status
  var STOPPING = false
  var STOPPED = false

  /** =====================================================
    * Creating Functions
    */
  //Create ProxyKeeper
  def newProxyKeeper(proxy:JProxy):PureKeeper[JProxy]

  //Create Output Serializer
  def newOutputSerializer:DataSerializer[String]

  //Create Failed Serilizer
  def newDiscardSerializer:DataSerializer[String]

  //Create Worker
  def newSpiderWorker:AbstractWorker

  //Create RetryWorker
  def newRetryWorker:AbstractWorker

  /** ==========================================
   * Members:
   */
  //ProxyKeepers和ProxyRouter
  def proxyList:Seq[JProxy]
  lazy val proxyKeepers:Seq[ActorRef] = proxyList.map{
    case proxy=>
      context.actorOf(
        Props(
          newProxyKeeper(proxy)
        ),proxy.address().toString.tail
      )
  }

  lazy val proxyRouter:ActorRef = context.actorOf(
    RoundRobinGroup(proxyKeepers.map(_.path.toString).toVector).props
    ,"ProxyRouter-RoundRobin"
  )

  //TargetList
  def targetList:Queue[String]
  lazy val targetKeeper:ActorRef = context.actorOf(
    Props{
      new QueueKeeper[String](targetList,"TargetKeeper")
    },"TargetKeeper"
  )

  //Serializers
  lazy val outputSerializer:ActorRef = context.actorOf(
    Props{
      newOutputSerializer
    },"OutputSerializer"
  )

  lazy val discardSerializer:ActorRef = context.actorOf(
    Props{
      newDiscardSerializer
    },"DiscardSerializer"
  )

  //SpiderWorkers&SpiderRouter
  def workerNum:Int

  lazy val spiderRouter:ActorRef = context.actorOf(
    BalancingPool(workerNum).props(
      Props{newSpiderWorker}
    )
    ,"SpiderRouter-Balancing"
  )

  //RetryKeeperr&RetryRouter

  //RetryKeeper
  lazy val retryKeeper:ActorRef = context.actorOf(
    Props{
      new QueueKeeper[RetryEntry](Queue[RetryEntry](),"RetryKeeper")
    },"RetryKeeper"
  )

  def retryWorkerNum:Int

  lazy val retryRouter:ActorRef = context.actorOf(
    BalancingPool(retryWorkerNum).props(
      Props{newRetryWorker}
    )
    ,"RetryRouter-Balancing"
  )

  self ! M_MASTER_INIT


  /** =====================================================
   * 消息处理：
   */
  override def receive: Receive = {
    //Initialize master
    case M_MASTER_INIT =>
      MessagesDealingFunctions.master_init
    //Start master
    case M_MASTER_START =>
      MessagesDealingFunctions.master_start
    //Target's dealing is successful
    case M_WORKER_SUCCEED(target:String) =>
      MessagesDealingFunctions.worker_succeed(target)
    //Target's dealing is failed
    case M_WORKER_FAILED(target:RetryEntry,proxy:JProxy) =>
      MessagesDealingFunctions.worker_failed(target,proxy)
    //Retry to deal the failed target
    case  M_WORKER_RETRY(entry) =>
      MessagesDealingFunctions.worker_retry(entry)
    //Discard the failed target
    case M_WORKER_DISCARD(entry) =>
      MessagesDealingFunctions.worker_discard(entry)
    //Stop master decently
    case M_MASTER_STOP =>
      MessagesDealingFunctions.master_stop
    //Stop master immediately
    case M_MASTER_STOP_IMMEDIATE =>
      MessagesDealingFunctions.master_stop_immediate
    //Target queue was empty
    case M_ELEMENT_EMPTY("TargetKeeper") =>
      MessagesDealingFunctions.target_empty
    //Proxy pool was empty
    case M_ELEMENT_EMPTY("ProxyKeeper") =>
      MessagesDealingFunctions.proxy_empty
    //Report the status of master
    case M_COMMON_REPORT =>
      MessagesDealingFunctions.commen_report
    //Return the master itself
    case M_COMMON_SELF =>
      sender ! this
  }

  @inline
  def getReport:String

  @inline
  def startNext: Unit ={
    if(!STOPPING){
      spiderRouter ! M_WORKER_NEXT
      STARTED_COUNT += 1
    }else{
      log.warning("Master has been stopped! Nothing should be continue...")
    }
  }

  def refreshProxies: Unit ={
    log.warning("Proxy routees are nearly exhausting!")
    log.info("Load new proxies")

    val appendProxies:Seq[JProxy] = FetchProxyListFromXiCi.getProxyInfoList().map{
      case (ip,port,_) =>
        ProxyUtils.createProxy(ip,port)
    }

    //Create new Proxy Keeper
    log.debug("Create new proxy keepers")
    val appendProxyKeepers:Seq[ActorRef] = appendProxies.map{
      case proxy=>
        try{
          context.actorOf(
            Props(
              newProxyKeeper(proxy)
            ),proxy.address().toString.tail
          )
        }catch{
          //If the Keeper for current proxy exists,return null to be filtered
          case e:InvalidActorNameException => null
          case e:Exception =>
            e.printStackTrace
            null
        }
    }.filter(_!=null)

    //Add new Proxy Keepers to ProxyRouter
    log.info("Append new proxy keepers to keeper router")
    appendProxyKeepers.foreach{
      case routee=>
        log.debug(s"Append proxy keeper [${routee.toString}] to keeper router")
        proxyRouter ! AddRoutee(ActorSelectionRoutee(context.actorSelection(routee.path)))
    }

  }

  @inline
  def futureProxyPoolSize:Future[Int]={
    (proxyRouter ? GetRoutees).mapTo[Routees].map(_.routees.length)
  }

  /** =======================================================
   *  Messages Dealing Functions(inline)
   */
  private[this] object MessagesDealingFunctions{
    @inline
    def master_init: Unit ={
      //Initialization
      log.info("***********Master Initialization***********")
      log.info(s"Proxy Router Prepared:${proxyRouter}")
      log.info(s"Spider Router Prepared:${spiderRouter}")
      log.info(s"Retry Router Prepared:${retryRouter}")
      log.info(s"Target Keeper Prepared:${targetKeeper}")
      log.info(s"Retry Keeper Prepared:${retryKeeper}")
      log.info(s"Output Serializer Prepared:${outputSerializer}")
      log.info(s"Discard Serializer Prepared:${discardSerializer}")

      sender ! M_COMMON_SUCCEED
    }

    @inline
    def master_start: Unit ={
      log.info("***********Master Start***********")
      STOPPING = false
      STOPPED = false
      //Update PROXY_ROUTEES
      (proxyRouter ? GetRoutees).mapTo[Routees].map{
        case routees =>

          //Broadcast starting message to all workers
          log.debug(s"Start ${workerNum} workers")
          spiderRouter ! Broadcast(M_WORKER_NEXT)
          STARTED_COUNT += workerNum
      }
    }

    @inline
    def worker_succeed(target:String): Unit ={
      log.info(s"Target [${target}] is completed")
      //Count of completed workers increment
      SUCCEED_COUNT += 1

      if(TARGET_EMPTY) STOPPING = true

      //如果Target池已空，同时所有已经启动的Spider都完成了
      if(STOPPING && STARTED_COUNT == (SUCCEED_COUNT + DISCARD_COUNT)){
        self ! M_MASTER_STOP_IMMEDIATE
      }else{
        //Start next worker's job
        log.debug("Imply a worker to start for the next")
        startNext
      }
    }

    @inline
    def worker_failed(target:RetryEntry,proxy:JProxy): Unit ={
      log.info(s"Target [${target.target}] with proxy [${proxy.address.toString.tail}] is failed, start retrying(${target.retries} times had been retry)")

      //记录失败数
      FAILED_COUNT += 1

      //Remove the failed proxy keeper from proxy router
      val failedProxy = context.actorSelection(self.path.child(proxy.address.toString.tail))
      log.info(s"Remove the proxy keeper [${failedProxy.pathString}] from proxy router")
      proxyRouter ! RemoveRoutee(ActorSelectionRoutee(failedProxy))

      //If number of proxy routees below 10,try to append proxy routees
      futureProxyPoolSize.foreach{
        case poolSize=>
          if(!PROXIES_REFRESHING && poolSize<=PORXIES_LOW_MARK){
            PROXIES_REFRESHING = true
            refreshProxies
            PROXIES_REFRESHING = false
          }

          //进行Retry
          self ! M_WORKER_RETRY(target)
      }
    }

    @inline
    def worker_retry(entry:RetryEntry): Unit ={
      log.info(s"Take the retrying request for target [${entry.target}]")
      //Whether retries for the target has reached the MAX_RETRY
      if( entry.retries >= MAX_RETRY ){
        log.warning(s"The retries of target [${entry.target}] excessed MAX_RETRY(${MAX_RETRY})! ")
        log.warning(s"The target [${entry.target}] was discarded!!")
        self ! M_WORKER_DISCARD(entry)

      }else{
        //Add the failed target to retry-keeper
        log.info(s"Add the target [${entry.target}] to retry keeper")
        (retryKeeper ? M_ELEMENT_ADD[RetryEntry](entry)).map{
          case M_COMMON_SUCCEED =>
            log.debug(s"Imply a retry worker to start for the next")
            retryRouter ! M_WORKER_NEXT
        }
      }
    }

    @inline
    def worker_discard(entry:RetryEntry): Unit ={
      //if it archived send the target to discard-serializer
      discardSerializer ! M_SERIALIZE_WRITE[String](entry.target)
      DISCARD_COUNT += 1

      //Try to start a new target dealing
      log.debug("Imply a worker to start for the next")
      startNext
    }


    @inline
    def master_stop_immediate: Unit ={
      //关闭FileWriter
      log.info("Close output serializer")
      outputSerializer ! M_SERIALIZER_CLOSE
      log.info("Close discard serializer")
      discardSerializer ! M_SERIALIZER_CLOSE

      STOPPED = true

      log.info("Stop the System")
      context.system.shutdown
    }

    @inline
    def master_stop: Unit ={
      STOPPING = true
      log.info("Master is stopping")
    }

    @inline
    def target_empty: Unit ={
      log.warning("TargetKeeper is empty!")
      TARGET_EMPTY = true
      FAILED_COUNT += 1
      STARTED_COUNT -= 1
      self ! M_MASTER_STOP
    }

    @inline
    def proxy_empty: Unit ={
      log.warning("ProxyPool is empty!")
      self ! M_MASTER_STOP
    }

    @inline
    def commen_report: Unit ={
      val report = getReport
      sender ! report
    }
  }

}
