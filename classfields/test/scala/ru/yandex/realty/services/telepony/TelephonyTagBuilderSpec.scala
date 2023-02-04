package ru.yandex.realty.services.telepony

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.offer.{ApartmentInfo, CategoryType, FlatType, Offer, OfferType, SalesAgentCategory}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.sites.SitesGroupingService

@RunWith(classOf[JUnitRunner])
class TelephonyTagBuilderSpec extends SpecBase with FeaturesStubComponent with PropertyChecks {

  val locationTestData =
    Table(
      ("subjectFederationId", "rgid", "sfid"),
      (Regions.MSK_AND_MOS_OBLAST, NodeRgid.MOSCOW_AND_MOS_OBLAST, Some(Regions.MSK_AND_MOS_OBLAST.toString)),
      (Regions.TATARSTAN, NodeRgid.TATARSTAN, Some(Regions.TATARSTAN.toString)),
      (Regions.KRASNODARSKYJ_KRAI, NodeRgid.KRASNODARSKYJ_KRAI, Some(Regions.KRASNODARSKYJ_KRAI.toString)),
      (Regions.SAMARSKAYA_OBLAST, NodeRgid.SAMARSKAYA_OBLAST, None),
      (Regions.YAROSLAVSKAYA_OBLAST, NodeRgid.YAROSLAVSKAYA_OBLAST, None)
    )

  trait TelephonyTagBuilderFixture {
    features.ExtendedRedirectTags.setNewState(true)

    val siteService = mock[SitesGroupingService]
    val manager = new TelephonyTagBuilder(siteService, features)

    val offer = new Offer()
    offer.setId(12321L)
    offer.setFromVos(true)
    offer.setOfferType(OfferType.SELL)
    offer.setCategoryType(CategoryType.APARTMENT)
    val apartmentInfo = new ApartmentInfo()
    apartmentInfo.setFlatType(FlatType.SECONDARY)
    offer.setApartmentInfo(apartmentInfo)
    offer.setPrimarySaleV2(false)
    val location = new Location()
    offer.setLocation(location)
    val saleAgent = offer.createAndGetSaleAgent()
    saleAgent.setCategory(SalesAgentCategory.AGENT)
  }

  "TelephonyTagBuilder " should {

    forAll(locationTestData) { (subjectFederationId: Int, rgid: Long, sfid: Option[String]) =>
      "build tag for subjFedId " + subjectFederationId + ", rgid " + rgid in new TelephonyTagBuilderFixture {
        location.setSubjectFederation(subjectFederationId, rgid)
        val tag: TelephonyTag = manager.build(offer)
        tag.sfid shouldBe sfid
      }
    }
  }

}
