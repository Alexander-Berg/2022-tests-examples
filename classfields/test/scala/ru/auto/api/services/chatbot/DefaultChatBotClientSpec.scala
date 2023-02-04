package ru.auto.api.services.chatbot

import akka.http.scaladsl.model.HttpMethods.GET
import akka.http.scaladsl.model.StatusCodes
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.managers.TestRequest
import ru.auto.api.model.gen.BasicGenerators._
import ru.auto.api.services.{HttpClientSpec, MockedHttpClient}
import ru.auto.api.util.Request
import ru.auto.chatbot.ApiModel.{ChatBotStateEssentials, ChatBotStateEssentialsResponse}

/**
  * TODO
  *
  * @author aborunov
  */
class DefaultChatBotClientSpec
  extends HttpClientSpec
  with MockedHttpClient
  with ScalaCheckPropertyChecks
  with TestRequest {

  val client = new DefaultChatBotClient(http)

  implicit override def request: Request = super.request

  "ChatBotClient" should {
    "return state essentials by room id" in {
      val roomId = readableString.next
      http.expectUrl(GET, s"/api/v1/room/state/current/$roomId/essentials")
      http.expectHeader("ACCEPT", "application/protobuf")
      val state = ChatBotStateEssentials.newBuilder().setOfferId("100500-hash").build()
      val response = ChatBotStateEssentialsResponse.newBuilder().setChatBotStateEssentials(state).build()
      http.respondWith(StatusCodes.OK, response)
      val result = client.getCurrentStateEssentials(roomId).futureValue
      result shouldBe response
    }

    "return default state on 404 from chat bot" in {
      val roomId = readableString.next
      http.expectUrl(GET, s"/api/v1/room/state/current/$roomId/essentials")
      val state = ChatBotStateEssentials.newBuilder().setOfferId("100500-hash").build()
      val response = ChatBotStateEssentialsResponse.newBuilder().setChatBotStateEssentials(state).build()
      http.respondWith(StatusCodes.NotFound, response)
      val result = client.getCurrentStateEssentials(roomId).futureValue
      result shouldBe ChatBotStateEssentialsResponse.getDefaultInstance
    }
  }
}
