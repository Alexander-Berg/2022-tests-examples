package ru.yandex.vertis.general.feed.logic.testkit

import general.feed.transformer.RawOffer
import ru.yandex.vertis.general.common.model.user.SellerId
import ru.yandex.vertis.general.feed.logic.PredictionEventsLogger
import zio.{Has, UIO, ULayer, ZLayer}

object TestPredictionEventsLogger {

  val test: ULayer[Has[PredictionEventsLogger.Service]] = ZLayer.succeed(
    new PredictionEventsLogger.Service {

      override def logPrediction(seller: SellerId, rawOffer: RawOffer, predictions: Map[String, Double]): UIO[Unit] =
        UIO.unit
    }
  )
}
