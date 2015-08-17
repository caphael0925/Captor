package com.captor.douban.book

import java.net.{Proxy=>JProxy}

import akka.actor.{ActorRef, Props, Actor}
import akka.routing.RoundRobinGroup
import akka.util.Timeout
import com.captor.dataopt.{FlatFileSerializerActor, JDBCDataLoader}
import com.captor.douban.book.BookCaptor.Config
import com.captor.keeper.{QueueKeeper, Keeper}
import com.captor.keeper.duration.{ExponentialDistIntervalGenerator, IntervalGeneratorLike}
import com.captor.keeper.strategy.PureKeeperWithIntervalStrategy
import com.captor.loader.FlatFileDataLoader
import com.captor.message.SignleRequest._
import com.captor.test.CrawlerTestCover
import com.captor.utils.ProxyUtils
import akka.pattern._
import scala.concurrent.duration._
import scala.collection.mutable.Queue
import akka.routing._
import scala.concurrent.ExecutionContext.Implicits.global


/**
 * Created by caphael on 15/8/12.
 */
class Master(masterConf:Config) extends Actor{

  implicit val timeout = Timeout(10 seconds)

  //这三个变量用来判断是否都爬取完成了
  var TARGET_EMPTY = false
  var STARTED_COUNT = 0
  var COMPLETED_COUNT = 0
  var STOP_COUNT = 0
  var FAILED_COUNT = 0

  var ISFINSHED = false

  val Config(_urlPrefix,_proxyList,_span,_times,_spiders,_outpath,_outDelim) = masterConf

  //创建Proxy的Keeper
  val proxyKeepers = ProxyUtils.loadProxies().map{
    case proxy=>
      context.actorOf(
        Props(
          new PureKeeperWithIntervalStrategy[JProxy](proxy) {
            //设置Keeper的Request频率为每分钟10次
            override def INTERVAL_GENERATOR: IntervalGeneratorLike = ExponentialDistIntervalGenerator(_span minutes, _times)
          }
            ),proxy.address().toString.tail
      )
  }

  //创建ProxyRouter
  val proxyRouter = context.actorOf(
    RoundRobinGroup(proxyKeepers.map(_.path.toString).toVector).props
    ,"ProxyRouter-RoundRobin"
  )

  //获取BookID
  //生成Loader
//  val loaderConf = <conf><url>jdbc:hive2://proxy:6666</url><sql>select distinct other_id from hujiao.hobby_book_id</sql></conf>
//  val bookIDList:Queue[String] = Queue(JDBCDataLoader.load(loaderConf).flatMap(x=>x):_*)
  val loaderConf = <conf><filename>cache/bookid.lst</filename></conf>
  val bookIDList:Queue[String] = Queue(FlatFileDataLoader.load(loaderConf).flatMap(x=>x):_*)

  //IDListKeeper保存所有需要爬取的数据
  val idKeeper = context.actorOf(
    Props{
      new QueueKeeper[String](bookIDList)
    },"IDKeeper"
  )

  //RetryKeeper：Spider爬失败的target存到这里
      val retryKeeper = context.actorOf(
        Props{
          new QueueKeeper[String](Queue())
        },"RetryKeeper"
      )

      //创建Serializer
      val fileWriter = context.actorOf(
        Props{
          new FlatFileSerializerActor(_outpath)
        },"FileWriter"
      )

      //创建Spider池
      val spiders = (1 to _spiders).map{
        case i=>
          context.actorOf(
            Props{
              new SpiderWorker(urlPrefix=_urlPrefix,proxyRouter = proxyRouter.path,targetKeeper = idKeeper.path,serializer = fileWriter.path)
            },s"SPIDER${i}"
          )
      }

      //创建Spider的Router
      val spiderRouter = context.actorOf(
        RoundRobinGroup(spiders.map(_.path.toString).toVector).props
        ,"SpiderRouter-RoundRobin"
      )

      //消息处理
      override def receive: Receive = {
      //启动Master
      case MASTER_START => {
        spiderRouter ! CRAWL_NEXT
        STARTED_COUNT += 1
      }

      //反馈当前运行状态，
      case MASTER_REPORT =>{
        val report = getReport
        sender ! report
      }

      //被告知TargetKeeper已空（该爬的都爬完了）
      case ELEMENT_EMPTY =>
        TARGET_EMPTY = true
        STOP_COUNT += 1

      //被告知spider已获得一个Proxy （可以开始爬下一个Target了）
      case CRAWL_GOT_PROXY =>
        //如果有Spider报告自己已经获取到Proxy，就开始爬取下一个Target
        spiderRouter ! CRAWL_NEXT
        STARTED_COUNT += 1

      //停止Master
      case MASTER_STOP =>
        //关闭FileWriter
        fileWriter ! SERIALIZER_CLOSE
        ISFINSHED = true

      //被告知Router已空 （没有可用的Proxy或Spider了）
      case (ROUTE_ROUTEE_EMPTY) =>
        self ! MASTER_STOP

      //被告知Spider的爬取失败了 处理
      case (CRAWL_FAIELD,target:String,proxy:JProxy)=>
        //记录失败数
        FAILED_COUNT += 1

        //把proxy从ProxyRouter里剔除
        val failedProxy = context.actorSelection(self.path.child(proxy.address.toString.tail))
        proxyRouter ! RemoveRoutee(ActorSelectionRoutee(failedProxy))

        //进行Retry
        self ! (CRAWL_RETRY,target)

    //重试
    case  (CRAWL_RETRY,target) =>
      //Retry的方式：将target加入到RetryKeeper

    //判断何时结束
    case CRAWL_COMPLETE =>{
      COMPLETED_COUNT += 1
      //如果Target池已空，同时所有已经启动的Spider都完成了
      if(TARGET_EMPTY && (STARTED_COUNT == (COMPLETED_COUNT + STOP_COUNT))){
        self ! MASTER_STOP
      }
    }

  }

  def getReport:String={
    s"RunningSpiders:${STARTED_COUNT - COMPLETED_COUNT - FAILED_COUNT}\n" +
      s"CompletedCrawlings:${COMPLETED_COUNT}\n" +
      s"FailedCrawlings:${FAILED_COUNT}\n" +
      s"${if(ISFINSHED) "Master had been Stopped!" else "Someone is still Running!"}"
  }
}
