package com.captor.keeper.strategy

import com.captor.keeper.duration.DurationGeneratorLike

/**
 * Created by caphael on 15/8/12.
 */
trait ScheduleStrategy{
  var LASTREQUEST = System.currentTimeMillis
  def DUR_GENERATOR:DurationGeneratorLike
}
