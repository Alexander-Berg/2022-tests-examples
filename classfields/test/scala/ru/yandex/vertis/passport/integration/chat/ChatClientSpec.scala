package ru.yandex.vertis.passport.integration.chat

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.http.scaladsl.unmarshalling.{PredefinedFromEntityUnmarshallers, Unmarshal}
import org.scalatest.WordSpec
import org.scalacheck.Gen
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.model.api.ApiModel.CreateMessageParameters
import ru.yandex.vertis.passport.test.Producer._
import ru.yandex.vertis.passport.test.{ModelGenerators, SpecBase}
import ru.yandex.vertis.passport.util.http.HttpClientMock
import ru.yandex.vertis.passport.{AkkaSupport, Domains}
import ru.yandex.vertis.protobuf.ProtobufUtils
import akka.http.scaladsl.model.headers.RawHeader

class ChatClientSpec
  extends WordSpec
  with SpecBase
  with AkkaSupport
  with HttpClientMock
  with ProtobufUtils
  with PredefinedFromEntityUnmarshallers {
  val domain = ModelGenerators.domain.next
  val client = new ChatClientImpl(domain, http, ChatConfig("http://localhost"))

  "ChatClient" should {
    "sendTechSupportNotification" should {
      "work" in {
        val userId = ModelGenerators.userId.next
        val text = ModelGenerators.readableString.next
        val idempotencyKey = Gen.option(Gen.identifier).next
        implicit val um = stringUnmarshaller.map { json =>
          fromJson(CreateMessageParameters.getDefaultInstance, json)
        }
        onRequest {
          case req @ HttpRequest(_, uri, _, entity, _) =>
            uri.path shouldBe Uri.Path(s"/api/1.x/$domain/techSupport/serviceNotification")
            withEntity(entity, um) { value =>
              value.getUserId shouldBe s"user:$userId"
              value.getPayload.getContentType shouldBe MimeType.TEXT_HTML
              value.getPayload.getValue shouldBe text
              value
            }
            val reqIdempotencyKey = req.headers.collectFirst {
              case RawHeader("X-Idempotency-Key", v) => v
            }
            reqIdempotencyKey shouldBe idempotencyKey

            HttpResponse(StatusCodes.OK)
        }
        client.sendTechSupportNotification(userId, text, idempotencyKey).futureValue
      }
    }
  }
}
