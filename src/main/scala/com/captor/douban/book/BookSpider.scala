package com.captor.douban.book

import java.net.{HttpURLConnection, InetSocketAddress, Proxy => JProxy}

import akka.actor.Actor
import akka.actor.Actor.Receive
import com.caphael.common.app.AbstractApp
import com.caphael.common.cli._
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import java.sql.{Connection, DriverManager, ResultSet};

import scala.io.Source

/**
* Created by caphael on 15/7/24.
*/

object BookSpider extends AbstractApp{
  case class Config(var urlPrefix:String,var proxyList:String="conf/proxy.xml") extends ConfigBase with HasIOput

  class Worker extends Actor{
    override def receive: Receive = {
      case proxy:JProxy=>{

      }
    }
  }

  override type CT = Config

  override def appName: String = "BookCrawler@Douban"

  override def runBody(): Unit = {




    val urlPrefix="https://api.douban.com/v2/book/"

//    booklst.map(id=>{
//      val proxy = getProxy()
//      val connection: HttpURLConnection = new java.net.URL("urlPrefix"+id).openConnection(proxy).asInstanceOf[HttpURLConnection]
//      connection.connect
//      val response = Source.fromInputStream(connection.getInputStream).getLines.mkString
//      connection.disconnect
//
//      (id,response)
//    }
//    )



  }



  override def setup(): Unit = {}

  override def parserSetup = {
    val parser = new OptionParser[CT](appName) with ParseIOput[CT]{
      opt[String]('u',"urlPrefix") action { case (x,c:CT)=>
        c.urlPrefix=x;c
      } text "The Prefix of URL which you want to Fetch." required

      opt[String]('p',"proxyList") action { case (x,c:CT)=>
        c.proxyList=x;c
      } text "The Path of Configuration File.(Default:conf/proxy.xml)" optional
    }

    parser.inputParser text "Input of Suffix List which you want to Fetch."
    parser
  }

  override var CONFIG: CT = new CT(urlPrefix="")

}
