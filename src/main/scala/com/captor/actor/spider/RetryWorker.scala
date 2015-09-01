package com.captor.actor.spider

import java.net.{Proxy => JProxy}

import akka.actor.ActorPath
import akka.pattern._
import akka.util.Timeout
import com.captor.actor.AbstractSpiderWorker
import com.captor.douban.DoubanCrawling
import com.captor.message._
import com.captor.utils.ProxyUtils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by caphael on 15/8/18.
 */
trait RetryWorker extends CaptorWorkerLike with Crawling{

  override def getRunMsg(tar:Any,proxy:JProxy):M_WORKER_RETRY_RUN = {
    tar match {
      case entry:RetryEntry =>
        M_WORKER_RETRY_RUN(entry,proxy)
    }
  }
  /** =======================================================
    *  Message Dealing
    */
  def receiveOthers:Receive ={


    //进行爬取
    case M_WORKER_RETRY_RUN(RetryEntry(target:String,retries:Int),proxy:JProxy)=>{
      try{
        val response = crawl(targetFormat(target),proxy)
        context.actorSelection(serializer) ! M_SERIALIZE_WRITE[String](s"${target},${response}")
        context.actorSelection(master) ! M_WORKER_SUCCEED(target)
      } catch{
        case e:Exception =>
          log.warning(e.getMessage)
          context.actorSelection(master) ! M_WORKER_FAILED(RetryEntry(target,retries+1),proxy)
      }

    }

  }


}
