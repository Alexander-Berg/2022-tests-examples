package ru.yandex.realty.unification.unifier.processor.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.gen.location.GeoObjectGenerator
import ru.yandex.realty.model.gen.{OfferModelGenerators, SiteGenerator}
import ru.yandex.realty.model.location.LocationAccuracy
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.unifier.processor.unifiers.SitesResolver
import ru.yandex.realty.util.location.LocationUtils

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class SiteLocationUnifierHelperSpec extends SpecBase with SiteGenerator {
  implicit private val traced = Traced.empty

  "SiteLocationUnifierHelper " should {
    val sitesResolver = mock[SitesResolver]
    val siteId = 123L
    val site = generateSite(siteIdOpt = Some(siteId), richLocation = true)
    val rawOffer = new RawOfferImpl()

    "set street location, if offer location doesn't have street " in {
      val offer = OfferModelGenerators.offerGen(siteId = Some(siteId)).next
      offer.getLocation.setComponents(GeoObjectGenerator.inexactComponentsGen().next.asJava)
      offer.getLocation.setAccuracy(LocationAccuracy.EXACT)

      val location =
        SiteLocationUnifierHelper.getHouseLocation(sitesResolver, rawOffer, offer, site, offer.getLocation, traced)
      location.getStreet shouldBe site.getHouseMatchFallbackLocation.getStreet
      location.getFullStreetAddress shouldBe LocationUtils.getFullStreetAddress(
        site.getHouseMatchFallbackLocation
      )
      location.getStructuredAddress shouldBe site.getHouseMatchFallbackLocation.getStructuredAddress
    }
    "doesn't change address field if offer have street " in {
      val offer = OfferModelGenerators.offerGen(siteId = Some(siteId)).next
      val exactLocation = richLocationGen().next
      offer.setLocation(exactLocation)

      val location =
        SiteLocationUnifierHelper.getHouseLocation(sitesResolver, rawOffer, offer, site, offer.getLocation, traced)
      location.getStreet shouldBe exactLocation.getStreet
      location.getFullStreetAddress shouldBe exactLocation.getFullStreetAddress
      location.getStructuredAddress shouldBe exactLocation.getStructuredAddress
    }

  }
}
