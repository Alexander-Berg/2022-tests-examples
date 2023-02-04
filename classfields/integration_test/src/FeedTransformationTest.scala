package ru.yandex.vertis.general.feed.transformer.integration_test

import common.zio.clients.s3.S3Client.S3Config
import common.zio.clients.s3.{S3Client, S3ClientLive}
import common.zio.grpc.client.GrpcClient.GrpcClient
import common.zio.grpc.client.{GrpcClientConfig, GrpcClientLive}
import common.zio.pureconfig.Pureconfig
import general.feed.transformer.FeedTransformerServiceGrpc.{FeedTransformerService, FeedTransformerServiceStub}
import ru.yandex.vertis.general.feed.transformer.integration_test.FeedTestUtils._
import common.zio.config.Configuration
import common.zio.config.Configuration.Configuration
import zio.blocking.Blocking
import zio.stream.ZSink
import zio.{ZIO, ZLayer, ZManaged}
import zio.test.Assertion._
import zio.test._

import java.io.File
import scala.sys.process._
import java.nio.file.Files

object FeedTransformationTest extends DefaultRunnableSpec {

  private def downloadTransformedResult(transformedFileName: String, expectedFile: File) = {
    S3Client
      .getObject(Bucket, s"$Prefix/$transformedFileName")
      .run(ZSink.fromFile(expectedFile.toPath))
  }

  private def testFeed(source: String, transformedFileName: String) = {
    val files = for {
      expectedFile <- ZManaged.makeEffect(Files.createTempFile(null, null))(Files.delete).map(_.toFile)
      actualFile <- ZManaged.makeEffect(Files.createTempFile(null, null))(Files.delete).map(_.toFile)
    } yield (expectedFile, actualFile)

    files.use { case (expected, actual) =>
      for {
        _ <- writeToFile(transformFeed(UrlPrefix + source), actual)
          .zipPar(downloadTransformedResult(transformedFileName, expected))
        out <- ZIO.effect(s"diff ${actual.getAbsolutePath} ${expected.getAbsolutePath}".lazyLines_!)
      } yield assert(out.mkString("\n"))(isEmptyString)
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("FeedTransformer")(
      TestCases.TestCases
        .map { testCase =>
          testM(testCase.description) {
            testFeed(testCase.originalFile, testCase.transformedResult)
          }
        }: _*
    ).provideLayerShared {
      val s3 = (Pureconfig.load[S3Config])("s3").toLayer.orDie >>> S3ClientLive.live
      val feedTransformerConfig = Pureconfig.load[GrpcClientConfig]("feed-transformer").toLayer.orDie
      val feedTransformer: ZLayer[Configuration, Nothing, GrpcClient[FeedTransformerService]] =
        feedTransformerConfig >>> GrpcClientLive.live(channel => new FeedTransformerServiceStub(channel))
      Configuration.live.orDie >>> (feedTransformer ++ s3) ++ Blocking.live
    }
}
