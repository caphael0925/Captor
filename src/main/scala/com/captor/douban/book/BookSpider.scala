package com.captor.douban.book

import java.net.{HttpURLConnection, InetSocketAddress, Proxy => JProxy}

import akka.actor.{Actor, Props, ActorSystem}
import akka.actor.Actor.Receive
import akka.util.Timeout
import akka.pattern._
import com.caphael.common.app.AbstractApp
import com.caphael.common.cli._
import com.captor.keeper.Keeper
import com.captor.keeper.duration._
import com.captor.keeper.strategy.PureKeeperWithDurationStrategy
import com.captor.loader.SparkSQLDataLoader
import com.captor.routing.RoundRobinRouter
import com.captor.utils.ProxyUtils
import com.captor.message.SignleRequest._
//import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.mutable._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
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

  override def appName: String = "BookSpider@Douban"

  override def runBody(): Unit = {
    //设置Future超时时间：10s
    implicit val timeout = Timeout(10 seconds)

    //创建ActorSystem
    val system = ActorSystem("test")

    //创建Proxy的Keeper
    val proxyKeepers = ProxyUtils.loadProxies().map{
      case proxy=>
        system.actorOf(
        Props{
          new PureKeeperWithDurationStrategy[JProxy](proxy) {
            //设置Keeper的Request频率为每分钟10次
            override def DUR_GENERATOR: DurationGeneratorLike = UniformDurationGenerator(1 minutes, 10)
          }
        }
        )
    }

    //创建ProxyRouter
    val proxyRouter = system.actorOf(
      Props{ new RoundRobinRouter(proxyKeepers)
      },"ProxyRouter-RoundRobin"
    )

    //获取BookID
    //生成Loader
    val conf = <conf><url>jdbc:hive2://proxy:6666</url><sql>select distinct other_id from hujiao.hobby_book_id</sql></conf>
    val bookIDList:Queue[String] = Queue(SparkSQLDataLoader.load(conf).flatMap(x=>x):_*)

    //IDListKeeper保存所有需要爬取的数据
    val idKeeper = system.actorOf(
      Props{
        new Keeper[Queue[String],String](bookIDList){
          override protected def HANDOUT: String = ELEMENT.dequeue
        }
      },"IDKeeper"
    )

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
