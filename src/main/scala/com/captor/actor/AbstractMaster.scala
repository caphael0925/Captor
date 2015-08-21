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
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import java.net.{Proxy=>JProxy}

/**
 * Created by caphael on 15/8/17.
 */
abstract class AbstractMaster extends Actor with ActorLogging{

  implicit var timeout = Timeout(10 seconds)

  /** =========================================
   * Master的内部控制变量
   */
  //记录TargetList是否为空
  var TARGET_EMPTY = false
  //启动了多少次Worker
  var STARTED_COUNT = 0
  //已完成了多少target
  var COMPLETED_COUNT = 0
  //有多少次Worker失败了
  var FAILED_COUNT = 0
  //放弃了多少target
  var DISCARD_COUNT = 0

  //Maxmium times to retry
  var MAX_RETRY = 5

  //Proxy Routees Control
  var PROXY_ROUTEES = 0

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
   * Children:
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

  //Serializer
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

  //RetryWorker&RetryRouter

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

    case M_MASTER_INIT =>
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

    /** ---------------------------------------------------
     * 启动Master
     */
    case M_MASTER_START =>
      log.info("***********Master Start***********")
      STOPPING = false
      STOPPED = false
      //Update PROXY_ROUTEES
      (proxyRouter ? GetRoutees).mapTo[Routees].map{
        case routees =>
          PROXY_ROUTEES = routees.routees.length

          //Broadcast starting message to all workers
          log.debug(s"Start ${workerNum} workers")
          spiderRouter ! Broadcast(M_WORKER_NEXT)
          STARTED_COUNT += workerNum
      }


    /** ---------------------------------------------------
     * 反馈当前运行状态
     */
    case M_COMMON_REPORT =>{
      val report = getReport
      sender ! report
    }

    /** ---------------------------------------------------
     * 返回Master本身
     */
    case M_COMMON_SELF =>
      sender ! this

    /** ---------------------------------------------------
     * 被告知TargetKeeper已空（该爬的都爬完了）
     */
    case M_ELEMENT_EMPTY("TargetKeeper") =>
      log.warning("TargetKeeper is empty!")
      TARGET_EMPTY = true
      FAILED_COUNT += 1
      STARTED_COUNT -= 1

    /** ---------------------------------------------------
     *  停止Master
     */
    case M_MASTER_STOP_NOW =>

      //关闭FileWriter
      log.info("Close output serializer")
      outputSerializer ! M_SERIALIZER_CLOSE
      log.info("Close discard serializer")
      discardSerializer ! M_SERIALIZER_CLOSE

      STOPPED = true

      log.info("Stop the System")
      context.system.shutdown

    case M_MASTER_STOP =>
      STOPPING = true
      log.info("Master is stopping")

    /** ---------------------------------------------------
     * 被告知Spider的爬取失败了 处理
     */
    case M_WORKER_FAILED(target:RetryEntry,proxy:JProxy) =>
      log.info(s"Target [${target.target}] with proxy [${proxy.address.toString.tail}] is failed, start retrying(${target.retries} times had been retry)")

      //记录失败数
      FAILED_COUNT += 1

      //If number of proxy routees below 10,try to append proxy routees
      if(PROXY_ROUTEES<=10){
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
        PROXY_ROUTEES += appendProxyKeepers.length

      }

      //Remove the failed proxy keeper from proxy router
      val failedProxy = context.actorSelection(self.path.child(proxy.address.toString.tail))
      log.info(s"Remove the proxy keeper [${failedProxy.pathString}] from proxy router")
      proxyRouter ! RemoveRoutee(ActorSelectionRoutee(failedProxy))
      PROXY_ROUTEES -= 1

      //进行Retry
      self ! M_WORKER_RETRY(target)

    /** ---------------------------------------------------
     * 重试
     */
    case  M_WORKER_RETRY(entry) =>
      log.info(s"Take the retrying request for target [${entry.target}]")
      //Whether retries for the target has reached the MAX_RETRY
      if( entry.retries >= MAX_RETRY ){
        log.warning(s"The retries of target [${entry.target}] excessed MAX_RETRY(${MAX_RETRY})! ")
        log.warning(s"The target [${entry.target}] was discarded!!")
        self ! M_WORKER_DISCARD(entry)
        
      }else{
        //Add the failed target into retry-keeper
        log.info(s"Add the target [${entry.target}] to retry keeper")
        (retryKeeper ? M_ELEMENT_ADD[RetryEntry](entry)).map{
          case M_COMMON_SUCCEED =>
            log.debug(s"Imply a retry worker to start for the next")
            retryRouter ! M_WORKER_NEXT
        }
      }

    /** ---------------------------------------------------
      * Discard
      */
    case M_WORKER_DISCARD(entry) =>
      //if it archived send the target to discard-serializer
      discardSerializer ! M_SERIALIZE_WRITE[String](entry.target)
      DISCARD_COUNT += 1

      //Try to start a new target dealing
      log.debug("Imply a worker to start for the next")
      startNext


    /** ---------------------------------------------------
     * 判断何时结束
     */
    case M_WORKER_COMPLETE(target:String) =>{

      log.info(s"Target [${target}] is completed")
      //Count of completed workers increment
      COMPLETED_COUNT += 1

      if(TARGET_EMPTY) STOPPING = true

      //如果Target池已空，同时所有已经启动的Spider都完成了
      if(STOPPING && STARTED_COUNT == (COMPLETED_COUNT + DISCARD_COUNT)){
        self ! M_MASTER_STOP_NOW
      }else{
        //Start next worker's job
        log.debug("Imply a worker to start for the next")
        startNext
      }
    }

  }

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
}
