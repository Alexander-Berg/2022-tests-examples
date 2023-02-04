package ru.yandex.realty.graph.algorithm

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.region.Regions
import ru.yandex.realty.railway.RailwayStationsTestComponents
import ru.yandex.realty.railway.RailwayStationsTestComponents.spbVybDirection

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RegionGraphRailwayAlgorithmSpec
  extends SpecBase
  with RegionGraphTestComponents
  with RailwayStationsTestComponents {

  "RegionGraphRailwayAlgorithm" should {
    "getRailwayDirections correctly" in {
      val msk = regionGraphProvider.get().getNodeByGeoId(Regions.SPB)
      val result = RegionGraphRailwayAlgorithm
        .getRailwayDirections(msk, railwayStationsProvider.get(), regionGraphProvider.get())
        .asScala
      result shouldEqual Seq(spbVybDirection)
    }
  }
}
