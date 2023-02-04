package ru.yandex.common.monitoring.util

import org.scalatest.{Matchers, WordSpec}

/**
  * Unit tests for [[CyclicBuffer]]
  *
  * @author incubos
  */
class CyclicBufferSpec extends WordSpec with Matchers {

  "CyclicBuffer constructor" should {
    "disallow zero size" in {
      intercept[IllegalArgumentException] {
        new CyclicBuffer[Unit](0)
      }
    }

    "disallow negative size" in {
      intercept[IllegalArgumentException] {
        new CyclicBuffer[Unit](-1)
      }
    }
  }

  "CyclicBuffer of size 1" should {
    val buf = new CyclicBuffer[Integer](1)

    "be empty" in {
      buf.toSeq should be('empty)
    }

    "remember element" in {
      buf.add(0)
      buf.toSeq should equal(Seq(0))
    }

    "replace element" in {
      buf.add(1)
      buf.toSeq should equal(Seq(1))
    }

    "get through overflow" in {
      buf.buffer.set(0, null)
      buf.toSeq should be('empty)
      buf.index.set(Integer.MAX_VALUE)
      buf.add(0)
      buf.toSeq should equal(Seq(0))
      buf.add(1)
      buf.toSeq should equal(Seq(1))
      buf.add(2)
      buf.toSeq should equal(Seq(2))
    }
  }

  "CyclicBuffer of size 2" should {
    val buf = new CyclicBuffer[Integer](2)

    "be empty" in {
      buf.toSeq should be('empty)
    }

    "remember element" in {
      buf.add(0)
      buf.toSeq should equal(Seq(0))
    }

    "add element" in {
      buf.add(1)
      buf.toSeq should equal(Seq(0, 1))
    }

    "eject first element" in {
      buf.add(2)
      buf.toSeq should equal(Seq(2, 1))
    }

    "eject second element" in {
      buf.add(3)
      buf.toSeq should equal(Seq(2, 3))
    }

    "get through overflow" in {
      buf.buffer.set(0, null)
      buf.buffer.set(1, null)
      buf.toSeq should be('empty)
      buf.index.set(Integer.MAX_VALUE)
      buf.add(0)
      buf.toSeq should equal(Seq(0))
      buf.add(1)
      buf.toSeq should equal(Seq(1, 0))
      buf.add(2)
      buf.toSeq should equal(Seq(1, 2))
      buf.add(3)
      buf.toSeq should equal(Seq(3, 2))
    }
  }
}
