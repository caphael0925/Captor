package com.captor.dataopt

import akka.actor.{Cancellable, Actor}
import akka.actor.Actor.Receive
import scala.concurrent.duration._
import com.captor.message.SignleRequest._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by caphael on 15/8/13.
 */
class FlatFileSerializerActor(out:String) extends FlatFileSerializer(out) with Actor{
  val REFLUSH_INTERVAL = 1 minutes
  var REFLUSH:Cancellable = context.system.scheduler.scheduleOnce(0 seconds,self,SERIALIZER_FLUSH_SCHEDULE)

  override def receive: Receive = {
    case msg:String=>
      write(msg)

    case SERIALIZER_FLUSH =>
      flush

    case SERIALIZER_CLOSE =>
      REFLUSH.cancel
      close
      context.stop(self)

    case SERIALIZER_FLUSH_SCHEDULE =>
      flush
      REFLUSH = context.system.scheduler.scheduleOnce(REFLUSH_INTERVAL,self,SERIALIZER_FLUSH_SCHEDULE)
  }

}
