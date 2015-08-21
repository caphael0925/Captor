package com.captor.douban

import com.captor.actor.spider.{RetryWorker, SpiderWorker}
import com.captor.actor.{AbstractWorker, AbstractMaster}
import com.captor.actor.keeper.{PureKeeperWithIntervalStrategy, PureKeeper}
import com.captor.actor.keeper.interval.{ExponentialDistIntervalGenerator, IntervalGeneratorLike}
import BookCaptor.Config
import com.captor.loader.FlatFileDataLoader
import com.captor.serializer.{FlatFileSerializer, DataSerializer}
import com.captor.utils.ProxyUtils

import scala.collection.mutable.Queue
import scala.concurrent.duration._

import java.net.{Proxy=>JProxy}


/**
 * Created by caphael on 15/8/17.
 */
class DoubanMaster(masterConf:Config) extends AbstractMaster{
  /**
   * Arrange Config
   */

  val Config(_urlPrefix,_proxyList,_span,_times,_spiders,_outpath,_failedpath,_outDelim) = masterConf
  
  /** =====================================================
    * Creating Functions
    */
  //Create ProxyKeeper
  override def newProxyKeeper(proxy:JProxy): PureKeeper[JProxy] = new PureKeeperWithIntervalStrategy[JProxy](proxy,"ProxyKeeper") {
    //Keep interval between keeper returns follows a specialized distribution(such like exponential distribution)
    override def INTERVAL_GENERATOR: IntervalGeneratorLike = 
      ExponentialDistIntervalGenerator(_span minutes, _times)
  }

  //Create Serializer
  override def newOutputSerializer: DataSerializer[String] = new FlatFileSerializer(_outpath)
  //Create Discard Serilizer
  override def newDiscardSerializer: DataSerializer[String] = new FlatFileSerializer(_failedpath)
  //Create Worker
  override def newSpiderWorker: AbstractWorker =  new SpiderWorker(master = self.path, proxyRouter = proxyRouter.path,targetKeeper = targetKeeper.path,serializer = outputSerializer.path) with DoubanCrawling{
    override def targetFormat(target: String): String = _urlPrefix + target
  }
  //Create RetryWorker
  override def newRetryWorker: AbstractWorker =  new RetryWorker(master = self.path, proxyRouter = proxyRouter.path,targetKeeper = retryKeeper.path,serializer = outputSerializer.path) with DoubanCrawling{
    override def targetFormat(target: String): String = _urlPrefix + target
  }

  //proxyList
  override val proxyList: Seq[JProxy] = ProxyUtils.loadFromXML()

  //TargetList
  val loaderConf = <conf><filename>cache/bookid.lst</filename></conf>
  override val targetList:Queue[String] = Queue(FlatFileDataLoader.load(loaderConf).flatMap(x=>x):_*)

  //SpiderWorkers&SpiderRouter
  override val workerNum: Int = _spiders

  //RetryWorker&RetryRouter
  override val retryWorkerNum: Int = 2

  override def getReport: String ={
    IndexedSeq(s"RunningTargets:${STARTED_COUNT - COMPLETED_COUNT - DISCARD_COUNT}" ,
      s"CompletedTargets:${COMPLETED_COUNT}" ,
      s"FailedProcesses:${FAILED_COUNT}" ,
      s"DiscardTargets:${DISCARD_COUNT}" ,
      s"${
        if(STOPPING) "Master is to be stopped(Stopping)"
        else if(STOPPED) "Master has been stopped"
        else "Master is Running!"
      }").mkString("\n")
  }
}
