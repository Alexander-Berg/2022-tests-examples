package ru.yandex.realty.clients.suggest

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.Json
import ru.yandex.realty.{AsyncSpecBase, SpecBase}
import ru.yandex.realty.http.{HttpClientMock, RequestAware}
import ru.yandex.vertis.generators.ProducerProvider
import yandex.maps.proto.suggest.Suggest.Response
import ru.yandex.realty.clients.suggest.SuggestGeoResponse._

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class GeoSuggestClientSpec
  extends AsyncSpecBase
  with PropertyChecks
  with RequestAware
  with HttpClientMock
  with ProducerProvider {

  private val client = new GeoSuggestClientImpl(httpService)

  "GeoSuggestClient" should {
    "successfully get suggest-geo" in {
      val resp = SuggestGeoResponse(
        Seq(
          GeoSuggestItem(
            `type` = "toponym",
            title = GeoSuggestTitle("Ленинский проспект"),
            subtitle = GeoSuggestTitle("Россия, Санкт-Петербург"),
            text = "Россия, Санкт-Петербург, Ленинский проспект ",
            geoId = 10174,
            tags = Seq("street"),
            action = Some("search"),
            uri = Some("ymapsbm1:\\/\\/geo?ll=30.238497%2C59.852081&spn=0.168634%2C0.015728&text=..."),
            distance = None,
            rectLower = None,
            rectUpper = None
          )
        )
      )
      expectSuggestGeo
      println(Json.stringify(Json.toJson(resp)))
      httpClient.respondWithJson(Json.stringify(Json.toJson(resp)))
      client.suggestGeo("ленин", Seq.empty[Int], None).futureValue shouldBe resp
    }

    "handle 400 in suggest-geo" in {
      expectSuggestGeo
      httpClient.respond(StatusCodes.BadRequest)
      interceptCause[IllegalArgumentException] {
        client.suggestGeo("ленин", Seq.empty[Int], None).futureValue
      }
    }

    "successfully get suggest-geo-mobile" in {
      expectSuggestGeoMobile
      httpClient.respondWith(Response.getDefaultInstance)
      client.suggestGeoMobile("ленин", Seq.empty[Int], None).futureValue shouldBe Response.getDefaultInstance
    }

    "handle 400 in suggest-geo-mobile" in {
      expectSuggestGeoMobile
      httpClient.respond(StatusCodes.BadRequest)
      interceptCause[IllegalArgumentException] {
        client.suggestGeoMobile("ленин", Seq.empty[Int], None).futureValue
      }
    }
  }

  private def expectSuggestGeo: Unit = {
    httpClient.expect(
      GET,
      new StringBuilder("/suggest-geo?text=%D0%BB%D0%B5%D0%BD%D0%B8%D0%BD&")
        .append("v=9&")
        .append("reverse_geo_name=0&")
        .append("full_path=0&")
        .append("highlight=0&")
        .append("short_name=0&")
        .append("add_coords=1&")
        .append("callback=&")
        .append("bases=metro,geo,biz&")
        .append("n=10")
        .toString()
    )
  }

  private def expectSuggestGeoMobile: Unit = {
    httpClient.expect(
      GET,
      new StringBuilder("/suggest-geo-mobile?text=%D0%BB%D0%B5%D0%BD%D0%B8%D0%BD&")
        .append("v=4&")
        .append("reverse_geo_name=0&")
        .append("full_path=0&")
        .append("highlight=0&")
        .append("short_name=0")
        .toString()
    )
  }
}
