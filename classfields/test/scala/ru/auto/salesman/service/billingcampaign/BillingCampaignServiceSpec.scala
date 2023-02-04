package ru.auto.salesman.service.billingcampaign

import auto.indexing.CampaignByClientOuterClass.CampaignHeaderList
import org.joda.time.{DateTime, DateTimeUtils}
import ru.auto.api.ApiOfferModel.{Offer, Section}
import ru.auto.salesman.Task
import ru.auto.salesman.billing.BootstrapClient.ProductTypeFilter
import ru.auto.salesman.billing.RequestContext
import ru.auto.salesman.client.moisha.{
  PriceResponse,
  Point => MoishaPoint,
  Product => MoishaProduct
}
import ru.auto.salesman.environment.{now, today}
import ru.auto.salesman.exceptions.ClientNotFoundException
import ru.auto.salesman.model.{Client, OfferCategories, ProductId}
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.PriceEstimateService.PriceRequest.{
  PaidCallClientContext,
  PaidCallClientOffer
}
import ru.auto.salesman.service.billingcampaign.BillingCampaignService.CallInfo
import ru.auto.salesman.service.call.cashback.domain.CallId
import ru.auto.salesman.service.impl.BillingServiceImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.feature.TestDealerFeatureService
import ru.auto.salesman.util.GeoUtils.{RegMoscow, RegSverdlovsk}
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.{
  BootstrapCampaignSource,
  BootstrapOrderSource,
  CustomerId
}

class BillingCampaignServiceSpec extends BaseSpec {

  private val dealerFeatureService =
    TestDealerFeatureService(priorityPlacementByPeriodsEnabled = true)

  private val billingCampaignService = new BillingCampaignService(
    BillingTestData.clientService,
    BillingTestData.detailedClientSource,
    BillingTestData.billingBootstrapClient,
    BillingTestData.clientDao,
    BillingTestData.billingCampaignClient,
    BillingTestData.moishaClient,
    new BillingServiceImpl(BillingTestData.billingBootstrapClient),
    dealerFeatureService,
    BillingTestData.priceRequestCreator,
    BillingTestData.priceServiceChooserTest,
    BillingTestData.paymentModelChecker
  )

  import BillingTestData._

  "BillingCampaignService" should {

    "get call campaign" in {
      mockClientResolve()
      (billingBootstrapClient
        .getCampaign(_: CustomerId, _: Long, _: ProductTypeFilter)(
          _: RequestContext
        ))
        .expects(*, *, *, *)
        .returningT(Iterable(campaignHeader))
      billingCampaignService
        .getCallCampaign(ClientId)
        .success
        .value shouldBe campaignHeader
    }

    "get match applications campaigns" in {
      mockClientResolve()
      (billingBootstrapClient
        .getCampaign(_: CustomerId, _: Long, _: ProductTypeFilter)(
          _: RequestContext
        ))
        .expects(*, *, *, *)
        .returningT(Iterable(campaignHeader))
      billingCampaignService
        .getMatchApplicationsCampaigns(ClientId)
        .success
        .value shouldBe Some(
        CampaignHeaderList
          .newBuilder()
          .addCampaignHeaders(campaignHeader)
          .build
      )
    }

    "return None if no applications campaigns found" in {
      mockClientResolve()
      (billingBootstrapClient
        .getCampaign(_: CustomerId, _: Long, _: ProductTypeFilter)(
          _: RequestContext
        ))
        .expects(*, *, *, *)
        .returningT(Nil)
      billingCampaignService
        .getMatchApplicationsCampaigns(ClientId)
        .success
        .value shouldBe None
    }

    "update campaign" in {
      mockClientResolve()
      (billingBootstrapClient
        .getCampaign(_: CustomerId, _: Long, _: ProductTypeFilter)(
          _: RequestContext
        ))
        .expects(*, *, *, *)
        .returningT(Iterable(campaignHeader))
      (billingBootstrapClient
        .order(_: BootstrapOrderSource)(_: RequestContext))
        .expects(*, *)
        .returningT(Order)
      (billingCampaignClient.updateCampaign _)
        .expects(*, *, "campaignId", *)
        .anyNumberOfTimes()
        .returningZ(campaignHeader)
      (billingBootstrapClient
        .campaign(_: BootstrapCampaignSource)(_: RequestContext))
        .expects(*, *)
        .returningT(campaignHeader)

      billingCampaignService
        .updateCallCarsNewCampaign(
          UpsertParams(
            ClientId,
            dayLimit = Some(1000000L),
            weekLimit = Some(5000000L),
            costPerCall = Some(600),
            enabled = Some(true),
            recalculateCostPerCall = None,
            createNew = true
          )
        )
        .success
        .value shouldBe campaignHeader
    }

    "create campaign" in {
      mockClientResolve()
      (billingBootstrapClient
        .order(_: BootstrapOrderSource)(_: RequestContext))
        .expects(*, *)
        .returningT(Order)
      (billingBootstrapClient
        .getCampaign(_: CustomerId, _: Long, _: ProductTypeFilter)(
          _: RequestContext
        ))
        .expects(*, *, *, *)
        .returningT(Iterable.empty)
      (billingBootstrapClient
        .campaign(_: BootstrapCampaignSource)(_: RequestContext))
        .expects(*, *)
        .returningT(campaignHeader)
      (billingCampaignClient.updateCampaign _)
        .expects(*, *, ExistingCampaignId, *)
        .twice()
        .returningZ(campaignHeader)
      (billingCampaignClient.createCampaign _)
        .expects(*, *, *)
        .returningZ(campaignHeader)
      (clientDao.getPoiId _)
        .expects(*)
        .returningZ(Some(ClientPoiId))

      billingCampaignService
        .updateCallCarsNewCampaign(
          UpsertParams(
            ClientId,
            dayLimit = Some(1000000L),
            weekLimit = Some(5000000L),
            costPerCall = Some(600),
            enabled = Some(true),
            recalculateCostPerCall = None,
            createNew = true
          )
        )
        .success
        .value shouldBe campaignHeader
    }

    "throw exception if campaign is missing and createNew=false passed" in {
      (detailedClientSource.unsafeResolve _)
        .expects(*, *)
        .anyNumberOfTimes()
        .returningZ(ClientDetails)
      (billingBootstrapClient
        .order(_: Model.BootstrapOrderSource)(_: RequestContext))
        .expects(*, *)
        .returningT(Order)
      (billingBootstrapClient
        .getCampaign(_: CustomerId, _: Long, _: ProductTypeFilter)(
          _: RequestContext
        ))
        .expects(*, *, *, *)
        .returningT(Iterable.empty)

      billingCampaignService
        .updateCallCarsNewCampaign(
          UpsertParams(
            ClientId,
            dayLimit = Some(1000000L),
            weekLimit = Some(5000000L),
            costPerCall = Some(600),
            enabled = Some(true),
            recalculateCostPerCall = None,
            createNew = false
          )
        )
        .failure
        .exception shouldBe a[CallCampaignNotFoundException]
    }

  }

  "BillingCampaignService.getCallInfo (when deposit is enabled)" should {

    "return call info with priority placement when vos marks found" in {
      (clientDao.get _)
        .expects(*)
        .returningZ(List(client))

      DateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis())
      (BillingTestData.priceServiceChooserTest
        .getCallPrice(
          _: Client,
          _: Option[Offer],
          _: Option[Section],
          _: Option[CallId],
          _: DateTime
        ))
        .expects(client, None, None, None, now())
        .returningZ(50000)

      billingCampaignService
        .getCallInfo(1)
        .success
        .value shouldBe CallInfo(50000, 3)
    }

    "return call info with zero deposit coefficient when vos marks found for sverdlovsk client" in {
      val sverdlovskClient =
        client.copy(regionId = RegSverdlovsk)

      (clientDao.get _)
        .expects(*)
        .returningZ(List(sverdlovskClient))

      DateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis())
      (BillingTestData.priceServiceChooserTest
        .getCallPrice(
          _: Client,
          _: Option[Offer],
          _: Option[Section],
          _: Option[CallId],
          _: DateTime
        ))
        .expects(sverdlovskClient, None, None, None, now())
        .returningZ(50000)

      billingCampaignService
        .getCallInfo(1)
        .success
        .value shouldBe CallInfo(50000, 0)
    }

    "throw exception when calls aren't available for client" in {
      (detailedClientSource.resolve _)
        .expects(*, *)
        .anyNumberOfTimes()
        .returningT(Some(ClientDetails))

      (clientDao.get _)
        .expects(*)
        .returningZ(List(client.copy(paidCallsAvailable = false)))

      billingCampaignService
        .getCallInfo(1L)
        .failure
        .exception shouldBe a[PaidCallDisabledException]
    }

    "throw exception when client not found" in {
      (detailedClientSource.resolve _)
        .expects(*, *)
        .anyNumberOfTimes()
        .returningT(Some(ClientDetails))
      (clientDao.get _)
        .expects(*)
        .returningZ(Nil)

      billingCampaignService
        .getCallInfo(1L)
        .failure
        .exception shouldBe a[ClientNotFoundException]
    }
  }

  "BillingCampaignService.getCallInfo (when deposit is disabled)" should {

    val billingCampaignService =
      BillingCampaignServiceSpec.this.billingCampaignService
        .copy(dealerFeatureService =
          dealerFeatureService.copy(
            callCampaignDepositDisabled = Task.succeed(true)
          )
        )

    "return call info with priority placement & deposit = 0 when vos marks found" in {
      (clientDao.get _)
        .expects(*)
        .returningZ(List(client))

      val currentTime = System.currentTimeMillis()
      DateTimeUtils.setCurrentMillisFixed(currentTime)

      (BillingTestData.priceServiceChooserTest
        .getCallPrice(
          _: Client,
          _: Option[Offer],
          _: Option[Section],
          _: Option[CallId],
          _: DateTime
        ))
        .expects(client, None, None, None, now())
        .returningZ(50000)

      billingCampaignService
        .getCallInfo(1)
        .success
        .value shouldBe CallInfo(50000, 0)
    }

    "return call info with zero deposit coefficient when vos marks found for sverdlovsk client" in {
      val sverdlovskClient =
        client.copy(regionId = RegSverdlovsk)

      (clientDao.get _)
        .expects(*)
        .returningZ(List(sverdlovskClient))

      val currentTime = System.currentTimeMillis()
      DateTimeUtils.setCurrentMillisFixed(currentTime)

      (BillingTestData.priceServiceChooserTest
        .getCallPrice(
          _: Client,
          _: Option[Offer],
          _: Option[Section],
          _: Option[CallId],
          _: DateTime
        ))
        .expects(sverdlovskClient, None, None, None, now())
        .returningZ(50000)

      billingCampaignService
        .getCallInfo(1)
        .success
        .value shouldBe CallInfo(50000, 0)
    }

    "throw exception when calls aren't available for client" in {
      (detailedClientSource.resolve _)
        .expects(*, *)
        .anyNumberOfTimes()
        .returningT(Some(ClientDetails))

      (clientDao.get _)
        .expects(*)
        .returningZ(List(client.copy(paidCallsAvailable = false)))

      billingCampaignService
        .getCallInfo(1L)
        .failure
        .exception shouldBe a[PaidCallDisabledException]
    }

    "throw exception when client not found" in {
      (detailedClientSource.resolve _)
        .expects(*, *)
        .anyNumberOfTimes()
        .returningT(Some(ClientDetails))
      (clientDao.get _)
        .expects(*)
        .returningZ(Nil)

      billingCampaignService
        .getCallInfo(1L)
        .failure
        .exception shouldBe a[ClientNotFoundException]
    }
  }

  "BillingCampaignService.calculateMatchApplicationProductCost" should {

    "return price of match-application product for moscow client" in {
      val client = BillingTestData.client.copy(regionId = RegMoscow)
      (clientDao.get _)
        .expects(*)
        .returningZ(List(client))

      val priceRequest =
        PriceRequest(
          PaidCallClientOffer(
            OfferCategories.Cars,
            Section.NEW,
            mark = None,
            model = None
          ),
          PaidCallClientContext(
            client.regionId,
            List("audi", "citroen"),
            hasPriorityPlacement = true
          ),
          ProductId.MatchApplicationCarsNew,
          today()
        )

      val product = ProductId.MatchApplicationCarsNew

      (priceRequestCreator
        .forMatchApplication(
          _: ProductId.MatchApplicationProduct,
          _: Client,
          _: Option[DateTime]
        ))
        .expects(product, client, None)
        .returningZ(priceRequest)

      val priceResponse =
        PriceResponse(
          points = List(
            MoishaPoint(MoishaProduct("match-application:cars:new", 50000))
          )
        )

      (moishaClient
        .estimate(_: PriceRequest))
        .expects(priceRequest)
        .returningF(priceResponse)

      billingCampaignService
        .calculateMatchApplicationProductCost(product, client.clientId)
        .success
        .value shouldBe 50000
    }

    "return 400 for regional client" in {
      (clientDao.get _)
        .expects(*)
        .returningZ(List(client.copy(regionId = RegSverdlovsk)))

      billingCampaignService
        .calculateMatchApplicationProductCost(
          ProductId.MatchApplicationCarsNew,
          client.clientId
        )
        .failure
        .exception shouldBe a[MatchApplicationUnavailable]
    }
  }

  private def mockClientResolve() {
    (detailedClientSource.unsafeResolve _)
      .expects(*, *)
      .anyNumberOfTimes()
      .returningZ(ClientDetails)

    (detailedClientSource.resolve _)
      .expects(*, *)
      .anyNumberOfTimes()
      .returningT(Some(ClientDetails))
  }
}
