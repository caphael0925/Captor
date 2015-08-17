package com.captor.dataopt

/**
 * Created by caphael on 15/8/12.
 */
abstract class DataSerializer[T] {
  def OUTNAME:String
  def write(elem:T)
}
