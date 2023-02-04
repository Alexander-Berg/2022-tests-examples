package ru.yandex.realty.developer.chat.api.v1.chat;

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.{ExceptionHandler, RejectionHandler, Route}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import play.api.libs.json.{JsObject, Json}
import ru.yandex.realty.akka.http.PlayJsonSupport
import ru.yandex.realty.api.handlers.SimpleApiRejectionHandler
import ru.yandex.realty.developer.chat.api.v1.ApiExceptionHandler
import ru.yandex.realty.developer.chat.backend.manager.ChatManager
import ru.yandex.realty.developer.chat.config.ConfigGen.DeveloperConfigGen
import ru.yandex.realty.developer.chat.config.DeveloperConfig
import ru.yandex.realty.developer.chat.model.DeveloperCrmTypeNamespace.DeveloperCrmType
import ru.yandex.realty.developer.chat.model.yachat.{OperatorRequest, YandexChatGen}
import ru.yandex.realty.http.HandlerSpecBase
import ru.yandex.realty.tracing.Traced

import java.util.UUID
import scala.concurrent.Future;

@RunWith(classOf[JUnitRunner])
class ChatHandlerSpec extends HandlerSpecBase with PropertyChecks with PlayJsonSupport {

  override protected def exceptionHandler: ExceptionHandler = ApiExceptionHandler.handler

  override protected def rejectionHandler: RejectionHandler = SimpleApiRejectionHandler.handler

  private val manager: ChatManager = mock[ChatManager]

  override def routeUnderTest: Route = new ChatHandler(manager).route

  private val jsonType = ContentTypes.`application/json`
  private def headerToken(token: String) = RawHeader("Authorization", s"Token $token")

  "POST /crm/webhook" should {

    "process YANDEX CHAT requests with valid token" in {

      val request = Post(s"/crm/webhook")

      forAll(YandexChatGen.OperatorRequestGen) { operatorRequest =>
        val token = UUID.randomUUID().toString
        val config = DeveloperConfigGen.next.copy(token = token, crmType = DeveloperCrmType.YANDEX_CHAT)
        (manager
          .processYandexChatMessage(_: OperatorRequest, _: DeveloperConfig)(_: Traced))
          .expects(where { (r, d, _) =>
            r == operatorRequest &&
            d == config
          })
          .returning(Future.unit)

        (manager
          .getConfigByToken(_: String))
          .expects(where { (inputToken: String) =>
            inputToken == token
          })
          .returning(Future.successful(Some(config)))

        request
          .withEntity(jsonType, Json.stringify(OperatorRequest.OperatorRequestFormat.writes(operatorRequest)))
          .withHeaders(headerToken(token)) ~>
          route ~>
          check {
            status should be(StatusCodes.OK)
            entityAs[JsObject] should be(ChatHandler.YandexChatOkResponse)
          }
      }
    }

  }
}
