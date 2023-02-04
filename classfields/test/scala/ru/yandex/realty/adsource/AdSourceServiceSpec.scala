package ru.yandex.realty.adsource

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.util.Partners

@RunWith(classOf[JUnitRunner])
class AdSourceServiceSpec extends SpecBase {

  val features = new SimpleFeatures
  features.AddPaidOnlyIfadSource.setNewState(true)
  val adSourceService = new AdSourceService(features)
  val yesterdayTimestamp: Long = DateTime.now.minusDays(1).getMillis
  val enabledAdSource = AdSource(AdSourceType.RTB_HOUSE, yesterdayTimestamp)

  "adSourceService " should {
    "should return paidOnly if adSource is enabled and partner is empty " in {
      val partnerId = None
      val paidOnly = adSourceService.getPaidOnlyParamOptExceptSpecialPartner(Some(enabledAdSource), partnerId)
      paidOnly shouldBe Some(AdSourceService.paidOnlyString)
    }
    "should return paidOnly if adSource is enabled and partner is non special " in {
      val partnerId = Some(11100101110011L)
      val paidOnly = adSourceService.getPaidOnlyParamOptExceptSpecialPartner(Some(enabledAdSource), partnerId)
      paidOnly shouldBe Some(AdSourceService.paidOnlyString)
    }

    "should return None if adSource is enabled but have pik partner id " in {
      val partnerId = Some(Partners.PikPartnerId)
      val paidOnly = adSourceService.getPaidOnlyParamOptExceptSpecialPartner(Some(enabledAdSource), partnerId)
      paidOnly shouldBe None
    }

    "should return None if adSource is enabled but have samolet partner id " in {
      val partnerId = Some(Partners.SamoletPartnerId)
      val paidOnly = adSourceService.getPaidOnlyParamOptExceptSpecialPartner(Some(enabledAdSource), partnerId)
      paidOnly shouldBe None
    }
  }
}
