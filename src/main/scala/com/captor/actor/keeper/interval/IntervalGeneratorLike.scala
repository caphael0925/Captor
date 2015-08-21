package com.captor.actor.keeper.interval

import scala.concurrent.duration.FiniteDuration
import scala.xml.Elem

/**
 * Created by caphael on 15/8/10.
 */
abstract class IntervalGeneratorLike {
  def nextInterval:FiniteDuration
}
