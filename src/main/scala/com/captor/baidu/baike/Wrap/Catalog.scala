package com.captor.baidu.baike.Wrap

import scala.xml.{Elem}
import com.captor.baidu.baike.MAIN_URL

/**
 * Created by caphael on 15/8/28.
 */
case class Catalog(val name:String,val url:String="",val children:Seq[Catalog]=Seq[Catalog]()){

  def toElem:Elem = <catalog name={name} url={MAIN_URL+url}></catalog>.copy(child = children.map(_.toElem))

  override def toString:String = toElem.toString
}

