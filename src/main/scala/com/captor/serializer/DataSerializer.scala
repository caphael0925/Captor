package com.captor.serializer

import akka.actor.{ActorLogging, Actor}
import com.captor.message.M_SERIALIZE_WRITE

/**
 * Created by caphael on 15/8/12.
 */
abstract class DataSerializer[T] extends Actor with ActorLogging{
  def OUTNAME:String
  def write(elem:T)

  def receive:Receive={
    case M_SERIALIZE_WRITE(msg:T)=>
      write(msg)

    case msg:Any =>
      matchOthers(msg)
  }

  def matchOthers:Receive

}
