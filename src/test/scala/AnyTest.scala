import com.captor.actor.keeper.PureKeeperWithIntervalStrategy

/**
 * Created by caphael on 15/7/24.
 */




object AnyTest extends App{


  import java.net.HttpURLConnection

  import akka.actor.{Props, ActorSystem}
  import akka.util.Timeout
  import com.captor.douban.{DoubanMaster, BookCaptor}
  import com.captor.message._
  import com.captor.utils.FetchProxyListFromXiCi
  import com.captor.utils.FetchProxyListFromPachong
  import com.captor.utils.ProxyUtils
  import com.captor.message._

  import scala.io.Source
  import akka.routing._

  import akka.pattern._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Await
  import java.net.{Proxy=>JProxy}

  implicit val timeout = Timeout(10 seconds)

  ProxyUtils.writeToXML(ProxyUtils.getProxyInfoList)

  val system = ActorSystem("Crawler")
  val conf = BookCaptor.Config(spiders = 10,outpath = "output/book.out",failedpath = "discard/book.dis")
  val master = system.actorOf(
    Props{
      new DoubanMaster(conf)
    },"MASTER"
  )

  master ! M_MASTER_START


  ProxyUtils.writeToXML(FetchProxyListFromXiCi.getProxyInfoList)

  val proxy = ProxyUtils.loadFromXML()(0)


  val url = "http://api.douban.com/v2/book/1443043"
  val connection: HttpURLConnection = new java.net.URL(url).openConnection(proxy).asInstanceOf[HttpURLConnection]
  Source.fromInputStream(connection.getInputStream).getLines.mkString

  //获取Master的运行报告
  (master ? M_COMMON_REPORT).foreach(println(_))
  val mself = Await.result[DoubanMaster]((master ? M_COMMON_SELF).mapTo[DoubanMaster],10 seconds)
  val kself =Await.result[PureKeeperWithIntervalStrategy[JProxy]]((mself.proxyKeepers(0) ? M_COMMON_SELF).mapTo[PureKeeperWithIntervalStrategy[JProxy]], 10 seconds)

  Await.result(mself.futureProxyPoolSize,10 seconds)

  //检查ProxyRouter容量
  val routees=Await.result[Routees]((mself.proxyRouter ? GetRoutees).mapTo[Routees],10 seconds)

  //检查SpiderRouter容量
  val spiders=(system.actorSelection(master.path.child("SpiderRouter-RoundRobin")) ? GetRoutees).mapTo[Routees]
  spiders.value.get.get.routees.length


}
