package ru.yandex.vertis.ydb.skypper

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.prepared.YdbInterpolatorUtils._
import ru.yandex.vertis.ydb.skypper.settings.QuerySettings
import ru.yandex.vertis.ydb.skypper.settings.ReadMode.{Online, Stale}

@RunWith(classOf[JUnitRunner])
class UpdateAndQueryTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  test("update and query") {
    ydb.update("series")(
      "upsert into series (series_id, release_date, title) values (1, 100500, 't1')"
    )
    val series = getSeries(ydb)
    series should have size 1
    series.head should have size 4
    series.head("series_id") shouldBe "Some[1]"
    series.head("release_date") shouldBe "Some[100500]"
    series.head("series_info") shouldBe "Empty[]"
    series.head("title") shouldBe """Some["t1"]"""
  }

  test("query prepared") {
    val series = getSeriesById(ydb, 1)
    series should have size 1
    series.head should have size 4
    series.head("series_id") shouldBe "Some[1]"
    series.head("release_date") shouldBe "Some[100500]"
    series.head("series_info") shouldBe "Empty[]"
    series.head("title") shouldBe """Some["t1"]"""
  }

  private def getSeriesById(ydb: YdbWrapper, series_id: Int): List[Map[String, String]] = {
    val headers: Seq[String] = Seq("series_id", "release_date", "series_info", "title")
    implicit val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
      headers.map(h => h -> rs.getColumn(h).toString()).toMap
    })
    ydb
      .queryPrepared("series_by_series_id")(
        ydb"select * from series where series_id = $series_id",
        QuerySettings(readMode = Stale)
      )
      .toList
  }

  private def getSeries(ydb: YdbWrapper): List[Map[String, String]] = {
    val headers: Seq[String] = Seq("series_id", "release_date", "series_info", "title")
    implicit val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
      headers.map(h => h -> rs.getColumn(h).toString()).toMap
    })
    ydb.query("series")("select * from series", settings = QuerySettings(readMode = Online)).toList
  }

  private def getTest2ById(ydb: YdbWrapper, series_id: Int): List[Map[String, String]] = {
    val headers: Seq[String] = Seq("series_id", "idx", "title")
    implicit val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
      headers.map(h => h -> rs.getColumn(h).toString()).toMap
    })
    ydb
      .queryPrepared("test2_by_series_id")(
        ydb"select * from test2 where series_id = $series_id",
        QuerySettings(readMode = Stale)
      )
      .toList
  }

  private def getTest2(ydb: YdbWrapper): List[Map[String, String]] = {
    val headers: Seq[String] = Seq("series_id", "idx", "title")
    implicit val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
      headers.map(h => h -> rs.getColumn(h).toString()).toMap
    })
    ydb.query("test2")("select * from test2", settings = QuerySettings(readMode = Online)).toList
  }
}
