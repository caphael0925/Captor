package com.captor.message


/**
 * Created by caphael on 15/8/12.
 */
object SignleRequest extends Enumeration with MessageLike{

  //Common Message
  val STATUS_REPORT = Value

  //Keeper Message
  val ELEMENT_REQUEST_SCHEDULE = Value
  val ELEMENT_REQUEST = Value
  val ELEMENT_REQUEST_INSTANT = ELEMENT_REQUEST
  val ELEMENT_EMPTY = Value
  val ELEMENT_ADD = Value


  //Spider Message
  val CRAWL_START = Value
  val CRAWL_NEXT = Value
  val CRAWL_STOP = Value
  val CRAWL_ABORT = Value
  val CRAWL_FAIELD = Value
  val CRAWL_RETRY = Value
  val CRAWL_COMPLETE = Value
  val CRAWL_COMPLETE_ALL = Value
  val CRAWL_GOT_PROXY = Value

  //Master Message
  val MASTER_INIT = Value
  val MASTER_START = Value
  val MASTER_STOP = Value
  val MASTER_REPORT = STATUS_REPORT

  //Serializer Message
  val SERIALIZER_FLUSH = Value
  val SERIALIZER_FLUSH_SCHEDULE = Value
  val SERIALIZER_CLOSE = Value

  //Router Message
  val ROUTE_ADD_ROUTEE = Value
  val ROUTE_REMOVE_ROUTEE = Value
  val ROUTE_ROUTEE_EMPTY = Value



}
