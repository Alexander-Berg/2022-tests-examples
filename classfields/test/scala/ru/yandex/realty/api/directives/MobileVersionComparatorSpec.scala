package ru.yandex.realty.api.directives

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.api.directives.MobileVersionComparator.compareVersions
import ru.yandex.realty.pushnoy.model.ClientOS

@RunWith(classOf[JUnitRunner])
class MobileVersionComparatorSpec extends SpecBase {
  "Versions comparison" should {
    val v1 = "12.8.4"

    "detect same versions" in {
      assert(compareVersions(v1, "12.8.4") == 0)
    }

    "detect greater versions" in {
      assert(compareVersions(v1, "13.8.4") < 0)
      assert(compareVersions(v1, "12.9.4") < 0)
      assert(compareVersions(v1, "12.8.5") < 0)
    }

    "detect lesser versions" in {
      assert(compareVersions(v1, "11.8.4") > 0)
      assert(compareVersions(v1, "12.7.4") > 0)
      assert(compareVersions(v1, "12.8.3") > 0)
    }
  }

  "Map-based version comparison" should {
    "check platform present in both actual and expected maps" in {
      assert(MobileVersionComparator.isVersionGreater(Map(ClientOS.IOS -> "1.2.3"), Map(ClientOS.IOS -> "1.2.3")))

      assert(MobileVersionComparator.isVersionGreater(Map(ClientOS.IOS -> "1.2.4"), Map(ClientOS.IOS -> "1.2.3")))
      assert(MobileVersionComparator.isVersionGreater(Map(ClientOS.IOS -> "1.3.0"), Map(ClientOS.IOS -> "1.2.3")))
      assert(MobileVersionComparator.isVersionGreater(Map(ClientOS.IOS -> "2.0.0"), Map(ClientOS.IOS -> "1.2.3")))

      assert(!MobileVersionComparator.isVersionGreater(Map(ClientOS.IOS -> "1.2.2"), Map(ClientOS.IOS -> "1.2.3")))
      assert(!MobileVersionComparator.isVersionGreater(Map(ClientOS.IOS -> "1.1.0"), Map(ClientOS.IOS -> "1.2.3")))
      assert(!MobileVersionComparator.isVersionGreater(Map(ClientOS.IOS -> "0.1.0"), Map(ClientOS.IOS -> "1.2.3")))
    }

    "disregard constrained platform that is not present in actual map" in {
      assert(
        MobileVersionComparator.isVersionGreater(
          Map(ClientOS.IOS -> "1.2.3"),
          Map(ClientOS.IOS -> "1.2.3", ClientOS.ANDROID -> "2.0.0")
        )
      )
    }

    "accept actual platform that is not not constrained" in {
      assert(
        MobileVersionComparator.isVersionGreater(
          Map(ClientOS.IOS -> "1.2.3"),
          Map(ClientOS.ANDROID -> "2.0.0")
        )
      )
    }
  }

}
