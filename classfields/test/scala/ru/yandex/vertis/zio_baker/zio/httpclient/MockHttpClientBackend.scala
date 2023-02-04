package ru.yandex.vertis.zio_baker.zio.httpclient

import ru.yandex.vertis.zio_baker.zio.httpclient.client.DefaultCapabilities
import sttp.client3.httpclient.zio.SttpClient
import sttp.monad.MonadError
import sttp.client3.{Request, Response}
import zio.{Task, ULayer, ZIO, ZLayer}

class MockHttpClientBackend(response: Response[_]) extends SttpClient.Service {

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  override def send[T, R >: DefaultCapabilities](request: Request[T, R]): Task[Response[T]] =
    ZIO.succeed(response.asInstanceOf[Response[T]])

  override def close(): Task[Unit] = ZIO.unit

  override def responseMonad: MonadError[Task] = ???
}

object MockHttpClientBackend {

  def live(response: Response[_]): ULayer[SttpClient] = ZLayer.succeed(new MockHttpClientBackend(response))
}
