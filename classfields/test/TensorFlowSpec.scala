package ru.yandex.vertis.general.classifiers.models.tensorflow.test

import common.zio.clients.s3.S3Client
import common.zio.clients.s3.testkit.TestS3
import ru.yandex.vertis.general.classifiers.models.TextClassificationModel
import ru.yandex.vertis.general.classifiers.models.tensorflow.{
  ReloadedTFTextClassificationModel,
  TFTextClassificationModel,
  TensorFlowInit
}
import common.zio.logging.Logging
import zio.blocking.Blocking
import zio.clock.Clock
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test._
import zio.{ZIO, ZLayer}

import scala.concurrent.duration._

object TensorFlowSpec extends DefaultRunnableSpec {

  private val modelConfig = TFTextClassificationModel.ModelConfig(
    inputNames = "title" :: Nil,
    outputName = "dense_2"
  )

  private val reloadModelConfig = ReloadedTFTextClassificationModel.Config(
    modelBucket = "bucket",
    modelKey = "key",
    modelConfig = modelConfig,
    refreshInterval = 1.minute
  )

  def spec =
    suite("TensorFlow")(
      testM("Применяет модель") {
        for {
          wordProbabilities <- TextClassificationModel.predictProbabilities("dog")
          _ = println(wordProbabilities)
          sentenceProbabilities <- TextClassificationModel.predictProbabilities("This is a sentence")
          _ = println(sentenceProbabilities)
        } yield assert(wordProbabilities.get("word"))(
          isSome(isGreaterThan(wordProbabilities.getOrElse("sentence", 2f)))
        ) &&
          assert(sentenceProbabilities.get("sentence"))(
            isSome(isGreaterThan(sentenceProbabilities.getOrElse("word", 2f)))
          )
      }
    ).provideCustomLayer {
      val testS3 = TestS3.live ++ Blocking.live >>> (for {
        s3 <- ZIO.service[S3Client.Service]
        _ <- s3.createBucket(reloadModelConfig.modelBucket).orDie
        stream <- ZIO.effect(TensorFlowSpec.getClass.getResourceAsStream(s"/sentence_model.zip"))
        content <- ZStream.fromInputStream(stream).runCollect
        _ <- s3.uploadContent[Blocking](
          reloadModelConfig.modelBucket,
          reloadModelConfig.modelKey,
          content.length,
          "application/octet-stream",
          ZStream.fromIterable(content)
        )
      } yield s3).toLayer

      val layer = (Blocking.live ++ Logging.live >>> TensorFlowInit.live) ++
        Blocking.live ++ Clock.live ++ Logging.live ++ ZLayer.succeed(reloadModelConfig) ++ testS3 >>>
        ReloadedTFTextClassificationModel.live

      layer.orDie
    }
}
