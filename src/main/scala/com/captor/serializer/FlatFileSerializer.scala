package com.captor.serializer

import java.io.PrintWriter

import akka.actor.Cancellable
import com.captor.message.{M_SERIALIZER_CLOSE, M_SERIALIZER_FLUSH, M_SERIALIZER_FLUSH_SCHEDULE}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created by caphael on 15/8/13.
 */
class FlatFileSerializer(out:String) extends DataSerializer[String]{

  override def OUTNAME: String = out
  lazy val WRITER = new PrintWriter(OUTNAME)

  override def write(elem: String): Unit = {
    WRITER.println(elem)
  }

  def FLUSH = WRITER.flush
  def CLOSE = WRITER.close

  var REFLUSH_INTERVAL = 1 minutes

  def setReflushInterval(interval:FiniteDuration):FlatFileSerializer = {
    REFLUSH_INTERVAL = interval
    this
  }

  var REFLUSH:Cancellable = context.system.scheduler.scheduleOnce(0 seconds,self,M_SERIALIZER_FLUSH_SCHEDULE)

  override def matchOthers: Receive = {

    case M_SERIALIZER_FLUSH =>
      FLUSH

    case M_SERIALIZER_CLOSE =>
      REFLUSH.cancel
      CLOSE
      context.stop(self)

    case M_SERIALIZER_FLUSH_SCHEDULE =>
      FLUSH
      REFLUSH = context.system.scheduler.scheduleOnce(REFLUSH_INTERVAL,self,M_SERIALIZER_FLUSH_SCHEDULE)
  }
}
