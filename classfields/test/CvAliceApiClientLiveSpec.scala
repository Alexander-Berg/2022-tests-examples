package ru.yandex.vertis.general.common.clients.computer_vision.alice_api.test

import common.zio.sttp.endpoint.Endpoint
import ru.yandex.vertis.general.common.clients.computer_vision.alice_api.{CvAliceApiClient, CvAliceApiClientLive}
import ru.yandex.vertis.general.common.clients.computer_vision.alice_api.CvAliceApiClient.Tag
import common.zio.sttp.Sttp
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.test.Assertion._
import zio.test._

import scala.io.Source

object CvAliceApiClientLiveSpec extends DefaultRunnableSpec {

  private val validStub = AsyncHttpClientZioBackend.stub.whenAnyRequest.thenRespond {
    Source.fromResource("answer.json")(scala.io.Codec.UTF8).getLines().mkString
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("CVAliceApiClientLive") {
      testM("parse response") {
        for {
          result <- CvAliceApiClient.getTags(
            "https://avatars.mds.yandex.net/get-o-yandex/3767261/dfe859e7ce003acc28ca00c805684d16/520x692"
          )
        } yield assert(result)(
          equalTo(
            Seq(
              Tag("кот", 0.5384796262),
              Tag("котики", 0.5226271749),
              Tag("кошка", 0.5111497045),
              Tag("кот ласковый", 0.5053853393),
              Tag("животные", 0.5032154322)
            )
          )
        )
      }
    }.provideCustomLayer((Endpoint.testEndpointLayer ++ Sttp.fromStub(validStub)) >>> CvAliceApiClientLive.live)
  }
}
