package ru.auto.salesman.service.impl

import org.joda.time.{DateTime, Minutes}
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category.{CARS, MOTO, TRUCKS}
import ru.auto.api.ApiOfferModel.Section.{NEW, USED}
import ru.auto.salesman.dao.ClientDao.ForId
import ru.auto.salesman.dao._
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.AdsRequestTypes.CarsUsed
import ru.auto.salesman.model.OfferCategories._
import ru.auto.salesman.model.ProductId._
import ru.auto.salesman.model._
import ru.auto.salesman.model.payment_model.PaymentModelChecker
import ru.auto.salesman.model.payment_model.PlacementPaymentModel.{
  Calls,
  Quota,
  Single,
  SingleWithCalls
}
import ru.auto.salesman.service._
import ru.auto.salesman.service.client.ClientServiceImpl
import ru.auto.salesman.service.impl.CampaignShowcaseImpl.callCampaign
import ru.auto.salesman.service.impl.CampaignShowcaseImplSpec._
import ru.auto.salesman.test.service.payment_model.TestPaymentModelFactory
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.billing.Model.InactiveReason

class CampaignShowcaseImplSpec extends BaseSpec {

  private val quotaRequestService = mock[QuotaRequestService]
  private val quotaService = mock[QuotaService]
  private val adsRequestDao = mock[AdsRequestDao]
  private val clientDao = mock[ClientDao]
  private val balanceClientDao = mock[BalanceClientDao]
  private val clientService = new ClientServiceImpl(clientDao)
  private val billingService = mock[BillingService]
  private val paymentModelChecker = mock[PaymentModelChecker]

  private val paymentModelFactory =
    TestPaymentModelFactory.withMockRegions(Set(RegionWithSingleWithCalls))

  implicit private val rc: RequestContext = AutomatedContext("test")

  private val campaignShowcase = new CampaignShowcaseImpl(
    quotaRequestService,
    quotaService,
    adsRequestDao,
    clientService,
    balanceClientDao,
    billingService,
    paymentModelFactory,
    paymentModelChecker
  )

  "resolve()" should {

    "resolve single cars:used campaign" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set(CarsUsed),
        paidCallsAvailableGen = false,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      forAll(clientGen) { client =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(List(AdsRequestDao.Record(clientId, CarsUsed)))
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe Set(
          Campaign(
            Single(CarsUsed),
            "cars:used",
            CARS,
            subcategories = Set(),
            Set(USED),
            size = Int.MaxValue,
            enabled = true
          )
        )
      }
    }

    "not resolve single cars:used campaign if no such single payment" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set(AdsRequestTypes.Commercial),
        paidCallsAvailableGen = false,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      forAll(clientGen) { client =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(List(AdsRequestDao.Record(clientId, CarsUsed)))
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe Set()
      }
    }

    "resolve quota cars:used campaign when SingleWithCalls disabled " in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = false,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      val carsUsedQuotaRequestGen = quotaRequestGen(QuotaPlacementCarsUsed)
      forAll(clientGen, carsUsedQuotaRequestGen) { (client, quotaRequest) =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(Nil)
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(List(quotaRequest))
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe Set(
          Campaign(
            Quota,
            "quota:placement:cars:used",
            CARS,
            subcategories = Set(),
            Set(USED),
            quotaRequest.settings.size,
            enabled = true
          )
        )
      }
    }

    "resolve paid only quota cars:used campaign" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = false,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      forAll(clientGen, storedQuotaGen(QuotaPlacementCarsUsed)) { (client, quota) =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(Nil)
        (quotaService
          .get(_: QuotaDao.Filter)(_: RequestContext))
          .expects(likeActiveQuota(clientId, testStart), rc)
          .returningT(List(quota))
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(true))
          .success
          .value shouldBe Set(
          Campaign(
            Quota,
            "quota:placement:cars:used",
            CARS,
            subcategories = Set(),
            Set(USED),
            quota.size,
            enabled = true
          )
        )
      }
    }

    "resolve active SingleWithCalls campaign when enabled for dealer" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true,
        regionIdGen = RegionWithSingleWithCalls
      )
      val activeCampaignGen = campaignHeaderGen(inactiveReasonGen = None)
      forAll(
        clientGen,
        balanceRecordGen,
        activeCampaignGen,
        Gen.posNum[BalanceClientId]
      ) { (client, baseBalanceClient, campaign, balanceAgencyId) =>
        val balanceClient =
          baseBalanceClient.copy(balanceAgencyId = Some(balanceAgencyId))
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _)
          .expects(clientId)
          .returningZ(Some(balanceClient))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(List())
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        (billingService.getCallCampaign _)
          .expects(DetailedClient(client, balanceClient))
          .returningZ(None)
        (billingService.getProductCampaign _)
          .expects(
            DetailedClient(client, balanceClient),
            ProductId.CallCarsUsed
          )
          .returningZ(Some(campaign))
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(true)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe Set(
          Campaign(
            SingleWithCalls,
            CallCarsUsed.toString,
            CARS,
            subcategories = Set(),
            Set(USED),
            Int.MaxValue,
            enabled = true
          )
        )
      }
    }

    "resolve active SingleWithCalls campaign when enabled for dealer [even ads-request]" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true,
        regionIdGen = RegionWithSingleWithCalls
      )
      val activeCampaignGen = campaignHeaderGen(inactiveReasonGen = None)
      forAll(
        clientGen,
        balanceRecordGen,
        activeCampaignGen,
        Gen.posNum[BalanceClientId]
      ) { (client, baseBalanceClient, campaign, balanceAgencyId) =>
        val balanceClient =
          baseBalanceClient.copy(balanceAgencyId = Some(balanceAgencyId))
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _)
          .expects(clientId)
          .returningZ(Some(balanceClient))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(List(AdsRequestDao.Record(clientId, CarsUsed)))
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        (billingService.getCallCampaign _)
          .expects(DetailedClient(client, balanceClient))
          .returningZ(None)
        (billingService.getProductCampaign _)
          .expects(
            DetailedClient(client, balanceClient),
            ProductId.CallCarsUsed
          )
          .returningZ(Some(campaign))
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(true)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe Set(
          Campaign(
            SingleWithCalls,
            CallCarsUsed.toString,
            CARS,
            subcategories = Set(),
            Set(USED),
            Int.MaxValue,
            enabled = true
          )
        )
      }
    }

    "resolve active SingleWithCalls campaign when enabled for dealer and no balance data" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true,
        regionIdGen = RegionWithSingleWithCalls
      )
      val activeCampaignGen = campaignHeaderGen(inactiveReasonGen = None)
      forAll(
        clientGen,
        activeCampaignGen,
        Gen.posNum[BalanceClientId]
      ) { (client, campaign, balanceAgencyId) =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _)
          .expects(clientId)
          .returningZ(None)
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(List())
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(true)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe Set()
      }
    }

    "not resolve manually disabled SingleWithCalls campaign" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true
      )
      val inactiveCampaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.MANUALLY_DISABLED)
      )
      forAll(clientGen, balanceRecordGen, inactiveCampaignGen) {
        (client, balanceClient, campaign) =>
          val testStart = now()
          import client.clientId
          (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
          (adsRequestDao.list _)
            .expects(AdsRequestDao.Filter.ForClient(clientId))
            .returningT(List(AdsRequestDao.Record(clientId, CarsUsed)))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          (quotaRequestService
            .get(_: QuotaRequestDao.Filter)(_: RequestContext))
            .expects(likeActualQuotaRequest(clientId, testStart), rc)
            .returningT(Nil)
          (billingService.getCallCampaign _)
            .expects(DetailedClient(client, balanceClient))
            .returningZ(None)
          (billingService.getProductCampaign _)
            .expects(
              DetailedClient(client, balanceClient),
              ProductId.CallCarsUsed
            )
            .returningZ(Some(campaign))
          (paymentModelChecker.singleWithCallsEnabledInRegion _)
            .expects(client.regionId)
            .returningZ(true)
          campaignShowcase
            .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
            .success
            .value shouldBe Set()
      }
    }

    "resolve inactive due to not manually_disabled reason SingleWithCalls campaign" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true
      )
      val activeCampaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.NO_ENOUGH_FUNDS)
      )
      forAll(clientGen, balanceRecordGen, activeCampaignGen) {
        (client, balanceClient, campaign) =>
          val testStart = now()
          import client.clientId
          (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          (adsRequestDao.list _)
            .expects(AdsRequestDao.Filter.ForClient(clientId))
            .returningT(List(AdsRequestDao.Record(clientId, CarsUsed)))
          (quotaRequestService
            .get(_: QuotaRequestDao.Filter)(_: RequestContext))
            .expects(likeActualQuotaRequest(clientId, testStart), rc)
            .returningT(Nil)
          (billingService.getCallCampaign _)
            .expects(DetailedClient(client, balanceClient))
            .returningZ(None)
          (billingService.getProductCampaign _)
            .expects(
              DetailedClient(client, balanceClient),
              ProductId.CallCarsUsed
            )
            .returningZ(Some(campaign))
          (paymentModelChecker.singleWithCallsEnabledInRegion _)
            .expects(client.regionId)
            .returningZ(true)
          campaignShowcase
            .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
            .success
            .value shouldBe Set(
            Campaign(
              SingleWithCalls,
              CallCarsUsed.toString,
              CARS,
              subcategories = Set(),
              Set(USED),
              Int.MaxValue,
              enabled = true
            )
          )
      }
    }

    "resolve call campaign" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      val activeCampaignGen = campaignHeaderGen(inactiveReasonGen = None)
      forAll(
        clientGen,
        balanceRecordGen,
        activeCampaignGen,
        Gen.posNum[BalanceClientId]
      ) { (client, baseBalanceClient, campaign, balanceAgencyId) =>
        val balanceClient =
          baseBalanceClient.copy(balanceAgencyId = Some(balanceAgencyId))
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _)
          .expects(clientId)
          .returningZ(Some(balanceClient))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(Nil)
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        (billingService.getCallCampaign _)
          .expects(DetailedClient(client, balanceClient))
          .returningZ(Some(campaign))
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe Set(
          Campaign(
            Calls,
            "call",
            CARS,
            subcategories = Set(),
            Set(NEW),
            Int.MaxValue,
            enabled = true
          )
        )
      }
    }

    "resolve call campaign for client without balance data" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      val activeCampaignGen = campaignHeaderGen(inactiveReasonGen = None)
      forAll(clientGen, activeCampaignGen) { (client, campaign) =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (balanceClientDao.get _).expects(clientId).returningZ(None)
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(Nil)
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe empty
      }
    }

    "not resolve manually disabled call campaign" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      val inactiveCampaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.MANUALLY_DISABLED)
      )
      forAll(clientGen, balanceRecordGen, inactiveCampaignGen) {
        (client, balanceClient, campaign) =>
          val testStart = now()
          import client.clientId
          (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          (paymentModelChecker.singleWithCallsEnabledInRegion _)
            .expects(client.regionId)
            .returningZ(false)
          (adsRequestDao.list _)
            .expects(AdsRequestDao.Filter.ForClient(clientId))
            .returningT(Nil)
          (quotaRequestService
            .get(_: QuotaRequestDao.Filter)(_: RequestContext))
            .expects(likeActualQuotaRequest(clientId, testStart), rc)
            .returningT(Nil)
          (billingService.getCallCampaign _)
            .expects(DetailedClient(client, balanceClient))
            .returningZ(Some(campaign))
          campaignShowcase
            .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
            .success
            .value shouldBe Set()
      }
    }

    "resolve inactive due to not manually_disabled reason call campaign" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      val activeCampaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.NO_ENOUGH_FUNDS)
      )
      forAll(clientGen, balanceRecordGen, activeCampaignGen) {
        (client, balanceClient, campaign) =>
          val testStart = now()
          import client.clientId
          (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          (paymentModelChecker.singleWithCallsEnabledInRegion _)
            .expects(client.regionId)
            .returningZ(false)
          (adsRequestDao.list _)
            .expects(AdsRequestDao.Filter.ForClient(clientId))
            .returningT(Nil)
          (quotaRequestService
            .get(_: QuotaRequestDao.Filter)(_: RequestContext))
            .expects(likeActualQuotaRequest(clientId, testStart), rc)
            .returningT(Nil)
          (billingService.getCallCampaign _)
            .expects(DetailedClient(client, balanceClient))
            .returningZ(Some(campaign))
          campaignShowcase
            .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
            .success
            .value shouldBe Set(
            Campaign(
              Calls,
              "call",
              CARS,
              subcategories = Set(),
              Set(NEW),
              Int.MaxValue,
              enabled = true
            )
          )
      }
    }

    "resolve all disabled campaigns without Single and quota:cars:used when SingleWithCalls enabled" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true,
        regionIdGen = RegionWithSingleWithCalls
      )
      val inactiveCampaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.MANUALLY_DISABLED)
      )
      forAll(clientGen, balanceRecordGen, inactiveCampaignGen) {
        (client, balanceClient, campaign) =>
          val testStart = now()
          import client.clientId
          (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
          (adsRequestDao.list _)
            .expects(AdsRequestDao.Filter.ForClient(clientId))
            .returningT(List())
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          (quotaRequestService
            .get(_: QuotaRequestDao.Filter)(_: RequestContext))
            .expects(likeActualQuotaRequest(clientId, testStart), rc)
            .returningT(Nil)
          (billingService.getProductCampaign _)
            .expects(
              DetailedClient(client, balanceClient),
              ProductId.CallCarsUsed
            )
            .returningZ(Some(campaign))
          (paymentModelChecker.singleWithCallsEnabledInRegion _)
            .expects(client.regionId)
            .returningZ(true)
          (billingService.getCallCampaign _)
            .expects(DetailedClient(client, balanceClient))
            .returningZ(Some(campaign))
          campaignShowcase
            .resolve(clientId, IncludeDisabled(true), PaidOnly(false))
            .success
            .value shouldBe Set(
            Campaign(
              SingleWithCalls,
              CallCarsUsed.toString,
              CARS,
              subcategories = Set(),
              Set(USED),
              Int.MaxValue,
              enabled = false
            ),
            Campaign(
              Calls,
              "call",
              CARS,
              subcategories = Set(),
              Set(NEW),
              Int.MaxValue,
              enabled = false
            ),
            Campaign(
              Quota,
              "quota:placement:moto",
              MOTO,
              subcategories = Set(
                Motorcycle,
                Atv,
                Snowmobile,
                Carting,
                Amphibious,
                Baggi,
                Scooters
              ),
              Set(NEW, USED),
              Int.MaxValue,
              enabled = false
            ),
            Campaign(
              Quota,
              "quota:placement:commercial",
              TRUCKS,
              subcategories = Set(
                Trailer,
                Lcv,
                Trucks,
                Artic,
                Bus,
                Swapbody,
                Agricultural,
                Construction,
                Autoloader,
                Crane,
                Dredge,
                Bulldozer,
                CraneHydraulics,
                Municipal
              ),
              Set(NEW, USED),
              Int.MaxValue,
              enabled = false
            )
          )
      }
    }

    "resolve all disabled campaigns without SingleWithCalls" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = true
      )
      val inactiveCampaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.MANUALLY_DISABLED)
      )
      forAll(clientGen, balanceRecordGen, inactiveCampaignGen) {
        (client, balanceClient, campaign) =>
          val testStart = now()
          import client.clientId
          (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
          (balanceClientDao.get _)
            .expects(clientId)
            .returningZ(Some(balanceClient))
          (adsRequestDao.list _)
            .expects(AdsRequestDao.Filter.ForClient(clientId))
            .returningT(Nil)
          (quotaRequestService
            .get(_: QuotaRequestDao.Filter)(_: RequestContext))
            .expects(likeActualQuotaRequest(clientId, testStart), rc)
            .returningT(Nil)
          (paymentModelChecker.singleWithCallsEnabledInRegion _)
            .expects(client.regionId)
            .returningZ(false)
          (billingService.getCallCampaign _)
            .expects(DetailedClient(client, balanceClient))
            .returningZ(Some(campaign))
          campaignShowcase
            .resolve(clientId, IncludeDisabled(true), PaidOnly(false))
            .success
            .value should contain theSameElementsAs Set(
            Campaign(
              Calls,
              "call",
              CARS,
              subcategories = Set(),
              Set(NEW),
              Int.MaxValue,
              enabled = false
            ),
            Campaign(
              Quota,
              "quota:placement:cars:used",
              CARS,
              subcategories = Set(),
              Set(USED),
              Int.MaxValue,
              enabled = false
            ),
            Campaign(
              Quota,
              "quota:placement:moto",
              MOTO,
              subcategories = Set(
                Motorcycle,
                Atv,
                Snowmobile,
                Carting,
                Amphibious,
                Baggi,
                Scooters
              ),
              Set(NEW, USED),
              Int.MaxValue,
              enabled = false
            ),
            Campaign(
              Quota,
              "quota:placement:commercial",
              TRUCKS,
              subcategories = Set(
                Trailer,
                Lcv,
                Trucks,
                Artic,
                Bus,
                Swapbody,
                Agricultural,
                Construction,
                Autoloader,
                Crane,
                Dredge,
                Bulldozer,
                CraneHydraulics,
                Municipal
              ),
              Set(NEW, USED),
              Int.MaxValue,
              enabled = false
            )
          )
      }
    }

    "resolve enabled moto quota campaign, and disabled quotas when SingleWithCalls disabled" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = false,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      val motoQuotaRequestGen =
        quotaRequestGen(QuotaPlacementMoto, quotaSizeGen = 10)
      forAll(clientGen, motoQuotaRequestGen) { (client, quotaRequest) =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(Nil)
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(List(quotaRequest))
        campaignShowcase
          .resolve(clientId, IncludeDisabled(true), PaidOnly(false))
          .success
          .value shouldBe Set(
          Campaign(
            Quota,
            "quota:placement:moto",
            MOTO,
            subcategories = Set(
              Motorcycle,
              Atv,
              Snowmobile,
              Carting,
              Amphibious,
              Baggi,
              Scooters
            ),
            Set(NEW, USED),
            10,
            enabled = true
          ),
          Campaign(
            Quota,
            "quota:placement:cars:new",
            CARS,
            subcategories = Set(),
            Set(NEW),
            Int.MaxValue,
            enabled = false
          ),
          Campaign(
            Quota,
            "quota:placement:cars:used",
            CARS,
            subcategories = Set(),
            Set(USED),
            Int.MaxValue,
            enabled = false
          ),
          Campaign(
            Quota,
            "quota:placement:commercial",
            TRUCKS,
            subcategories = Set(
              Trailer,
              Lcv,
              Trucks,
              Artic,
              Bus,
              Swapbody,
              Agricultural,
              Construction,
              Autoloader,
              Crane,
              Dredge,
              Bulldozer,
              CraneHydraulics,
              Municipal
            ),
            Set(NEW, USED),
            Int.MaxValue,
            enabled = false
          )
        )
      }
    }

    "dont get balance info if no Calls or SingleWithCalls company available" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set.empty[AdsRequestType],
        paidCallsAvailableGen = false,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      val activeCampaignGen = campaignHeaderGen(
        inactiveReasonGen = Some(InactiveReason.MANUALLY_DISABLED)
      )
      forAll(clientGen, activeCampaignGen) { (client, campaign) =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(Nil)
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(false), PaidOnly(false))
          .success
          .value shouldBe Set()
      }
    }

    "resolve one enabled single campaign, and disabled quotas" in {
      val clientGen = clientRecordGen(
        singlePaymentsGen = Set(CarsUsed),
        paidCallsAvailableGen = false,
        regionIdGen = RegionWithoutSingleWithCalls
      )
      forAll(clientGen) { client =>
        val testStart = now()
        import client.clientId
        (clientDao.get _).expects(ForId(clientId)).returningZ(List(client))
        (adsRequestDao.list _)
          .expects(AdsRequestDao.Filter.ForClient(clientId))
          .returningT(List(AdsRequestDao.Record(clientId, CarsUsed)))
        (quotaRequestService
          .get(_: QuotaRequestDao.Filter)(_: RequestContext))
          .expects(likeActualQuotaRequest(clientId, testStart), rc)
          .returningT(Nil)
        (paymentModelChecker.singleWithCallsEnabledInRegion _)
          .expects(client.regionId)
          .returningZ(false)
        campaignShowcase
          .resolve(clientId, IncludeDisabled(true), PaidOnly(false))
          .success
          .value shouldBe Set(
          Campaign(
            Single(CarsUsed),
            "cars:used",
            CARS,
            subcategories = Set(),
            Set(USED),
            Int.MaxValue,
            enabled = true
          ),
          Campaign(
            Quota,
            "quota:placement:moto",
            MOTO,
            subcategories = Set(
              Motorcycle,
              Atv,
              Snowmobile,
              Carting,
              Amphibious,
              Baggi,
              Scooters
            ),
            Set(NEW, USED),
            Int.MaxValue,
            enabled = false
          ),
          Campaign(
            Quota,
            "quota:placement:cars:new",
            CARS,
            subcategories = Set(),
            Set(NEW),
            Int.MaxValue,
            enabled = false
          ),
          Campaign(
            Quota,
            "quota:placement:commercial",
            TRUCKS,
            subcategories = Set(
              Trailer,
              Lcv,
              Trucks,
              Artic,
              Bus,
              Swapbody,
              Agricultural,
              Construction,
              Autoloader,
              Crane,
              Dredge,
              Bulldozer,
              CraneHydraulics,
              Municipal
            ),
            Set(NEW, USED),
            Int.MaxValue,
            enabled = false
          )
        )
      }
    }
  }

  "call campaign" should {

    "be correct" in {
      callCampaign.paymentModel shouldBe Calls
      callCampaign.tag shouldBe "call"
      callCampaign.category shouldBe CARS
      callCampaign.subcategories shouldBe Set()
      callCampaign.section shouldBe Set(NEW)
      callCampaign.size shouldBe Int.MaxValue
    }
  }

  private def likeActualQuotaRequest(clientId: ClientId, now: DateTime) =
    argThat { filter: QuotaRequestDao.Filter =>
      filter match {
        case QuotaRequestDao.Actual(
              `clientId`,
              time,
              false,
              QuotaEntities.Dealer
            ) =>
          now ~= time
        case _ =>
          false
      }
    }

  private def likeActiveQuota(clientId: ClientId, now: DateTime) =
    argThat { filter: QuotaDao.Filter =>
      filter match {
        case QuotaDao.Active(`clientId`, time, QuotaEntities.Dealer) =>
          now ~= time
        case _ =>
          false
      }
    }
}

object CampaignShowcaseImplSpec {

  val RegionWithSingleWithCalls = RegionId(3228)
  val RegionWithoutSingleWithCalls = RegionId(3229)

  implicit class RichDateTime(private val dt: DateTime) extends AnyVal {

    def ~=(other: DateTime): Boolean =
      math.abs(Minutes.minutesBetween(dt, other).getMinutes) < 2
  }
}
