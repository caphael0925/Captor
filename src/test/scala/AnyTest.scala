/**
 * Created by caphael on 15/7/24.
 */




object AnyTest extends App{
  import akka.actor.Actor
  import akka.util.Timeout
  import java.net.InetSocketAddress
  import java.net.{Proxy => JProxy}
  import com.captor.keeper.duration.DurationGeneratorLike
  import com.captor.keeper.strategy.TimesLimitStrategy
  import scala.concurrent.duration._
  import akka.actor.{Props, ActorSystem}
  import com.captor.keeper.Keeper
  import akka.pattern._
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Await
  import scala.xml.{Elem, XML}
  import com.captor.keeper.duration.UniformDurationGenerator
  import com.captor.routing.ProxyRoundRobinRouter
  import com.captor.utils.{ProxyUtils}


  implicit val timeout = Timeout(10 seconds)

  val system = ActorSystem("test")

  val router = system.actorOf(
   Props{ new ProxyRoundRobinRouter(ProxyUtils.loadProxies()) {
     override def newDurationGenerator: DurationGeneratorLike = UniformDurationGenerator(1 minutes, 1)
   }
   },"ProxyRouter-RoundRobin"
  )

//  val f = router ? ""

  val calls =(1 to 100).map(x=> router ? "")

  calls.foreach(x=>println(x.value))

//  val res = Await.result[JProxy](f.mapTo[JProxy],10 seconds)

//  println(res)

  system.shutdown
//  class Tester extends Actor{
//    override def receive: Receive = {
//      case _ => {
//        val f = (keeper ? "").mapTo[JProxy]
//        f.foreach(println(_))
//      }
//    }
//  }
//
//  val tester = system.actorOf(Props[Tester],"tester")
//
//  for(i <- 1 to 10){
//    tester ! ""
//  }



}
