package ru.yandex.realty.api.routes.v1.handlers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.api.ProtoResponse
import ru.yandex.realty.api.routes.{defaultExceptionHandler, defaultRejectionHandler, PresetsHandler}
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.managers.presets.PresetsManager
import ru.yandex.realty.proto.search.FrontPagePresets
import ru.yandex.realty.request.Request

import scala.concurrent.Future

/**
  * Specs on [[ru.yandex.realty.api.routes.PresetsHandler]].
  *
  * @author nstaroverova
  */

@RunWith(classOf[JUnitRunner])
class PresetsHandlerSpec extends HandlerSpecBase {

  private val manager: PresetsManager = mock[PresetsManager]

  override def routeUnderTest: Route = new PresetsHandler(manager).route

  "GET /common/presets" should {
    val rgId = 587795
    val request = Get("/presets?rgid=" + rgId)
    val badRequest = Get("/presets")
    val correctResult = FrontPagePresets.newBuilder().build()

    "success rgId" in {
      (manager
        .extractPresets(_: Long)(_: Request))
        .expects(rgId, *)
        .returning(Future.successful(FrontPagePresets.newBuilder().build()))

      request ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          entityAs[ProtoResponse.FrontPagePresetsResponse] should be(PresetsHandler.toResponse(correctResult))
        }
    }

    "fail if rgId was not set" in {
      badRequest ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }
  }

  override protected val exceptionHandler: ExceptionHandler = defaultExceptionHandler

  override protected val rejectionHandler: RejectionHandler = defaultRejectionHandler
}
