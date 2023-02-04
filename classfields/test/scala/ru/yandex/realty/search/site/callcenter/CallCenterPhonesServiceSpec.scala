package ru.yandex.realty.search.site.callcenter

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.region.Regions

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class CallCenterPhonesServiceSpec extends SpecBase with PropertyChecks with RegionGraphTestComponents {

  private val regionsTestData =
    Table(
      ("geocoderId", "regionName", "phones", "phoneRegion"),
      (Regions.KAZAN, "Казань", Seq("943"), Some(Regions.KAZAN)),
      (102028, "Советский район города Казань", Seq("943"), Some(Regions.KAZAN)),
      (11121, "Альметьевск", Seq.empty, None),
      (116705, "округ Балашиха", Seq("926"), Some(Regions.MSK_AND_MOS_OBLAST)),
      (Regions.SPB_AND_LEN_OBLAST, "Лен область", Seq("921"), Some(Regions.SPB_AND_LEN_OBLAST)),
      (Regions.SPB, "Питер", Seq("921"), Some(Regions.SPB_AND_LEN_OBLAST))
    )

  "CallCenterPhonesService " should {
    forAll(regionsTestData) { (geocoderId: Int, regionName: String, phones: Seq[String], phoneRegion: Option[Int]) =>
      "getCallCenterPhones for " + regionName in {
        val phones = callCenterPhonesService.getCallCenterPhones(geocoderId)
        phones shouldBe phones
      }

      "getCallCenterPhonesWithRegion for " + regionName in {
        val phonesWithRegion = callCenterPhonesService.getCallCenterPhonesByRegionJava(geocoderId)
        if (phones.nonEmpty) {
          val entry = phonesWithRegion.entrySet().iterator().next()
          val geoId = entry.getKey
          val resultPhones = entry.getValue.asScala
          geoId shouldBe phoneRegion.get
          resultPhones shouldBe phones
        } else {
          phonesWithRegion shouldBe empty
        }
      }
    }
  }

  "CallCenterRegionsService " should {

    forAll(regionsTestData) { (geocoderId: Int, regionName: String, phones: Seq[String], phoneRegion: Option[Int]) =>
      "getCallCenterRegionForGivenGeo for " + regionName in {
        val geoIdOpt = callCenterRegionsService.getCallCenterRegionForGivenGeo(geocoderId)
        geoIdOpt shouldBe phoneRegion
      }
    }
  }

  private val callCenterRegionsService = new DefaultCallCenterRegionsService(
    Set(Regions.MSK_AND_MOS_OBLAST, Regions.SPB_AND_LEN_OBLAST, Regions.VORONEZHSKAYA_OBLAST, Regions.KAZAN),
    Set.empty,
    regionGraphProvider
  )

  private val callCenterPhonesService = new CallCenterPhonesService(
    Map(
      Regions.MSK_AND_MOS_OBLAST -> Seq("926"),
      Regions.SPB_AND_LEN_OBLAST -> Seq("921"),
      Regions.VORONEZHSKAYA_OBLAST -> Seq("943"),
      Regions.KAZAN -> Seq("943")
    ),
    callCenterRegionsService
  )

}
