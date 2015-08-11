package com.captor.loader

import java.sql.{ResultSetMetaData, ResultSet, DriverManager}

import scala.xml.Elem

/**
 * Created by caphael on 15/8/11.
 */
trait LoadFromSparkSQL extends DataLoader[Seq[String]]{
  case class Config(
                   val url:String,
                   val sql:String
                     )

  def load(conf:Elem):Seq[Seq[String]] ={
    val c = parse(conf)

    Class.forName("org.apache.hive.jdbc.HiveDriver")
    val conn = DriverManager.getConnection(c.url)

    val colPattern = "".r

    val ret = try{
      val statement = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
      val rs = statement.executeQuery(c.sql)

      val meta = rs.getMetaData

      def results:Stream[Seq[String]] = {if(rs.next) extractRow(rs,meta.getColumnCount) else null } #:: results
      results.takeWhile(_!=null)

    }finally {
      conn.close()
    }

    ret
  }

  def parse(conf:Elem): Config ={
    conf match {
      case <conf>
        <url>{cUrl}</url>
        <sql>{cSql}</sql>
        </conf> => Config(cUrl.text,cSql.text.toLowerCase)
    }
  }

  def extractRow(rs:ResultSet,cols:Int):Seq[String]={
    (1 to cols).map{
      rs.getString(_)
    }
  }
}
