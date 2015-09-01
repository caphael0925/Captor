package com.captor.actor.keeper

/**
 * Created by caphael on 15/8/7.
 */

import java.text.SimpleDateFormat
import java.util.Date

import com.captor.message.{M_COMMON_REPORT, M_ELEMENT_NEXT_HANDOUT, M_ELEMENT_RETURN, M_ELEMENT_REQUEST_SCHEDULE}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

abstract class PureKeeperWithIntervalStrategy[T](elem:T,name:String) extends PureKeeper[T](elem,name) with IntervalStrategy{

  override def postInstant(out:T):Unit = {
    super.postInstant(out)
    LAST_SCHEDULED = if (System.currentTimeMillis>LAST_SCHEDULED) System.currentTimeMillis else LAST_SCHEDULED
  }
  
  def postSchedule(duration:FiniteDuration): Unit ={
    context.system.scheduler.scheduleOnce(duration,sender, M_ELEMENT_RETURN[T](HANDOUT))
  }

  override def receiveOthers:Receive = {
    case M_ELEMENT_REQUEST_SCHEDULE =>
      val next = nextInterval
      postSchedule(next)
      LAST_SCHEDULED = System.currentTimeMillis + next.toMillis

    case M_COMMON_REPORT =>
      val nextTime:String = if (LAST_SCHEDULED <= System.currentTimeMillis) "AnyTime" else {
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(LAST_SCHEDULED))
      }
      val nextDurMin:Long = (LAST_SCHEDULED - System.currentTimeMillis).milli.toMinutes
      val ret = "The next handout report:\n" +
        s"Time:${nextTime}\n" +
        s"Duration:${if(nextDurMin<0) 0 else nextDurMin} Min"
      sender ! ret

  }
}
