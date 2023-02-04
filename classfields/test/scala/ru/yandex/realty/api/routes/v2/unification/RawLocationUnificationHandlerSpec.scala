package ru.yandex.realty.api.routes.v2.unification

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.api.routes.{defaultExceptionHandler, defaultRejectionHandler}
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.unification.{RawLocationUnificationManager, RawLocationUnificationRequest}
import ru.yandex.realty.proto.unified.offer.address.LocationUnified
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future
import scala.language.implicitConversions

@RunWith(classOf[JUnitRunner])
class RawLocationUnificationHandlerSpec extends HandlerSpecBase with PropertyChecks {

  val manager: RawLocationUnificationManager = mock[RawLocationUnificationManager]
  override def routeUnderTest: Route = new RawLocationUnificationHandler(manager).route
  override protected val exceptionHandler: ExceptionHandler = defaultExceptionHandler
  override protected val rejectionHandler: RejectionHandler = defaultRejectionHandler

  "RawLocationUnificationHandler" should {
    "return unified location" in {
      val request = Get(s"/unification/raw-location?offerId=1&address=benua")

      val correctResponse = LocationUnified
        .newBuilder()
        .setRawAddress("benua")
        .build()

      (manager
        .getUnifiedLocationForRawLocation(_: RawLocationUnificationRequest)(_: Traced))
        .expects(RawLocationUnificationRequest("1", Option("benua"), None, ignoreCoordinates = false), *)
        .returning(Future.successful(correctResponse))

      request ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          entityAs[LocationUnified] should be(correctResponse)
        }
    }

    "fail if required parameter offerId is not provided for unified location request" in {
      val request = Get(s"/unification/raw-location")
      request ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }
  }

}
