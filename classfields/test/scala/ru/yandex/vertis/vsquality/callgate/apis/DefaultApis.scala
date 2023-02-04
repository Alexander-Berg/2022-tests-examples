package ru.yandex.vertis.vsquality.callgate.apis

import cats.effect.{Concurrent, ContextShift, Timer}
import cats.syntax.all._
import ru.yandex.vertis.vsquality.callgate.apis.impl._
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.http_client_utils.config.HttpClientConfig
import ru.yandex.vertis.vsquality.utils.http_client_utils.factory.SttpBackendFactory
import sttp.client3.SttpBackend

/**
  * Default apis instances for tests
  */
trait DefaultApis {

  private val defaultConfig: HttpClientConfig = HttpClientConfig("")

  protected def getSttpBackend[F[_]: Concurrent: ContextShift: Timer: Awaitable](
      config: HttpClientConfig = defaultConfig): SttpBackend[F, Any] = {
    SttpBackendFactory(config).use(_.pure[F]).await
  }

  // hobo
  private val hoboClientUri = "http://hobo-api.vrts-slb.test.vertis.yandex.net:80"
  private val hoboOperator = "manager"

  protected def getHoboClient[F[_]: Concurrent: ContextShift: Timer: Awaitable]: HoboClient[F] = {
    implicit val backend: SttpBackend[F, Any] = getSttpBackend()
    new HttpHoboClientImpl(hoboClientUri, hoboOperator)
  }
}
