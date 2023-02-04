package ru.auto.salesman.service.price

import java.io.IOException

import cats.data.NonEmptyList
import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.model._
import ru.auto.salesman.service.PriceEstimateService
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.PriceEstimateService.PriceRequest.priceToFunds
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.DateTimeInterval

trait PriceEstimateServiceSpec extends BaseSpec {

  def service: PriceEstimateService

  import PriceEstimateServiceSpec._

  "AsyncPriceEstimateService" should {
    "process correct request" in {
      service.estimate(request).success
    }

    "process correct batch request" in {
      service.estimateBatch(requests).success
    }

    "fail if can't extract deadline" in {
      service
        .estimate(requestWithEmptyResponse)
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }

    "fail on bad request" in {
      service
        .estimate(requestWithClientError)
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }

    "fail on other errors" in {
      service
        .estimate(requestWithOtherError)
        .failure
        .exception shouldBe an[IOException]
    }
  }

  "priceToFunds()" should {

    "convert price to funds" in {
      priceToFunds(BigDecimal(22)).success.value shouldBe 2200
      priceToFunds(BigDecimal(1.57)).success.value shouldBe 157
      priceToFunds(BigDecimal(0.28)).success.value shouldBe 28
      priceToFunds(BigDecimal(1.1111)).success.value shouldBe 111
      priceToFunds(BigDecimal(9.9999)).success.value shouldBe 999
    }

    "fail if price is too big" in {
      priceToFunds(BigDecimal(Long.MaxValue)).failure.exception shouldBe an[
        ArithmeticException
      ]
    }
  }
}

object PriceEstimateServiceSpec {

  val offer = PriceRequest.ClientOffer(
    price = 0,
    creationDate = DateTime.now(),
    category = OfferCategories.Cars,
    section = Section.USED,
    mark = "AUDI",
    model = "A3"
  )

  val context = PriceRequest.ClientContext(
    RegionId(1L),
    offerPlacementDay = None,
    productTariff = None
  )

  val request = PriceRequest(
    offer,
    context,
    ProductId.Placement,
    DateTimeInterval.daysFromCurrent(3)
  )

  val requests = NonEmptyList.of(request, request, request)

  val requestWithEmptyResponse = request.copy(product = ProductId.Fresh)

  val requestWithClientError = request.copy(product = ProductId.Color)

  val requestWithOtherError = request.copy(product = ProductId.Top)
}
