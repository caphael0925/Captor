import java.net.HttpURLConnection

import org.openqa.selenium.phantomjs.PhantomJSDriver

import scala.io.Source

/**
 * Created by caphael on 15/8/24.
 */
object WeiboTest {

  import com.captor.utils._

  val proxies = FetchProxyListFromXiCi.getProxyInfoList.map{case (ip,port,_) =>ProxyUtils.createProxy(ip,port)}

  val url = "http://weibo.com"

  val driver = new PhantomJSDriver()


}
