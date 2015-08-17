package com.captor.loader

import com.captor.dataopt.{Closable, DataLoader}

import scala.io.Source
import scala.xml.Elem

/**
* Created by caphael on 15/8/12.
*/
object FlatFileDataLoader extends DataLoader[Seq[String]]{
  var filename:String=""
  var delim:String=","
  var hasHeader:Boolean = false

  def load(conf:Elem):Seq[Seq[String]]={
    parseConf(conf)

    Source.fromFile(filename).getLines().map{
      case s:String=>
        s.split(delim).toSeq
    }.toStream
  }

  def parseConf(conf:Elem):Unit={
    conf.child.foreach{
      case <filename>{t}</filename> => filename = t.text
      case <delim>{t}</delim> => delim = t.text
      case <hasHeader>{t}</hasHeader> => hasHeader = t.text.toBoolean
      case _ =>
    }
  }

}
