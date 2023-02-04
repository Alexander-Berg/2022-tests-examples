package ru.yandex.realty2.extdataloader.loaders.graph

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.geosrc.GeoSrcTestComponents
import ru.yandex.realty.geosrc.GeoSrcTestComponents.SokolnikiMetro
import ru.yandex.realty.graph.RegionTreeTestComponents.{MskAndMo, SpbAndLo}
import ru.yandex.realty.graph.core.NodeMetro
import ru.yandex.realty.graph.{RegionGraphBuilder, RegionTreeTestComponents}
import ru.yandex.realty.graph.dao.RegionGraphDao
import ru.yandex.realty.metro.MetroRawCitiesTestComponents
import ru.yandex.realty.metro.MetroRawCitiesTestComponents.{komsomolskaya, parkKulturi}
import ru.yandex.realty.model.geometry.Point
import ru.yandex.realty.model.location.{CityCenter, GeoPoint}
import ru.yandex.realty.model.region.{NodeRgid, Regions}
import ru.yandex.realty.storage.CityCenterStorage
import ru.yandex.realty.storage.verba.{VerbaDictionary, VerbaStorage}
import ru.yandex.realty.util.tracing.NoopTracingProvider
import ru.yandex.realty2.extdataloader.loaders.graph.RegionGraphDaoTest.deletedIds
import ru.yandex.verba2.model.Dictionary

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RegionGraphBuilderSpec
  extends AsyncSpecBase
  with NoopTracingProvider
  with MetroRawCitiesTestComponents
  with GeoSrcTestComponents
  with RegionTreeTestComponents {

  private val verbaProvider: Provider[VerbaStorage] = () =>
    new VerbaStorage(
      Seq(
        new Dictionary(
          1L,
          1L,
          VerbaDictionary.SHORTNAMES.getCode,
          VerbaDictionary.SHORTNAMES.getCode,
          "",
          Seq.empty.asJava
        )
      ).asJava
    )
  private val regionGraphDao: RegionGraphDao = new RegionGraphDaoTest()
  private val centerPoint = new GeoPoint(55.439f, 37.175f)
  private val cityCenterStorage = CityCenterStorage(
    Map(Regions.MSK_AND_MOS_OBLAST -> new CityCenter(Regions.MSK_AND_MOS_OBLAST, new Point(centerPoint), centerPoint))
  )
  private val builder = new RegionGraphBuilder(
    geoSrcProvider,
    regionTreeProvider,
    metroRawCitiesProvider,
    verbaProvider,
    regionGraphDao,
    tracingSupport,
    () => cityCenterStorage
  )

  "NewRegionGraphBuilder" should {
    "build region graph" in {
      val regionGraph = builder.buildRegionGraph().futureValue
      val expected = Set(0, 225, 1, 213, 98580, 116995, 20402, 20358, 10174, 2)
      regionGraph.getNodes.asScala.map(_.getGeoId.toInt).toSet shouldBe expected
    }

    "do not add federal districts" in {
      val regionGraph = builder.buildRegionGraph().futureValue
      val federalDistricts = Set(3, 17)
      regionGraph.getNodes.asScala.map(_.getGeoId.toInt).toSet.intersect(federalDistricts) shouldBe Set.empty
    }

    "add special regions" in {
      val regionGraph = builder.buildRegionGraph().futureValue
      regionGraph.getNodeById(NodeRgid.NEW_MOSCOW) != null shouldBe true
      regionGraph.getNodeById(NodeRgid.MOSCOW_WITHOUT_NEW_MOSCOW) != null shouldBe true
      regionGraph.getNodeById(NodeRgid.LEN_OBLAST) != null shouldBe true
      regionGraph.getNodeById(NodeRgid.MOS_OBLAST) != null shouldBe true
    }

    "add metro" in {
      val regionGraph = builder.buildRegionGraph().futureValue
      val sokolniki = regionGraph.getNodeByGeoId(SokolnikiMetro.geoId.toInt).asInstanceOf[NodeMetro]
      sokolniki.getArrivalPoints.asScala.size shouldBe 2
      sokolniki.getRgbColor.contains("#") shouldBe false
      regionGraph.getNodeByGeoId(komsomolskaya.geoId) == null shouldBe true
      regionGraph.getNodeByGeoId(parkKulturi.geoId) == null shouldBe true
    }

    "add deleted node ids" in {
      val regionGraph = builder.buildRegionGraph().futureValue
      regionGraph.getDeletedIds.asScala.toSet shouldBe deletedIds.toSet
      regionGraph.getNodeById(199441) shouldBe regionGraph.getNodeByGeoId(1)
    }

    "add bounding box to nodes without toponym" in {
      val regionGraph = builder.buildRegionGraph().futureValue
      val spbAndLo = regionGraph.getNodeByGeoId(SpbAndLo.getId)
      spbAndLo.getLt != null shouldBe true
      spbAndLo.getRb != null shouldBe true
      spbAndLo.getPoint != null shouldBe true
    }

    "replace region center for msk and mos obl" in {
      val regionGraph = builder.buildRegionGraph().futureValue
      val node = regionGraph.getNodeByGeoId(MskAndMo.getId)
      node should not be null
      node.getPoint shouldBe centerPoint
    }

  }
}
