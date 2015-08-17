package com.captor.douban.book

/**
 * Created by caphael on 15/8/12.
 */
import java.net.{Proxy => JProxy, HttpURLConnection}

import scala.io.Source

class Spider {

  def crawl(url:String,proxy:JProxy):(String,String)={
    val connection: HttpURLConnection = new java.net.URL(url).openConnection(proxy).asInstanceOf[HttpURLConnection]
    connection.setRequestProperty("Content-type", "application/x-java-serialized-object")

    val response = try{
      Source.fromInputStream(connection.getInputStream).getLines.mkString
    }finally {
      connection.disconnect
    }

    (url,s"<response>${response}</response>")
  }

}
