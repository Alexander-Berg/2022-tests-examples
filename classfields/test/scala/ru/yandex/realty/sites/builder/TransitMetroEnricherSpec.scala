package ru.yandex.realty.sites.builder

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.context.CacheHolder
import ru.yandex.realty.graph.core.{GeoObjectType, Node}
import ru.yandex.realty.graph.{MutableRegionGraph, RegionGraph}
import ru.yandex.realty.http.RequestTimeout
import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.model.message.RealtySchema
import ru.yandex.realty.proto.unified.offer.address.Metro
import ru.yandex.realty.proto.unified.offer.address.TransportDistance.TransportType.{ON_FOOT, PUBLIC_TRANSPORT}
import ru.yandex.realty.tracing.Traced

import java.net.SocketTimeoutException
import java.util
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class TransitMetroEnricherSpec extends AsyncSpecBase {

  implicit val trace: Traced = Traced.empty

  "TransitMetroEnricher" should {

    "set empty metro list if no geoPoint given" in {
      val location = new Location()
      val metroEnricher = new TransitMetroEnricher(
        mock[Provider[RegionGraph]],
        mock[CacheHolder],
        mock[GeohubClient]
      )

      metroEnricher.enrich(location).futureValue

      location.getMetro shouldBe empty
    }

    "set empty metro list for undefined geoPoint" in {
      val location = new Location()
      location.setModerationPoint(GeoPoint.UNKNOWN)
      val metroEnricher = new TransitMetroEnricher(
        mock[Provider[RegionGraph]],
        mock[CacheHolder],
        mock[GeohubClient]
      )

      metroEnricher.enrich(location).futureValue

      location.getMetro shouldBe empty
    }

    "set metro list from geohubClient" in {
      val location = new Location()
      val geoPoint = new GeoPoint(1.0f, 2.0f)
      location.setModerationPoint(geoPoint)

      val response: RealtySchema.MetroWithDistanceResponse = initMetroWithDistanceResponse

      val geohubClient = stub[GeohubClient]
      (geohubClient
        .getNearestMetroSync(_: GeoPoint)(_: Traced))
        .when(geoPoint, *)
        .returns(response)

      val metroEnricher = new TransitMetroEnricher(
        mock[Provider[RegionGraph]],
        mock[CacheHolder],
        geohubClient
      )

      metroEnricher.enrich(location).futureValue

      val metros = location.getMetro
      checkMetrosForLocation(metros, response)
    }

    "set metro list if geohubClient has thrown exception" in {
      val location = new Location()
      val geoPoint = new GeoPoint(1.0f, 2.0f)
      location.setModerationPoint(geoPoint)

      val geohubClient = stub[GeohubClient]
      (geohubClient
        .getNearestMetroSync(_: GeoPoint)(_: Traced))
        .when(geoPoint, *)
        .throws(RequestTimeout("service", "httpEndpoint", new SocketTimeoutException()))

      val regionGraph = new MutableRegionGraph()
      val metroNode = Node.createNodeForGeoObjectType(GeoObjectType.METRO_STATION)
      metroNode.setGeoId(8)
      metroNode.setId(10)
      metroNode.setPoint(new GeoPoint(1.0001f, 1.99999f))
      regionGraph.setRoot(metroNode)
      val metroEnricher = new TransitMetroEnricher(
        () => regionGraph,
        mock[CacheHolder],
        geohubClient
      )

      metroEnricher.enrich(location).futureValue

      val metros = location.getMetro
      metros should have size 1
      metros.get(0).getGeoId should be(8)
    }
  }

  private def initMetroWithDistanceResponse = {
    val response = RealtySchema.MetroWithDistanceResponse
      .newBuilder()
      .addMetrosWithDistance(
        RealtySchema.MetroWithDistanceMessage
          .newBuilder()
          .setId(1)
          .setGeoId(2)
          .setTimeOnFoot(10)
          .setTimeOnTransport(2)
      )
      .addMetrosWithDistance(
        RealtySchema.MetroWithDistanceMessage
          .newBuilder()
          .setId(3)
          .setGeoId(4)
          .setTimeOnFoot(45)
          .setTimeOnTransport(15)
      )
      .build()
    response
  }

  private def checkMetrosForLocation(metros: util.List[Metro], response: RealtySchema.MetroWithDistanceResponse) = {
    metros should have size response.getMetrosWithDistanceCount

    val geoIdToMetros = metros.asScala.groupBy(_.getGeoId)
    val geoIdToMessages = response.getMetrosWithDistanceList.asScala.groupBy(_.getGeoId)

    geoIdToMetros.map(entry => {
      val messages = geoIdToMessages.getOrElse(entry._1, fail("no message for geoId"))
      messages should have size 1
      entry._2 should have size 1

      val typeToDistances = entry._2.head.getDistancesList.asScala.groupBy(_.getTransportType.name())
      typeToDistances
        .getOrElse(ON_FOOT.name, fail("no time for travel on foot"))
        .head
        .getTime
        .getSeconds should be(messages.head.getTimeOnFoot * 60)
      typeToDistances
        .getOrElse(PUBLIC_TRANSPORT.name, fail("no time for travel on transport"))
        .head
        .getTime
        .getSeconds should be(messages.head.getTimeOnTransport * 60)
    })
  }
}
