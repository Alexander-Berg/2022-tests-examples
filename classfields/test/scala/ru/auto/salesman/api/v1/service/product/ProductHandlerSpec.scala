package ru.auto.salesman.api.v1.service.product

import akka.http.scaladsl.model.StatusCodes
import com.google.protobuf.util.Timestamps
import ru.auto.api.ResponseModel.VinHistoryPaymentStatusResponse.PaymentStatus.PAID
import ru.auto.api.ResponseModel.{ResponseStatus, VinHistoryPaymentStatusResponse}
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.model.user.ApiModel.{
  VinHistoryPurchaseRecord,
  VinHistoryPurchasesForVin
}
import ru.auto.salesman.service.async.{
  AsyncDealerVinHistoryApplyService,
  AsyncVinHistoryService
}
import ru.auto.salesman.util.{OperatorContext, RequestContext}
import scala.collection.JavaConverters._

class ProductHandlerSpec extends RoutingSpec {

  val asyncVinHistoryService: AsyncVinHistoryService =
    mock[AsyncVinHistoryService]

  val asyncDealerVinHistoryApplyService: AsyncDealerVinHistoryApplyService =
    mock[AsyncDealerVinHistoryApplyService]

  private val route = new ProductHandler(
    asyncVinHistoryService,
    asyncDealerVinHistoryApplyService
  ).route

  implicit val rc: RequestContext = OperatorContext("foo", "10")

  "GET /vin-history/client/{clientId}" should {

    "return vin-history payment status" in {
      toMockFunction3(
        asyncVinHistoryService.checkVinHistory(_: Long, _: String)(
          _: RequestContext
        )
      )
        .expects(123L, "vinnumber123", rc)
        .returningF(PAID)
      val uri = "/vin-history/client/123?vin=vinnumber123"
      val expectedResponse = VinHistoryPaymentStatusResponse
        .newBuilder()
        .setPaymentStatus(PAID)
        .setStatus(ResponseStatus.SUCCESS)
        .build()

      Get(uri).withHeaders(RequestIdentityHeaders) ~> seal(route) ~> check {
        responseAs[VinHistoryPaymentStatusResponse] shouldBe expectedResponse
        status shouldBe StatusCodes.OK
      }
    }
  }

  "GET /vin-history/purchases?vin=vin=1" should {
    "return vin history purchases by VIN" in {
      val givenVinHistoryPurchasesForVin = VinHistoryPurchasesForVin
        .newBuilder()
        .setVin("vin-1")
        .addAllRecords(
          Seq(
            VinHistoryPurchaseRecord
              .newBuilder()
              .setUserId("dealer:123")
              .setHoldId("hold-id-1")
              .setCreatedAt(Timestamps.fromSeconds(1))
              .setDeadline(Timestamps.fromSeconds(1))
              .build()
          ).asJava
        )
        .build()

      toMockFunction2(
        asyncVinHistoryService.getVinHistoryPurchasesByVin(_: String)(
          _: RequestContext
        )
      )
        .expects("vin-1", rc)
        .returningF(givenVinHistoryPurchasesForVin)

      Get("/vin-history/purchases?vin=vin-1").withHeaders(
        RequestIdentityHeaders
      ) ~> seal(route) ~> check {
        responseAs[
          VinHistoryPurchasesForVin
        ] shouldBe givenVinHistoryPurchasesForVin
        status shouldBe StatusCodes.OK
      }
    }
  }

}
