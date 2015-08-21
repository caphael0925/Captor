package com.captor.dataloader

import scala.xml.Elem

/**
 * Created by caphael on 15/8/11.
 */
abstract class DataLoader[T] {
  def load(conf:Elem):Seq[T]
}
