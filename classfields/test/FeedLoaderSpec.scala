package ru.yandex.vertis.general.feed.logic.test

import common.zio.clients.s3.testkit.TestS3
import common.zio.logging.Logging
import common.zio.sttp.model.{NamedRequest, SttpError}
import common.zio.sttp.Sttp
import common.zio.sttp.Sttp.Sttp
import common.zio.tagging.syntax._
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.common.model.user.SellerId.UserId
import ru.yandex.vertis.general.feed.logic.FeedLoader
import ru.yandex.vertis.general.feed.logic.FeedLoader.{FeedLoader, UploadConfig}
import ru.yandex.vertis.general.feed.model.NamespaceId
import ru.yandex.vertis.general.feed.testkit.TracingMock
import sttp.client3.Response
import sttp.model.StatusCode
import zio.blocking.Blocking
import zio.clock.Clock
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{IO, UIO, ZLayer}

import java.io.File
import java.net.URI

object FeedLoaderSpec extends DefaultRunnableSpec {
  val InnerUrl = "https://s3.mds.yandex.net/classified-test/feed/testing_feed.xml"
  val PublicUrl = "https://someurl.ru/feed.xml"
  val SellerId: SellerId = UserId(1L)
  val nameSpaceId: NamespaceId = NamespaceId.default

  val TestResponse: Response[Either[String, File]] = Response(
    body = Right(new File("file")).withLeft[String],
    code = new StatusCode(0)
  )

  private val MockSttp = new Sttp.Service {

    override def send[T](request: NamedRequest[T]): IO[SttpError, Response[T]] =
      IO.succeed(TestResponse.asInstanceOf[Response[T]])
  }

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("FeedLoader")(
      testM("Downloads feed from inner web") {
        val plainSttp = ZLayer.succeed(MockSttp)
        val proxySttp = ZLayer.succeed(MockSttp).tagged[Sttp.WithProxy]

        for {
          _ <- FeedLoader
            .downloadAndSave(InnerUrl, SellerId, nameSpaceId)
            .provideLayer(plainSttp ++ proxySttp >>> loaderLayer)
        } yield assertCompletes
      },
      testM("Downloads feed from public web") {
        val plainSttp = ZLayer.succeed(MockSttp)
        val proxySttp = ZLayer.succeed(MockSttp).tagged[Sttp.WithProxy]

        for {
          _ <- FeedLoader
            .downloadAndSave(PublicUrl, SellerId, nameSpaceId)
            .provideLayer(plainSttp ++ proxySttp >>> loaderLayer)
        } yield assertCompletes
      }
    )
  }

  private def loaderLayer: ZLayer[Sttp with Sttp.SttpWithProxy, Throwable, FeedLoader] = {
    val proxySttpMock = ZLayer.requires[Sttp]
    val plainSttpMock = ZLayer.requires[Sttp.SttpWithProxy]
    val uploadConfig = UIO(UploadConfig(new URI(""), "")).toLayer

    Clock.live ++ TracingMock.pseudoEmpty ++ proxySttpMock ++ plainSttpMock ++ TestS3.mocked ++
      uploadConfig ++ Logging.live ++ Blocking.live >>> FeedLoader.live
  }
}
