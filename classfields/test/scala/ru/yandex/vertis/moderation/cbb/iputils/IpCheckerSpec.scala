package ru.yandex.vertis.moderation.cbb.iputils

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase

@RunWith(classOf[JUnitRunner])
class IpCheckerSpec extends SpecBase {

  "ipChecker" should {
    "works correct with empty data" in {
      val ipChecker = new BinarySearchIpChecker(List.empty)
      ipChecker.isBlocked(IpAddress("0.0.0.0")) should be(false)
    }
  }

  "ipChecker" should {
    "works correct with overlapped ip ranges" in {
      val ipChecker =
        new BinarySearchIpChecker(
          List(
            IpAddress("0.0.0.0") to IpAddress("0.0.0.1"),
            IpAddress("0.0.0.1") to IpAddress("0.0.0.2")
          )
        )
      ipChecker.isBlocked(IpAddress("0.0.0.0")) shouldBe true
      ipChecker.isBlocked(IpAddress("0.0.0.1")) shouldBe true
      ipChecker.isBlocked(IpAddress("0.0.0.2")) shouldBe true
      ipChecker.isBlocked(IpAddress("0.0.0.3")) shouldBe false
    }
  }

  "ipChecker" should {
    "works correct with ipv4 and ipv6 ranges" in {
      val ipv6 = IpAddress("2001:0db8:11a3:09d7:1f34:8a2e:07a0:765d")
      val ipv4 = IpAddress("123.123.0.0")
      val ipChecker =
        new BinarySearchIpChecker(
          List(
            ipv4 to ipv4,
            ipv6 to ipv6
          )
        )
      ipChecker.isBlocked(ipv6) shouldBe true
      ipChecker.isBlocked(ipv4) shouldBe true
    }
  }

  "ipChecker" should {
    "works correct with data from group #61 cbb.test" in {
      val ipChecker =
        new BinarySearchIpChecker(
          List(
            IpAddress("62.165.62.106") to IpAddress("62.165.62.106"),
            IpAddress("70.168.73.50") to IpAddress("70.168.73.50"),
            IpAddress("199.106.235.0") to IpAddress("199.106.235.127"),
            IpAddress("199.106.235.1") to IpAddress("199.106.235.1"),
            IpAddress("216.34.38.66") to IpAddress("216.34.38.66")
          )
        )

      ipChecker.isBlocked(IpAddress("216.34.38.66")) shouldBe true
      ipChecker.isBlocked(IpAddress("62.165.62.106")) shouldBe true
      ipChecker.isBlocked(IpAddress("199.106.235.100")) shouldBe true
      ipChecker.isBlocked(IpAddress("62.165.62.105")) shouldBe false
    }
  }

  "ipChecker" should {
    "works correct with data from group #132 cbb.test" in {
      val ipChecker =
        new BinarySearchIpChecker(
          List(
            IpAddress("217.117.64.2") to IpAddress("217.117.64.2"),
            IpAddress("217.117.64.236") to IpAddress("217.117.64.236")
          )
        )

      ipChecker.isBlocked(IpAddress("217.117.64.2")) shouldBe true
      ipChecker.isBlocked(IpAddress("217.117.64.236")) shouldBe true
      ipChecker.isBlocked(IpAddress("217.117.64.240")) shouldBe false
    }
  }
}
