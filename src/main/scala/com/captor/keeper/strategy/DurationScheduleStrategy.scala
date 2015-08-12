package com.captor.keeper.strategy

/**
 * Created by caphael on 15/8/12.
 */
import scala.concurrent.duration._

trait DurationScheduleStrategy extends ScheduleStrategy{
  def getDuration: FiniteDuration ={
    val generatedDur = DUR_GENERATOR.getDuration
    val pastDur = (System.currentTimeMillis-LASTREQUEST) milli
    val duration = if( pastDur >= generatedDur) {0 seconds} else {generatedDur-pastDur}
    duration
  }
}
