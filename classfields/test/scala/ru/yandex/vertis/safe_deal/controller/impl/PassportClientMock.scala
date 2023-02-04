package ru.yandex.vertis.safe_deal.controller.impl

import ru.yandex.passport.model.api.{api_model => passport}
import ru.yandex.vertis.zio_baker.model.User
import ru.yandex.vertis.zio_baker.zio.client.passport.PassportClient
import ru.yandex.vertis.zio_baker.zio.httpclient.RequestSupport
import ru.yandex.vertis.zio_baker.zio.httpclient.config.HttpClientConfig
import zio._

class PassportClientMock() extends PassportClient.Service with RequestSupport {

  override protected def httpClientConfig: HttpClientConfig =
    throw new UnsupportedOperationException("Calling a stub method")

  override def getUserEssentials(user: User): Task[passport.UserEssentials] =
    Task.succeed(passport.UserEssentials.defaultInstance)

  override def search(searchBy: PassportClient.SearchBy): Task[Seq[passport.User]] =
    Task.succeed(Seq.empty)
}
