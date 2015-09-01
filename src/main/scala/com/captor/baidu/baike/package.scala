package com.captor.baidu

import com.captor.baidu.baike.Wrap.Catalog

/**
 * Created by caphael on 15/8/28.
 */
package object baike {
  val MAIN_URL = "http://baike.baidu.com"

  val CATALOG_ROOT_NAME = "全部"
  val CATALOG_ROOT_URL = CATALOG_ROOT_NAME
  val CATALOG_ROOT = Catalog(name=CATALOG_ROOT_NAME, url = MAIN_URL)
}
