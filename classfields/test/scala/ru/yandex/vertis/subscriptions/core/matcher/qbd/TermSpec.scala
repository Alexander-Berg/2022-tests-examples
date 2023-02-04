package ru.yandex.vertis.subscriptions.core.matcher.qbd

import DSL._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

/** Specs on [[ru.yandex.vertis.subscriptions.core.matcher.qbd.Term]] and its instances
  */
@RunWith(classOf[JUnitRunner])
class TermSpec extends WordSpec with Matchers {

  "PointTerm" should {
    val p = point("key", "value")
    "cover same value" in {
      p.covers(point("key", "value")) should be(true)
    }
    "not cover other value" in {
      p.covers(point("key", "other-value")) should be(false)
    }
    "not cover points" in {
      p.covers(points("key", "value1", "value2")) should be(false)
    }
    "not cover range" in {
      p.covers(range("key", 10, 20)) should be(false)
    }
    "not be empty" in {
      p.isEmpty should be(false)
    }
  }

  "PointTerm with AnyValue" should {
    val p = point("key", PointTerm.AnyValue)
    "cover any point term" in {
      p.covers(point("key", "any other value")) should be(true)
    }
    "not cover other term types" in {
      val p = point("key", PointTerm.AnyValue)
      p.covers(range("key", 10, 20)) should be(false)
    }
    "not be empty" in {
      p.isEmpty should be(false)
    }
  }

  "PointsTerm" should {
    val p = points("key", "value1", "value2", "value3")
    "cover same value" in {
      p.covers(points("key", "value1", "value2", "value3")) should be(true)
    }
    "cover subset" in {
      p.covers(points("key", "value1", "value3")) should be(true)
    }
    "cover point" in {
      p.covers(point("key", "value1")) should be(true)
    }
    "not cover range" in {
      p.covers(range("key", 10, 20)) should be(false)
    }
    "not be empty" in {
      p.isEmpty should be(false)
    }
    "be empty" in {
      points("key").isEmpty should be(true)
    }
  }

  "PointsTerm with AnyValue" should {
    val p = points("key", "value1", "value2", PointTerm.AnyValue)
    "cover subset" in {
      p.covers(points("key", "value1", "value3")) should be(true)
    }
    "cover exist point" in {
      p.covers(point("key", "value1")) should be(true)
    }
    "cover other point" in {
      p.covers(point("key", "any other value")) should be(true)
    }
    "not cover range" in {
      p.covers(range("key", 10, 20)) should be(false)
    }
  }

  "IntRangeTerm" should {
    val r = range("key", 1, 100)
    "cover same value" in {
      r.covers(range("key", 1, 100)) should be(true)
    }
    "cover subrange" in {
      r.covers(range("key", 40, 60)) should be(true)
    }
    "cover superrange" in {
      r.covers(range("key", -200, 300)) should be(true)
    }
    "cover contained int point" in {
      r.covers(point("key", "50")) should be(true)
    }
    "not cover not contained int point" in {
      r.covers(point("key", "150")) should be(false)
    }
    "not cover non-int point" in {
      r.covers(point("key", "value")) should be(false)
    }
    "cover points" in {
      r.covers(points("key", "50", "150")) should be(true)
    }
    "not cover points" in {
      r.covers(points("key", "150", "200")) should be(false)
    }
    "not be empty" in {
      r.isEmpty should be(false)
    }
  }

}
