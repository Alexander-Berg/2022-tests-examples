package ru.yandex.common.monitoring.metric

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

/**
 * Tests on [[ExpiringMapSupport]].
 *
 * @author dimas
 */
class ExpiringMapSupportSpec
  extends WordSpec
  with Matchers {

  "ExpiringMapSupport" should {
    "preserve actual values for expiration time" in {
      val ttl = 1000.milliseconds
      val support = new ExpiringMapSupport[Int, Int](ttl)

      support.put(1, 1)
      support.asMap().get(1) should be(Some(1))

      Thread.sleep((ttl / 4).toMillis)

      support.asMap().get(1) should be(Some(1))

      support.put(1, 2)

      Thread.sleep((ttl / 4).toMillis)

      support.asMap().get(1) should be(Some(2))

      Thread.sleep(ttl.toMillis)

      support.asMap().get(1) should be(None)
    }
  }
}
