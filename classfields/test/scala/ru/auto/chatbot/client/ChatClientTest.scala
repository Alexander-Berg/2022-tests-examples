package ru.auto.chatbot.client

import org.scalatest.FunSuite
import ru.yandex.vertis.MimeType
import ru.yandex.vertis.chat.model.api.ApiModel.{CreateMessageParameters, MessagePayload}
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client._

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-02-19.
  */
class ChatClientTest extends FunSuite {

  private val botId = "user:42893462"

  private val httpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with TracedHttpClient
      with RetryHttpClient

  private val chatHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("chat-api-auto-server.vrts-slb.test.vertis.yandex.net", 80, "http")
    new RemoteHttpService(name = "chat", endpoint = endpoint, client = httpClient)
  }

  val client = new ChatClient(chatHttpService)

  test("get room") {

    Await.result(client.getRooms(botId), 10.seconds)
  }

  test("get messages") {
    Await.result(client.getMessages(botId, "642d2d4025c36032f68493061d7f6994"), 10.seconds)
  }

  test("send message") {
    val payload = MessagePayload
      .newBuilder()
      .setContentType(MimeType.TEXT_PLAIN)
      .setValue("test")
    val messageParameters = CreateMessageParameters
      .newBuilder()
      .setRoomId("642d2d4025c36032f68493061d7f6994")
      .setUserId("user:42893462")
      .setPayload(payload)
      .build()

    Await.result(client.sendMessage(botId, messageParameters), 10.seconds)
  }
}
