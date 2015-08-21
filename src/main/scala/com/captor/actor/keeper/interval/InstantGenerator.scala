package com.captor.actor.keeper.interval

import scala.concurrent.duration._

/**
 * Created by caphael on 15/8/12.
 */
object InstantGenerator extends IntervalGeneratorLike{
  override def nextInterval: FiniteDuration = 0 seconds
}
