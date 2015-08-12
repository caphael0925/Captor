package com.captor.routing

/**
 * Created by caphael on 15/8/11.
 */

import akka.actor.{ActorRef, Props, Actor}
import akka.routing.{Routee, ActorRefRoutee, RoundRobinRoutingLogic, Router}
import com.captor.message.SignleRequest

class RoundRobinRouter(actors:Seq[ActorRef]) extends Actor{

  lazy val router = {
    val routees = actors.map{
      case x=>
//        context watch x
        ActorRefRoutee(x).asInstanceOf[Routee]
    }.toVector

    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive:Receive = {
    case m:SignleRequest.Value =>
      router.route(m, sender())
  }

}