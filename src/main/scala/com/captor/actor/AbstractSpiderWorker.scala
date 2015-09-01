package com.captor.actor

import akka.actor.{ActorPath, ActorLogging, Actor}
import akka.actor.Actor.Receive

/**
 * Created by caphael on 15/8/17.
 */
abstract class AbstractSpiderWorker(val master:ActorPath,val proxyRouter:ActorPath,val targetKeeper:ActorPath,val serializer:ActorPath) extends Actor with ActorLogging{

}
