package ru.yandex.realty.unification.unifier.processor.enrichers

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.microdistricts.MicroDistrictsTestComponents
import ru.yandex.realty.microdistricts.MicroDistrictsTestComponents.{elamash, gbi, pionersky, ugoZapadniy, vtuzgorodok}
import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.model.offer.Offer

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ToponymEnricherSpec extends AsyncSpecBase with RegionGraphTestComponents with MicroDistrictsTestComponents {

  implicit val trace: Traced = Traced.empty

  private val enricher = new ToponymsEnricher(
    regionGraphProvider,
    microDistrictsProvider
  )
  private val gbiOffer =
    buildOffer(id = 1, rgid = 559132, point = GeoPoint.getPoint(56.834344727992594f, 60.68881559900001f))
  private val rostovNaDonuOffer = buildOffer(id = 2, rgid = 214386, point = GeoPoint.getPoint(47.22208f, 39.720356f))

  "ToponymEnricher" should {

    "correctly enrich location with micro districts" in {
      enricher.enrich(gbiOffer).futureValue
      gbiOffer.getLocation.getToponymIds.asScala.toSet shouldBe Set(gbi.id)
    }

    "don't enrich location with micro districts if there are no micro districts in the geo" in {
      enricher.enrich(rostovNaDonuOffer).futureValue
      rostovNaDonuOffer.getLocation.getToponymIds.size() shouldBe 0
    }
  }

  private def buildOffer(id: Long, rgid: Long, point: GeoPoint): Offer = {
    val offer = new Offer()
    offer.setId(id)
    val location = new Location()
    location.setRegionGraphId(rgid)
    location.setManualPoint(point)
    offer.setLocation(location)
    offer
  }

}
