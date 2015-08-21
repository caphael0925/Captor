package com.captor.actor.spider

import java.net.{HttpURLConnection, Proxy=>JProxy}

import scala.io.Source

/**
 * Created by caphael on 15/8/21.
 */
trait Crawling {
  def crawl(url:String,proxy:JProxy):String
}
