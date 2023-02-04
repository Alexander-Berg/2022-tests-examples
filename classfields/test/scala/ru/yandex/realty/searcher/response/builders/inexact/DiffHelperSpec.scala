package ru.yandex.realty.searcher.response.builders.inexact

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.prop.TableDrivenPropertyChecks._
import ru.yandex.realty.proto.search.inexact.DiffTrend
import ru.yandex.realty.util

@RunWith(classOf[JUnitRunner])
class DiffHelperSpec extends WordSpec with Matchers {

  "DiffHelperTest" should {

    "getDiffAndTrend" in {

      val testData =
        Table(
          ("range", "actualValue", "expDiff", "expTrend"),
          (util.Range.create(5f, 10f), 15.0f, 5.0f, DiffTrend.MORE),
          (util.Range.create(null, 10f), 15.0f, 5.0f, DiffTrend.MORE),
          (util.Range.create(null, 10f), 5.0f, 0.0f, DiffTrend.EQUAL),
          (util.Range.create(5f, 10f), 3.0f, 2.0f, DiffTrend.LESS),
          (util.Range.create(5f, null), 3.0f, 2.0f, DiffTrend.LESS),
          (util.Range.create(null, 10f), 5.0f, 0.0f, DiffTrend.EQUAL),
          (util.Range.create(null, null), 5.0f, 0.0f, DiffTrend.DIFF_TREND_UNKNOWN)
        )

      forAll(testData) { (range: util.Range, actualValue: Float, expDiff: Float, expTrend: DiffTrend) =>
        val res = DiffHelper.getDiffAndTrend(range, actualValue)
        res should be(expDiff, expTrend)
      }
    }
  }
}
