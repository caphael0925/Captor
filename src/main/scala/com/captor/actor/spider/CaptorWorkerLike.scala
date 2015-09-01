package com.captor.actor.spider

import java.net.{Proxy => JProxy}

import akka.actor.Actor.Receive
import akka.util.Timeout
import com.captor.actor.AbstractSpiderWorker
import akka.util.Timeout
import com.captor.message._
import com.captor.utils.ProxyUtils
import scala.concurrent.duration._
import akka.pattern._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

/**
 * Created by caphael on 15/8/18.
 */
trait CaptorWorkerLike extends AbstractSpiderWorker{

  implicit var timeout = Timeout(10 seconds)

  val SCHEDULE_DURATION = 1 minutes

  def targetFormat(target:String):String

  def setTimeout(to:FiniteDuration):CaptorWorkerLike ={
    timeout = Timeout(to)
    this
  }

  def receive:Receive={

    //启动爬取下一个Target的流程
    case M_WORKER_NEXT =>
      context.actorSelection(proxyRouter) ! M_ELEMENT_REQUEST_SCHEDULE

    //如果发现得到的Proxy是个Striker，就休息一分钟，再申请一个可用的
    case M_ELEMENT_RETURN(ProxyUtils.STRIKER) =>
      log.info(s"Got a Striker, wait for ${SCHEDULE_DURATION} to request again")
      context.system.scheduler.scheduleOnce(SCHEDULE_DURATION,self,M_WORKER_NEXT)
      context.actorSelection(master) ! M_ROUTER_REFRESH

    //Got a return with proxy
    case M_ELEMENT_RETURN(proxy:JProxy)=>{
      //Request a target from target-keeper
      val futureTar = (context.actorSelection(targetKeeper) ? M_ELEMENT_REQUEST)
      futureTar.map{
        //If got a return with TargetID，tell itself to run
        case M_ELEMENT_RETURN(tar)=>
          self ! getRunMsg(tar,proxy)

        // If target-keeper is empty forward M_ELEMENT_EMPTY to master
        case msg:M_ELEMENT_EMPTY =>{
          context.actorSelection(master) ! msg
        }
      }
    }

    //If proxy-keeper is empty forward M_ELEMENT_EMPTY to master
    case msg:M_ELEMENT_EMPTY =>
      context.parent ! msg

    case msg:Any =>
      receiveOthers(msg)

  }

  def receiveOthers:Receive
  def getRunMsg(tar:Any,proxy:JProxy):Any
}
