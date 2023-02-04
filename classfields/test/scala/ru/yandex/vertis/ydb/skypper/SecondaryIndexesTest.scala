package ru.yandex.vertis.ydb.skypper

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.tracing.Traced

class SecondaryIndexesTest extends AnyFunSuite with Matchers with InitTestYdb {
  implicit private val trace: Traced = Traced.empty

  implicit private val reads: YdbReads[Map[String, String]] = YdbReads(rs => {
    (0 until rs.getColumnCount).map(idx => rs.getColumnName(idx) -> rs.getColumn(idx).toString()).toMap
  })

  test("query by index") {
    ydb.update("upsert")(
      "upsert into series_with_index (series_id, release_date, title) values (1, 100500, 't1')"
    )

    val data = ydb.query("select")("""select *
        |from series_with_index view idx_release_date
        |where release_date = 100500""".stripMargin).next()

    data shouldBe Map(
      "release_date" -> "Some[100500]",
      "series_id" -> "Some[1]",
      "series_info" -> "Empty[]",
      "title" -> """Some["t1"]"""
    )
  }
}
