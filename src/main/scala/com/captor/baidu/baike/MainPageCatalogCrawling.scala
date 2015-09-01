package com.captor.baidu.baike

import java.net.{HttpURLConnection, Proxy}

import com.captor.actor.spider.Crawling
import com.captor.baidu.baike.Wrap.Catalog
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}

import scala.io.Source
import scala.collection.JavaConversions._
/**
 * Created by caphael on 15/8/27.
 */
trait MainPageCatalogCrawling extends Crawling{
  override def crawl(url: String, proxy: Proxy): Catalog = {
    val connection: HttpURLConnection = new java.net.URL(url).openConnection(proxy).asInstanceOf[HttpURLConnection]
    analyze(connection)
  }

  def crawl(url:String):Catalog = {
    val connection: HttpURLConnection = new java.net.URL(url).openConnection().asInstanceOf[HttpURLConnection]
    analyze(connection)
  }

  def analyze(connection:HttpURLConnection):Catalog = {
    connection.setRequestProperty("Content-type", "application/x-java-serialized-object")

    val response = try{
      val pageSource = Source.fromInputStream(connection.getInputStream).getLines.mkString
      val doc = Jsoup.parse(pageSource)
      extract(doc)
    }catch {
      case e:Exception=>
        throw e
    }finally {
      connection.disconnect
    }

    response
  }

  def extract(doc:Document):Catalog = {
    //Find the Catalog Block
    val catalogBlock = doc.getElementsByClass("catalog-wrapper").get(0)
    val catalogs = catalogBlock.getElementsByTag("dl")

    val all = for(catalog:Element <- catalogs.toList) yield {
      val superCatalog = catalog.getElementsByTag("a").get(0) match {case x=>Catalog(name = x.text,url=x.attr("href"))}
      superCatalog.copy(
        children =  for(sub:Element <- catalog.getElementsByTag("dd").get(0).getElementsByTag("a").toList) yield {
          Catalog(name = sub.text,url = sub.attr("href")
        )
      })
    }

    CATALOG_ROOT.copy(children = all)
  }

}
