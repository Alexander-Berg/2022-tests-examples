package ru.auto.api.model

import ru.auto.api.BaseSpec

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 26.07.17
  */
class VersionSpec extends BaseSpec {
  "Version" should {
    "parse from string" in {
      Version("1") shouldBe Version(1)
      Version("1.0") shouldBe Version(1, 0)
      Version("1.0.3") shouldBe Version(1, 0, 3)
      Version("2.3.99") shouldBe Version(2, 3, 99)
      Version("2.3_abba") shouldBe Version(2, 3, "abba")
      Version("5.4.3.5") shouldBe Version(5, 4, 3, "5")
      Version("4.14.0_4.14") shouldBe Version(4, 14, 0, "4.14")
      Version("8.2.0.1") shouldBe Version(8, 2, 0, "1")
      Version("4.8.0_MEIZU") shouldBe Version(4, 8, 0, "MEIZU")
    }
    "have correct toString" in {
      Version("5.3.0").toString shouldBe "5.3.0"
      Version("4.8.0_MEIZU").toString shouldBe "4.8.0_MEIZU"
      Version("4.14.0_4.14").toString shouldBe "4.14.0_4.14"
      Version("8.2.0.1").toString shouldBe "8.2.0_1"
    }
    "be comparable as api" in {
      implicit val order: Ordering[Version] = Version.ApiVersionOrdering

      Version("1.1") should be > Version("1.0")
      Version("1.1") should be > Version("1.1.3")
      Version("2") should be > Version("1.1.3")
      Version("1.1.22") should be > Version("1.1.3")
      Version("1.1.22") should be >= Version("1.1.22")
      Version("4.8.0_MEIZU") should be >= Version("4.8.0")
      Version("8.1.2") should be >= Version("8.1.2")
      Version("8.1.3") should be >= Version("8.1.2")
      Version("8.2.0") should be >= Version("8.1.2")
      Version("4.14.0_4.14") should be < Version("4.14")
    }

    "be comparable as app version" in {
      implicit val order: Ordering[Version] = Version.AppVersionOrdering

      Version("1.1") should be > Version("1.0")
      Version("1.1") should be >= Version("1.1.3")
      Version("2") should be > Version("1.1.3")
      Version("1.1.22") should be > Version("1.1.3")
      Version("1.1.22") should be >= Version("1.1.22")
      Version("4.8.0_MEIZU") should be >= Version("4.8.0")
      Version("8.1.2") should be >= Version("8.1.2")
      Version("8.1.3") should be >= Version("8.1.2")
      Version("8.2.0") should be >= Version("8.1.2")
      Version("4.14.0_4.14") should be >= Version("4.14")
      Version("9.34.0.16216") should be < Version("9.37")
      Version("10.9.0.20972") should be > Version("8.8.0")
    }

    "throw exception on invalid version string" in {
      intercept[IllegalArgumentException](Version("1..3"))
      intercept[IllegalArgumentException](Version(".1"))
      intercept[IllegalArgumentException](Version("aa"))
      intercept[IllegalArgumentException](Version("1_abba"))
    }
  }
}
