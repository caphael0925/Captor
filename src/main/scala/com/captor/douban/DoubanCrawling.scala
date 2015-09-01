package com.captor.douban

/**
 * Created by caphael on 15/8/12.
 */
import java.net.{Proxy => JProxy, HttpURLConnection}

import com.captor.actor.spider.Crawling

import scala.io.Source

trait DoubanCrawling extends Crawling{

  def crawl(url:String,proxy:JProxy):String={
    val connection: HttpURLConnection = new java.net.URL(url).openConnection(proxy).asInstanceOf[HttpURLConnection]
    analyze(connection)
  }

  def crawl(url:String):String= {
    val connection: HttpURLConnection = new java.net.URL(url).openConnection().asInstanceOf[HttpURLConnection]
    analyze(connection)
  }

  def analyze(connection: HttpURLConnection):String = {
    connection.setRequestProperty("Content-type", "application/x-java-serialized-object")

    val response = try{
      Source.fromInputStream(connection.getInputStream).getLines.mkString
    }catch {
      case e:Exception=>
        throw e
    }finally {
      connection.disconnect
    }

    response
  }
}
