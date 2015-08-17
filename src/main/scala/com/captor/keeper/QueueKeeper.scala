package com.captor.keeper

import java.util.NoSuchElementException

import scala.collection.mutable.Queue

import com.captor.message.SignleRequest._

/**
 * Created by caphael on 15/8/12.
 */
class QueueKeeper[T](queue:Queue[T]) extends Keeper[Queue[T],T](queue){
  override protected def HANDOUT: T = ELEMENT.dequeue

  override protected def caseOtherExceptions:PartialFunction[Exception,Unit]={
    case e:NoSuchElementException =>
      e.printStackTrace
      sender ! ELEMENT_EMPTY
  }


  override protected def matchOthers:Receive = {
    case (ELEMENT_ADD,e:T) =>
      ELEMENT.enqueue(e)
  }
}
