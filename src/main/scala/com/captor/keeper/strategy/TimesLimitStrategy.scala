package com.captor.keeper.strategy

/**
 * Created by caphael on 15/8/7.
 *
 *  TimesLimitStrategy：
 *    这个策略涉及到两个关键参数：span和times
 *    span:FiniteDuration     给定一个时间段
 *    times:Long              给定最大次数
 *
 *    表明Keeper在span这段时间中，公布Element的次数不要超过times次
 *    举例：span = 1 minutes , times = 10L
 *    表明Keeper公布Element不能超过每分钟10次
 *
 */

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

trait TimesLimitStrategy[T] extends KeeperStrategyLike[T]{

  override def __request:Unit={
    val generatedDur = DUR_GENERATOR.getDuration
    val pastDur = (System.currentTimeMillis-LASTREQUEST) milli

    val duration = if( pastDur >= generatedDur) 0 seconds else generatedDur-pastDur

    context.system.scheduler.scheduleOnce(duration,sender, elem)

    LASTREQUEST = System.currentTimeMillis
  }
}
