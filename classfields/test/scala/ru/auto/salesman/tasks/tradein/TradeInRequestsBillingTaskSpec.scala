package ru.auto.salesman.tasks.tradein

import cats.data.NonEmptyList
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, Matchers}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.salesman.billing.{RequestContext => BillingRequestContext}
import ru.auto.salesman.dao.TradeInRequestDao
import ru.auto.salesman.dao.TradeInRequestDao.Patch.SetBillingStatusAndCost
import ru.auto.salesman.dao.TradeInRequestDao.PatchFilter.WithRequestId
import ru.auto.salesman.dao.TradeInRequestDao.TradeInRequestCoreData
import ru.auto.salesman.environment.today
import ru.auto.salesman.model.ProductId.NonPromotionProduct
import ru.auto.salesman.model.TradeInRequest.Statuses.{Free, Paid}
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.{
  AccountId,
  AdsRequestTypes,
  AgencyId,
  BalanceClientId,
  CityId,
  ClientId,
  DetailedClient,
  Funds,
  ProductDuration,
  ProductId,
  ProductTariff,
  RegionId,
  RetryCount,
  TradeInRequest,
  TransactionId
}
import ru.auto.salesman.service.BillingEventProcessor.BillingEventResponse
import ru.auto.salesman.service.PriceEstimateService.{PriceRequest, PriceResponse}
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.service.{
  BillingEventProcessor,
  DetailedClientSource,
  PriceEstimateService,
  PriceExtractor
}
import ru.auto.salesman.tasks.TradeInRequestsBillingTask
import ru.auto.salesman.tasks.trade_in.TradeInRequestsBilling
import ru.auto.salesman.tasks.tradein.TradeInRequestsBillingTaskSpec._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.CryptoFunctions.md5
import ru.auto.salesman.util.money.Money.Kopecks
import ru.auto.salesman.util.RequestContext
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.util.Try

class TradeInRequestsBillingTaskSpec
    extends BaseSpec
    with ScalaFutures
    with Matchers
    with BeforeAndAfter {

  implicit val bc: BillingRequestContext = BillingRequestContext {
    "trade-in-requests-billing-spec"
  }

  val clientSource = mock[DetailedClientSource]
  val billingEventProcessor = mock[BillingEventProcessor]
  val tradeInRequestDao = mock[TradeInRequestDao]
  val priceEstimateService = mock[PriceEstimateService]

  val task = new TradeInRequestsBillingTask(
    clientSource,
    billingEventProcessor,
    tradeInRequestDao,
    priceEstimateService
  )

  toMockFunction2 {
    clientSource.unsafeResolve(_: ClientId, _: Boolean)
  }

  private val processBillingEvent = toMockFunction8 {
    billingEventProcessor.process(
      _: DetailedClient,
      _: NonPromotionProduct,
      _: PriceEstimateService.PriceRequest,
      _: TransactionId,
      _: Option[ProductTariff]
    )(_: BillingEventResponse => Try[_])(
      _: RequestContext,
      _: BillingRequestContext
    )
  }

  private val updateRequestBillingStatus = toMockFunction2 {
    tradeInRequestDao.update(
      _: TradeInRequestDao.Patch,
      _: NonEmptyList[TradeInRequestDao.PatchFilter]
    )
  }

  private def startsWith(expected: String) = argThat { actual: String =>
    actual.startsWith(expected)
  }

  "TradeInRequestsBillingTask.buyRequest()" should {
    "work fine" in {
      val product = ProductId.TradeInRequestCarsNew

      val expHoldPrefix =
        s"trade-in-request:cars:new:" + md5(s"$TestClientId:777")

      processBillingEvent
        .expects(TestClient, product, *, startsWith(expHoldPrefix), *, *, *, *)
        .returningT(TestBillingEventResponse)

      val request =
        TradeInRequestsBilling.Request(
          product,
          requestId = 777L,
          TestPriceRequest,
          TestClient
        )

      task
        .buyRequest(request)
        .success
        .value shouldBe TestBillingEventResponse
    }
  }

  "TradeInRequestsBillingTask.afterRequest()" should {
    "work fine" in {
      updateRequestBillingStatus
        .expects(
          SetBillingStatusAndCost(Paid, cost = 100L),
          NonEmptyList.of(WithRequestId(777L))
        )
        .returningT(())

      val request = TradeInRequestsBilling.Request(
        ProductId.TradeInRequestCarsNew,
        requestId = 777L,
        TestPriceRequest,
        TestClient
      )

      task
        .afterBuyRequest(request, TestBillingEventResponse)
        .success
        .value shouldBe unit
    }
  }

  "TradeInRequestsBillingTask.getProduct()" should {
    "return product for cars new" in {
      val request = TestTradeInRequest(Category.CARS, Section.NEW)

      task.getProduct(request).value shouldBe ProductId.TradeInRequestCarsNew
    }

    "return product for cars used" in {
      val request = TestTradeInRequest(Category.CARS, Section.USED)

      task.getProduct(request).value shouldBe ProductId.TradeInRequestCarsUsed
    }

    "fail on moto category" in {
      val request = TestTradeInRequest(Category.MOTO, Section.NEW)

      task.getProduct(request) shouldBe empty
    }
  }

  "TradeInRequestsBillingTask.processRequest()" should {
    "do not call billingEventProcessor if price is 0" in {
      mockPrice(price = 0L)

      updateRequestBillingStatus
        .expects(
          SetBillingStatusAndCost(Free, cost = 0),
          NonEmptyList.of(WithRequestId(777L))
        )
        .returningT(())

      processBillingEvent
        .expects(*, *, *, *, *, *, *, *)
        .never()

      val request = TradeInRequestsBilling.Request(
        ProductId.TradeInRequestCarsNew,
        requestId = 777L,
        TestPriceRequest,
        TestClient
      )

      task.processRequest(request, RetryCount(0)).success
    }

    "call billingEventProcessor if price is not 0" in {
      mockPrice(price = 100L)

      val product = ProductId.TradeInRequestCarsNew

      val expHoldPrefix =
        s"trade-in-request:cars:new:" + md5(s"$TestClientId:777")

      processBillingEvent
        .expects(TestClient, product, *, startsWith(expHoldPrefix), *, *, *, *)
        .returningT(TestBillingEventResponse)

      val request =
        TradeInRequestsBilling.Request(
          product,
          requestId = 777L,
          TestPriceRequest,
          TestClient
        )

      task
        .processRequest(request, RetryCount(1))
        .success
    }
  }

  private def mockPrice(price: Funds): Unit = {
    (priceEstimateService
      .estimate(_: PriceRequest))
      .expects(*)
      .returningZ(new PriceResponse(Array[Byte](), DateTimeUtil.now()))

    val priceExtractor = mock[PriceExtractor]

    (priceEstimateService.extractor _)
      .expects(*)
      .returning(priceExtractor)

    (priceExtractor.productInfo _)
      .expects(*, *)
      .returningZ(
        ProductInfo(
          ProductId.TradeInRequestCarsNew,
          Kopecks(price),
          prolongPrice = None,
          duration = Some(ProductDuration.days(60)),
          tariff = None,
          appliedExperiment = None,
          policyId = None
        )
      )

  }

}

object TradeInRequestsBillingTaskSpec {

  val TestClientId: ClientId = 20101
  val TestAgencyId: Option[AgencyId] = None
  val TestBalanceClientId: BalanceClientId = 2010100
  val TestBalanceAgencyId: Option[BalanceClientId] = None
  val TestAccountId: AccountId = 1

  val TestTradeInRequestId = 777L

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

  val TestPriceRequest = PriceRequest(
    PriceRequest.DefaultClientOffer,
    PriceRequest.ClientContext(
      TestClient.regionId,
      offerPlacementDay = None,
      productTariff = None
    ),
    ProductId.MatchApplicationCarsNew,
    interval = today()
  )

  val TestBillingEventResponse = BillingEventResponse(
    DateTimeUtil.now(),
    Some(100L),
    Some(100L),
    promocodeFeatures = List.empty,
    Some("hold-id"),
    Some(TestClientId),
    agencyId = None,
    companyId = None,
    regionId = Some(RegionId(1L))
  )

  def TestTradeInRequest(category: Category, section: Section) =
    TradeInRequestCoreData(
      TestTradeInRequestId,
      TestClientId,
      Some(OfferIdentity("111-asdf")),
      category,
      section,
      None,
      Some(OfferIdentity("222-adsa")),
      Some(Category.CARS),
      "+70000000000",
      Some("Vasya"),
      TradeInRequest.Statuses.New,
      0L,
      DateTime.now()
    )

}
