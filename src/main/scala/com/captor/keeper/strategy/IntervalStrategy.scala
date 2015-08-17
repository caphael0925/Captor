package com.captor.keeper.strategy

/**
 * Created by caphael on 15/8/12.
 */

import com.captor.keeper.duration.IntervalGeneratorLike

import scala.concurrent.duration._

trait IntervalStrategy{
  var LASTREQUEST = System.currentTimeMillis
  def INTERVAL_GENERATOR:IntervalGeneratorLike

  def nextInterval: FiniteDuration ={
    val generatedDur = INTERVAL_GENERATOR.getInterval
    val pastDur = (System.currentTimeMillis-LASTREQUEST) milli
    val duration = if( pastDur >= generatedDur) {0 seconds} else {generatedDur-pastDur}
    duration
  }
}
