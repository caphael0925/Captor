package com.captor.actor.spider

import akka.actor.Actor.Receive
import akka.util.Timeout
import com.captor.actor.AbstractWorker
import akka.util.Timeout
import scala.concurrent.duration._

import scala.concurrent.duration.FiniteDuration

/**
 * Created by caphael on 15/8/18.
 */
trait SpiderWorkerLike extends AbstractWorker{

  implicit var timeout = Timeout(10 seconds)

  def targetFormat(target:String):String

  def setTimeout(to:FiniteDuration):SpiderWorkerLike ={
    timeout = Timeout(to)
    this
  }

}
