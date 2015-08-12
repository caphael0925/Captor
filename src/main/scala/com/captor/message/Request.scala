package com.captor.message


/**
 * Created by caphael on 15/8/12.
 */
object SignleRequest extends Enumeration with MessageLike{

    val ELEMENT_REQUEST_SCHEDULE = Value
    val ELEMENT_REQUEST = Value
    val ELEMENT_REQUEST_INSTANT = ELEMENT_REQUEST

}
