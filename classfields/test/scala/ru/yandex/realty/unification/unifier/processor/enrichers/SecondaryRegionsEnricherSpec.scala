package ru.yandex.realty.unification.unifier.processor.enrichers

import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.context.CacheHolder
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.tracing.Traced

import scala.collection.JavaConverters._

class SecondaryRegionsEnricherSpec extends AsyncSpecBase with RegionGraphTestComponents {

  val enricher = new SecondaryRegionsEnricher(
    regionGraphProvider,
    mock[CacheHolder]
  )

  implicit val trace: Traced = Traced.empty

  "SecondaryRegionsEnricher" should {
    "add neighbour cities & urban districts" in {

      val location: Location = new Location()
      location.setModerationPoint(new GeoPoint(44.616593f, 37.959012f))
      location.setRegionGraphId(RegionGraphTestComponents.GelendzhikDistrictNode.getId)

      enricher.enrich(location).futureValue

      val secondaryRgids = location.getSecondaryRegions.asScala.map(_.id).toSet
      secondaryRgids.sameElements(
        Set(
          RegionGraphTestComponents.NovorossijskCityNode.getId,
          RegionGraphTestComponents.NovorossijskUrbanDistrictNode.getId
        )
      ) shouldBe true
    }

    "add nothing when large distance " in {

      val location: Location = new Location()
      location.setModerationPoint(new GeoPoint(43.712157f, 39.617416f))
      location.setRegionGraphId(RegionGraphTestComponents.SochiDistrictNode.getId)

      enricher.enrich(location).futureValue

      val secondaryRgids = location.getSecondaryRegions.asScala.map(_.id).toSet
      secondaryRgids shouldBe (Set.empty)
    }

    "add neighbour cities in moscow obl in short distance " in {
      val location: Location = new Location()

      location.setModerationPoint(new GeoPoint(55.774030f, 37.870066f))
      location.setRegionGraphId(RegionGraphTestComponents.BalashihaCityNode.getId)

      enricher.enrich(location).futureValue

      val secondaryRgids = location.getSecondaryRegions.asScala.map(_.id).toSet
      secondaryRgids.contains(RegionGraphTestComponents.ReytovCityNode.getId) shouldBe true

    }

    "don't add neighbour cities in special distance in Mos obl " in {
      val location: Location = new Location()

      location.setModerationPoint(new GeoPoint(55.835227f, 37.957064f))
      location.setRegionGraphId(RegionGraphTestComponents.BalashihaCityNode.getId)

      enricher.enrich(location).futureValue

      val secondaryRgids = location.getSecondaryRegions.asScala.map(_.id).toSet
      secondaryRgids.contains(RegionGraphTestComponents.ReytovCityNode.getId) shouldBe false
    }
  }
}
