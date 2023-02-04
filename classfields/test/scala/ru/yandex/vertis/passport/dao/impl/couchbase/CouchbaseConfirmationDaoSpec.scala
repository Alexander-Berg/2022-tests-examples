package ru.yandex.vertis.passport.dao.impl.couchbase

import ru.yandex.vertis.passport.dao.ConfirmationDaoSpec
import ru.yandex.vertis.passport.test.CouchbaseSupport

/**
  * tests for [[CouchbaseConfirmationDao]]
  *
  * @author zvez
  */
class CouchbaseConfirmationDaoSpec extends ConfirmationDaoSpec with CouchbaseSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  override val confirmationDao = new CouchbaseConfirmationDao(testBucket)
}
