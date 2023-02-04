package ru.yandex.realty.misc.managers.region

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.api.ProtoResponse.RegionAutodetectResponse
import ru.yandex.realty.clients.laas.{LaasClient, LaasResponse}
import ru.yandex.realty.graph.core.GeoObjectType.{CITY, COUNTRY, SUBJECT_FEDERATION}
import ru.yandex.realty.graph.core.{GeoObjectType, Name, Node}
import ru.yandex.realty.graph.{MutableRegionGraph, RegionGraph}
import ru.yandex.realty.misc.UserInputDictionary
import ru.yandex.realty.model.geometry.Polygon
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.model.region.{GeoIntent, NodeRgid, RegionAutodetectQuery}
import ru.yandex.realty.proto.RegionType
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.geo.GeoHelper
import ru.yandex.realty.{proto, AsyncSpecBase}

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class RegionAutodetectManagerSpec extends AsyncSpecBase {

  private class Fixture {
    val geoHelperProvider: Provider[GeoHelper] = mock[Provider[GeoHelper]]
    val laasClient: LaasClient = mock[LaasClient]
    val regionAutodetectManager = new RegionAutodetectManager(geoHelperProvider, laasClient)

    val OffersCount = 1000
    val Ip = "12.34.56.78"

    def query(
      point: Option[GeoPoint] = None,
      ip: Option[String] = None,
      polygon: Option[Polygon] = None,
      geoIntent: Option[GeoIntent] = None
    ): RegionAutodetectQuery =
      RegionAutodetectQuery(point, ip, polygon, geoIntent)

    def geoPoint(latitude: Float, longitude: Float): GeoPoint =
      new GeoPoint(latitude, longitude)

    private def node(
      rgid: Long,
      geoId: Int,
      name: String,
      `type`: GeoObjectType,
      point: GeoPoint,
      lt: GeoPoint,
      rb: GeoPoint
    ) = {
      val node = Node.createNodeForGeoObjectType(`type`)
      val nodeName = new Name
      nodeName.setDisplay(name)

      node.setId(rgid)
      node.setGeoId(geoId)
      node.setName(nodeName)
      node.setPoint(point)
      node.setLt(lt)
      node.setRb(rb)

      node
    }

    val CountryNode: Node = node(
      NodeRgid.RUSSIA,
      225,
      "Россия",
      COUNTRY,
      geoPoint(61.698654f, 99.50541f),
      geoPoint(81.886116f, 19.484768f),
      geoPoint(41.185997f, -168.872f)
    )

    val SubjectNode: Node = node(
      426660,
      11119,
      "Республика Татарстан",
      SUBJECT_FEDERATION,
      geoPoint(55.35034f, 50.91102f),
      geoPoint(56.67493f, 47.259155f),
      geoPoint(53.974216f, 54.266438f)
    )

    val CityNode: Node = node(
      582357,
      43,
      "Казань",
      CITY,
      geoPoint(55.798553f, 49.106327f),
      geoPoint(55.936447f, 48.823513f),
      geoPoint(55.603363f, 49.381927f)
    )

    val DefaultNode: Node = node(
      NodeRgid.MOSCOW_AND_MOS_OBLAST,
      123,
      "Москва и МО",
      SUBJECT_FEDERATION,
      geoPoint(55.75322f, 37.62251f),
      geoPoint(56.961323f, 35.14411f),
      geoPoint(54.255665f, 40.20486f)
    )

    val RegionGraph: RegionGraph = {
      CountryNode.addChildrenId(SubjectNode.getId)
      CountryNode.addChildrenId(DefaultNode.getId)
      DefaultNode.addParentId(CountryNode.getId)
      SubjectNode.addParentId(CountryNode.getId)
      SubjectNode.addChildrenId(CityNode.getId)
      CityNode.addParentId(SubjectNode.getId)

      val graph = new MutableRegionGraph
      List(CountryNode, DefaultNode, SubjectNode, CityNode).foreach(graph.addNode)
      graph
    }

    def mockGeoHelper() {
      val geoHelper = mock[GeoHelper]
      (geoHelper.getRegionGraph: () => RegionGraph)
        .expects()
        .anyNumberOfTimes()
        .returns(RegionGraph)
      (geoHelper
        .getOffersCount(_: Node))
        .expects(*)
        .anyNumberOfTimes()
        .returns(OffersCount)
      (geoHelper
        .getName(_: Node))
        .expects(*)
        .anyNumberOfTimes
        .onCall { node: Node =>
          node.getName.getDisplay
        }

      (geoHelperProvider.get: () => GeoHelper)
        .expects()
        .anyNumberOfTimes()
        .returns(geoHelper)
    }

    def mockLaasClient(geoId: Int) {
      val laasResponse = new LaasResponse(0, 0, geoId, 0, 0, false, false)
      (laasClient
        .getLocation(_: String)(_: Traced))
        .expects(*, *)
        .anyNumberOfTimes()
        .returns(Future.successful(laasResponse))
    }

    def assertGeoPointDefined(geoPoint: proto.GeoPoint) {
      geoPoint shouldNot be(null)
      geoPoint.getDefined should be(true)
    }

    def assertSubjectFound(response: RegionAutodetectResponse, rgid: Long, name: String) {
      val region = response.getResponse.getRegion
      region.getRgid shouldEqual rgid.toInt
      region.getName shouldEqual name
      assertGeoPointDefined(region.getPoint)
      assertGeoPointDefined(region.getLt)
      assertGeoPointDefined(region.getRb)
      region.getTotalOffers shouldEqual OffersCount
      region.getType shouldEqual RegionType.SUBJECT_FEDERATION
      region.getSearchParamsCount shouldEqual 1

      region.containsSearchParams(UserInputDictionary.Rgid) shouldEqual true
      val rgidListValue = region.getSearchParamsOrThrow(UserInputDictionary.Rgid)
      rgidListValue.getValuesCount shouldEqual 1
      rgidListValue.getValues(0).getStringValue shouldEqual rgid.toString
    }

    def assertSubjectFound(response: RegionAutodetectResponse): Unit =
      assertSubjectFound(response, SubjectNode.getId, SubjectNode.getName.getDisplay)

    def assertDefaultSubjectFound(response: RegionAutodetectResponse): Unit =
      assertSubjectFound(response, DefaultNode.getId, DefaultNode.getName.getDisplay)

    mockGeoHelper()
  }

  "RegionAutodetectManager" should {

    implicit val traced: Traced = Traced.empty

    "find region by geo intent with city rgid" in new Fixture {
      assertSubjectFound {
        val geoIntent = GeoIntent(Some(CityNode.getId), None)
        regionAutodetectManager.regionAutodetect(query(geoIntent = Some(geoIntent))).futureValue
      }
    }

    "find region by geo intent with subject federation rgid" in new Fixture {
      assertSubjectFound {
        val geoIntent = GeoIntent(Some(SubjectNode.getId), None)
        regionAutodetectManager.regionAutodetect(query(geoIntent = Some(geoIntent))).futureValue
      }
    }

    "find region by point inside city" in new Fixture {
      assertSubjectFound {
        val point = geoPoint(55.758553f, 49.006327f)
        regionAutodetectManager.regionAutodetect(query(point = Some(point))).futureValue
      }
    }

    "find region by point inside subject federation" in new Fixture {
      assertSubjectFound {
        val point = geoPoint(55.05034f, 50.01102f)
        regionAutodetectManager.regionAutodetect(query(point = Some(point))).futureValue
      }
    }

    "find region by polygon" in new Fixture {
      assertSubjectFound {
        val polygon = new Polygon(Array(55.698553f, 55.898553f, 55.898553f), Array(49.106327f, 49.006327f, 49.206327f))
        regionAutodetectManager.regionAutodetect(query(polygon = Some(polygon))).futureValue
      }
    }

    "find region by ip of city" in new Fixture {
      mockLaasClient(CityNode.getGeoId)
      assertSubjectFound {
        regionAutodetectManager.regionAutodetect(query(ip = Some(Ip))).futureValue
      }
    }

    "find region by ip of subject federation" in new Fixture {
      mockLaasClient(SubjectNode.getGeoId)
      assertSubjectFound {
        regionAutodetectManager.regionAutodetect(query(ip = Some(Ip))).futureValue
      }
    }

    "choose default region when no query parameters provided" in new Fixture {
      assertDefaultSubjectFound {
        regionAutodetectManager.regionAutodetect(query()).futureValue
      }
    }
  }
}
