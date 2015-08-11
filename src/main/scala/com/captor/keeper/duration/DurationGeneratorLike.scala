package com.captor.keeper.duration

import scala.concurrent.duration.FiniteDuration
import scala.xml.Elem

/**
 * Created by caphael on 15/8/10.
 */
abstract class DurationGeneratorLike {
  def getDuration:FiniteDuration
}
