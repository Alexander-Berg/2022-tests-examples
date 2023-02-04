package ru.auto.salesman.service.impl.moisha

import cats.data.NonEmptyList
import org.apache.http.HttpStatus

import java.io.IOException
import org.joda.time.DateTime
import ru.auto.salesman.client.json.JsonExecutor
import ru.auto.salesman.client.json.JsonExecutor.UnexpectedStatusException
import ru.auto.salesman.model.user.PaymentReasons
import ru.auto.salesman.model.{OfferTypes, ProductId, RegionId, UserSellerType}
import ru.auto.salesman.service.PriceEstimateService
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.PriceEstimateService.PriceRequest._
import ru.auto.salesman.service.impl.moisha.MoishaPriceEstimateService.MoishaResolver
import ru.auto.salesman.service.impl.moisha.MoishaPriceEstimateServiceSpec.{
  mockMoishaClient,
  requestWithBadRequestToRemote
}
import ru.auto.salesman.service.impl.moisha.model.{
  MoishaInterval,
  MoishaPoint,
  MoishaProduct,
  MoishaRequestId,
  MoishaResponse
}
import ru.auto.salesman.service.price.PriceEstimateServiceSpec
import ru.auto.salesman.test.MockitoOngoingStubs
import ru.auto.salesman.util.{AutomatedContext, PriceRequestContextType}

class MoishaPriceEstimateServiceSpec extends PriceEstimateServiceSpec {

  val resolver = new MoishaResolver("moisha-test")

  def service: PriceEstimateService =
    new MoishaPriceEstimateService(mockMoishaClient)(resolver)

  private val rc = AutomatedContext("test")

  "MoishaResolver.context()" should {
    "return batch url" in {
      val query = DefaultBatchQuery

      resolver.context(QueryWithRC(query, rc)) shouldBe
        "api/1.x/service/moisha-test/prices/batch"
    }

    "return client context url" in {
      val query =
        ClientContext(
          clientRegionId = RegionId(1L),
          offerPlacementDay = None,
          productTariff = None
        )

      resolver.context(QueryWithRC(query, rc)) shouldBe
        "api/1.x/service/moisha-test/price"
    }

    "return user context url" in {
      val query = UserContext(
        OfferTypes.Regular,
        UserSellerType.Usual,
        canAddFree = true,
        numByMark = Some(1),
        numByModel = Some(1),
        invalidVin = false,
        experiment = "",
        autoApply = false,
        paymentReason = Some(PaymentReasons.FreeLimit)
      )

      resolver.context(QueryWithRC(query, rc)) shouldBe
        "api/1.x/service/moisha-test/price"
    }

    "return subscription context" in {
      val query = OffersHistoryReportsContext(
        reportsCount = 1L,
        contextType = Some(PriceRequestContextType.VinHistory),
        geoId = Some(RegionId(213L)),
        contentQuality = None,
        experiment = None
      )

      resolver.context(QueryWithRC(query, rc)) shouldBe
        "api/1.x/service/moisha-test-subscriptions/price"
    }

    "return quota context" in {
      val query =
        QuotaContext(
          clientRegionId = RegionId(1L),
          clientMarks = Nil,
          amount = 100,
          tariff = None
        )

      resolver.context(QueryWithRC(query, rc)) shouldBe
        "api/1.x/service/moisha-test-quota/price"
    }

    "return parts context" in {
      val query = PartsQuotaContext(RegionId(1L), amount = 100)

      resolver.context(QueryWithRC(query, rc)) shouldBe
        "api/1.x/service/parts-quota/price"
    }
  }

  "MoishaPriceEstimateService#estimate" should {
    "raise an IllegalArgumentException if Moisha returns BadRequest" in {
      service
        .estimate(requestWithBadRequestToRemote)
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }
  }

  "MoishaPriceEstimateService#estimateBatch" should {
    "raise an IllegalArgumentException if Moisha returns BadRequest" in {
      service
        .estimateBatch(NonEmptyList.one(requestWithBadRequestToRemote))
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }
  }
}

object MoishaPriceEstimateServiceSpec extends MockitoOngoingStubs {

  import MoishaJsonProtocol._
  import PriceEstimateServiceSpec._
  import ru.auto.salesman.environment._
  import ru.yandex.vertis.mockito.MockitoSupport.{?, mock, when, eq => ~=}
  import spray.json._

  import scala.language.implicitConversions

  implicit def requestAsJson(request: PriceRequest): JsValue = request.toJson

  val todayEnd = now().withTimeAtStartOfDay().plusDays(1).minus(1)
  val tomorrowEnd = todayEnd.plusDays(1)

  val successResponsePart = JsObject(
    "points" -> JsArray(
      JsObject(
        "interval" -> JsObject(
          "to" -> JsString(IsoDateTimeFormatter.print(todayEnd))
        )
      ),
      JsObject(
        "interval" -> JsObject(
          "to" -> JsString(IsoDateTimeFormatter.print(tomorrowEnd))
        )
      )
    )
  )

  private val moishaPoint = MoishaPoint(
    "test-price-policy",
    MoishaInterval(
      DateTime.now(),
      DateTime.now()
    ),
    MoishaProduct(
      "all_sale_placement",
      Nil,
      200
    )
  )

  private val moishaResponse = MoishaResponse(
    List(moishaPoint, moishaPoint, moishaPoint),
    MoishaRequestId(priceRequestId = None)
  )

  val emptyResponsePart = JsObject(
    "points" -> JsArray()
  )

  val requestWithBadRequestToRemote: PriceRequest =
    request.copy(product = ProductId.Call)

  val mockMoishaClient = mock[JsonExecutor]

  when(mockMoishaClient.post(?, ~=(request))(?, ?, ?))
    .thenReturnZ(successResponsePart)

  when(mockMoishaClient.post(?, ~=(requests.toList.toJson))(?, ?, ?))
    .thenReturnZ(List(moishaResponse, moishaResponse).toJson)

  when(mockMoishaClient.post(?, ~=(requestWithEmptyResponse))(?, ?, ?))
    .thenReturnZ(emptyResponsePart)

  when(mockMoishaClient.post(?, ~=(requestWithClientError))(?, ?, ?))
    .thenThrowZ(new IllegalArgumentException)

  when(mockMoishaClient.post(?, ~=(requestWithOtherError))(?, ?, ?))
    .thenThrowZ(new IOException())

  when(mockMoishaClient.post(?, ~=(requestWithBadRequestToRemote))(?, ?, ?))
    .thenThrowZ(
      UnexpectedStatusException(HttpStatus.SC_BAD_REQUEST, "Boom")
    )

  when(
    mockMoishaClient
      .post(?, ~=(JsArray(requestWithBadRequestToRemote)))(?, ?, ?)
  )
    .thenThrowZ(
      UnexpectedStatusException(HttpStatus.SC_BAD_REQUEST, "Boom")
    )
}
