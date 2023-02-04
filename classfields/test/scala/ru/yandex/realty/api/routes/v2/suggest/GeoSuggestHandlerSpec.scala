package ru.yandex.realty.api.routes.v2.suggest

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.api.ProtoResponse.GeoSuggestResponse
import ru.yandex.realty.api.routes.{defaultExceptionHandler, defaultRejectionHandler}
import ru.yandex.realty.clients.geohub.GeohubClient
import ru.yandex.realty.converters.GeoPointProtoConverter
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.suggests.{GeoSuggestManager, GeoSuggestRequest}
import ru.yandex.realty.model.location.GeoPoint
import ru.yandex.realty.suggest.geo.{GeoSuggestItem, GeoSuggests}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class GeoSuggestHandlerSpec extends HandlerSpecBase with PropertyChecks {

  val manager: GeoSuggestManager = mock[GeoSuggestManager]
  val geohub: GeohubClient = mock[GeohubClient]
  override def routeUnderTest: Route = new GeoSuggestHandler(manager, geohub).route
  override protected val exceptionHandler: ExceptionHandler = defaultExceptionHandler
  override protected val rejectionHandler: RejectionHandler = defaultRejectionHandler

  "GeoSuggestHandler" should {
    "return suggest for text" in {
      val request = Get(s"/suggest/geo?text=%D0%B1%D0%B5%D0%BD%D1%83%D0%B0")

      val correctResponse = GeoSuggestResponse
        .newBuilder()
        .setResponse(
          GeoSuggests
            .newBuilder()
            .addItems(
              GeoSuggestItem
                .newBuilder()
                .setTitle("Бeнуа")
                .setSubtitle("Бизнес-центр · Пискарёвский проспект, 2к2Щ")
                .setCoordinates(GeoPointProtoConverter.toProto(GeoPoint.getPoint(30.0f, 60.0f)))
                .build()
            )
        )
        .build()

      (manager
        .suggest(_: GeoSuggestRequest)(_: Traced))
        .expects(GeoSuggestRequest("бенуа", Seq.empty[Int], None), *)
        .returning(Future.successful(correctResponse))

      request ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          entityAs[GeoSuggestResponse] should be(correctResponse)
        }

    }

    "fail if required parameter text is not provided" in {
      val request = Get(s"/suggest/geo")
      request ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }
  }
}
