package ru.auto.api.services.pushnoy

import java.util.concurrent.atomic.AtomicReference
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.typesafe.config.ConfigFactory
import org.apache.http.client.utils.URIBuilder
import org.scalatest.concurrent.Eventually._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.{BeforeAndAfterAll, Inspectors}
import play.api.libs.json.Json
import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.util.JsonMatchers._

import scala.concurrent.Promise

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 13/07/2017.
  */
class DefaultPushnoyClientIntTest
  extends HttpClientSuite
  with BeforeAndAfterAll
  with IntegrationPatience
  with Inspectors {

  override protected def config: HttpClientConfig = {
    HttpClientConfig("pushnoy-api-http-api.vrts-slb.test.vertis.yandex.net", 80)
  }

  val client = new DefaultPushnoyClient(http)
  test("add and get user devices") {
    val tokenInfo1 = TokenInfoGen.next
    val deviceId1 = tokenInfo1.uuid

    val tokenInfo2 = TokenInfoGen.next
    val deviceId2 = tokenInfo2.uuid

    client.addTokenInfo(deviceId1, tokenInfo1).futureValue
    client.addTokenInfo(deviceId2, tokenInfo2).futureValue

    val userId = DeviceUidGen.next.takeRight(10)
    client.attachDeviceToUser(userId, deviceId1).futureValue
    client.attachDeviceToUser(userId, deviceId2).futureValue
    client.attachDeviceToUser(userId, deviceId2).futureValue

    val devices = List(tokenInfo1, tokenInfo2)

    val userDevices = client.getUserDevicesToken(userId).futureValue

    userDevices shouldBe devices
  }

  test("add and get device information") {
    val tokenInfo = TokenInfoGen.next
    val deviceInfo = DeviceInfoGen.next
    val deviceId = tokenInfo.uuid

    client.addTokenInfo(deviceId, tokenInfo).futureValue
    client.addDeviceInfo(deviceId, deviceInfo).futureValue
    val getToken = client.getTokenInfo(deviceId).futureValue
    val getInfo = client.getDeviceInfo(deviceId).futureValue

    getToken shouldBe tokenInfo
    getInfo shouldBe deviceInfo
  }

  test("get secret sign") {
    val secretSign = client.getSecretSign("testUser").futureValue

    assert(secretSign.getTs.nonEmpty)
    assert(secretSign.getSign.nonEmpty)
  }

  test("detach device") {

    val tokenInfo = TokenInfoGen.next
    val deviceId = tokenInfo.uuid

    val tokenInfo2 = TokenInfoGen.next
    val deviceId2 = tokenInfo2.uuid

    val deviceId3 = DeviceUidGen.next.takeRight(10)

    val user = DeviceUidGen.next.takeRight(10)

    client.addTokenInfo(deviceId, tokenInfo).futureValue
    client.addTokenInfo(deviceId2, tokenInfo2).futureValue
    client.addTokenInfo(deviceId3, tokenInfo2).futureValue
    client.attachDeviceToUser(user, deviceId).futureValue
    client.attachDeviceToUser(user, deviceId2).futureValue
    val getTokens = client.getUserDevicesToken(user).futureValue

    val tokens = List(tokenInfo, tokenInfo2)

    getTokens shouldBe tokens

    client.detachUsersFromDevice(deviceId).futureValue

    val userDevices2 = client.getUserDevicesToken(user).futureValue

    userDevices2.size shouldBe 1
    userDevices2.head shouldBe tokenInfo2
  }

  test("push message to user") {
    val user = DeviceUidGen.next.takeRight(10)
    val payload = PushInfoGen.next

    val tokenInfo = TokenInfoGen.next
    val deviceId = tokenInfo.uuid

    val deviceInfo = DeviceInfoGen.next

    client.addTokenInfo(deviceId, tokenInfo).futureValue
    client.addDeviceInfo(deviceId, deviceInfo).futureValue
    client.attachDeviceToUser(user, deviceId).futureValue

    val res1 = client.pushToUser(user, payload, Targets.Websocket).futureValue
    res1.getCount shouldBe 1
    val res2 = client.pushToUser(user, payload, Targets.Devices).futureValue
    res2.getCount shouldBe 1
    val res3 = client.pushToUser(user, payload, Targets.All).futureValue
    res3.getCount shouldBe 1
  }

  test("sign and receive") {
    pending
    val userId = DeviceUidGen.next.takeRight(10)
    val push = PushInfoGen.next
    val sign = client.getSecretSign(userId).futureValue

    val url = new URIBuilder(s"wss://${sign.getHost}/v2/subscribe/websocket")
      .addParameter("sign", sign.getSign)
      .addParameter("ts", sign.getTs)
      .addParameter("service", "autoru")
      .addParameter("user", userId)
      .addParameter("client", "autoru")
      .addParameter("session", "session")
      .toString

    val lastMessage = new AtomicReference[Message]()

    implicit val akka: ActorSystem = ActorSystem("test", ConfigFactory.empty())
    implicit val materializer: Materializer = Materializer.createMaterializer(akka)

    val flow: Flow[Message, Message, Promise[Option[Message]]] =
      Flow.fromSinkAndSourceMat(Sink.foreach[Message](lastMessage.set), Source.maybe[Message])(Keep.right)

    val (upgradeResponse, promise) =
      Http().singleWebSocketRequest(WebSocketRequest(url), flow)

    try {
      upgradeResponse.futureValue.response.status shouldBe StatusCodes.SwitchingProtocols

      val res = client.pushToUser(userId, push, target = Targets.Websocket).futureValue
      res.getCount shouldBe 1

      eventually {
        lastMessage.get() should not be null
      }
    } finally {
      promise.success(None)
      akka.terminate()
    }
    val json = Json.parse(lastMessage.get.asTextMessage.getStrictText)
    (json \ "message").as[String] should matchJson("""{
        |  "action" : "deeplink",
        |  "body" : "<200 первых символов из сообщения>",
        |  "push_name" : "Новое сообщение",
        |  "title" : "Новое сообщение по <марка> <модель> <поколение>",
        |  "url" : "autoru://app/chat/<room_id>"
        |}""".stripMargin)
  }
}
