package ru.yandex.realty.api.routes.v1.event

import akka.http.scaladsl.model.{ContentTypes, HttpHeader, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.adsource.AdSource
import ru.yandex.realty.api.model.response.Response
import ru.yandex.realty.api.routes.{defaultExceptionHandler, defaultRejectionHandler}
import ru.yandex.realty.event.RealtyEventModelGen
import ru.yandex.realty.events.EventBatch
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.events.EventManager
import ru.yandex.realty.request.Request
import ru.yandex.vertis.protobuf.ProtobufUtils
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Future

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class EventLogHandlerSpec extends HandlerSpecBase with PropertyChecks {

  override def routeUnderTest: Route = new EventLogHandler(manager).route
  override protected def exceptionHandler: ExceptionHandler = defaultExceptionHandler
  override protected def rejectionHandler: RejectionHandler = defaultRejectionHandler

  val manager: EventManager = mock[EventManager]

  val request200Ok = Post(s"/event/log?crc=123456")
  val badRequestNoBody = Post(s"/event/log?crc=123456")
  val badRequestGet = Get(s"/event/log?crc=123456")

  private val jsonType = ContentTypes.`application/json`

  "/event/log" should {
    "successfully log event message" in {
      forAll(RealtyEventModelGen.eventBatchGen) { eventBatch =>
        (manager
          .logEvents(_: EventBatch, _: Option[AdSource], _: Seq[HttpHeader])(_: Request))
          .expects(eventBatch, None, *, *)
          .returning(Future.successful(()).map(_ => Response(Unit)))

        request200Ok.withEntity(jsonType, ProtobufUtils.toJson(eventBatch, compact = false).getBytes) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
          }
      }
    }

    "return bad request if no body provided" in {
      badRequestNoBody ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
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
