package com.captor.utils

/**
 * Created by caphael on 15/8/6.
 */

import org.openqa.selenium.By
import org.openqa.selenium.phantomjs.PhantomJSDriver
import java.net.{Proxy => JProxy}
import scala.xml.{Elem, NodeSeq, PrettyPrinter, XML}

object FetchProxyListFromXiCi {

  val TAR_URL="http://www.xici.net.co/nt"

  def getProxyInfoList():IndexedSeq[(String,Int,String)]={
    try{
      val driver = new PhantomJSDriver()

      driver.get(TAR_URL)
      val rows= driver.findElementsByXPath("//table[@id='ip_list']//tr")

      val statPattern = ".*([0-9]+)$".r

      //Ignore table's header
      val res = for(i <- 1 until rows.size) yield {
        val cells = rows.get(i).findElements(By.tagName("td"))
        val ip = cells.get(2).getText
        val port = cells.get(3).getText.toInt
        val ptype = cells.get(6).getText
        (ip,port,ptype)
      }

      driver.close

      res
    }catch{
      case e:Exception =>
        e.printStackTrace
        IndexedSeq[(String,Int,String)]()
    }

  }



}
