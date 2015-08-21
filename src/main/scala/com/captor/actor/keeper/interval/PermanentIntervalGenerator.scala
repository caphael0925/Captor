package com.captor.actor.keeper.interval

import scala.concurrent.duration.FiniteDuration

/**
 * Created by caphael on 15/8/7.
 */
class PermanentIntervalGenerator(val NEXT:FiniteDuration) extends IntervalGeneratorLike{
  //返回每次指派对象的访问间隔
  override def nextInterval: FiniteDuration = {
    NEXT
  }
}

object PermanentIntervalGenerator{
  //可以直接给出时间段
  def apply(dura:FiniteDuration):PermanentIntervalGenerator = new PermanentIntervalGenerator(dura)

  //也可以给定单位时间，以及单位时间内允许的最大次数
  def apply(span:FiniteDuration,times:Long):PermanentIntervalGenerator= {
    new PermanentIntervalGenerator( span / times)
  }
}
