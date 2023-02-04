package ru.yandex.realty.landings

import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsNumber, JsObject, Json}
import ru.yandex.common.util.IOUtils
import ru.yandex.realty.canonical.base.NeededRegionsSearcher
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.graph.core.GeoObjectType
import ru.yandex.realty.graph.serialize.RegionGraphProtoConverter
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.regions.RegionNamesTranslations
import ru.yandex.realty.urls.sitemap.RequestRegionExtractor

import scala.collection.JavaConverters._

class RequestRegionExtractorSpec extends WordSpec with Matchers {
  private val regionGraph: RegionGraph = RegionGraphProtoConverter.deserialize(
    IOUtils.gunzip(
      getClass.getClassLoader.getResourceAsStream("region_graph-8-2.data")
    )
  )

  private val populatedRgids: Set[Long] =
    Json.parse(
      scala.io.Source
        .fromResource("rgid_to_geoid.json")
        .getLines()
        .mkString("")
    ) match {
      case JsObject(underlying) if underlying.values.forall(_.isInstanceOf[JsNumber]) =>
        underlying
          .collect {
            case (rgidStr, JsNumber(geoId)) => rgidStr.toLong -> geoId.intValue()
          }
          .toMap
          .keySet
      case _ => throw new RuntimeException(s"Incorrect resource")
    }

  private val extractor = new RequestRegionExtractor(
    () => regionGraph,
    () =>
      RegionNamesTranslations(
        populatedRgids.toSeq.map(r => r.toString -> r).toMap,
        populatedRgids.map(Long.box).asJava
      ),
    Iterable.empty
  )

  "RequestRegionExtractor" should {
    "filter geo with single child" in {
      val rgid = 17383346L // красноярский округ c одним потомком

      val res = extractor.extractRgids(rgid, None, hasStreet = false, None, GeoObjectType.values().toSet)

      res should not contain rgid
    }

    "filter geo with incorrect metro" in {
      val metro = regionGraph.getNodesByName("Таганская").asScala.head

      val rgids = Seq(NodeRgid.MOSCOW_AND_MOS_OBLAST, NodeRgid.RUSSIA, NodeRgid.SPB)

      rgids.foreach { rgid =>
        extractor.extractRgids(rgid, Some(metro.getGeoId), hasStreet = false, None, GeoObjectType.values().toSet) should not contain rgid
      }

      extractor.extractRgids(
        NodeRgid.MOSCOW,
        Some(metro.getGeoId),
        hasStreet = false,
        None,
        GeoObjectType.values().toSet
      ) should contain(NodeRgid.MOSCOW)
    }

    "filter geo with incorrect street" in {

      val rgids = Seq(NodeRgid.MOSCOW_AND_MOS_OBLAST, NodeRgid.RUSSIA)

      rgids.foreach { rgid =>
        extractor.extractRgids(rgid, None, hasStreet = true, None, GeoObjectType.values().toSet) should not contain rgid
      }

      extractor.extractRgids(NodeRgid.MOSCOW, None, hasStreet = true, None, GeoObjectType.values().toSet) should contain(
        NodeRgid.MOSCOW
      )
    }

    "correctly return moscow + oblast in landings" in {
      extractor.extractRgids(
        NodeRgid.MOSCOW,
        metroGeoId = None,
        hasStreet = false,
        siteId = None,
        types = NeededRegionsSearcher.LandingsGeoTypes
      ) should contain theSameElementsAs Iterable(
        NodeRgid.MOSCOW,
        NodeRgid.MOSCOW_AND_MOS_OBLAST
      )

    }
  }

}
