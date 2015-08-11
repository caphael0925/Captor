package com.captor.routing

/**
 * Created by caphael on 15/8/11.
 */

import akka.actor.{ActorRef, Props, Actor}
import akka.routing.{Routee, ActorRefRoutee, RoundRobinRoutingLogic, Router}

abstract class RoundRobinRouter extends Actor{
  def actors:Seq[ActorRef]

  lazy val router = {
    val routees = actors.map{
      case x=>
//        context watch x
        ActorRefRoutee(x).asInstanceOf[Routee]
    }.toVector

    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive:Receive = {
    case _ =>
      router.route(true, sender())
  }

}