package com.captor.douban

import java.net.{HttpURLConnection, InetSocketAddress, Proxy => JProxy}

import akka.actor.{Actor, Props, ActorSystem}
import akka.actor.Actor.Receive
import akka.util.Timeout
import akka.pattern._
import com.caphael.common.app.AbstractApp
import com.caphael.common.cli._
import com.captor.dataloader.JDBCDataLoader
import com.captor.actor.keeper.{PureKeeperWithIntervalStrategy, Keeper}
import com.captor.actor.keeper.interval._
import com.captor.utils.ProxyUtils
//import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
/**
* Created by caphael on 15/7/24.
*/

object BookCaptor extends AbstractApp{
  case class Config(
                     var urlPrefix:String="http://api.douban.com/v2/book/",
                     var proxyList:String="conf/proxy.xml",
                     var span:Long=1,
                     var times:Long=6,
                     var spiders:Int=2,
                     var outpath:String = "output/target.out",
                     var failedpath:String = "discard/target.out",
                     var outDelim:String = ","
                     ) extends ConfigBase with HasIOput

  override type CT = Config

  override def appName: String = "BookSpider@Douban"

  override def runBody(): Unit = {
    //设置Future超时时间：10s
    implicit val timeout = Timeout(10 seconds)

    //创建ActorSystem
    val system = ActorSystem("test")


  }

  override def setup(): Unit = {}

  override def parserSetup = {
    val parser = new OptionParser[CT](appName){
      opt[String]('u',"urlPrefix") action { case (x,c:CT)=>
        c.urlPrefix=x;c
      } text "The Prefix of URL which you want to Fetch." required

      opt[String]('p',"proxyList") action { case (x,c:CT)=>
        c.proxyList=x;c
      } text "The Path of Configuration File.(Default:conf/proxy.xml)" optional

      opt[Long]('s',"span") action { case (x,c:CT)=>
        c.span=x;c
      } text "Unit span for ProxyKeeper Scheduling. It Worked with parameter \"times\"(Unit:Seconds)." required

      opt[Long]('n',"times") action { case (x,c:CT)=>
        c.times=x;c
      } text "Times Limit of Request for ProxyKeeper Scheduling. It Worked with parameter \"span\"." required

      opt[Int]("spiders") action { case (x,c:CT)=>
        c.spiders=x;c
      } text "Number of Spiders.(Default:2)" optional

    }

    parser
  }

  override var CONFIG: CT = new CT(urlPrefix="")

}
