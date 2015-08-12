//package com.captor.routing
//
//import akka.actor.{Props, ActorRef}
//import com.captor.keeper.Keeper
//import com.captor.keeper.duration.{DurationGeneratorLike}
//import java.net.{Proxy=>JProxy}
//
///**
// * Created by caphael on 15/8/11.
// */
//
//abstract class ProxyRoundRobinRouter(proxies:Seq[JProxy]) extends RoundRobinRouter{
//  def newDurationGenerator:DurationGeneratorLike
//
//  override val actors: Seq[ActorRef] = proxies.map{
//      case proxy=>
//        context.actorOf(
//          Props{
//            new Keeper[JProxy](proxy,newDurationGenerator) with TimesLimitStrategyPureWithDurationStrategy[JProxy]
//          }
//        )
//    }
//}
