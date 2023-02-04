package ru.yandex.vertis.ydb.skypper

import com.yandex.ydb.core.{StatusCode, UnexpectedResultException}
import com.yandex.ydb.table.transaction.TxControl
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.FutureConverters.RichCompletableFuture

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class SessionBusyTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  test("session busy") {
    ydb.update("series")("upsert into series (series_id, title) values (1, 't1')")
    ydb.update("test2")("upsert into test2 (series_id, title, idx) values (1, 't1', 2)")

    val ex = intercept[UnexpectedResultException] {
      ydb.rawExecute("") { session =>
        val txControl = TxControl.onlineRo()
        val f1 = session
          .executeDataQuery("select * from series where series_id = 1", txControl)
          .asScala
          .map(result => {
            result.expect("").getResultSet(0)
          })
        val f2 = session
          .executeDataQuery("select * from test2 where series_id = 1", txControl)
          .asScala
          .map(result => {
            result.expect("").getResultSet(0)
          })
        val f3 = session
          .executeDataQuery("select title from series where series_id = 1", txControl)
          .asScala
          .map(result => {
            result.expect("").getResultSet(0)
          })
        val f4 = session
          .executeDataQuery("select title from test2 where series_id = 1", txControl)
          .asScala
          .map(result => {
            result.expect("").getResultSet(0)
          })
        Await.result(f1, Duration.Inf)
        Await.result(f2, Duration.Inf)
        Await.result(f3, Duration.Inf)
        Await.result(f4, Duration.Inf)
      }
    }

    ex.getStatusCode shouldBe StatusCode.SESSION_BUSY
  }
}
