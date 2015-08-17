
/**
 * Created by caphael on 15/7/24.
 */




object AnyTest extends App{


  import java.net.HttpURLConnection

  import akka.actor.{Props, ActorSystem}
  import akka.util.Timeout
  import com.captor.douban.book.BookCaptor.Config
  import com.captor.douban.book.Master
  import com.captor.utils.ProxyUtils

  import scala.io.Source
  import akka.routing._

  import com.captor.message.SignleRequest._
  import akka.pattern._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(10 seconds)

  val system = ActorSystem("Crawler")
  val conf = Config().copy(spiders = 8)
  val master = system.actorOf(
    Props{
      new Master(conf)
    },"MASTER"
  )

  val proxy = ProxyUtils.loadProxies()(0)

  val url = "http://api.douban.com/v2/book/1443043"
  val connection: HttpURLConnection = new java.net.URL(url).openConnection(proxy).asInstanceOf[HttpURLConnection]
  Source.fromInputStream(connection.getInputStream).getLines.mkString


  master ! MASTER_START

  //获取Master的运行报告
  (master ? MASTER_REPORT).foreach(println(_))

  //检查ProxyRouter容量
  val proxies=(system.actorSelection(master.path.child("ProxyRouter-RoundRobin")) ? GetRoutees).mapTo[Routees]
  proxies.value.get.get.routees.length

  //检查SpiderRouter容量
  val spiders=(system.actorSelection(master.path.child("SpiderRouter-RoundRobin")) ? GetRoutees).mapTo[Routees]
  spiders.value.get.get.routees.length


}
