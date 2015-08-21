package com.captor.actor.keeper

/**
 * Created by caphael on 15/8/12.
 */
abstract class PureKeeper[T](elem:T,name:String) extends Keeper[T,T](elem,name){
  override def HANDOUT:T = ELEMENT

}
