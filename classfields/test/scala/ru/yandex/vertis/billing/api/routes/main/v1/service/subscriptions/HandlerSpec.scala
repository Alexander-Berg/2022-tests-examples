package ru.yandex.vertis.billing.api.routes.main.v1.service.subscriptions

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.routes.main.v1.service.subscriptions.Handler
import ru.yandex.vertis.billing.api.routes.main.v1.view.NotificationEventView
import ru.yandex.vertis.billing.model_core.NotificationEvent
import ru.yandex.vertis.billing.model_core.gens.{NotificationEventGen, Producer}
import ru.yandex.vertis.billing.model_core.proto.Conversions
import ru.yandex.vertis.util.akka.http.protobuf.Protobuf

/**
  * Spec on [[Handler]]
  *
  * @author ruslansd
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = "/api/1.x/service/autoru/subscriptions"

  import NotificationEventView.modelUnmarshaller

  "Renderer" should {

    val event = NotificationEventGen.next
    val protoEvent = Conversions.toMessage(event).toByteArray

    "respond with correct json to protobuf data" in {
      Post(url("/"), HttpEntity(Protobuf.contentType, protoEvent)) ~>
        route ~>
        check {
          status should be(StatusCodes.OK)
          val resp = responseAs[NotificationEvent]
          resp shouldBe event
        }
    }
  }
}
