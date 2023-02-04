package ru.yandex.vertis.ydb.skypper

import java.util.concurrent.Executors

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._
import ru.yandex.vertis.ydb.skypper.settings.QuerySettings
import ru.yandex.vertis.ydb.skypper.settings.ReadMode.Online

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@RunWith(classOf[JUnitRunner])
class ConcurrentTransactionsTest extends AnyFunSuite with Matchers with InitTestYdb {

  implicit private val trace: Traced = Traced.empty
  private val one = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  test("two concurrent transactions") {
    ydb.update("series")("upsert into series (series_id, title) values (1, 't1')")
    ydb.update("test2")("upsert into test2 (series_id, title, idx) values (1, 't1', 2)")

    val f1 = Future {
      (1 to 20).foreach { _ =>
        ydb.transaction("inc_series_title") { executor =>
          implicit val reads: YdbReads[String] = YdbReads(_.getColumn(0).getUtf8)
          val value = executor.query("series_title_by_series_id")("select title from series where series_id = 1").next()
          val next = "t" + (value.replace("t", "").toInt + 1)
          executor.updatePrepared("series_title_by_series_id")(ydb"update series set title = $next where series_id = 1")
          ydb.transaction("inc_test2_title") { executor =>
            val value = executor.query("test2_title_by_series_id")("select title from test2 where series_id = 1").next()
            val next = "t" + (value.replace("t", "").toInt + 1)
            executor.lastUpdatePrepared("test2_title_by_series_id")(
              ydb"update test2 set title = $next where series_id = 1"
            )
          }
        }
        Thread.sleep(30)
      }
    }(one)

    val f2 = incAsync(ydb, 1)
    Await.result(f1, Duration.Inf)
    Await.result(f2, Duration.Inf)

    val series = getSeries(ydb)
    series should have size 1
    series.head should have size 4
    series.head("series_id") shouldBe "Some[1]"
    series.head("release_date") shouldBe "Empty[]"
    series.head("series_info") shouldBe "Empty[]"
    series.head("title") shouldBe """Some["t41"]"""

    val test2 = getTest2(ydb)
    test2 should have size 1
    test2.head should have size 3
    test2.head("series_id") shouldBe "Some[1]"
    test2.head("idx") shouldBe "Some[2]"
    test2.head("title") shouldBe """Some["t21"]"""

    ydb.stats.getAcquiredCount shouldBe 0
  }

  private def incAsync(ydb: YdbWrapper, i: Int = 1): Future[Unit] = {
    implicit val reads: YdbReads[String] = YdbReads(_.getColumn(0).getUtf8)
    val f = ydb.transactionAsync("inc_series_title") { executor =>
      val resultF = executor.queryAsync("series_title_by_series_id")("select title from series where series_id = 1")
      for {
        value <- resultF.map(_.next())
        next = "t" + (value.replace("t", "").toInt + 1)
        _ <- executor.updatePreparedAsync("series_title_by_series_id")(
          ydb"update series set title = $next where series_id = 1"
        )
      } yield ()
    }
    f.flatMap(_ => {
      Thread.sleep(300); if (i < 20) incAsync(ydb, i + 1) else Future.unit
    })
  }

  private def getSeries(ydb: YdbWrapper): List[Map[String, String]] = {
    val headers: Seq[String] = Seq("series_id", "release_date", "series_info", "title")
    implicit val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
      headers.map(h => h -> rs.getColumn(h).toString()).toMap
    })
    ydb.query("series")("select * from series", settings = QuerySettings(readMode = Online)).toList
  }

  private def getTest2(ydb: YdbWrapper): List[Map[String, String]] = {
    val headers: Seq[String] = Seq("series_id", "idx", "title")
    implicit val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
      headers.map(h => h -> rs.getColumn(h).toString()).toMap
    })
    ydb.query("test2")("select * from test2", settings = QuerySettings(readMode = Online)).toList
  }
}
