package ru.yandex.vertis.ydb.skypper

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class ParallelTransactionReadWrites extends AnyFunSuite with Matchers with InitTestYdb {

  implicit private val trace: Traced = Traced.empty
  implicit private val ydbReads: YdbReads[String] = YdbReads(_.getColumn(0).getUtf8)

  test("parallel transaction read writes") {
    ydb.update("")("upsert into series (series_id, title) values (1, 't1')")
    ydb.update("")("upsert into test2 (series_id, title, idx) values (1, 't1', 2)")

    val f1 = Future {
      for (_ <- 1 to 20) {
        ydb.transaction("") { e =>
          val t1 = e.query("series_title_by_series_id")("select title from series where series_id = 1").next()
          val t2 = e.query("test2_title_by_series_id")("select title from test2 where series_id = 1").next()
          t1 shouldBe t2
          Thread.sleep(30)
        }
      }
    }

    val f2 = incAsync(1)

    Await.result(f1, Duration.Inf)
    Await.result(f2, Duration.Inf)

    val t1 = ydb.query("")("select title from series where series_id = 1").next()
    val t2 = ydb.query("")("select title from test2 where series_id = 1").next()
    t1 shouldBe t2
  }

  private def incAsync(i: Int): Future[Unit] = {
    val f = ydb.transactionAsync("") { e =>
      for {
        t1 <- e.queryAsync("series_title_by_series_id")("select title from series where series_id = 1").map(_.next())
        n1 = "t" + (t1.replace("t", "").toInt + 1)
        t2 <- e.queryAsync("test2_title_by_series_id")("select title from test2 where series_id = 1").map(_.next())
        n2 = "t" + (t2.replace("t", "").toInt + 1)
        _ <- e.updatePreparedAsync("series_title_by_series_id")(ydb"update series set title = $n1 where series_id = 1")
        _ <- e.lastUpdatePreparedAsync("test2_title_by_series_id")(
          ydb"update test2 set title = $n2 where series_id = 1"
        )
      } yield {}
    }
    f.onComplete { _ =>
      if (i < 20) {
        Thread.sleep(60)
        incAsync(i + 1)
      }
    }
    f
  }
}
