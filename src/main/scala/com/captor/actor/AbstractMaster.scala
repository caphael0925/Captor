package com.captor.actor

import akka.actor._
import akka.routing._
import akka.util.Timeout
import akka.pattern._

import com.captor.actor.keeper.{PureKeeper, QueueKeeper}
import com.captor.actor.spider.RetryEntry
import com.captor.message._
import com.captor.serializer.DataSerializer
import com.captor.utils.{FetchProxyListFromPachong, ProxyUtils, FetchProxyListFromXiCi}

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

  //Maxmium running workers
  var MAX_RUNNINGS = 0

  //Proxy Control
  //When size of proxy pool below PORXIES_LOW_MARK, refresh the proxy pool
  def PORXIES_LOW_MARK = MAX_RUNNINGS
//  def PROXIES_RETAIN =
  var PROXIES_REFRESHING = false

  //Processing Control Status
  var STOPPING = false
  var STOPPED = false
  var PAUSING = false

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
  def newSpiderWorker:AbstractSpiderWorker

  //Create RetryWorker
  def newRetryWorker:AbstractSpiderWorker

  /** ==========================================
   * Members:
   */
  //ProxyKeepers和ProxyRouter

  def proxyList:Seq[JProxy]
  lazy val proxyKeepers:Seq[ActorRef] = (proxyList :+ ProxyUtils.STRIKER).map{
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
    /**
     * Master Control
     */
    //Initialize master
    case M_MASTER_INIT =>
      MessagesDealingFunctions.master.init
    //Start master
    case M_MASTER_START =>
      MessagesDealingFunctions.master.start
    //Pause the master
    case M_MASTER_PAUSE =>
      MessagesDealingFunctions.master.pause
    //Resume the master
    case M_MASTER_RESUME =>
      MessagesDealingFunctions.master.resume
    //Stop master decently
    case M_MASTER_STOP =>
      MessagesDealingFunctions.master.stop
    //Stop master immediately
    case M_MASTER_STOP_IMMEDIATE =>
      MessagesDealingFunctions.master.stop_immediate

    /**
     * Worker Control
     */
    //Target's dealing is successful
    case M_WORKER_SUCCEED(target:String) =>
      MessagesDealingFunctions.worker.succeed(target)
    //Target's dealing is failed
    case M_WORKER_FAILED(target:RetryEntry,proxy:JProxy) =>
      MessagesDealingFunctions.worker.failed(target,proxy)
    //Retry to deal the failed target
    case  M_WORKER_RETRY(entry) =>
      MessagesDealingFunctions.worker.retry(entry)
    //Discard the failed target
    case M_WORKER_DISCARD(entry) =>
      MessagesDealingFunctions.worker.discard(entry)

    /**
     * Keeper Control
     */
    //Target queue was empty
    case M_ELEMENT_EMPTY("TargetKeeper") =>
      MessagesDealingFunctions.keeper.target_empty
    //Proxy pool was empty
    case M_ELEMENT_EMPTY("ProxyKeeper") =>
      MessagesDealingFunctions.keeper.proxy_empty

    /**
     * Router Control
     */
    case M_ROUTER_REFRESH =>
      MessagesDealingFunctions.router.refresh

    case M_ROUTER_REFRESH_FORCE =>
      MessagesDealingFunctions.router.refreshProxies

    /**
     * Others
     */
    //Report the status of master
    case M_COMMON_REPORT =>
      MessagesDealingFunctions.common.report
    //Return the master itself
    case M_COMMON_SELF =>
      sender ! this
    //
  }





  @inline
  def futureProxyPoolSize:Future[Int]={
    (proxyRouter ? GetRoutees).mapTo[Routees].map(_.routees.length)
  }

  @inline
  def getReport: String ={
    IndexedSeq(s"RunningTargets:${STARTED_COUNT - SUCCEED_COUNT - DISCARD_COUNT}" ,
      s"CompletedTargets:${SUCCEED_COUNT}" ,
      s"FailedProcesses:${FAILED_COUNT}" ,
      s"RetryQueueSize:${Await.result(retryKeeper ? M_ELEMENT_SIZE,30 seconds)}" ,
      s"DiscardTargets:${DISCARD_COUNT}" ,
      s"${
        if(STOPPING) "Master is to be stopped(Stopping)"
        else if(STOPPED) "Master has been stopped"
        else if(PAUSING) "Master is pausing"
        else "Master is Running!"
      }").mkString("\n")
  }

  /** =======================================================
   *  Messages Dealing Functions(inline)
   */
  private[this] object MessagesDealingFunctions{

    object master{

      @inline
      def init: Unit ={
        //Initialization
        log.info("***********Master Initialization***********")
        log.info(s"Proxy Router Prepared:${proxyRouter}")
        log.info(s"Spider Router Prepared:${spiderRouter}")
        log.info(s"Retry Router Prepared:${retryRouter}")
        log.info(s"Target Keeper Prepared:${targetKeeper}")
        log.info(s"Retry Keeper Prepared:${retryKeeper}")
        log.info(s"Output Serializer Prepared:${outputSerializer}")
        log.info(s"Discard Serializer Prepared:${discardSerializer}")

        (spiderRouter ? GetRoutees).mapTo[Routees].foreach{
          case routees=>
            MAX_RUNNINGS = routees.routees.length
        }
      }

      @inline
      def start: Unit ={
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
      def pause: Unit ={
        PAUSING = true
        log.warning("Master would be paused!")
      }

      @inline
      def resume: Unit ={
        PAUSING = false

        (retryKeeper ? M_ELEMENT_SIZE).mapTo[Int].foreach{
          case retries=>
            router.refreshProxies

            futureProxyPoolSize.foreach{
              _=>
                (1 to retries).foreach{
                  _=>
                    worker.retryNext
                }

                if(MAX_RUNNINGS-retries>=0){
                  (1 to MAX_RUNNINGS-retries).foreach{
                    _=>
                      worker.startNext
                  }
                }
            }
        }

      }

      @inline
      def stop_immediate: Unit ={
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
      def stop: Unit ={
        STOPPING = true
        log.info("Master is stopping")
      }

    }

    object worker{

      @inline
      def succeed(target:String): Unit ={
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
      def failed(target:RetryEntry,proxy:JProxy): Unit ={
        log.info(s"Target [${target.target}] with proxy [${proxy.address.toString.tail}] is failed, start retrying(${target.retries} times had been retry)")

        //记录失败数
        FAILED_COUNT += 1

        //Remove the failed proxy keeper from proxy router
        val failedProxy = context.actorSelection(self.path.child(proxy.address.toString.tail))
        log.info(s"Remove the proxy keeper [${failedProxy.pathString}] from proxy router")
        proxyRouter ! RemoveRoutee(ActorSelectionRoutee(failedProxy))

        //Refresh the Routees of ProxyRouter
        router.refresh

        //进行Retry
        self ! M_WORKER_RETRY(target)

      }

      @inline
      def retry(entry:RetryEntry): Unit ={
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
              retryNext
          }
        }
      }

      @inline
      def discard(entry:RetryEntry): Unit ={
        //if it archived send the target to discard-serializer
        discardSerializer ! M_SERIALIZE_WRITE[String](entry.target)
        DISCARD_COUNT += 1

        //Try to start a new target dealing
        log.debug("Imply a worker to start for the next")
        startNext
      }

      @inline
      def startNext: Unit ={
        if (STOPPING){
          log.warning("Master has been stopped! Nothing should be continue...")
        }else if (PAUSING){
          log.warning("Master has been paused! Nothing should be continue...")
        }else {
          if (PROXIES_REFRESHING) {
            futureProxyPoolSize.foreach{
              poolSize =>
                spiderRouter ! M_WORKER_NEXT
                STARTED_COUNT += 1
            }
          }else {
            spiderRouter ! M_WORKER_NEXT
            STARTED_COUNT += 1
          }
        }
      }

      @inline
      def retryNext: Unit ={
        if (PAUSING) {
        }else{
          retryRouter ! M_WORKER_NEXT
        }
      }
    }

    object keeper{
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
    }

    object router{
      def refreshProxies: Future[Int] ={
        log.warning("Proxy routees are nearly exhausting!")
        log.info("Load new proxies")

        val appendProxies:Seq[JProxy] = (FetchProxyListFromXiCi.getProxyInfoList() ++ FetchProxyListFromPachong.getProxyInfoList()).map{
          case (ip,port,_) =>
            ProxyUtils.createProxy(ip,port)
        }

        //Create new Proxy Keeper
        log.debug("Create new proxy keepers")
        val appendProxyKeepers:Seq[ActorPath] = appendProxies.map{
          case proxy=>
            try{
              context.actorOf(
                Props(
                  newProxyKeeper(proxy)
                ),proxy.address().toString.tail
              ).path
            }catch{
              //If the Keeper for current proxy exists,return null to be filtered
              case e:InvalidActorNameException => {
                self.path.child(proxy.address().toString.tail)
              }
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
            proxyRouter ! AddRoutee(ActorSelectionRoutee(context.actorSelection(routee)))
        }

        //Return size of future proxy pool to trigger management message actions
        futureProxyPoolSize
      }


      @inline
      def refresh: Unit ={
        //If number of proxy routees below 10,try to append proxy routees
        futureProxyPoolSize.foreach{
          case poolSize=>
            if(!PROXIES_REFRESHING && poolSize<=PORXIES_LOW_MARK){
              PROXIES_REFRESHING = true
              refreshProxies.foreach{
                case newPoolSize =>
                  log.info(s"Proxy pool size has been extended to ${newPoolSize}")
                  PROXIES_REFRESHING = false
              }
            }
        }
      }
    }

    object common{
      @inline
      def report: Unit ={
        val report = getReport
        sender ! report
      }
    }


  }

}
