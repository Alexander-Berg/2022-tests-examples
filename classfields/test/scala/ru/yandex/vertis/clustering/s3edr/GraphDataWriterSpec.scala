package ru.yandex.vertis.clustering.s3edr

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.s3edr.core.storage.{DataType, InstanceHeader, Meta, Version}

@RunWith(classOf[JUnitRunner])
class GraphDataWriterSpec extends BaseSpec {

  case class TestCase(description: String, currentDate: DateTime, metas: List[Meta], expectedResult: List[Meta])

  private def meta(version: Version, date: String): Meta =
    Meta(DataType("a", 1), InstanceHeader("a", version, DateTime.parse(date)))

  private val testCases = List(
    TestCase(
      description = "don't drop metas for last day",
      currentDate = DateTime.parse("2019-10-03T21:20"),
      metas = List(
        meta(16, "2019-10-02T21:21"),
        meta(18, "2019-10-03T13:11"),
        meta(22, "2019-10-03T21:20")
      ),
      expectedResult = Nil
    ),
    TestCase(
      description = "retain only one meta per day",
      currentDate = DateTime.parse("2019-10-03T21:20"),
      metas = List(
        meta(16, "2019-09-29T11:21"),
        meta(17, "2019-09-29T12:11")
      ),
      expectedResult = List(
        meta(17, "2019-09-29T12:11")
      )
    ),
    TestCase(
      description = "drop outdated metas",
      currentDate = DateTime.parse("2019-10-03T21:20"),
      metas = List(
        meta(1, "2018-09-13T11:11"),
        meta(4, "2019-09-26T11:11"),
        meta(5, "2019-09-27T11:11"),
        meta(6, "2019-09-27T19:11"),
        meta(13, "2019-10-01T17:11"),
        meta(14, "2019-10-02T11:11"),
        meta(15, "2019-10-02T21:11"),
        meta(16, "2019-10-03T01:11"),
        meta(22, "2019-10-03T21:20")
      ),
      expectedResult = List(
        meta(1, "2018-09-13T11:11"),
        meta(4, "2019-09-26T11:11"),
        meta(6, "2019-09-27T19:11"),
        meta(15, "2019-10-02T21:11")
      )
    )
  )

  "GraphDataWriter" should {
    testCases.foreach {
      case TestCase(description, currentDate, metas, expectedResult) =>
        description in {
          val actualResult = GraphDataWriter.findOutdatedMetas(metas, currentDate)
          actualResult shouldBe expectedResult
        }
    }
  }
}
