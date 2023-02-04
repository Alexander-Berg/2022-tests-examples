package ru.yandex.vertis.general.feed.transformer.integration_test

import common.zio.clients.s3.S3Client.{S3Client, S3Config}
import common.zio.clients.s3.{S3Client, S3ClientLive}
import common.zio.files.ZFiles
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClientConfig, GrpcClientLive}
import common.zio.pureconfig.Pureconfig
import general.feed.transformer.FeedTransformerServiceGrpc.{FeedTransformerService, FeedTransformerServiceStub}
import org.apache.commons.codec.binary.Hex.encodeHexString
import org.apache.commons.codec.digest.DigestUtils
import ru.yandex.vertis.general.feed.transformer.integration_test.FeedTestUtils._
import common.zio.app.BaseApp
import common.zio.app.BaseApp.BaseEnvironment
import common.zio.logging.Logging
import zio.stream.ZStream
import zio.{ZIO, ZLayer}

import java.nio.file.Files

object FeedResultsGenerator extends BaseApp {
  override type Env = BaseEnvironment with GrpcClient[FeedTransformerService] with S3Client

  override def makeEnv: ZLayer[BaseEnvironment, Throwable, Env] = {
    val base = ZLayer.requires[BaseEnvironment]
    val feedTransformerConfig = Pureconfig.load[GrpcClientConfig]("feed-transformer").toLayer
    val feedTransformer: ZLayer[BaseEnvironment, Throwable, GrpcClient[FeedTransformerService]] =
      (base ++ feedTransformerConfig) >>> GrpcClientLive.live(channel => new FeedTransformerServiceStub(channel))

    val s3 = Pureconfig.load[S3Config]("s3").toLayer >>> S3ClientLive.live
    base ++ feedTransformer ++ s3
  }

  override def program: ZIO[Env, Throwable, Any] = {
    ZIO.foreach_(TestCases.TestCases) { testCase =>
      ZFiles
        .makeTempFile("", "")
        .use { destination =>
          for {
            _ <- writeToFile(transformFeed(UrlPrefix + testCase.originalFile), destination)
            hash <- zio.blocking
              .effectBlocking(DigestUtils.md5(Files.newInputStream(destination.toPath)))
              .map(encodeHexString)
            size <- zio.blocking.effectBlocking(Files.size(destination.toPath))
            file = s"${testCase.originalFile}-$hash"
            _ <- S3Client.uploadContent(
              Bucket,
              s"$Prefix/$file",
              size,
              "text/plain",
              ZStream.fromFile(destination.toPath)
            )
            _ <- Logging
              .info(
                s"Hash changed for ${testCase.originalFile}. Before: ${testCase.transformedResult}. After: $file"
              )
              .when(testCase.transformedResult != file)
          } yield ()
        }
    }
  }
}
