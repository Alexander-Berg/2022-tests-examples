package ru.yandex.vertis.telepony.service.impl

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.model.{GeoFallbackRule, TypedDomains}
import ru.yandex.vertis.telepony.model.PhoneTypes._
import ru.yandex.vertis.telepony.properties.{DomainDynamicPropertiesReader, DynamicProperties}

class GeoFallbackRulesServiceImplSpec extends SpecBase with MockitoSupport {

  private def mockProperties(propertyValue: Boolean): DomainDynamicPropertiesReader = {
    val mockProperties = mock[DynamicProperties]
    when(mockProperties.getValue(TypedDomains.autoru_def, DynamicProperties.EnabledGeoFallbackRulesProperty))
      .thenReturn(propertyValue)
    new DomainDynamicPropertiesReader(TypedDomains.autoru_def, mockProperties)
  }

  "FallbackRulesService" should {

    "don't mix disabled configurable rules" in {
      val rules: Seq[GeoFallbackRule] = Seq(
        GeoFallbackRule(
          originRegion = 10176,
          originPhoneType = Mobile,
          fallbackRegion = 10842,
          fallbackPhoneType = Mobile,
          status = GeoFallbackRule.Statuses.Configurable
        )
      )

      val attributes = Seq(10176 -> Mobile)
      val geoFallbackRulesService = new GeoFallbackRulesServiceImpl(rules, mockProperties(false))
      val actualResult = geoFallbackRulesService.mixFallbackAttributes(attributes)

      actualResult should contain theSameElementsAs attributes
    }

    "mix enabled configurable rules" in {
      val rules: Seq[GeoFallbackRule] = Seq(
        GeoFallbackRule(
          originRegion = 10176,
          originPhoneType = Mobile,
          fallbackRegion = 10842,
          fallbackPhoneType = Mobile,
          status = GeoFallbackRule.Statuses.Configurable
        )
      )

      val geoFallbackRulesService = new GeoFallbackRulesServiceImpl(rules, mockProperties(true))
      val expectedResult = Seq(10176 -> Mobile, 10842 -> Mobile)
      val actualResult = geoFallbackRulesService.mixFallbackAttributes(Seq(10176 -> Mobile))

      actualResult should contain theSameElementsAs expectedResult
    }

    "mix only enabled rules" in {
      val rules: Seq[GeoFallbackRule] = Seq(
        GeoFallbackRule(
          originRegion = 10176,
          originPhoneType = Mobile,
          fallbackRegion = 10842,
          fallbackPhoneType = Mobile,
          status = GeoFallbackRule.Statuses.Enabled
        ),
        GeoFallbackRule(
          originRegion = 11004,
          originPhoneType = Mobile,
          fallbackRegion = 10995,
          fallbackPhoneType = Mobile,
          status = GeoFallbackRule.Statuses.Disabled
        )
      )

      val geoFallbackRulesService = new GeoFallbackRulesServiceImpl(rules, mockProperties(false))
      val expectedAttributes = Seq(10176 -> Mobile, 10842 -> Mobile, 11004 -> Mobile)
      val actualAttributes = geoFallbackRulesService.mixFallbackAttributes(Seq(10176 -> Mobile, 11004 -> Mobile))
      val expectedGeoCandidates = Seq(10176, 10842, 11004)
      val actualGeoCandidates = geoFallbackRulesService.mixFallbackGeoCandidates(Seq(10176, 11004), Mobile)

      actualAttributes should contain theSameElementsAs expectedAttributes
      actualGeoCandidates should contain theSameElementsAs expectedGeoCandidates
    }
  }
}
