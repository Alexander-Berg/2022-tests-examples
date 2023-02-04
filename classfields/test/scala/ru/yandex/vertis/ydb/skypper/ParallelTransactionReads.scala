package ru.yandex.vertis.ydb.skypper

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class ParallelTransactionReads extends AnyFunSuite with Matchers with BeforeAndAfterAll with InitTestYdb {

  implicit private val trace: Traced = Traced.empty

  test("parallel transaction reads") {
    ydb.transaction("") { executor =>
      executor.update("into_series")("upsert into series (series_id, title) values (1, 't1')")
      executor.update("into_test2")("upsert into test2 (series_id, title, idx) values (1, 't1', 2)")
    }

    val f = ydb.transactionAsync("") { executor =>
      for {
        series <- getSeriesById(executor, 1)
        test2 <- getTest2ById(executor, 1)
      } yield {
        series.head("title") + test2.head("title")
      }
    }

    val result = Await.result(f, Duration.Inf)
    result shouldBe """Some["t1"]Some["t1"]"""
  }

  private def getSeriesById(ydb: YdbQueryExecutor, series_id: Int): Future[List[Map[String, String]]] = {
    val headers: Seq[String] = Seq("series_id", "release_date", "series_info", "title")
    ydb
      .queryPreparedAsync("series_by_series_id")(ydb"select * from series where series_id = $series_id")(
        YdbReads(rs => {
          headers.map(h => h -> rs.getColumn(h).toString()).toMap
        })
      )
      .map(_.toList)
  }

  private def getTest2ById(ydb: YdbQueryExecutor, series_id: Int): Future[List[Map[String, String]]] = {
    val headers: Seq[String] = Seq("series_id", "idx", "title")
    ydb
      .queryPreparedAsync("test2_by_series_id")(ydb"select * from test2 where series_id = $series_id")(YdbReads(rs => {
        headers.map(h => h -> rs.getColumn(h).toString()).toMap
      }))
      .map(_.toList)
  }
}
