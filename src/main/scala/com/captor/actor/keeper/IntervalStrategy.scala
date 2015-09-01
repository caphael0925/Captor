package com.captor.actor.keeper

/**
 * Created by caphael on 15/8/12.
 */

import com.captor.actor.keeper.interval.IntervalGeneratorLike

import scala.concurrent.duration._

trait IntervalStrategy{
  var LAST_SCHEDULED = System.currentTimeMillis
  def INTERVAL_GENERATOR:IntervalGeneratorLike

  def nextInterval: FiniteDuration ={
    val generatedDur = INTERVAL_GENERATOR.nextInterval
    val pastDur = (System.currentTimeMillis-LAST_SCHEDULED) milli
    val duration = if( pastDur >= generatedDur) {0 seconds} else {generatedDur-pastDur}
    duration
  }
}
