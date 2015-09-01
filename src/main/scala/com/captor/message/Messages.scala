package com.captor.message

import java.net.{Proxy=>JProxy}

import com.captor.actor.spider.RetryEntry

/**
 * Created by caphael on 15/8/17.
 */

/** ========================================
  * Master Messages
  */
//初始化Master
case object M_MASTER_INIT
//启动Master
case object M_MASTER_START
//停止Master
private[captor] case object M_MASTER_STOP_IMMEDIATE
//安全停止
case object M_MASTER_STOP
//暂停Master
case object M_MASTER_PAUSE
//恢复Master
case object M_MASTER_RESUME


/** ========================================
  * Common Messages
  */
//运行报告
case object M_COMMON_REPORT
//Return the Actor itself
case object M_COMMON_SELF
//Reply of Succeed
case object M_COMMON_SUCCEED

/** ========================================
 *  Worker Messages
 */
//Deal the next target
case object M_WORKER_NEXT
//Imply master the Spider has got its proxy so that the master can start the next crawling
case object M_WORKER_NEXT_PREPARED
//Stop the worker
case object M_WORKER_STOP
//Imply retry-worker which target needs for retrying
case class M_WORKER_RETRY(target: RetryEntry)
//Imply retry-worker to run
case class M_WORKER_RETRY_RUN(target: RetryEntry,proxy:JProxy)
//Imply worker to run
case class M_WORKER_RUN(target:String,proxy:JProxy)
//Imply master which target with a proxy had been failed
case class M_WORKER_FAILED(target:RetryEntry,proxy:JProxy)
//Crawling of this spider had been completed
case class M_WORKER_SUCCEED(target:String)
//Discard the target
case class M_WORKER_DISCARD(target: RetryEntry)

/** ========================================
 *  Keeper Messages
 */
//Request element with scheduling
case object M_ELEMENT_REQUEST_SCHEDULE
//Request element instantly
case object M_ELEMENT_REQUEST
//Request elements from QueueKeeper
case object M_ELEMENT_QUEUE
//Request size of queue from QueueKeeper
case object M_ELEMENT_SIZE
//Clean the Queue Keeper
case object M_ELEMENT_CLEAR
//The next time keeper to hand out element
case object M_ELEMENT_NEXT_HANDOUT
//Return the element
case class M_ELEMENT_RETURN[T](private val elem:T){
  def get:T = elem
}
//Add element to keeper
case class M_ELEMENT_ADD[T](val elem:T)
//Keeper is empty
case class M_ELEMENT_EMPTY(keeper:String)

/** ========================================
  * Serializer Messages
  */
//关闭Serializer
case object M_SERIALIZER_CLOSE
//Flush serializer buffer
case object M_SERIALIZER_FLUSH
//Scheduling reflush serializer
case object M_SERIALIZER_FLUSH_SCHEDULE
//Imply the serializer what to write down
case class M_SERIALIZE_WRITE[T](msg:T)

/** ========================================
  * Routing Messages
  */
//Router的Routee已经空了
case object M_ROUTER_ROUTEE_EMPTY
//Refresh Routees of ProxyRouter decently
case object M_ROUTER_REFRESH
//Refresh Routees of ProxyRouter by force
case object M_ROUTER_REFRESH_FORCE