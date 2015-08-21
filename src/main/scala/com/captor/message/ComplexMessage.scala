//package com.captor.message
//
///**
// * Created by caphael on 15/8/17.
// */
//
//import java.net.{Proxy=>JProxy}
//
//import com.captor.actor.spider.RetryEntry
//
//
///** ==================================================
// *  Worker Messages
// */
////Imply retry-worker which target needs for retrying
//case class M_WORKER_RETRY(target: RetryEntry)
////Imply retry-worker to run
//case class M_WORKER_RETRY_RUN(target: RetryEntry,proxy:JProxy)
////Imply worker to run
//case class M_WORKER_RUN(target:String,proxy:JProxy)
////Imply master which target with a proxy had been failed
//case class M_WORKER_FAILED(target:RetryEntry,proxy:JProxy)
////Crawling of this spider had been completed
//case class M_WORKER_COMPLETE(target:String)
////Discard the target
//case class M_WORKER_DISCARD(target: RetryEntry)
//
///** ==================================================
// *  Serializer Message
// */
////Imply the serializer what to write down
//case class M_SERIALIZE_WRITE[T](msg:T)
//
///** ==================================================
//  * Keeper Message
//  */
////Return the element
//case class M_ELEMENT_RETURN[T](private val elem:T){
//  def get:T = elem
//}
////Add element to keeper
//case class M_ELEMENT_ADD[T](val elem:T)
////Keeper is empty
//case class M_ELEMENT_EMPTY(keeper:String)