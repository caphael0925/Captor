package com.captor.keeper.strategy

/**
 * Created by caphael on 15/8/7.
 */

import com.captor.message.SignleRequest._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class PureKeeperWithIntervalStrategy[T](elem:T) extends PureKeeper[T](elem) with IntervalStrategy{

  override def postInstant(out:T):Unit = {
    super.postInstant(out)
    LASTREQUEST = System.currentTimeMillis
  }
  
  def postSchedule(duration:FiniteDuration): Unit ={
    context.system.scheduler.scheduleOnce(duration,sender, HANDOUT)
  }

  override def matchOthers:Receive = {
    case ELEMENT_REQUEST_SCHEDULE =>
      postSchedule(nextInterval)
      LASTREQUEST = System.currentTimeMillis
  }
}
