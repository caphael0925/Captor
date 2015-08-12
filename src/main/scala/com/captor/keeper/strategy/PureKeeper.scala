package com.captor.keeper.strategy

import com.captor.keeper.Keeper

/**
 * Created by caphael on 15/8/12.
 */
abstract class PureKeeper[T](elem:T) extends Keeper[T,T](elem){
  override def HANDOUT:T = ELEMENT

}
