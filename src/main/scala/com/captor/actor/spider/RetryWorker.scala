package com.captor.actor.spider

import java.net.{Proxy => JProxy}

import akka.actor.ActorPath
import akka.pattern._
import akka.util.Timeout
import com.captor.actor.AbstractWorker
import com.captor.douban.DoubanCrawling
import com.captor.message._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by caphael on 15/8/18.
 */
abstract class RetryWorker (val master:ActorPath,val proxyRouter:ActorPath,val targetKeeper:ActorPath,val serializer:ActorPath) extends SpiderWorkerLike with Crawling{


  /** =======================================================
    *  Message Dealing
    */
  override def receive: Receive = {

    //启动爬取下一个Target的流程
    case M_WORKER_NEXT =>
      context.actorSelection(proxyRouter) ! M_ELEMENT_REQUEST_SCHEDULE

    //Got a return with proxy
    case M_ELEMENT_RETURN(proxy:JProxy)=>{
      //Request a target from target-keeper
      val futureTar = (context.actorSelection(targetKeeper) ? M_ELEMENT_REQUEST)
      futureTar.map{
        //If got a return with TargetID，tell itself to run
        case M_ELEMENT_RETURN(tar:RetryEntry)=>
          self ! M_WORKER_RETRY_RUN(tar,proxy)

        // If target-keeper is empty forward M_ELEMENT_EMPTY to master
        case msg:M_ELEMENT_EMPTY =>{
          context.parent ! msg
        }
      }
    }

    //If proxy-keeper is empty forward M_ELEMENT_EMPTY to master
    case msg:M_ELEMENT_EMPTY =>
      context.parent ! msg


    //进行爬取
    case M_WORKER_RETRY_RUN(RetryEntry(target:String,retries:Int),proxy:JProxy)=>{
      try{
        val response = crawl(targetFormat(target),proxy)
        context.actorSelection(serializer) ! M_SERIALIZE_WRITE[String](s"${target},${response}")
        context.system.actorSelection(master) ! M_WORKER_COMPLETE(target)
      } catch{
        case e:Exception =>
          log.warning(e.getMessage)
          context.system.actorSelection(master) ! M_WORKER_FAILED(RetryEntry(target,retries+1),proxy)
      }

    }

  }

}
