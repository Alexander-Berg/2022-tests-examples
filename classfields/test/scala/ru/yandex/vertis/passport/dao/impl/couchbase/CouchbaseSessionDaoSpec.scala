package ru.yandex.vertis.passport.dao.impl.couchbase

import ru.yandex.vertis.passport.dao.{SessionDao, SessionDaoSpec}
import ru.yandex.vertis.passport.test.CouchbaseSupport

/**
  * Tests for [[CouchbaseSessionDao]]
  *
  * @author zvez
  */
class CouchbaseSessionDaoSpec extends SessionDaoSpec with CouchbaseSupport {

  import scala.concurrent.ExecutionContext.Implicits.global

  val sessionDao: SessionDao = new CouchbaseSessionDao(testBucket)
}
