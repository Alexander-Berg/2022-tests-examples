package ru.yandex.realty.searcher.controllers.phone.redirect.offer

import org.joda.time.{DateTime, DateTimeUtils}
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.features.FeaturesStubComponent
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.offer.OfferType
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.util.time.TimezoneUtils

@RunWith(classOf[JUnitRunner])
class DefaultSecondaryOfferPersonalRedirectAvailabilityServiceSpec
  extends SpecBase
  with RegionGraphTestComponents
  with FeaturesStubComponent
  with PropertyChecks
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    DateTimeUtils.setCurrentMillisSystem()
  }

  private val service = new DefaultSecondaryOfferPersonalRedirectAvailabilityService(
    regionGraphProvider,
    features
  )

  private val offerTestData =
    Table(
      ("geocoderId", "featureEnabled", "offerType", "expected"),
      (Regions.MOSCOW, false, OfferType.SELL, false),
      (Regions.REUTOV, false, OfferType.SELL, false),
      (Regions.MSK_AND_MOS_OBLAST, true, OfferType.RENT, true),
      (Regions.KRASNODARSKYJ_KRAI, false, OfferType.SELL, false),
      (Regions.MOSCOW, true, OfferType.SELL, true),
      (Regions.REUTOV, true, OfferType.SELL, true),
      (Regions.MSK_AND_MOS_OBLAST, true, OfferType.SELL, true),
      (Regions.KRASNODARSKYJ_KRAI, true, OfferType.RENT, false)
    )

  "DefaultSecondaryOfferPersonalRedirectAvailabilityService" should {
    forAll(offerTestData) { (geocoderId: Int, featureEnabled: Boolean, offerType: OfferType, expected: Boolean) =>
      "check if offer isApplicableForPersonalRedirect for geo: " + geocoderId + ", feature: " + featureEnabled + ", offerType: " + offerType in {
        features.SecondaryOfferPersonalPhoneRedirectsEnabled.setNewState(featureEnabled)
        DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-04-15T14:00:01").getMillis)
        service.isApplicableForPersonalRedirect(geocoderId, Some(Regions.MSK_AND_MOS_OBLAST), offerType) shouldBe expected
      }
    }
    "check that personal redirects are not available at night for SELL" in {
      features.SecondaryOfferPersonalPhoneRedirectsEnabled.setNewState(true)
      DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-04-15T22:00:01").getMillis)
      service.isApplicableForPersonalRedirect(
        Regions.MSK_AND_MOS_OBLAST,
        Some(Regions.MSK_AND_MOS_OBLAST),
        OfferType.SELL
      ) shouldBe false
    }

    "check that personal redirects are available at night for RENT" in {
      features.SecondaryOfferPersonalPhoneRedirectsEnabled.setNewState(true)
      DateTimeUtils.setCurrentMillisFixed(DateTime.parse("2022-04-15T22:00:01").getMillis)
      service.isApplicableForPersonalRedirect(
        Regions.MSK_AND_MOS_OBLAST,
        Some(Regions.MSK_AND_MOS_OBLAST),
        OfferType.RENT
      ) shouldBe true
    }

    "check all regions have corresponding timezone" in {
      DefaultSecondaryOfferPersonalRedirectAvailabilityService.PersonalRedirectRegions
        .exists(geoId => TimezoneUtils.findBySubjectFederationId(geoId).isEmpty) shouldBe false
    }
  }
}
