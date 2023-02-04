package ru.yandex.realty.feeds.utils.direct

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.feeds.utils.ValueRange

@RunWith(classOf[JUnitRunner])
class ValueRangeSpec extends WordSpec with Matchers {

  private def subRangesSpecs: Seq[(ValueRange[Int], ValueRange[Int], Boolean)] = Seq(
    (ValueRange(1, 4), ValueRange(2, 3), true),
    (ValueRange(1, 3), ValueRange(2, 3), false),
    (ValueRange(2, 3), ValueRange(2, 3), false),
    (ValueRange(3, 3), ValueRange(2, 3), false),
    (ValueRange(None, Some(3)), ValueRange(2, 3), false),
    (ValueRange(None, Some(3)), ValueRange(None, Some(3)), false),
    (ValueRange(None, Some(3)), ValueRange(None, Some(4)), false),
    (ValueRange(None, Some(3)), ValueRange(Some(2), None), false)
  )

  "Range" should {
    "correctly return isSubRange" in {
      subRangesSpecs.foreach {
        case (base, toCheck, expected) =>
          withClue(s"$base.isSubRange($toCheck)") {
            base.isSubRangeExclusive(toCheck) shouldBe expected
          }

      }
    }
  }
}
