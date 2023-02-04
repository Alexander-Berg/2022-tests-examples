package ru.yandex.vertis.general.wizard.core.service

import ru.yandex.vertis.general.wizard.core.service.UrlBuilder.UrlBuilder
import ru.yandex.vertis.general.wizard.model.{Url, UrlRequest}
import zio.test.mock
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.Mock

object UrlBuilderMock extends Mock[UrlBuilder] {
  object BuildSingle extends Effect[UrlRequest, Throwable, Url]
  object BuildMultiple extends Effect[Seq[UrlRequest], Throwable, Seq[Url]]

  override val compose: URLayer[Has[mock.Proxy], UrlBuilder] =
    ZLayer.fromService { proxy =>
      new UrlBuilder.Service {
        override def build(urlRequest: UrlRequest): Task[Url] = proxy(BuildSingle, urlRequest)

        override def build(urlRequests: Seq[UrlRequest]): Task[Seq[Url]] = proxy(BuildMultiple, urlRequests)
      }
    }
}
