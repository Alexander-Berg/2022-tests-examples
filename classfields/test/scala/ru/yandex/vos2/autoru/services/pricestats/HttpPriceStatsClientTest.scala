package ru.yandex.vos2.autoru.services.pricestats

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.auto.core.AutoSchemaVersions
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer
import ru.yandex.vos2.AutoruModel.AutoruOffer.{CarInfo, Location, Seller}
import ru.yandex.vos2.util.HttpBlockingPool.Instance
import ru.yandex.vos2.util.http.MockHttpClientHelper
import ru.yandex.vertis.baker.util.logging.Logging

class HttpPriceStatsClientTest extends AnyFunSuite with MockHttpClientHelper with Logging {

  val Host = "autoru-price-estimator-01-sas.test.vertis.yandex.net"
  val Port = 34398
  val Path = "/v3/stats"
  implicit val trace = Traced.empty

  private def getTestOfferBuilder =
    AutoruOffer
      .newBuilder()
      .setCategory(Category.CARS)
      .setVersion(AutoSchemaVersions.CAR_AD_VERSION)
      .setCarInfo(
        CarInfo
          .newBuilder()
          .setMark("BMW")
          .setModel("3ER")
          .setSuperGenId(7744658)
      )
      .setSeller(Seller.newBuilder().setPlace(Location.newBuilder().setGeobaseId(213)))

  private val SuccessResponse =
    """
      |{
      |    "stats": {
      |        "model": {
      |           "deprecation": {
      |               "avg_in_percentage": -8
      |           }
      |        }
      |    }
      |}
    """.stripMargin

  private val NoModelResponse =
    """
      |{
      |    "stats": {
      |
      |    }
      |}
    """.stripMargin

  private val NoDeprecationResponse =
    """
      |{
      |    "stats": {
      |        "model": {
      |
      |        }
      |    }
      |}
    """.stripMargin

  private val NoAvgInPercentageResponse =
    """
      |{
      |    "stats": {
      |        "model": {
      |           "deprecation": {
      |           }
      |        }
      |    }
      |}
    """.stripMargin

  test("Offer with mark, model, superGen & rid") {
    val priceStatsClient = new HttpPriceStatsClient(Host, Port, Path, TestOperationalSupport) {
      override val client = new Instance(mockHttpClient(200, SuccessResponse))
    }
    val testOffer = getTestOfferBuilder.build()
    priceStatsClient.getAvgPricePercentageDiff(testOffer) match {
      case Some(value) => assert(value == -8)
      case None => log.error("No response from priceStats client")
    }
  }

  test("No model in response") {
    val priceStatsClient = new HttpPriceStatsClient(Host, Port, Path, TestOperationalSupport) {
      override val client = new Instance(mockHttpClient(200, NoModelResponse))
    }
    val testOffer = getTestOfferBuilder.build()
    intercept[UnknownAvgPercentageDiffException] {
      priceStatsClient.getAvgPricePercentageDiff(testOffer)
    }
  }

  test("No deprecation in response") {
    val priceStatsClient = new HttpPriceStatsClient(Host, Port, Path, TestOperationalSupport) {
      override val client = new Instance(mockHttpClient(200, NoDeprecationResponse))
    }
    val testOffer = getTestOfferBuilder.build()
    intercept[UnknownAvgPercentageDiffException] {
      priceStatsClient.getAvgPricePercentageDiff(testOffer)
    }
  }

  test("No avgInPercentage in response") {
    val priceStatsClient = new HttpPriceStatsClient(Host, Port, Path, TestOperationalSupport) {
      override val client = new Instance(mockHttpClient(200, NoAvgInPercentageResponse))
    }
    val testOffer = getTestOfferBuilder.build()
    intercept[UnknownAvgPercentageDiffException] {
      priceStatsClient.getAvgPricePercentageDiff(testOffer)
    }
  }

  test("Empty answer") {
    val priceStatsClient = new HttpPriceStatsClient(Host, Port, Path, TestOperationalSupport) {
      override val client = new Instance(mockHttpClient())
    }
    val testOffer = getTestOfferBuilder.build()
    intercept[UnknownAvgPercentageDiffException] {
      priceStatsClient.getAvgPricePercentageDiff(testOffer)
    }
  }
}
