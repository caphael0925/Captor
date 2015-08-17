package com.captor.utils

/**
 * Created by caphael on 15/8/6.
 */

import org.openqa.selenium.By
import org.openqa.selenium.phantomjs.PhantomJSDriver

import scala.xml.{Elem, NodeSeq, PrettyPrinter, XML}

object FetchProxyListFromXiCi {

  val TAR_URL="http://www.xici.net.co/nt"

  def getProxyList():Seq[(String,Int,String)]={
    val driver = new PhantomJSDriver()

    driver.get(TAR_URL)
    val rows= driver.findElementsByXPath("//table[@id='ip_list']//tr")

    //把表头移除
    rows.remove(0)

    val statPattern = ".*([0-9]+)$".r

    val res = for(i <- 0 until rows.size) yield {
      val cells = rows.get(i).findElements(By.tagName("td"))
      val ip = cells.get(2).getText
      val port = cells.get(3).getText.toInt
      val ptype = cells.get(6).getText
      (ip,port,ptype)
    }

    driver.close

    res
  }

  def updateConf(proxiesInfo:Seq[(String,Int,String)],confFile:String="conf/proxy.xml"): Unit ={
    val confXML=XML.load(confFile)

    val newProxies:NodeSeq = proxiesInfo.map{
      case (ipinfo,portinfo,status)=>
      <proxy ip={s"${ipinfo}"} port={s"${portinfo}"} />
    }

    val proxiesChildren = confXML \ "proxies" apply 0 match{
      case e:Elem => e.copy(child = newProxies)
    }

    val newConfXML = confXML match {
      case e:Elem => e.copy(child = proxiesChildren)
    }

    val pPrinter = new PrettyPrinter(width = 100,step =4)
    val prettyRes = XML.loadString(pPrinter.formatNodes(newConfXML))

    XML.save(confFile,prettyRes)

  }
}
