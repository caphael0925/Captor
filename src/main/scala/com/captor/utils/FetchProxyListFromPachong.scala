package com.captor.utils

/**
 * Created by caphael on 15/8/6.
 */

import scala.xml.{PrettyPrinter, Elem, NodeSeq, XML}

import org.openqa.selenium.By
import org.openqa.selenium.phantomjs.PhantomJSDriver

object FetchProxyListFromPachong {

  val TAR_URL="http://pachong.org/anonymous.html"

  def getProxyList(statLevel:Int=2): IndexedSeq[(String,Int,Int)] ={
    val driver = new PhantomJSDriver()

    driver.get(TAR_URL)
    val rows= driver.findElementsByXPath("//tbody//tr[@data-type='anonymous']")

    val statPattern = ".*([0-9]+)$".r

    val res = for(i <- 0 until rows.size) yield {
      val cells = rows.get(i).findElements(By.tagName("td"))
      val ip = cells.get(1).getText
      val port = cells.get(2).getText.toInt
      val status = cells.get(5).findElement(By.tagName("span")).getAttribute("class") match { case statPattern(n)=>n.toInt}
      (ip,port,status)
    }

    driver.close

    res.filter{
      case (ip,port,status)=> status <= statLevel
    }
  }

  def updateConf(proxiesInfo:IndexedSeq[(String,Int,Int)]): Unit ={
    val confXML=XML.load("conf/proxy.xml")

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

    XML.save("conf/proxy.xml",prettyRes)

  }
}
