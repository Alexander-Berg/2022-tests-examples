package common.clients.sms.test

import common.clients.sms.model._
import common.clients.sms.{SmsSender, SmsSenderLive}
import common.zio.sttp.endpoint.Endpoint
import common.zio.sttp.Sttp
import sttp.client3.Response
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.Method
import zio._
import zio.test.Assertion._
import zio.test._

object SmsSenderLiveSpec extends DefaultRunnableSpec {

  private val NotFoundStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespondNotFound()

  private val response = """<?xml version="1.0" encoding="windows-1251"?>
                   |<doc>
                   |    <message-sent id="127000000003456" />
                   |    <gates ids="15" />
                   |</doc>""".stripMargin

  private val testSmsSenderConfig = SmsSenderConfig("test-sender", "test-route", "test-application")
  private val testSmsSenderConfigLayer = ZLayer.succeed(testSmsSenderConfig)

  private val queryParams = Map(
    "route" -> testSmsSenderConfig.route,
    "sender" -> testSmsSenderConfig.sender,
    "phone" -> "88005553535",
    "text" -> "test",
    "utf8" -> "1"
  )

  private val responseStub = AsyncHttpClientZioBackend.stub.whenRequestMatchesPartial {
    case r
        if r.uri.path.endsWith(List("sendsms")) &&
          r.uri.params.toMap == queryParams && r.method == Method.GET =>
      Response.ok(response)
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SmsSenderLive")(
      testM("handle 404 error") {
        for {
          response <- SmsSender.send(SmsMessage(None, "test"), PhoneDeliveryParams("88005553535")).run
        } yield assert(response)(fails(equalTo(SmsDeliveryException(SmsError.Undefined, "Not found"))))
      }.provideCustomLayer(createEnvironment(NotFoundStub)),
      testM("handle correct response") {
        for {
          response <- SmsSender.send(SmsMessage(None, "test"), PhoneDeliveryParams("88005553535"))
        } yield assert(response)(equalTo("127000000003456"))
      }.provideCustomLayer(createEnvironment(responseStub))
    )
  }

  def createEnvironment(
      sttpBackendStub: Sttp.ZioSttpBackendStub): ZLayer[Any, Nothing, Has[SmsSender.Service]] = {

    (Endpoint.testEndpointLayer ++ Sttp.fromStub(sttpBackendStub) ++ testSmsSenderConfigLayer) >>>
      ZLayer.fromServices[Endpoint, Sttp.Service, SmsSenderConfig, SmsSender.Service](
        new SmsSenderLive(_, _, _)
      )
  }
}
