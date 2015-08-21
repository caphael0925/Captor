package com.captor.actor.keeper

import java.util.NoSuchElementException

import com.captor.message._

import scala.collection.mutable.Queue


/**
 * Created by caphael on 15/8/12.
 */
class QueueKeeper[T](queue:Queue[T],name:String) extends Keeper[Queue[T],T](queue,name){
  override protected def HANDOUT: T = ELEMENT.dequeue

  override protected def caseOtherExceptions:PartialFunction[Exception,Unit]={
    case e:NoSuchElementException =>
      e.printStackTrace
      sender ! M_ELEMENT_EMPTY(name)
  }


  override protected def matchOthers:Receive = {
    case M_ELEMENT_ADD(e:T) =>
      ELEMENT.enqueue(e)
      sender ! M_COMMON_SUCCEED

    case M_ELEMENT_QUEUE =>
      sender ! ELEMENT

    case M_ELEMENT_SIZE =>
      sender ! ELEMENT.length

    case M_ELEMENT_CLEAR =>
      ELEMENT.clear()
  }
}