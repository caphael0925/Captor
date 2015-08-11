package com.captor.utils

import java.net.{InetSocketAddress, Proxy => JProxy}

import scala.xml.XML

/**
 * Created by caphael on 15/8/11.
 */
object ProxyUtils {
  def loadProxies(conf:String="conf/proxy.xml"):Seq[JProxy]={
    val confXML = XML.load(conf)

    (confXML \\ "proxy").map{
      case e=>
        new JProxy(JProxy.Type.HTTP,new InetSocketAddress(e.attribute("ip").get.text,e.attribute("port").get.text.toInt))
    }
  }
}
