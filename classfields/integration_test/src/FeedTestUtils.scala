package ru.yandex.vertis.general.feed.transformer.integration_test

import common.zio.grpc.client.GrpcClient
import common.zio.grpc.client.GrpcClient.GrpcClient
import general.feed.transformer.FeedTransformerServiceGrpc.FeedTransformerService
import general.feed.transformer.{TransformFeedRequest, TransformFeedResponse}
import zio.stream.ZStream
import zio.{ZIO, ZManaged}

import java.io.{File, PrintWriter}

object FeedTestUtils {

  case class Config(bucket: String, prefix: String)

  val Bucket = "vertis-feeds"
  val Prefix = "general-test"

  val UrlPrefix = s"http://$Bucket.s3.yandex.net/$Prefix/"

  def transformFeed(url: String): ZStream[GrpcClient[FeedTransformerService], Throwable, TransformFeedResponse] = {
    GrpcClient[FeedTransformerService]
      .stream[TransformFeedResponse]((s, o) => s.transformFeed(TransformFeedRequest(url), o))
  }

  def writeToFile[R](
      stream: ZStream[R, Throwable, TransformFeedResponse],
      destination: File): ZIO[R, Throwable, Unit] = {
    ZManaged
      .fromAutoCloseable(ZIO.effect(new PrintWriter(destination)))
      .use { os =>
        stream.foreach(r => ZIO.effect(os.println(r.asMessage.toProtoString)))
      }
  }
}
