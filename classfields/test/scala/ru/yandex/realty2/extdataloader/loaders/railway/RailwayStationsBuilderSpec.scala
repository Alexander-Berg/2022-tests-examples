package ru.yandex.realty2.extdataloader.loaders.railway

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.SpecBase
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.location.RailwayStation
import ru.yandex.realty.model.region.NodeRgid

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RailwayStationsBuilderSpec extends SpecBase with RegionGraphTestComponents {
  private val regionStationsProvider: Provider[Seq[RailwayStation]] = mock[Provider[Seq[RailwayStation]]]
  private val builder = new RailwayStationsBuilder(regionGraphProvider, regionStationsProvider)

  "RailwayStationsBuilder" should {
    "enrich railway station correctly" in {
      val railwayStation = new RailwayStation(55.778353f, 37.654377f, "Москва (Ленинградский вокзал)", 60073)
      val result = builder.enrichAndSerialize(railwayStation)
      val expected = Set(NodeRgid.RUSSIA, NodeRgid.MOSCOW, NodeRgid.MOSCOW_AND_MOS_OBLAST)
      result.getRgidsList.asScala.toSet shouldEqual expected
    }
  }
}
