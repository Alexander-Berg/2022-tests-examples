package ru.yandex.realty.unification.unifier.processor.enrichers

import com.google.protobuf.Int64Value
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.geohub.proto.api.routes.RoutesList
import ru.yandex.realty.model.geometry.{HolePolygon, Polygon}
import ru.yandex.realty.model.location.{GeoPoint, Location}
import ru.yandex.realty.proto.RouteDistance
import ru.yandex.realty.storage.GeometryStorage.GeometryEntry
import ru.yandex.realty.storage.{GeometryCodes, GeometryStorage}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

class RouteDistanceEnricherSpec extends AsyncSpecBase with RegionGraphTestComponents {

  implicit val trace: Traced = Traced.empty
  "RouteDistanceEnricherSpec" should {
    "enrich location routes and highway with shortest path" in {

      val geometryStorage: GeometryStorage = mock[GeometryStorage]
      val geohubClient: GeohubClient = mock[GeohubClient]

      val routeDistanceEnricher = new RouteDistanceEnricher(
        regionGraphProvider,
        () => geometryStorage,
        geohubClient
      )

      val location: Location = new Location()
      location.setModerationPoint(new GeoPoint(55.52612f, 37.941162f))
      location.setRegionGraphId(741964L)

      val latitudes: Array[Float] = Array(20f, 20f, 30f, 30f)
      val longitudes: Array[Float] = Array(20f, 30f, 20f, 30f)
      val polygon = new Polygon(latitudes, longitudes)
      (geometryStorage
        .getGeometry(_: String))
        .expects(GeometryCodes.MKAD)
        .returns(Some(GeometryEntry("MKAD", "MKAD", new HolePolygon(polygon))))

      val routeDistance0: RouteDistance = RouteDistance
        .newBuilder()
        .setDistance(10)
        .setPoint(ru.yandex.realty.proto.GeoPoint.newBuilder().setLatitude(10f).setLongitude(10f))
        .build()
      val routeDistance1: RouteDistance = RouteDistance
        .newBuilder()
        .setDistance(20)
        .setHighwayId(Int64Value.newBuilder().setValue(16))
        .setPoint(ru.yandex.realty.proto.GeoPoint.newBuilder().setLatitude(20f).setLongitude(20f))
        .build()
      val response = RoutesList
        .newBuilder()
        .addRoutes(0, routeDistance0)
        .addRoutes(1, routeDistance1)
        .build()

      (geohubClient
        .polygonDistance(_: GeoPoint, _: String))
        .expects(location.getExactPoint, "MKAD")
        .returning(Future.successful(response))

      routeDistanceEnricher.enrich(location).futureValue

      location.getRouteDistances shouldBe response.getRoutesList
      location.getHighwayAndDistance.getDistance shouldBe 10
      location.getHighwayAndDistance.getId shouldBe 0
    }
  }
}
