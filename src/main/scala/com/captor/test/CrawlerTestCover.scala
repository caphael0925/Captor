package com.captor.test

import java.net.{Proxy => JProxy}

import com.captor.douban.DoubanCrawling

/**
 * Created by caphael on 15/8/13.
 */
trait CrawlerTestCover extends DoubanCrawling{

  override def crawl(url:String,proxy:JProxy):String={
    val timestamp = new java.text.SimpleDateFormat("yyyy MM-dd HH:mm:ss").format(new java.util.Date (System.currentTimeMillis))
    val response = s"[${timestamp}]Crawling URL:${url} by ${proxy}"
    println(s"Crawling URL:${url} by ${proxy}")
    response
  }
}
