package ru.yandex.realty.clients.prediction

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Ignore}
import ru.yandex.realty.http.{HttpEndpoint, RemoteHttpService, TestHttpClient}
import ru.yandex.realty.model.message.RealtySchema.OfferMessage
import ru.yandex.realty.model.message.RealtySchema.PricePredictionCacheKey.ModelType
import ru.yandex.realty.proto.offer.{OfferCategory, OfferType}
import ru.yandex.realty.tracing.Traced

import scala.concurrent.Await
import scala.concurrent.duration._

@Ignore
@RunWith(classOf[JUnitRunner])
class PriceEstimatorClientTest extends FlatSpec with TestHttpClient {

  implicit private val trace: Traced = Traced.empty

  "PricePredictionClient" should "get predicted prices" in {
    val request = OfferMessage
      .newBuilder()
      .setOfferId("168295225243098625")
      .setOfferTypeInt(OfferType.RENT.getNumber)
      .setCategoryTypeInt(OfferCategory.APARTMENT.getNumber)
      .setDescription(
        "ЖК Некрасовка. метро \"Выхино\" 15 минут транспортом . " +
          "Новый дом рядом с открывающейся в 2018 году ст. метро Некрасовка-2 мин. пешком. " +
          "Окна во двор. Кухня 10 кв.м., комната 19 кв.м. Балкон. Железная дверь. Интернет. " +
          "Квартира после ремонта с мебелью и техникой. Сдается впервые только на длительный срок. " +
          "Дополнительно оплачивается вода, водоотведение, свет по счетчикам"
      )
      .build()

    val client = new PriceEstimatorClientImpl(
      new RemoteHttpService(
        "unit-test",
        HttpEndpoint("realty-price-predictor-api.vrts-slb.test.vertis.yandex.net", 80),
        testClient
      )
    )

    val f = client.getPricePrediction(request, ModelType.PLAIN)
    val response = Await.result(f, 30.seconds)
    println(response)
  }
}
