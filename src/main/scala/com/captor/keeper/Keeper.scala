package com.captor.keeper

import akka.event.Logging
import akka.event.slf4j.Logger
import com.captor.keeper.duration.IntervalGeneratorLike
import akka.actor.Actor
import com.captor.message.SignleRequest._

/**
 * Created by caphael on 15/8/10.
 *
 *  Keeper作为一个容器用来存放特定对象
 *  Keeper会根据特定的Strategy向外部发送其持有的Element
 *  KepperStrategy会以Trait的方式混入到Keeper中
 *
 */
abstract class Keeper[I,O](val ELEMENT:I) extends Actor{

  protected val log = Logging(context.system,this)
  protected def HANDOUT:O

  protected def postInstant(out:O):Unit = {
    sender ! out
  }

  protected def matchOthers:Receive={
    case msg:Any => matchNothing(msg)
  }
  protected def matchNothing(msg:Any):Unit = log.error(s"Match Error! Unacceptable Message:${msg}")

  protected def caseOtherExceptions:PartialFunction[Exception,Unit]={
    case _ =>
  }

  def receive:Receive={
    /**
     *  Keeper默认情况下只处理ELEMENT_REQUEST_INSTANT
     *  如果接收到的消息不是ELEMENT_REQUEST_INSTANT，就丢给matchOthers处理
     *  默认情况下matchOther会记录一条MatchError的错误日志
     *  可以通过重写matchOthers来扩展Keeper
     */
    case ELEMENT_REQUEST =>
      try{
        postInstant(HANDOUT)
      }catch{
        case e:Exception =>
          caseOtherExceptions(e)
      }

    case msg:Any =>
      matchOthers(msg)

  }
}
