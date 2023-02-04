package ru.yandex.realty.geohub.managers

import akka.http.scaladsl.model.HttpMethods.GET
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.common.util.collections.MultiMap
import ru.yandex.realty.SpecBase
import ru.yandex.realty.citycenter.CityCenterFinder
import ru.yandex.realty.geohub.proto.api.routes.RoutesList
import ru.yandex.realty.graph.RegionGraph
import ru.yandex.realty.http.HttpClientMock
import ru.yandex.realty.model.geometry.Polygon
import ru.yandex.realty.model.location.{CityCenter, GeoPoint, HighwaySector}
import ru.yandex.realty.router.RouterClient
import ru.yandex.realty.storage.GeometryStorage
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.HighwayLocator
import yandex.maps.proto.common2.response.ResponseOuterClass.Response

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@RunWith(classOf[JUnitRunner])
class PolygonDistanceManagerSpec extends SpecBase with HttpClientMock {

  "PolygonDistanceManager " should {

    val regionGraph: RegionGraph = mock[RegionGraph]
    val geometryStorage: GeometryStorage = mock[GeometryStorage]
    val highwayLocator: HighwayLocator = new HighwayLocator(new MultiMap[Integer, HighwaySector]())
    val cityCenterFinder: CityCenterFinder = mock[CityCenterFinder]
    val routerClient: RouterClient = new RouterClient(httpService)

    val polygonDistanceManager: PolygonDistanceManager = new PolygonDistanceManager(
      () => regionGraph,
      () => geometryStorage,
      () => highwayLocator,
      cityCenterFinder,
      routerClient
    )

    "return empty RoutesList in case no cityCenter was found" in {
      (cityCenterFinder.findCentersInRegion(_: GeoPoint)).expects(*).returning(Option.empty).once()

      val geoPoint: GeoPoint = new GeoPoint(55f, 36f)
      val routesListF: Future[RoutesList] = polygonDistanceManager.buildRoutes(
        geoPoint,
        "MKAD"
      )(Traced.empty)

      val routes: RoutesList = Await.result(routesListF, 1.seconds)
      routes.getRoutesCount should be(0)
    }

    "check Router client is called in success case" in {
      val geometry = new Polygon(Array(1f, 2f, 3f), Array(1f, 2f, 3f))
      val centerPoint = new GeoPoint(56f, 35f)
      val center = new CityCenter(1, geometry, centerPoint)
      (cityCenterFinder.findCentersInRegion(_: GeoPoint)).expects(*).returning(Option(center)).once()

      val point: GeoPoint = new GeoPoint(55f, 36f)

      httpClient expect (
        GET,
        s"/v2/route" +
          s"?rll=${point.getLongitude},${point.getLatitude}~${centerPoint.getLongitude},${centerPoint.getLatitude}" +
          s"&origin=realty-unification" +
          s"&mode=approx" +
          s"&results=10"
      )
      httpClient.respondWith(200, Response.getDefaultInstance)

      polygonDistanceManager.buildRoutes(point, "MKAD")(Traced.empty)
    }

  }

}
