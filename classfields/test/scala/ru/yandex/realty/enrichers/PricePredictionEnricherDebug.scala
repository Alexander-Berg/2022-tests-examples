package ru.yandex.realty.enrichers

import java.io.FileInputStream
import java.util.Properties
import ru.yandex.realty.application.{EnvironmentType, PropertiesHolder}
import ru.yandex.realty.cache.PricePredictionCacheSupplier
import ru.yandex.realty.cache.redis.RedisConfig
import ru.yandex.realty.clients.prediction.{PriceEstimatorClient, PriceEstimatorClientImpl}
import ru.yandex.realty.features.FeatureStub
import ru.yandex.realty.http.{HttpEndpoint, RemoteHttpService}
import ru.yandex.realty.model.history.OfferHistory
import ru.yandex.realty.model.message.RealtySchema.OfferMessage
import ru.yandex.realty.model.offer.Predictions
import ru.yandex.realty.model.raw.RawOfferImpl
import ru.yandex.realty.model.serialization.OfferProtoConverter
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.unification.OfferWrapper

import scala.concurrent.ExecutionContext.global

object PricePredictionEnricherDebug extends App {

  implicit val ec = global

  private val properties = new Properties()
  properties.setProperty("ops.port", "6666")
  PropertiesHolder.create(properties, EnvironmentType.DEVELOPMENT)

  val cacheSupplier = new PricePredictionCacheSupplier with TestOperationalComponents {
    override def pricePredictionRedisConfig: RedisConfig = RedisConfig(
      "price-prediction-cache-test",
      "man-yo6071aj1so8l1hj.db.yandex.net:26379,sas-jiju458z5hgl69go.db.yandex.net:26379,vla-3ahf6c8pgt6vrbqp.db.yandex.net:26379",
      System.getenv("REDIS_PRICE_PREDICTION_PASSWORD")
    )
  }

  val enricher = new PricePredictionEnricher(
    cache = cacheSupplier.pricePredictionCache,
    priceEstimatorClient = new PriceEstimatorClientImpl(
      new RemoteHttpService(
        HttpEndpoint("realty-price-predictor-api.vrts-slb.test.vertis.yandex.net"),
        "PricePredictionEnricherDebug-PriceEstimatorClient"
      )
    ),
    resendFeature = new FeatureStub(false)
  )

  val offerMessage = resource.managed(new FileInputStream("offer.bin")).acquireAndGet(OfferMessage.parseFrom)
  val offer = OfferProtoConverter.fromMessage(offerMessage)
  offer.setPredictions(new Predictions())
  val ow = new OfferWrapper(new RawOfferImpl, offer, OfferHistory.justArrived())

  enricher.process(ow)(Traced.empty)

  println(ow.getOffer.getPredictions)
}
