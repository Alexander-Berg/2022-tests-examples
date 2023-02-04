package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import org.scalamock.handlers.CallHandler
import ru.auto.cabinet.ApiModel.DealerStock.StockCategory
import ru.auto.cabinet.ApiModel._
import ru.auto.salesman.Task
import ru.auto.salesman.client.cabinet.CabinetClient
import ru.auto.salesman.client.cabinet.model.{CabinetApiException, ManagerNotFound}
import ru.auto.salesman.client.loyalty.LoyaltyClient
import ru.auto.salesman.client.loyalty.model.{
  PlacementDiscountRequest,
  PlacementDiscountResponse
}
import ru.auto.salesman.dao.CashbackPeriodDao
import ru.auto.salesman.model.PeriodId
import ru.auto.salesman.model.cashback.CashbackPeriod
import ru.auto.salesman.service.LoyaltyParamsFetcher.{
  LoyaltyParams,
  PlacementDiscountResult
}
import ru.auto.salesman.service.RewardService.NoCashbackPeriodException
import ru.auto.salesman.service.cached.CachedLoyaltyParamsFetcherImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.feature.TestDealerFeatureService
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.util.GeoUtils._

class LoyaltyParamsFetcherSpec extends BaseSpec with BasicSalesmanGenerators {
  private val cabinetClient = mock[CabinetClient]
  private val cashbackPeriodDao = mock[CashbackPeriodDao]
  private val loyaltyClient = mock[LoyaltyClient]
  private val dealerFeatureService = TestDealerFeatureService()

  private val fetcher =
    new LoyaltyParamsFetcherImpl(
      cashbackPeriodDao,
      cabinetClient,
      loyaltyClient,
      dealerFeatureService
    )

  private val period = CashbackPeriod(
    id = PeriodId(10),
    start = DateTime.now().minusMonths(1),
    finish = DateTime.now(),
    isActive = false,
    previousPeriod = None
  )

  private val detailedClient: DetailedClient =
    DetailedClient
      .newBuilder()
      .setId(20101L)
      .setProperties(ClientProperties.newBuilder.setRegionId(RegMoscow))
      .build

  val discount: CustomerDiscount = CustomerDiscount
    .newBuilder()
    .setCustomerId(20101L)
    .setCustomerType(CustomerType.CLIENT)
    .setProduct(CustomerDiscount.Product.PLACEMENT)
    .setPercent(5)
    .build

  private val customerDiscountsResponse: GetCustomerDiscountsResponse =
    GetCustomerDiscountsResponse
      .newBuilder()
      .addDiscounts(discount)
      .build

  private val loyaltyDiscountRequest: PlacementDiscountRequest =
    PlacementDiscountRequest(
      20101L,
      period.id,
      period.start,
      period.finish
    )

  private val loyaltyDiscountResponse: PlacementDiscountResponse =
    PlacementDiscountResponse(10)

  private val managerRecord: ManagerRecord = ManagerRecord
    .newBuilder()
    .setId(20101L)
    .setFio("fio")
    .build()

  "LoyaltyParametersFetcher" should {
    "fetch parameters by client id and period id" in {

      (cabinetClient.getClientDetails _)
        .expects(20101L)
        .returningZ(detailedClient)
      (cabinetClient.getManager _)
        .expects(20101L)
        .returningZ(managerRecord)
      (cashbackPeriodDao.getById _)
        .expects(PeriodId(10))
        .returningZ(Some(period))
      (cabinetClient.getClientDiscounts _)
        .expects(20101L, None)
        .returningZ(customerDiscountsResponse)
      mockStocks()

      fetcher
        .getParams(PeriodId(10), 20101)
        .success
        .value shouldBe LoyaltyParams(
        extrabonus = Some(ExtraBonus.UNDER_1000_CARS),
        hasFullStock = true,
        period = period,
        regionId = RegMoscow,
        managerFio = Some("fio"),
        placementDiscountResult = PlacementDiscountResult.Discount(5)
      )
    }
    "fetch discount from loyalty instead of cabinet" in {

      val fetcher = new LoyaltyParamsFetcherImpl(
        cashbackPeriodDao,
        cabinetClient,
        loyaltyClient,
        dealerFeatureService.copy(loyaltyNewPlacementDiscountRegions = Set(RegMoscow))
      )

      (cabinetClient.getClientDetails _)
        .expects(20101L)
        .returningZ(detailedClient)
      (cabinetClient.getManager _)
        .expects(20101L)
        .returningZ(managerRecord)
      (cashbackPeriodDao.getById _)
        .expects(PeriodId(10))
        .returningZ(Some(period))
      (loyaltyClient.getOrCalcPlacementDiscount _)
        .expects(loyaltyDiscountRequest)
        .returningZ(loyaltyDiscountResponse)
      mockStocks()

      fetcher
        .getParams(PeriodId(10), 20101)
        .success
        .value shouldBe LoyaltyParams(
        extrabonus = Some(ExtraBonus.UNDER_1000_CARS),
        hasFullStock = true,
        period = period,
        regionId = RegMoscow,
        managerFio = Some("fio"),
        placementDiscountResult = PlacementDiscountResult.Discount(10)
      )
    }

    "fill in params even with manager absence" in {

      (cabinetClient.getClientDetails _)
        .expects(20101L)
        .returningZ(detailedClient)
      (cabinetClient.getManager _)
        .expects(20101L)
        .throwingZ(new ManagerNotFound("""Error accessing cabinet.getManagerName(), response:
            |{"error":{"message":"User is not found or manager is not assigned to user","status":404}},
            |uri: /api/1.x/client/ъуъ/manager/internal""".stripMargin))

      (cashbackPeriodDao.getById _)
        .expects(PeriodId(10))
        .returningZ(Some(period))
      (cabinetClient.getClientDiscounts _)
        .expects(20101L, None)
        .returningZ(customerDiscountsResponse)
      mockStocks()

      fetcher
        .getParams(PeriodId(10), 20101)
        .success
        .value shouldBe LoyaltyParams(
        extrabonus = Some(ExtraBonus.UNDER_1000_CARS),
        hasFullStock = true,
        period = period,
        regionId = RegMoscow,
        managerFio = None,
        placementDiscountResult = PlacementDiscountResult.Discount(5)
      )
    }

    "fetch parameters with cached region by client id and period id" in {
      val fetcher =
        new LoyaltyParamsFetcherImpl(
          cashbackPeriodDao,
          cabinetClient,
          loyaltyClient,
          dealerFeatureService
        ) with CachedLoyaltyParamsFetcherImpl

      (cabinetClient.getClientDetails _)
        .expects(20101L)
        .returningZ(detailedClient)
        .once()

      (cabinetClient.getManager _)
        .expects(20101L)
        .returningZ(managerRecord)
        .twice()

      (cashbackPeriodDao.getById _)
        .expects(PeriodId(10))
        .returningZ(Some(period))
        .twice()

      (cabinetClient.getClientDiscounts _)
        .expects(20101L, None)
        .returningZ(customerDiscountsResponse)
        .twice()

      mockStocks().twice()

      fetcher
        .getParams(PeriodId(10), 20101)
        .success
        .value shouldBe LoyaltyParams(
        extrabonus = Some(ExtraBonus.UNDER_1000_CARS),
        hasFullStock = true,
        period = period,
        regionId = RegMoscow,
        managerFio = Some("fio"),
        placementDiscountResult = PlacementDiscountResult.Discount(5)
      )

      fetcher
        .getParams(PeriodId(10), 20101)
        .success
        .value shouldBe LoyaltyParams(
        extrabonus = Some(ExtraBonus.UNDER_1000_CARS),
        hasFullStock = true,
        period = period,
        regionId = RegMoscow,
        managerFio = Some("fio"),
        placementDiscountResult = PlacementDiscountResult.Discount(5)
      )
    }

    "fail on nonexistent period" in {
      (cashbackPeriodDao.getById _)
        .expects(PeriodId(10))
        .returningZ(None)
      (cabinetClient.getClientDetails _)
        .expects(20101L)
        .returningZ(detailedClient)
      (cabinetClient.getManager _)
        .expects(20101L)
        .returningZ(managerRecord)
      mockStocks()

      fetcher
        .getParams(PeriodId(10), 20101)
        .failure
        .exception shouldBe a[NoCashbackPeriodException]
    }

    "fail on nonexistent client" in {
      (cashbackPeriodDao.getById _)
        .expects(PeriodId(10))
        .returningZ(Some(period))
      (cabinetClient.getClientDetails _)
        .expects(20101L)
        .throwingZ(new CabinetApiException("client not found"))
      (cabinetClient.getManager _)
        .expects(20101L)
        .returningZ(managerRecord)
      mockStocks()

      fetcher
        .getParams(PeriodId(10), 20101)
        .failure
        .exception shouldBe a[CabinetApiException]
    }
  }

  private def mockStocks(): CallHandler[Task[DealerStocks]] =
    (cabinetClient.getClientStocks _)
      .expects(20101L)
      .returningZ(
        DealerStocks
          .newBuilder()
          .addStocks(
            DealerStock
              .newBuilder()
              .setStockCategory(StockCategory.CARS_USED)
              .setFullStock(true)
              .setExtraBonus(ExtraBonus.UNDER_1000_CARS)
              .build()
          )
          .build()
      )
}
