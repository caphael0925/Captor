package com.captor.keeper

import com.captor.keeper.duration.DurationGeneratorLike
import akka.actor.Actor

/**
 * Created by caphael on 15/8/10.
 *
 *  Keeper作为一个容器用来存放特定对象
 *  Keeper会根据特定的Strategy向外部发送其持有的Element
 *  KepperStrategy会以Trait的方式混入到Keeper中
 *
 */
abstract class Keeper[T](val elem:T,val durGen:DurationGeneratorLike) extends Actor{
  var LASTREQUEST:Long = 0L
  def DUR_GENERATOR=durGen

  def __request

  override def receive: Receive = {
    case _ => {
      __request
    }
  }
}
