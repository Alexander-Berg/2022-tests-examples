package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.billing.{BootstrapClient, RequestContext => BillingRequestContext}
import ru.auto.salesman.client.billing.model.BillingProductType
import ru.auto.salesman.client.{GuardianClient, PromocoderClient}
import ru.auto.salesman.environment.{now, today}
import ru.auto.salesman.model.FeatureCount.Items
import ru.auto.salesman.model.billing.VsBilling.{
  bootstrapCampaignSource,
  customProduct,
  customerId
}
import ru.auto.salesman.model.{
  AccountId,
  AdsRequestTypes,
  AgencyId,
  BalanceClientId,
  CityId,
  ClientId,
  DetailedClient,
  FeatureCount,
  FeatureInstance,
  FeatureOrigin,
  FeaturePayload,
  FeatureUnits,
  Funds,
  ModifiedPrice,
  OfferCategories,
  PriceModifierFeature,
  ProductDuration,
  ProductId,
  ProductTariff,
  PromocoderUser,
  RegionId,
  TransactionId,
  UserTypes
}
import ru.auto.salesman.service.BillingEventProcessor.NoRetryBillingEvent
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.service.PromocoderFeatureService.LoyaltyArgs
import ru.auto.salesman.service._
import ru.auto.salesman.service.impl.BillingEventProcessorImplSpec.{
  BillingServiceException,
  _
}
import ru.auto.salesman.service.tskv.billing.BillingEventTskvLogger
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.money.Money.Kopecks
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.billing.Model
import ru.yandex.vertis.billing.Model.BootstrapCampaignSource
import ru.yandex.vertis.util.time.DateTimeUtil

import java.util.UUID
import scala.util.control.NoStackTrace
import scala.util.{Success, Try}
import collection.JavaConverters._

class BillingEventProcessorImplSpec extends BaseSpec {

  private val holdSource = mock[HoldSource]
  private val billingBootstrapClient = mock[BootstrapClient]
  private val billingEventLogger = mock[BillingEventTskvLogger]
  private val priceEstimator = mock[PriceEstimateService]
  private val promocoderSource = mock[PromocoderSource]
  private val promocoderClient = mock[PromocoderClient]
  private val priceExtractor = mock[PriceExtractor]
  private val promocoderFeatureService = mock[PromocoderFeatureService]
  private val billingService = mock[BillingService]

  implicit private val ac: RequestContext = AutomatedContext {
    "billing-event-processor-spec"
  }

  implicit val bc: BillingRequestContext = BillingRequestContext {
    "billing-event-processor-spec"
  }

  private val processor = new BillingEventProcessorImpl(
    holdSource,
    billingBootstrapClient,
    billingEventLogger,
    priceEstimator,
    promocoderSource,
    promocoderFeatureService,
    promocoderClient,
    billingService
  )

  private val getOrCreateCampaign = toMockFunction2 {
    billingBootstrapClient.campaign(_: BootstrapCampaignSource)(
      _: BillingRequestContext
    )
  }

  private val getCampaign = toMockFunction2 {
    billingService.getCampaign(_: DetailedClient, _: BillingProductType)
  }

  private val estimatePrice = toMockFunction1 {
    priceEstimator.estimate(_: PriceEstimateService.PriceRequest)
  }

  private val getPriceExtractor = toMockFunction1 {
    priceEstimator.extractor(_: PriceEstimateService.PriceResponse)
  }

  private val extractProductInfo = toMockFunction2 {
    priceExtractor.productInfo(_: ProductId, _: DateTime)
  }

  private val getPromocoderUserFeatures = toMockFunction1 {
    promocoderSource.getFeaturesForUser
  }

  private val modifyPrice = toMockFunction6 {
    promocoderFeatureService.modifyPrice(
      _: List[FeatureInstance],
      _: ProductId,
      _: Funds,
      _: Long,
      _: Option[ApiOfferModel.Offer],
      _: Option[LoyaltyArgs]
    )
  }

  private val makeHold = toMockFunction6 {
    holdSource.hold(
      _: BalanceClientId,
      _: Option[BalanceClientId],
      _: TransactionId,
      _: ProductId,
      _: AccountId,
      _: Funds
    )
  }

  private val logBillingEvent = toMockFunction2 {
    billingEventLogger.apply(_: BillingEvent, _: Map[String, String])
  }

  "process()" should {
    "work fine" in {
      val product = ProductId.TradeInRequestCarsNew

      getOrCreateCampaign
        .expects(bootstrapCampaignSource(TestClient, product), *)
        .returningT(
          TestProductCampaign(product, enabled = true, productTariff = None)
        )

      val requestId = UUID.randomUUID().toString
      val priceRequest =
        TestPriceRequest(TestClient, product, Some(requestId))
      val testPrice = 777L

      mockProductAndPrice(product, testPrice, requestId)

      val promocoderFeatures = Nil
      mockFeatures(
        product,
        testPrice,
        testPrice,
        promocoderFeatures,
        Section.NEW
      )

      mockHold(product, testPrice)

      val response =
        processor
          .process(
            TestClient,
            product,
            priceRequest,
            TestHoldId,
            productTariff = None
          )(r =>
            Try {
              r.holdId shouldBe Some(TestHoldId)
              r.price shouldBe Some(testPrice)
            }
          )
          .success
          .value

      val expectedDeadline = DateTimeUtil.now().plusDays(180)

      response.holdId shouldBe Some(TestHoldId)
      response.price shouldBe Some(testPrice)
      response.actualPrice shouldBe Some(testPrice)
      response.promocodeFeatures shouldBe List.empty
      response.clientId shouldBe Some(TestClientId)
      response.deadline should ~=(expectedDeadline)
    }

    "fail on empty product duration" in {
      val product = ProductId.TradeInRequestCarsNew

      getOrCreateCampaign
        .expects(bootstrapCampaignSource(TestClient, product), *)
        .returningT(
          TestProductCampaign(product, enabled = true, productTariff = None)
        )

      val requestId = UUID.randomUUID().toString

      val priceRequest =
        TestPriceRequest(TestClient, product, Some(requestId))
      val testPrice = 777L

      mockProductAndPrice(product, testPrice, requestId, duration = None)

      val promocoderFeatures = Nil
      mockFeatures(
        product,
        testPrice,
        testPrice,
        promocoderFeatures,
        Section.NEW
      )

      processor
        .process(
          TestClient,
          product,
          priceRequest,
          TestHoldId,
          productTariff = None
        )(r =>
          Try {
            r.holdId shouldBe Some(TestHoldId)
            r.price shouldBe Some(testPrice)
          }
        )
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }

    "use cashback and do not hold (test for CreditApplication)" in {
      val product = ProductId.CreditApplication
      val price = 777L

      getOrCreateCampaign
        .expects(bootstrapCampaignSource(TestClient, product), *)
        .returningT(
          TestProductCampaign(product, enabled = true, productTariff = None)
        )

      val priceRequest = TestProductAccessCreditPriceRequest(TestClient)

      val features = mockSuccessCashbackFlow(product, priceRequest, price)

      val response =
        processor
          .process(
            TestClient,
            product,
            priceRequest,
            TestHoldId,
            productTariff = None
          )(r =>
            Try {
              r.holdId shouldBe Some(TestHoldId)
              r.price shouldBe Some(0L)
              r.promocodeFeatures shouldBe features
            }
          )
          .success
          .value

      response.holdId shouldBe Some(TestHoldId)
      response.price shouldBe Some(0L)
      response.actualPrice shouldBe Some(price)
      response.promocodeFeatures shouldBe features
      response.clientId shouldBe Some(TestClientId)
    }

    "get campaign by tariff from billing service if tariff is set, then work fine" in {
      val product = ProductId.SingleCreditApplication
      val tariff = ProductTariff.ApplicationCreditSingleTariffCarsNew
      val price = 777L

      getCampaign
        .expects(TestClient, BillingProductType(tariff))
        .returningZ(
          Some(TestProductCampaign(product, enabled = true, Some(tariff)))
        )

      val priceRequest =
        TestProductAccessCreditPriceRequest(TestClient).copy(product = product)

      val features = mockSuccessCashbackFlow(product, priceRequest, price)

      val response =
        processor
          .process(TestClient, product, priceRequest, TestHoldId, Some(tariff))(r =>
            Try {
              r.holdId shouldBe Some(TestHoldId)
              r.price shouldBe Some(0L)
              r.promocodeFeatures shouldBe features
            }
          )
          .success
          .value

      response.holdId shouldBe Some(TestHoldId)
      response.price shouldBe Some(0L)
      response.actualPrice shouldBe Some(price)
      response.promocodeFeatures shouldBe features
      response.clientId shouldBe Some(TestClientId)
    }

    "get campaign by tariff from billing service if tariff is set, then throws NoCampaignForProductTariff if there is no campaign" in {
      val product = ProductId.SingleCreditApplication
      val tariff = ProductTariff.ApplicationCreditSingleTariffCarsNew

      getCampaign
        .expects(TestClient, BillingProductType(tariff))
        .returningZ(None)

      val priceRequest = TestProductAccessCreditPriceRequest(TestClient)

      processor
        .process(TestClient, product, priceRequest, TestHoldId, Some(tariff))(_ =>
          Success(())
        )
        .failure
        .exception shouldBe an[NoRetryBillingEvent]
    }

    "get campaign by tariff from billing service if tariff is set, then throws NoRetryBillingEvent if campaign not active" in {
      val product = ProductId.SingleCreditApplication
      val tariff = ProductTariff.ApplicationCreditSingleTariffCarsNew

      getCampaign
        .expects(TestClient, BillingProductType(tariff))
        .returningZ(
          Some(
            TestProductCampaign(product, enabled = false, productTariff = None)
          )
        )

      val priceRequest = TestProductAccessCreditPriceRequest(TestClient)

      processor
        .process(TestClient, product, priceRequest, TestHoldId, Some(tariff))(_ =>
          Success(())
        )
        .failure
        .exception shouldBe an[NoRetryBillingEvent]
    }

    "get campaign by tariff from billing service if tariff is set, then throws error on other errors" in {
      val product = ProductId.SingleCreditApplication
      val tariff = ProductTariff.ApplicationCreditSingleTariffCarsNew

      getCampaign
        .expects(TestClient, BillingProductType(tariff))
        .throwingZ(BillingServiceException)

      val priceRequest = TestProductAccessCreditPriceRequest(TestClient)

      processor
        .process(TestClient, product, priceRequest, TestHoldId, Some(tariff))(_ =>
          Success(())
        )
        .failure
        .exception shouldBe a[Exception]
    }

    "modifyPrice for used cars with tariff ApplicationCreditSingleTariffCarsUsed" in {
      val product = ProductId.SingleCreditApplication
      val tariff = ProductTariff.ApplicationCreditSingleTariffCarsUsed
      val priceRequest =
        TestProductPriceRequest(TestClient, product, Some(tariff))
      val price = 777L

      val promocoderFeatures = List(TestFeature)

      getPromocoderUserFeatures
        .expects(PromocoderUser(TestClientId, UserTypes.ClientUser))
        .returningZ(promocoderFeatures)

      val modifiedPrice = ModifiedPrice(
        0,
        List(
          PriceModifierFeature(
            TestFeature,
            FeatureCount(1, FeatureUnits.Items),
            price
          )
        )
      )

      modifyPrice
        .expects(
          promocoderFeatures,
          product,
          price,
          1L,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.USED, TestClient.regionId))
        )
        .returningZ(modifiedPrice)

      processor
        .modifyPriceWithFeatures(
          TestClient,
          product,
          priceRequest.context,
          Some(tariff),
          price
        )
        .success
        .value shouldBe modifiedPrice

    }

    "modifyPrice for new cars with tariff ApplicationCreditSingleTariffCarsNew" in {
      val product = ProductId.SingleCreditApplication
      val tariff = ProductTariff.ApplicationCreditSingleTariffCarsNew
      val priceRequest =
        TestProductPriceRequest(TestClient, product, Some(tariff))
      val price = 777L

      val promocoderFeatures = List(TestFeature)

      getPromocoderUserFeatures
        .expects(PromocoderUser(TestClientId, UserTypes.ClientUser))
        .returningZ(promocoderFeatures)

      val modifiedPrice = ModifiedPrice(price)

      modifyPrice
        .expects(
          promocoderFeatures,
          product,
          price,
          1L,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.NEW, TestClient.regionId))
        )
        .returningZ(modifiedPrice)

      processor
        .modifyPriceWithFeatures(
          TestClient,
          product,
          priceRequest.context,
          Some(tariff),
          price
        )
        .success
        .value shouldBe modifiedPrice

    }

    "use cashback for ApplicationCreditSingle with tariff ApplicationCreditSingleTariffCarsUsed" in {
      val product = ProductId.SingleCreditApplication
      val tariff = ProductTariff.ApplicationCreditSingleTariffCarsUsed
      val priceRequest =
        TestProductPriceRequest(TestClient, product, Some(tariff))
      val price = 777L

      getCampaign
        .expects(TestClient, BillingProductType(tariff))
        .returningZ(
          Some(TestProductCampaign(product, enabled = true, Some(tariff)))
        )

      val features = mockSuccessCashbackFlow(product, priceRequest, price)

      val response =
        processor
          .process(TestClient, product, priceRequest, TestHoldId, Some(tariff))(r =>
            Try {
              r.holdId shouldBe Some(TestHoldId)
              r.price shouldBe Some(0L)
              r.promocodeFeatures shouldBe features
            }
          )
          .success
          .value

      response.holdId shouldBe Some(TestHoldId)
      response.price shouldBe Some(0L)
      response.actualPrice shouldBe Some(price)
      response.promocodeFeatures shouldBe features
      response.clientId shouldBe Some(TestClientId)

    }

    "use % discount and make hold on rest funds" in {
      val product = ProductId.TradeInRequestCarsNew

      getOrCreateCampaign
        .expects(bootstrapCampaignSource(TestClient, product), *)
        .returningT(
          TestProductCampaign(product, enabled = true, productTariff = None)
        )
      val requestId = UUID.randomUUID().toString
      val priceRequest = TestPriceRequest(TestClient, product, Some(requestId))

      val testPrice = 2000L
      val priceWithDiscount = 1000L

      mockProductAndPrice(product, testPrice, requestId)

      val promocoderFeatures = List(TestFeature)
      val features = mockFeatures(
        product,
        testPrice,
        priceWithDiscount = priceWithDiscount,
        features = promocoderFeatures,
        section = Section.NEW
      )

      mockHold(product, priceWithDiscount)

      val response =
        processor
          .process(
            TestClient,
            product,
            priceRequest,
            TestHoldId,
            productTariff = None
          )(r =>
            Try {
              r.holdId shouldBe Some(TestHoldId)
              r.price shouldBe Some(priceWithDiscount)
            }
          )
          .success
          .value

      val expectedDeadline = DateTimeUtil.now().plusDays(180)

      response.holdId shouldBe Some(TestHoldId)
      response.price shouldBe Some(priceWithDiscount)
      response.actualPrice shouldBe Some(testPrice)
      response.promocodeFeatures shouldBe features
      response.clientId shouldBe Some(TestClientId)
      response.deadline should ~=(expectedDeadline)
    }

    "use % discount then promocode, decrease 2 promocodes and dont make hold" in {
      val product = ProductId.TradeInRequestCarsNew

      getOrCreateCampaign
        .expects(bootstrapCampaignSource(TestClient, product), *)
        .returningT(
          TestProductCampaign(product, enabled = true, productTariff = None)
        )

      val testPrice = 2000L
      val priceWithDiscount = 0L

      val requestId = UUID.randomUUID().toString

      mockProductAndPrice(product, testPrice, requestId)

      val secondFeature = TestFeature.copy(id = TestFeature.id + "2")
      val promocoderFeatures = List(TestFeature, secondFeature)

      val features = mockFeatures(
        product,
        testPrice,
        features = promocoderFeatures,
        section = Section.NEW
      )

      logBillingEvent
        .expects(*, *)
        .returningZ(())

      val response =
        processor
          .process(
            TestClient,
            product,
            TestPriceRequest(TestClient, product, Some(requestId)),
            TestHoldId,
            productTariff = None
          )(r =>
            Try {
              r.holdId shouldBe Some(TestHoldId)
              r.price shouldBe Some(priceWithDiscount)
            }
          )
          .success
          .value

      val expectedDeadline = DateTimeUtil.now().plusDays(180)

      response.holdId shouldBe Some(TestHoldId)
      response.price shouldBe Some(priceWithDiscount)
      response.actualPrice shouldBe Some(testPrice)
      response.promocodeFeatures shouldBe features
      response.clientId shouldBe Some(TestClientId)
      response.deadline should ~=(expectedDeadline)
    }

  }

  private def mockSuccessCashbackFlow(
      product: ProductId,
      priceRequest: PriceEstimateService.PriceRequest,
      price: Funds
  ): List[PriceModifierFeature] = {
    val requestId = UUID.randomUUID().toString

    mockProductAndPrice(
      product,
      price,
      requestId = priceRequest.priceRequestId.getOrElse(requestId),
      priceRequest = Some(priceRequest)
    )

    val promocoderFeatures = List(TestFeature)
    val features = mockFeatures(product, price, features = promocoderFeatures)

    makeHold
      .expects(*, *, *, *, *, *)
      .never()

    logBillingEvent
      .expects(*, *)
      .returningZ(())

    features
  }

  private def mockProductAndPrice(
      product: ProductId,
      price: Funds,
      requestId: String,
      duration: Option[ProductDuration] = Some(ProductDuration.days(180)),
      priceRequest: Option[PriceEstimateService.PriceRequest] = None
  ): Unit = {
    val actualPriceRequest =
      priceRequest.getOrElse(
        TestPriceRequest(TestClient, product, Some(requestId))
      )

    estimatePrice
      .expects(actualPriceRequest)
      .returningZ(TestPriceResponse)

    val testProductInfo =
      ProductInfo(
        product,
        Kopecks(price),
        None,
        duration,
        None,
        appliedExperiment = None,
        policyId = None
      )

    extractProductInfo
      .expects(product, *)
      .returningZ(testProductInfo)

    getPriceExtractor
      .expects(TestPriceResponse)
      .returning(priceExtractor)
  }

  private def mockFeatures(
      product: ProductId,
      price: Funds,
      priceWithDiscount: Funds = 0L,
      features: List[FeatureInstance],
      section: Section = Section.USED
  ): List[PriceModifierFeature] = {
    getPromocoderUserFeatures
      .expects(PromocoderUser(TestClientId, UserTypes.ClientUser))
      .returningZ(features)

    val modifiedPrice = ModifiedPrice(
      priceWithDiscount,
      features.map(f =>
        PriceModifierFeature(
          f,
          FeatureCount(1, FeatureUnits.Items),
          priceWithDiscount
        )
      )
    )

    val loyaltyArgs = product match {
      case ProductId.VinHistory => None
      case _ => Some(LoyaltyArgs(Category.CARS, section, TestClient.regionId))
    }

    modifyPrice
      .expects(
        features,
        product,
        price,
        1L,
        None,
        loyaltyArgs
      )
      .returningZ(modifiedPrice)

    features.foreach(f =>
      (promocoderClient.changeFeatureCountIdempotent _)
        .expects(f.id, TestHoldId, Items(1))
        .returningZ(f.copy(count = Items(0)))
    )

    modifiedPrice.features
  }

  def mockHold(product: ProductId, price: Funds): Unit = {
    val holdState = GuardianClient.HoldStates.Ok

    makeHold
      .expects(
        TestBalanceClientId,
        TestBalanceAgencyId,
        TestHoldId,
        product,
        TestAccountId,
        price
      )
      .returningT(holdState)

    logBillingEvent
      .expects(*, *)
      .returningZ(())
  }
}

object BillingEventProcessorImplSpec {

  private val TestClientId: ClientId = 20101
  private val TestAgencyId: Option[AgencyId] = None
  private val TestBalanceClientId: BalanceClientId = 2010100
  private val TestBalanceAgencyId: Option[BalanceClientId] = None
  private val TestAccountId: AccountId = 1
  private val TestHoldId = "holdId"

  private val TestClient = DetailedClient(
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

  private def buildGood(customId: String): Model.Good = {
    val cost = Model.Cost
      .newBuilder()
      .setVersion(1)
      .setPerCall(Model.Cost.PerCall.newBuilder.setUnits(222L))
      .build()

    val custom = Model.Good.Custom.newBuilder
      .setId(customId)
      .setCost(cost)
      .build()

    val good =
      Model.Good
        .newBuilder()
        .setVersion(1)
        .setCustom(custom)
        .build()

    good
  }

  private def buildProduct(goods: List[Model.Good]): Model.Product =
    Model.Product.newBuilder
      .setVersion(1)
      .addAllGoods(goods.asJava)
      .build()

  private def TestProductCampaign(
      product: ProductId,
      enabled: Boolean,
      productTariff: Option[ProductTariff]
  ) = {
    val owner = Model.CustomerHeader.newBuilder
      .setVersion(1)
      .setId(customerId(TestClient))
      .build()

    val order = Model.Order.newBuilder
      .setVersion(1)
      .setOwner(customerId(TestClient))
      .setId(TestAccountId)
      .setText("order")
      .setCommitAmount(5L)
      .setApproximateAmount(0L)
      .build

    val settings = Model.CampaignSettings
      .newBuilder()
      .setVersion(1)
      .setIsEnabled(enabled)
      .build()

    val billingProduct = productTariff
      .map { tariff =>
        buildProduct(List(buildGood(tariff.entryName)))
      }
      .getOrElse(customProduct(product))

    Model.CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setOwner(owner)
      .setId("campaignId")
      .setOrder(order)
      .setProduct(billingProduct)
      .setSettings(settings)
      .build()
  }

  private def TestPriceRequest(
      client: DetailedClient,
      product: ProductId,
      requestId: Option[String]
  ) =
    PriceEstimateService.PriceRequest(
      PriceEstimateService.PriceRequest.DefaultClientOffer,
      PriceEstimateService.PriceRequest
        .ClientContext(
          client.regionId,
          offerPlacementDay = None,
          productTariff = None
        ),
      product,
      interval = today(),
      requestId = requestId.orElse(Some(UUID.randomUUID().toString))
    )

  private def TestProductAccessCreditPriceRequest(client: DetailedClient) =
    PriceEstimateService.PriceRequest(
      PriceEstimateService.PriceRequest.DefaultClientOffer,
      PriceEstimateService.PriceRequest
        .AccessCreditApplicationContext(
          client.regionId,
          client.cityId,
          OfferCategories.Cars,
          Section.USED
        ),
      ProductId.CreditApplication,
      interval = today()
    )

  private def TestProductPriceRequest(
      client: DetailedClient,
      productId: ProductId,
      productTariff: Option[ProductTariff]
  ) =
    PriceEstimateService.PriceRequest(
      PriceEstimateService.PriceRequest.DefaultClientOffer,
      PriceEstimateService.PriceRequest
        .ClientContext(client.regionId, Some(client.cityId), productTariff),
      productId,
      interval = today()
    )

  private val testProduct = ProductId.CreditApplication
  private val featureId = s"$testProduct:promo_salesman-test:96eb92e69602f216"
  private val featureTag = testProduct.toString

  private val testUser =
    PromocoderUser(TestClient.clientId, UserTypes.ClientUser)
  private val featurePayload = FeaturePayload(FeatureUnits.Items)
  private val featureCount = FeatureCount(10L, FeatureUnits.Items)
  private val featureOrigin = FeatureOrigin("origin")

  private val TestFeature = FeatureInstance(
    featureId,
    featureOrigin,
    featureTag,
    testUser.toString,
    featureCount,
    now().minusDays(1),
    now().plusDays(2),
    featurePayload
  )

  private val TestPriceResponse =
    new PriceEstimateService.PriceResponse(new Array[Byte](0), DateTime.now())

  case object BillingServiceException extends Exception with NoStackTrace

}
