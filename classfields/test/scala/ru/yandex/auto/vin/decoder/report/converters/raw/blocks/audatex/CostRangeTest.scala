package ru.yandex.auto.vin.decoder.report.converters.raw.blocks.audatex

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

class CostRangeTest extends AnyFunSuite with Matchers {

  private val ranges = Array(
    CostRange(0, 10000),
    CostRange(10000, 25000),
    CostRange(200000, 300000),
    CostRange(2500000, 2600000),
    CostRange(8000000, None)
  )

  private val rangesData = Table(
    ("value", "expected", "nearest_edge"),
    (1, ranges(0), 0),
    (10000, ranges(1), 10000),
    (24999, ranges(1), 25000),
    (200000, ranges(2), 200000),
    (2536235, ranges(3), 2500000),
    (2546235, ranges(3), 2500000),
    (2556235, ranges(3), 2600000),
    (2566235, ranges(3), 2600000),
    (240004323, ranges(4), 8000000)
  )

  forAll(rangesData) { (value, expected, nearestEdge) =>
    test(s"calculate price ranges of value=$value;expected=$expected;nearest_edge=$nearestEdge") {
      val range = CostRange(value)
      assert(range == expected)
    }
  }
}
