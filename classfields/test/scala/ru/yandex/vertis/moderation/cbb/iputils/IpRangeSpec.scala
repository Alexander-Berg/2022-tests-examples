package ru.yandex.vertis.moderation.cbb.iputils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

@RunWith(classOf[JUnitRunner])
class IpRangeSpec extends SpecBase {
  "ip range 0.0.0.10 - 0.0.0.12" should {
    "contains three ip addresses" in {
      val start = IpAddress("0.0.0.10")
      val end = IpAddress("0.0.0.12")
      val range = start to end
      range.contains(start) should be(true)
      range.contains(IpAddress("0.0.0.11")) should be(true)
      range.contains(end) should be(true)
      range.contains(IpAddress("0.0.0.9")) should be(false)
      range.contains(IpAddress("0.0.0.13")) should be(false)
    }
  }

  "end" should {
    "not be less than start" in {
      assertThrows[IllegalArgumentException] {
        IpAddress("0.0.0.10") to IpAddress("0.0.0.9")
      }
    }
  }

  "range with mixed ipv4 and ipv6" should {
    "throw IllegalArgumentException" in {
      assertThrows[IllegalArgumentException] {
        IpAddress("0.0.0.10") to IpAddress("2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d")
      }
    }
  }
}
