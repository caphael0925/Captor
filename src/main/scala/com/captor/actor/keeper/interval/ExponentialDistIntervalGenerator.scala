package com.captor.actor.keeper.interval

import scala.concurrent.duration._
import org.apache.spark.mllib.random.ExponentialGenerator

/**
* Created by caphael on 15/8/13.
*/
class ExponentialDistIntervalGenerator(meanDur:FiniteDuration) extends IntervalGeneratorLike{
  val generator = new ExponentialGenerator(meanDur.toSeconds)
  override def nextInterval: FiniteDuration = {
    generator.nextValue seconds
  }
}

object ExponentialDistIntervalGenerator{
  def apply(meanDur:FiniteDuration):ExponentialDistIntervalGenerator = new ExponentialDistIntervalGenerator(meanDur)
  def apply(span:FiniteDuration,times:Long):ExponentialDistIntervalGenerator = new ExponentialDistIntervalGenerator(span / times)
}