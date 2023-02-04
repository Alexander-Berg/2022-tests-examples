package ru.auto.api.services.billing

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import ru.auto.api.http.HttpClientConfig
import ru.auto.api.model.{AutoruProduct, ModelGenerators}
import ru.auto.api.services.HttpClientSuite
import ru.auto.api.services.billing.DefaultMoishaClient.TimeBoundariesProvider

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 04.04.17
  */
class DefaultMoishaClientIntTest extends HttpClientSuite {

  override protected def config: HttpClientConfig =
    HttpClientConfig("http", "moisha-api-01-sas.test.vertis.yandex.net", 34410)

  private val client = new DefaultMoishaClient(http)

  implicit val testBoundariesProvider: TimeBoundariesProvider = (days: Int) => {
    val from = OffsetDateTime.parse("2018-12-10T15:25:51.902+03:00").truncatedTo(ChronoUnit.DAYS)
    val to = from.plusDays(days).minus(1, ChronoUnit.MILLIS)
    (from, to)
  }

  ignore("get price") {
    val offer = ModelGenerators.OfferGen.next

    val price = client.getPrice(offer, 1L, Some(213L), AutoruProduct.Premium, None).futureValue

    price.product.product shouldBe AutoruProduct.Premium.name
    price.product.total should be > 0L
  }

  ignore("get price without passing city id") {
    val offer = ModelGenerators.OfferGen.next

    val price = client.getPrice(offer, 1L, None, AutoruProduct.Premium, None).futureValue

    price.product.product shouldBe AutoruProduct.Premium.name
    price.product.total should be > 0L
  }

  test("get quotas") {
    val quotas = client.getQuotas(213, List("OPEL"), Some(1)).futureValue
    quotas should not be empty
  }

  test("get quotas with empty city id") {
    val quotas = client.getQuotas(213, List("BMW", "AUDI"), None).futureValue
    quotas should not be empty
  }
}
