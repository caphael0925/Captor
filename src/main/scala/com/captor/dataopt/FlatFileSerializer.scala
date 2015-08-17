package com.captor.dataopt

import java.io.PrintWriter

/**
 * Created by caphael on 15/8/13.
 */
abstract class FlatFileSerializer(out:String) extends DataSerializer[String]{

  override def OUTNAME: String = out
  lazy val WRITER = new PrintWriter(OUTNAME)

  override def write(elem: String): Unit = {
    WRITER.println(elem)
  }

  def flush = WRITER.flush
  def close = WRITER.close

}
