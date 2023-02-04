package ru.yandex.realty.api.routes.v2.handlers.rent

import akka.http.scaladsl.model.{ContentTypes, HttpHeader, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.api.model.response.{ApiUnitResponseBuilder, Response}
import ru.yandex.realty.api.routes.{defaultExceptionHandler, defaultRejectionHandler}
import ru.yandex.realty.api.routes.v2.rent.RentEventHandler
import ru.yandex.realty.event.RealtyEventModelGen
import ru.yandex.realty.events.RentFrontEventBatch
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.rent.RentEventManager
import ru.yandex.realty.request.Request
import ru.yandex.vertis.protobuf.ProtobufUtils

import scala.concurrent.Future

class RentEventHandlerSpec extends HandlerSpecBase with PropertyChecks {

  override def routeUnderTest: Route = new RentEventHandler(manager).route
  override protected def exceptionHandler: ExceptionHandler = defaultExceptionHandler
  override protected def rejectionHandler: RejectionHandler = defaultRejectionHandler

  val manager: RentEventManager = mock[RentEventManager]

  val request200Ok = Post(s"/rent/event/log")
  val badRequestGet = Get(s"/rent/event/log")

  private val jsonType = ContentTypes.`application/json`

  "/event/log" should {
    "successfully log event message" in {
      forAll(RealtyEventModelGen.rentFrontEventBatch) { eventBatch =>
        (manager
          .logFrontEventBatch(_: RentFrontEventBatch, _: Seq[HttpHeader])(_: Request))
          .expects(eventBatch, *, *)
          .returning(Future.successful(()).map(_ => ApiUnitResponseBuilder.Instance))

        request200Ok.withEntity(jsonType, ProtobufUtils.toJson(eventBatch, compact = false).getBytes) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "return bad request for wrong http method" in {
      badRequestGet ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }
  }
}
