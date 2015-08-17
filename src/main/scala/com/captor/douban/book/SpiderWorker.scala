package com.captor.douban.book

import java.net.{Proxy=>JProxy}

import akka.actor.Status.Success
import akka.actor.{ActorPath, ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import com.captor.message.SignleRequest._

/**
 * Created by caphael on 15/8/12.
 */
class SpiderWorker(val urlPrefix:String,val proxyRouter:ActorPath,val targetKeeper:ActorPath,val serializer:ActorPath) extends Spider with Actor{

  implicit val timeout = Timeout(10 seconds)

  override def receive: Receive = {

    //启动爬取下一个Target的流程
    case CRAWL_NEXT =>{
      context.actorSelection(proxyRouter) ! ELEMENT_REQUEST_SCHEDULE
    }

    //得到Proxy后进行爬取
    case proxy:JProxy=>{
      //向TargetKeeper申请一个TargetID
      val futureTar = (context.actorSelection(targetKeeper) ? ELEMENT_REQUEST_INSTANT)
      futureTar.map{
        //如果返回的是一个String，视为TargetID，则通知自己进行爬取
        case tar:String=>
          self ! (tar,proxy)
          context.parent ! CRAWL_GOT_PROXY

        //如果得知TargetKeeper已空，则将信息转发给Master
        case ELEMENT_EMPTY =>{
          context.parent ! ELEMENT_EMPTY
        }
      }
    }

    //进行爬取
    case (tarid:String,proxy:JProxy)=>{
      try{
        val response = crawl(urlPrefix+tarid,proxy)
        context.actorSelection(serializer) ! s"${tarid},${response}"
        context.parent ! CRAWL_COMPLETE
      } catch{
        case e:Exception =>
          e.printStackTrace
          println(s"Failed Proxy: ${proxy}")
          context.parent ! (CRAWL_FAIELD,tarid,proxy)
      }

    }

    //终止爬取
    case CRAWL_STOP =>
      context.stop(self)

  }
}
