package com.captor.keeper.duration

import scala.concurrent.duration._

/**
 * Created by caphael on 15/8/12.
 */
object InstantGenerator extends IntervalGeneratorLike{
  override def getInterval: FiniteDuration = 0 seconds
}
