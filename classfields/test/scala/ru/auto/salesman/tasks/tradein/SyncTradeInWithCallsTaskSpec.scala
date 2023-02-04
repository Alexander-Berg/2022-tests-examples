package ru.auto.salesman.tasks.tradein

import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.billing.{BootstrapClient, RequestContext => BillingRequestContext}
import ru.auto.salesman.client.billing.BillingCampaignClient
import ru.auto.salesman.client.billing.model.CampaignPatch
import ru.auto.salesman.dao.ClientDao
import ru.auto.salesman.model._
import ru.auto.salesman.model.billing.VsBilling.customProduct
import ru.auto.salesman.model.payment_model.PlacementPaymentModel
import ru.auto.salesman.service.{BillingService, CampaignShowcase, DetailedClientSource}
import ru.auto.salesman.tasks.tradein.SyncTradeInWithCallsTaskSpec._
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.{BootstrapCampaignSource, BootstrapOrderSource}

class SyncTradeInWithCallsTaskSpec extends BaseSpec with BeforeAndAfter {

  val clientDao = mock[ClientDao]
  val clientSource = mock[DetailedClientSource]
  val billingService = mock[BillingService]
  val billingBootstrapClient = mock[BootstrapClient]
  val billingCampaignClient = mock[BillingCampaignClient]
  val campaignShowcase = mock[CampaignShowcase]

  val task = new SyncTradeInWithCallsTask(
    clientDao,
    clientSource,
    billingService,
    billingBootstrapClient,
    billingCampaignClient,
    campaignShowcase,
    clientIds = None,
    dryRun = false
  )

  private val getClientMock = toMockFunction2 {
    clientSource.unsafeResolve(_: ClientId, _: Boolean)
  }

  private val getClientTariffsMock = toMockFunction3 {
    campaignShowcase.resolve(_: ClientId, _: IncludeDisabled, _: PaidOnly)
  }

  private val getOrCreateOrderMock = toMockFunction2 {
    billingBootstrapClient.order(_: BootstrapOrderSource)(
      _: BillingRequestContext
    )
  }

  private val getOrCreateCampaignMock = toMockFunction2 {
    billingBootstrapClient.campaign(_: BootstrapCampaignSource)(
      _: BillingRequestContext
    )
  }

  private val getProductCampaignMock = toMockFunction2 {
    billingService.getProductCampaign(_: DetailedClient, _: ProductId)
  }

  private val updateCampaignMock = toMockFunction4 {
    billingCampaignClient.updateCampaign(
      _: BalanceClientId,
      _: Option[Long],
      _: String,
      _: CampaignPatch
    )
  }

  "SyncTradeInWithCallsTask.processClient()" should {
    "enable trade-in campaign if calls campaign is enabled" in {
      val tariffs = Set(
        TestClientTariff(
          Category.CARS,
          Section.NEW,
          PlacementPaymentModel.Calls,
          enabled = true
        ),
        TestClientTariff(
          Category.CARS,
          Section.USED,
          PlacementPaymentModel.Quota,
          enabled = true
        )
      )

      getClientTariffsMock
        .expects(*, *, *)
        .returningZ(tariffs)

      getClientMock
        .expects(*, *)
        .returningZ(TestClient)

      getOrCreateOrderMock
        .expects(*, *)
        .returningT(Model.Order.getDefaultInstance)

      getOrCreateCampaignMock.expects(*, *).returningT {
        TestProductCampaign(
          "test-tradeIn-campaign",
          ProductId.TradeInRequestCarsNew,
          enabled = false
        )
      }

      updateCampaignMock
        .expects(
          TestBalanceClientId,
          TestBalanceAgencyId,
          "test-tradeIn-campaign",
          CampaignPatch(TestAccountId, enabled = Some(true))
        )
        .returningZ {
          TestProductCampaign(
            "test-tradeIn-campaign",
            ProductId.TradeInRequestCarsNew,
            enabled = true
          )
        }

      task.processClient(TestClientId).success.value shouldBe (())
    }

    "do nothing on non-calls payment model for cars new" in {
      val tariffs = Set(
        TestClientTariff(
          Category.CARS,
          Section.NEW,
          PlacementPaymentModel.Quota,
          enabled = true
        )
      )

      getClientTariffsMock
        .expects(*, *, *)
        .returningZ(tariffs)

      task.processClient(TestClientId).success.value shouldBe (())
    }

    "disable trade-in campaign if client does not have cars new tariff" in {
      val tariffs = Set(
        TestClientTariff(
          Category.CARS,
          Section.USED,
          PlacementPaymentModel.Quota,
          enabled = true
        )
      )

      getClientTariffsMock
        .expects(*, *, *)
        .returningZ(tariffs)

      getClientMock
        .expects(*, *)
        .returningZ(TestClient)

      getProductCampaignMock
        .expects(*, *)
        .returningZ(
          Some {
            TestProductCampaign(
              "test-tradeIn-campaign",
              ProductId.TradeInRequestCarsNew,
              enabled = true
            )
          }
        )

      updateCampaignMock
        .expects(
          TestBalanceClientId,
          TestBalanceAgencyId,
          "test-tradeIn-campaign",
          CampaignPatch(TestAccountId, enabled = Some(false))
        )
        .returningZ {
          TestProductCampaign(
            "test-tradeIn-campaign",
            ProductId.TradeInRequestCarsNew,
            enabled = true
          )
        }

      task.processClient(TestClientId).success.value shouldBe (())
    }

    "do nothing if client does not have cars new tariff and trade-in campaign" in {
      val tariffs = Set(
        TestClientTariff(
          Category.CARS,
          Section.USED,
          PlacementPaymentModel.Quota,
          enabled = true
        )
      )

      getClientTariffsMock
        .expects(*, *, *)
        .returningZ(tariffs)

      getClientMock
        .expects(*, *)
        .returningZ(TestClient)

      getProductCampaignMock
        .expects(*, *)
        .returningZ(None)

      task.processClient(TestClientId).success.value shouldBe (())
    }
  }

  "setCampaignIsEnabled()" should {
    "do nothing if same campaign passed" in {
      val campaign =
        TestProductCampaign(
          "test-tradeIn-campaign",
          ProductId.TradeInRequestCarsNew,
          enabled = true
        )

      task
        .setCampaignIsEnabled(TestClient, Some(campaign), enabled = true)
        .success
        .value shouldBe (())
    }

    "do nothing if no campaign passed" in {
      task
        .setCampaignIsEnabled(TestClient, campaign = None, enabled = true)
        .success
        .value shouldBe (())
    }
  }

  "getEnabledCarsNewTariff()" should {
    "return cars new campaign" in {
      val tariffs = Set(
        TestClientTariff(
          Category.CARS,
          Section.NEW,
          PlacementPaymentModel.Calls,
          enabled = true
        ),
        TestClientTariff(
          Category.CARS,
          Section.USED,
          PlacementPaymentModel.Quota,
          enabled = true
        )
      )

      getClientTariffsMock
        .expects(*, *, *)
        .returningZ(tariffs)

      task.getEnabledCarsNewTariff(TestClientId).success.value shouldBe Some {
        TestClientTariff(
          Category.CARS,
          Section.NEW,
          PlacementPaymentModel.Calls,
          enabled = true
        )
      }
    }

    "return None if cars new tariff is disabled" in {
      val tariffs = Set(
        TestClientTariff(
          Category.CARS,
          Section.NEW,
          PlacementPaymentModel.Calls,
          enabled = false
        ),
        TestClientTariff(
          Category.CARS,
          Section.USED,
          PlacementPaymentModel.Quota,
          enabled = true
        )
      )

      getClientTariffsMock
        .expects(*, *, *)
        .returningZ(tariffs)

      task.getEnabledCarsNewTariff(TestClientId).success.value shouldBe None
    }

    "return None if no cars new tariff" in {
      val tariffs = Set(
        TestClientTariff(
          Category.CARS,
          Section.USED,
          PlacementPaymentModel.Quota,
          enabled = true
        )
      )

      getClientTariffsMock
        .expects(*, *, *)
        .returningZ(tariffs)

      task.getEnabledCarsNewTariff(TestClientId).success.value shouldBe None
    }
  }
}

object SyncTradeInWithCallsTaskSpec {

  val TestClientId: ClientId = 20101
  val TestAgencyId: Option[AgencyId] = None
  val TestBalanceClientId: BalanceClientId = 2010100
  val TestBalanceAgencyId: Option[BalanceClientId] = None
  val TestAccountId: AccountId = 1

  val TestClient = DetailedClient(
    TestClientId,
    TestAgencyId,
    TestBalanceClientId,
    TestBalanceAgencyId,
    None,
    None,
    RegionId(1L),
    CityId(1123L),
    TestAccountId,
    isActive = true,
    firstModerated = true,
    singlePayment = Set(AdsRequestTypes.CarsUsed)
  )

  def TestProductCampaign(
      campaignId: String,
      product: ProductId,
      enabled: Boolean
  ) = {
    val settings = Model.CampaignSettings
      .newBuilder()
      .setIsEnabled(enabled)
      .buildPartial()

    Model.CampaignHeader
      .newBuilder()
      .setId(campaignId)
      .setSettings(settings)
      .setProduct(customProduct(product))
      .buildPartial()
  }

  def TestClientTariff(
      category: Category,
      section: Section,
      paymentModel: PlacementPaymentModel,
      enabled: Boolean
  ) =
    Campaign(
      paymentModel,
      tag = s"$category-$section",
      category,
      subcategories = Set(),
      Set(section),
      size = 99999,
      enabled
    )

}
