package com.captor.dataloader

import java.sql.{DriverManager, ResultSet}

import scala.xml.Elem

/**
 * Created by caphael on 15/8/11.
 */
object JDBCDataLoader extends DataLoader[Seq[String]]{
  var url:String=""
  var sql:String=""
  var driver:String="org.apache.hive.jdbc.HiveDriver"

  def load(conf:Elem):Seq[Seq[String]] ={
    val c = parseConf(conf)

    Class.forName(driver)
    val conn = DriverManager.getConnection(url)

    val ret = try{
      val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      val rs = statement.executeQuery(sql)

      val meta = rs.getMetaData

      def results:Stream[Seq[String]] = {if(rs.next) extractRow(rs,meta.getColumnCount) else null } #:: results
      results.takeWhile(_!=null).toArray

    }finally {
      conn.close()
    }

    ret
  }

  def parseConf(conf:Elem): Unit ={
    conf.child.foreach {
      case <url>{t}</url> => url = t.text
      case <sql>{t}</sql> => sql = t.text.toLowerCase
      case <driver>{t}</driver> => driver = t.text
      case _ =>
    }
  }

  def extractRow(rs:ResultSet,cols:Int):Seq[String]={
    (1 to cols).map{
      rs.getString(_)
    }
  }
}
