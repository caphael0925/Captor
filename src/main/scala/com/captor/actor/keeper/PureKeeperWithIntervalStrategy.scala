package com.captor.actor.keeper

/**
 * Created by caphael on 15/8/7.
 */

import com.captor.message.{M_ELEMENT_RETURN, M_ELEMENT_REQUEST_SCHEDULE}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

abstract class PureKeeperWithIntervalStrategy[T](elem:T,name:String) extends PureKeeper[T](elem,name) with IntervalStrategy{

  override def postInstant(out:T):Unit = {
    super.postInstant(out)
    LASTREQUEST = System.currentTimeMillis
  }
  
  def postSchedule(duration:FiniteDuration): Unit ={
    context.system.scheduler.scheduleOnce(duration,sender, M_ELEMENT_RETURN[T](HANDOUT))
  }

  override def matchOthers:Receive = {
    case M_ELEMENT_REQUEST_SCHEDULE =>
      postSchedule(nextInterval)
      LASTREQUEST = System.currentTimeMillis
  }
}
