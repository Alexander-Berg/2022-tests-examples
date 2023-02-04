package ru.yandex.vertis.ydb.skypper

import com.yandex.ydb.core.{StatusCode, UnexpectedResultException}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.exceptions.RollbackException
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._

@RunWith(classOf[JUnitRunner])
class RollbackExceptionRequestTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  test("possible to continue after wrong request but end with RollbackException") {
    ydb.update("series")("upsert into series (series_id, title) values (1, 't1')")
    ydb.update("test2")("upsert into test2 (series_id, title, idx) values (1, 't1', 2)")

    intercept[RollbackException] {
      ydb.transaction("") { e =>
        getSeriesById(e, 1)

        val ex0 = intercept[UnexpectedResultException] {
          e.query("")("select1")(YdbReads(rs => rs.getColumnCount))
        }
        ex0.getStatusCode shouldBe StatusCode.GENERIC_ERROR

        val result = getSeriesById(e, 1).head
        result("title") shouldBe """Some["t1"]"""
      }
    }
  }

  private def getSeriesById(ydb: YdbQueryExecutor, series_id: Int): List[Map[String, String]] = {
    val headers: Seq[String] = Seq("series_id", "release_date", "series_info", "title")
    ydb
      .queryPrepared("series_by_series_id")(ydb"select * from series where series_id = $series_id")(YdbReads(rs => {
        headers.map(h => h -> rs.getColumn(h).toString()).toMap
      }))
      .toList
  }
}
