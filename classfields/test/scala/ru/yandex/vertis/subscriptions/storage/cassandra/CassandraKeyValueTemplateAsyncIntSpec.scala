package ru.yandex.vertis.subscriptions.storage.cassandra

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.storage.{Format, KeyValueTemplateAsync, KeyValueTemplateAsyncSpecBase}

import scala.concurrent.duration._

/**
  * Specs on [[ru.yandex.vertis.subscriptions.storage.cassandra.KeyValueTemplate]]
  */
@RunWith(classOf[JUnitRunner])
class CassandraKeyValueTemplateAsyncIntSpec extends KeyValueTemplateAsyncSpecBase with TestCassandra {

  override def spanScaleFactor: Double = 5

  override def keyValue[T: Format](): KeyValueTemplateAsync[T] =
    KeyValueTemplateAsync.cassandraInstance[T](
      testSession,
      "key_value",
      3,
      autoInitSchema = true,
      Duration.Inf,
      1.day,
      instanceId
    )

  override def cleanDatabase(): Unit =
    testSession.execute(s"TRUNCATE ${firstDao.table}")

}
