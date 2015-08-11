package com.captor.keeper.duration

import scala.concurrent.duration.FiniteDuration

/**
 * Created by caphael on 15/8/7.
 */
class UniformDurationGenerator(val span:FiniteDuration,val times:Long) extends DurationGeneratorLike{
  //返回每次指派对象的访问间隔
  //确保每次得到的Duration都相同，如果span和times除不开，就向上取整
  val NEXT:FiniteDuration = {
    span / times
  }

  override def getDuration: FiniteDuration = {
    NEXT
  }
}

object UniformDurationGenerator{
  def apply(span:FiniteDuration,times:Long):UniformDurationGenerator= new UniformDurationGenerator(span,times)
}
