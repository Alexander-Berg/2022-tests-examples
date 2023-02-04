package ru.yandex.vertis.general.common.clients.computer_vision.alice_api.testkit

import ru.yandex.vertis.general.common.clients.computer_vision.alice_api.CvAliceApiClient
import ru.yandex.vertis.general.common.clients.computer_vision.alice_api.CvAliceApiClient.{CvAliceApiClient, Tag}
import zio.{Task, ULayer, ZIO, ZLayer}

object CvAliceApiClientTest extends CvAliceApiClient.Service {

  override def getTags(avatarsUrl: String): Task[List[CvAliceApiClient.Tag]] =
    ZIO.succeed(
      List(
        Tag("котики", 0.5226271749),
        Tag("кошка", 0.5111497045),
        Tag("кот", 0.5384796262),
        Tag("кот ласковый", 0.5053853393),
        Tag("животные", 0.5032154322)
      )
    )

  val layer: ULayer[CvAliceApiClient] = ZLayer.succeed(CvAliceApiClientTest)
}
