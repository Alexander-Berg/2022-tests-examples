package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.api.ResponseModel.VinHistoryApplyResponse.PaymentStatus
import ru.auto.salesman.billing.{RequestContext => BillingRequestContext}
import ru.auto.salesman.model.ProductId.NonPromotionProduct
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{
  CityId,
  ClientId,
  DetailedClient,
  ProductTariff,
  RegionId,
  TransactionId
}
import ru.auto.salesman.service.BillingEventProcessor.BillingEventResponse
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.VinHistoryService.Request
import ru.auto.salesman.service.{
  BillingEventProcessor,
  DetailedClientSource,
  VinHistoryService
}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.CacheControl.NoCache
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

import scala.util.Try

class DealerVinHistoryBuyServiceImplSpec extends BaseSpec {
  "DealerVinHistoryBuyServiceImpl" should {
    val clientSource: DetailedClientSource = mock[DetailedClientSource]
    val billingEventProcessor: BillingEventProcessor =
      mock[BillingEventProcessor]
    val vinHistoryService: VinHistoryService = mock[VinHistoryService]
    val dealerVinHistoryBuyServiceImpl = new DealerVinHistoryBuyServiceImpl(
      clientSource = clientSource,
      billingEventProcessor = billingEventProcessor,
      vinHistoryService = vinHistoryService
    )
    implicit val rc: RequestContext = AutomatedContext("unit-test", NoCache)

    "success buy vin history " in {
      val request = generateBuyViewRequest()
      val regionId = RegionId(123)
      (clientSource
        .unsafeResolve(_: ClientId, _: Boolean))
        .expects(request.clientId, false)
        .returningZ(generateDetailedClient(request.clientId, regionId))

      (
        billingEventProcessor
          .process(
            _: DetailedClient,
            _: NonPromotionProduct,
            _: PriceRequest,
            _: TransactionId,
            _: Option[ProductTariff]
          )(_: BillingEventResponse => Try[_])(
            _: RequestContext,
            _: BillingRequestContext
          )
        )
        .expects(
          generateDetailedClient(request.clientId, regionId),
          *,
          *,
          *,
          *,
          *,
          *,
          *
        )
        .returningT(
          generateBillingEventResponse(request.clientId, regionId)
        )

      val response = dealerVinHistoryBuyServiceImpl
        .buyView(request)
        .success
        .value

      response.regionId shouldBe Some(RegionId(123))
      response.price shouldBe Some(169)
      response.actualPrice shouldBe Some(965)
      response.clientId shouldBe Some(request.clientId)
      response.regionId shouldBe Some(regionId)
      response.paymentStatus shouldBe PaymentStatus.OK
    }
  }

  private def generateBuyViewRequest() =
    Request(
      clientId = 123,
      vin = "test123",
      offerId = Option(AutoruOfferId(id = 123L, hash = "ddz"))
    )

  private def generateDetailedClient(clientId: ClientId, regionId: RegionId) =
    new DetailedClient(
      clientId = clientId,
      agencyId = None,
      balanceClientId = 100,
      balanceAgencyId = None,
      categorizedClientId = None,
      companyId = None,
      regionId = regionId,
      cityId = CityId(1L),
      accountId = 33L,
      isActive = true,
      firstModerated = true,
      singlePayment = Set.empty,
      productKey = None,
      paidCallsAvailable = false
    )

  private def generateBillingEventResponse(
      clientId: ClientId,
      regionId: RegionId
  ) =
    new BillingEventResponse(
      deadline = DateTime.now(),
      price = Some(169L),
      actualPrice = Some(965),
      promocodeFeatures = Nil,
      holdId = None,
      clientId = Some(clientId),
      agencyId = None,
      companyId = None,
      regionId = Some(regionId)
    )
}
