package com.captor.utils

import java.net.{InetSocketAddress, Proxy => JProxy}

import scala.xml.{PrettyPrinter, Elem, NodeSeq, XML}

/**
 * Created by caphael on 15/8/11.
 */
object ProxyUtils {
  def loadFromXML(conf:String="conf/proxy.xml"):Seq[JProxy]={
    val confXML = XML.load(conf)

    (confXML \\ "proxy").map{
      case e=>
        createProxy(e.attribute("ip").get.text,e.attribute("port").get.text.toInt)
    }
  }

  def writeToXML(proxiesInfo:Seq[(String,Int,String)],confFile:String="conf/proxy.xml"): Unit ={
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

  def createProxy(ip:String,port:Int,ptype:String="HTTP"):JProxy={
    ptype match {
      case "HTTP" =>
        new JProxy(JProxy.Type.HTTP,new InetSocketAddress(ip,port))
    }
  }

}
