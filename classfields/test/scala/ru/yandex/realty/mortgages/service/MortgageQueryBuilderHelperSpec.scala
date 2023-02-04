package ru.yandex.realty.mortgages.service

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.main.ComponentsTest

@RunWith(classOf[JUnitRunner])
class MortgageQueryBuilderHelperSpec extends SpecBase with ComponentsTest {
  "MortgageQueryBuilderHelper " should {
    "take subject federation geoid from by region rgid" in {
      val subjectFederationGeoId =
        MortgageQueryBuilderHelper.takeSubjectFederationGeoIdFromRgid(
          RegionGraphTestComponents.KazanCityNode.getId,
          regionGraphProvider.get()
        )
      subjectFederationGeoId shouldBe RegionGraphTestComponents.TatarstanSubjectFederationNode.getGeoId
    }

    "throw IllegalArgumentException if cant take subject_federation from geoId" in {
      assertThrows[IllegalArgumentException] {
        MortgageQueryBuilderHelper.takeSubjectFederationGeoIdFromRgid(2L, regionGraphProvider.get())
      }
    }
  }

}
