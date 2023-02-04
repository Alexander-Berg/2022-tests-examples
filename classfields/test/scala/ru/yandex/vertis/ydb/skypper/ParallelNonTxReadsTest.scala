package ru.yandex.vertis.ydb.skypper

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._
import ru.yandex.vertis.ydb.skypper.settings.QuerySettings
import ru.yandex.vertis.ydb.skypper.settings.ReadMode.Stale

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class ParallelNonTxReadsTest extends AnyFunSuite with Matchers with BeforeAndAfterAll with InitTestYdb {

  implicit private val trace: Traced = Traced.empty

  test("parallel non-tx reads") {
    ydb.update("series")("upsert into series (series_id, title) values (1, 't1')")
    ydb.update("test2")("upsert into test2 (series_id, title, idx) values (1, 't1', 2)")

    val f = for {
      series <- getSeriesById(ydb, 1)
      test2 <- getTest2ById(ydb, 1)
    } yield {
      series.head("title") + test2.head("title")
    }

    val result = Await.result(f, Duration.Inf)
    result shouldBe """Some["t1"]Some["t1"]"""
  }

  private def getSeriesById(ydb: YdbWrapper, series_id: Int): Future[List[Map[String, String]]] = {
    val headers: Seq[String] = Seq("series_id", "release_date", "series_info", "title")
    implicit val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
      headers.map(h => h -> rs.getColumn(h).toString()).toMap
    })
    ydb
      .queryPreparedAsync("series_by_series_id")(
        ydb"select * from series where series_id = $series_id",
        QuerySettings(readMode = Stale)
      )
      .map(_.toList)
  }

  private def getTest2ById(ydb: YdbWrapper, series_id: Int): Future[List[Map[String, String]]] = {
    val headers: Seq[String] = Seq("series_id", "idx", "title")
    ydb
      .queryPreparedAsync("test2_by_series_id")(
        ydb"select * from test2 where series_id = $series_id",
        QuerySettings(readMode = Stale)
      )(YdbReads(rs => {
        headers.map(h => h -> rs.getColumn(h).toString()).toMap
      }), trace)
      .map(_.toList)
  }
}
