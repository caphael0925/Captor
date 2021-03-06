package com.captor.douban

import com.captor.actor.spider.{RetryWorker, SpiderWorker}
import com.captor.actor.{AbstractSpiderWorker, AbstractMaster}
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
  override def newSpiderWorker: AbstractSpiderWorker =  new AbstractSpiderWorker(master = self.path, proxyRouter = proxyRouter.path,targetKeeper = targetKeeper.path,serializer = outputSerializer.path)
    with SpiderWorker with DoubanCrawling{
    override def targetFormat(target: String): String = _urlPrefix + target
  }
  //Create RetryWorker
  override def newRetryWorker: AbstractSpiderWorker =  new AbstractSpiderWorker(master = self.path, proxyRouter = proxyRouter.path,targetKeeper = retryKeeper.path,serializer = outputSerializer.path)
    with RetryWorker with DoubanCrawling{
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

}
