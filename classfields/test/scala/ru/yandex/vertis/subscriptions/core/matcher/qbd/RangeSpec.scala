package ru.yandex.vertis.subscriptions.core.matcher.qbd

import org.scalatest.{Matchers, WordSpec}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/** Specs on [[ru.yandex.vertis.subscriptions.core.matcher.qbd.AbstractRange]]
  */
@RunWith(classOf[JUnitRunner])
class RangeSpec extends WordSpec with Matchers {

  "Int range [0, 100]" should {
    val r = IntRange(Some(0), Some(100))

    "contains 0" in {
      r.contains(0) should be(true)
    }

    "not contains -1" in {
      r.contains(-1) should be(false)
    }

    "contains 100" in {
      r.contains(100) should be(true)
    }

    "not contains 101" in {
      r.contains(101) should be(false)
    }

    "contains 50" in {
      r.contains(50) should be(true)
    }

    "intersects with [0, 1]" in {
      r.intersects(IntRange(Some(0), Some(1))) should be(true)
    }

    "intersects with [-10, 0]" in {
      r.intersects(IntRange(Some(-10), Some(0))) should be(true)
    }

    "not intersects with [-10, -1]" in {
      r.intersects(IntRange(Some(-10), Some(-1))) should be(false)
    }

    "intersects with [100, 200]" in {
      r.intersects(IntRange(Some(100), Some(200))) should be(true)
    }

    "not intersects with [101, 200]" in {
      r.intersects(IntRange(Some(101), Some(200))) should be(false)
    }

    "intersects with (-inf, inf)" in {
      r.intersects(IntRange(None, None)) should be(true)
    }

    "intersects with (-inf, 1]" in {
      r.intersects(IntRange(None, Some(1))) should be(true)
    }

    "not intersects with (-inf, -1]" in {
      r.intersects(IntRange(None, Some(-1))) should be(false)
    }
  }

}
