package ru.yandex.realty.router

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.realty.model.location.GeoPoint
import yandex.maps.proto.common2.response.ResponseOuterClass.Response

import scala.util.Success

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class RouterClientSpec extends AsyncSpecBase with PropertyChecks with RequestAware with HttpClientMock {

  private val client = new RouterClient(httpService)

  "RouterClient" should {
    "successfully get route" in {

      val from = GeoPoint.getPoint(55.734071f, 37.589401f)
      val to = GeoPoint.getPoint(55.733841f, 37.592780f)

      expectGetRoute(from, to)

      httpClient.respondWith(Response.getDefaultInstance)

      client.getRoutes(Request(from, to)) shouldBe Success(Seq.empty[Route])
    }

    "process bad request" in {
      val from = GeoPoint.getPoint(55.734071f, 37.589401f)
      val to = GeoPoint.getPoint(1000f, 2000f)
      expectGetRoute(from, to)

      httpClient.respond(StatusCodes.BadRequest)

      client.getRoutes(Request(from, to)).isFailure shouldBe true
    }
  }

  private def expectGetRoute(from: GeoPoint, to: GeoPoint): Unit = {
    val rll = s"rll=${from.getLongitude},${from.getLatitude}~${to.getLongitude},${to.getLatitude}"
    httpClient.expect(GET, s"/v2/route?$rll&origin=realty-unification&mode=approx")
  }
}
