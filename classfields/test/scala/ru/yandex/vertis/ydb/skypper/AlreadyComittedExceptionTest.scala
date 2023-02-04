package ru.yandex.vertis.ydb.skypper

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.exceptions.AlreadyCommittedException
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._

@RunWith(classOf[JUnitRunner])
class AlreadyComittedExceptionTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty
  implicit private val reads: YdbReads[String] = YdbReads(rs => rs.getColumn(0).getUtf8)
  test("already committed exception") {
    ydb.update("series")("upsert into series (series_id, title) values (1, 't1')")
    ydb.update("test2")("upsert into test2 (series_id, title, idx) values (1, 't1', 2)")

    intercept[AlreadyCommittedException] {
      ydb.transaction("") { e =>
        val seriesId = 1
        val title = e.queryPrepared("")(ydb"select title from series where series_id = $seriesId").next()
        val next = "t" + (title.filter(_.isDigit).toInt + 1)
        e.lastUpdatePrepared("")(ydb"update series set title = $next where series_id = $seriesId")
        e.queryPrepared("")(ydb"select title from series where series_id = $seriesId").next()
      }
    }
  }
}
