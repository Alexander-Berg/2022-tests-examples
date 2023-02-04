package ru.yandex.vertis.passport.dao.impl.couchbase

import ru.yandex.vertis.passport.dao.{MarkerDao, MarkerDaoSpec}
import ru.yandex.vertis.passport.test.CouchbaseSupport

/**
  *
  * @author zvez
  */
class CouchbaseMarkerDaoSpec extends MarkerDaoSpec with CouchbaseSupport {
  import scala.concurrent.ExecutionContext.Implicits.global

  override val dao: MarkerDao = new CouchbaseMarkerDao(testBucket)
}
