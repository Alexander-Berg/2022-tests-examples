package ru.yandex.vertis.ydb.skypper

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._
import ru.yandex.vertis.ydb.skypper.settings.ReadMode.Online
import ru.yandex.vertis.ydb.skypper.settings.{QuerySettings, TransactionSettings}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class ParallelTransactionAsyncTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  test("parallel transactionAsync") {
    ydb.update("series")("upsert into series (series_id, title) values (1, 't0')")
    val count = 100
    val futures = (1 to count).map { i =>
      incAsync(ydb, count, i).recoverWith {
        case _ =>
          Future.unit
      }
    }

    val f = Future.sequence(futures)

    Await.result(f, Duration.Inf)
    ydb.stats.getAcquiredCount shouldBe 0
  }

  private def incAsync(ydb: YdbWrapper, count: Int, idx: Int): Future[Unit] = {
    implicit val reads: YdbReads[String] = YdbReads(_.getColumn(0).getUtf8)
    ydb.transactionAsync(
      "inc_series_title",
      settings = TransactionSettings()
    ) { executor =>
      val result = executor.query("series_title_by_series_id")("select title from series where series_id = 1")
      val value = result.next()
      val next = "t" + (value.replace("t", "").toInt + 1)
      if (idx == 5) {
        throw new IllegalArgumentException
      }
      executor.updatePreparedAsync("series_title_by_series_id")(
        ydb"update series set title = $next where series_id = 1"
      )
    }
  }

  private def getSeries(ydb: YdbWrapper): List[Map[String, String]] = {
    val headers: Seq[String] = Seq("series_id", "release_date", "series_info", "title")
    implicit val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
      headers.map(h => h -> rs.getColumn(h).toString()).toMap
    })
    ydb.query("series")("select * from series", settings = QuerySettings(readMode = Online)).toList
  }
}
