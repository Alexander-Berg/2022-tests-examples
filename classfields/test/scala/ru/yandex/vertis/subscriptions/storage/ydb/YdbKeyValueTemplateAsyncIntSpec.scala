package ru.yandex.vertis.subscriptions.storage.ydb

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.time.{Seconds, Span}
import ru.yandex.vertis.subscriptions.storage.{Format, KeyValueTemplateAsync, KeyValueTemplateAsyncSpecBase}
import ru.yandex.vertis.ydb.Ydb

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await

@RunWith(classOf[JUnitRunner])
class YdbKeyValueTemplateAsyncIntSpec extends KeyValueTemplateAsyncSpecBase with TestYdb {

  override val patienceConfig = PatienceConfig(timeout = scaled(Span(3, Seconds)))

  override def keyValue[T: Format](): KeyValueTemplateAsync[T] =
    KeyValueTemplateAsync.ydbInstance[T](
      ydbWrapper,
      "key_value",
      3,
      autoInitSchema = true,
      instanceId
    )

  override def cleanDatabase(): Unit = {
    import Ydb.ops._
    val action = zioRuntime.unsafeRunToFuture {
      Ydb.runTx {
        ydbWrapper.execute(s"DELETE FROM ${firstDao.table}").ignoreResult.withAutoCommit
      }
    }
    Await.result(action, 1.minute)
  }

}
