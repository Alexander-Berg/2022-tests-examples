package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.api.ResponseModel.VinHistoryApplyResponse.PaymentStatus
import ru.auto.salesman.model.ClientId
import ru.auto.salesman.service.BillingEventProcessor.BillingEventResponse
import ru.auto.salesman.service.{DealerVinHistoryBuyService, VinHistoryService}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.CacheControl.NoCache
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

class DealerVinHistoryApplyServiceImplSpec extends BaseSpec {
  "DealerVinHistoryApplyServiceImpl" should {
    val vinHistoryService = mock[VinHistoryService]
    val dealerVinHistoryBuyService = mock[DealerVinHistoryBuyService]
    val dealerVinHistoryApplyService = new DealerVinHistoryApplyServiceImpl(
      vinHistoryService,
      dealerVinHistoryBuyService
    )
    implicit val rc: RequestContext = AutomatedContext("unit-test", NoCache)

    "vin history report not bought need buy new vin history report in dealerVinHistoryBuyService" in {
      val clientId: ClientId = 123
      val vin = "test1"
      (vinHistoryService
        .alreadyPaid(_: ClientId, _: String))
        .expects(clientId, vin)
        .returningT(false)

      (dealerVinHistoryBuyService
        .buyView(_: VinHistoryService.Request)(_: RequestContext))
        .expects(*, *)
        .returningT(generateBuyServiceResponse())
        .once()

      dealerVinHistoryApplyService
        .applyVinHistory(
          clientId = clientId,
          vin = vin,
          offerId = None
        )
        .success
        .value shouldBe PaymentStatus.OK
    }

    "vin history already purchased return status " in {
      val clientId: ClientId = 123
      val vin = "test1"

      (vinHistoryService
        .alreadyPaid(_: ClientId, _: String))
        .expects(clientId, vin)
        .returningT(true)
        .once()

      (dealerVinHistoryBuyService
        .buyView(_: VinHistoryService.Request)(_: RequestContext))
        .expects(*, *)
        .never()

      dealerVinHistoryApplyService
        .applyVinHistory(
          clientId = clientId,
          vin = vin,
          offerId = None
        )
        .success
        .value shouldBe PaymentStatus.ALREADY_PAID
    }
  }

  private def generateBuyServiceResponse(): VinHistoryService.Response =
    VinHistoryService.Response(
      paymentStatus = PaymentStatus.OK,
      billingEventResponse = new BillingEventResponse(
        deadline = DateTime.now(),
        price = None,
        actualPrice = None,
        promocodeFeatures = Nil,
        holdId = None,
        clientId = None,
        agencyId = None,
        companyId = None,
        regionId = None
      )
    )
}
