package ru.auto.api.managers.dealer

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.google.protobuf.Timestamp
import org.mockito.Mockito._
import org.scalatest.OptionValues
import org.scalatest.time.{Second, Span}
import ru.auto.api.ApiOfferModel.Category.{CARS, MOTO, TRUCKS}
import ru.auto.api.ApiOfferModel.Section.{NEW, USED}
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.BaseSpec
import ru.auto.api.PriceModel.KopeckPrice
import ru.auto.api.ResponseModel.AvailableTariffsResponse.TariffType.{CALLS, QUOTA, SINGLE, SINGLE_WITH_CALLS}
import ru.auto.api.ResponseModel.AvailableTariffsResponse.{PlacementPriceInfo, PriorityPlacement, Range, ServicePrice, Tariff, TruckClass}
import ru.auto.api.ResponseModel.OfferCountResponse
import ru.auto.api.auth.Application
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.features.FeatureManager.DealerVasProductsFeatures
import ru.auto.api.geo.Tree
import ru.auto.api.managers.dealer.DealerTariffManager.FetchSettings
import ru.auto.api.managers.searcher.SearcherManager
import ru.auto.api.model.AutoruProduct._
import ru.auto.api.model.CategorySelector.StrictCategory
import ru.auto.api.model.ModelGenerators.{badgesOfferGen, RegionGen}
import ru.auto.api.model._
import ru.auto.api.model.billing.{BalanceClient, BalanceId}
import ru.auto.api.model.gen.DeprecatedBillingModelGenerators._
import ru.auto.api.model.salesman.Campaign
import ru.auto.api.model.salesman.PaymentModel.{Calls, Quota, Single, SingleWithCalls}
import ru.auto.api.model.searcher.SearcherRequest
import ru.auto.api.services.billing.MoishaClient
import ru.auto.api.services.billing.MoishaClient._
import ru.auto.api.services.billing.util.{fromCents, toCents}
import ru.auto.api.services.cabinet.{CabinetApiClient, ClientMarkView, ClientPropertiesView, ClientView}
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.salesman.SalesmanClient.CallInfo
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.TimeUtils.TimeProvider
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.salesman.dealer.ProductsPriceRequestOuterClass.ProductsPriceResponse
import ru.auto.salesman.dealer.ProductsPriceRequestOuterClass.ProductsPriceResponse.ProductPrice
import ru.auto.salesman.dealer.ProductsPriceRequestOuterClass.ProductsPriceResponse.ProductPrice.{Price => ProtoPrice}
import ru.yandex.vertis.billing.Model.Limits.Limit
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport

import java.time.OffsetDateTime
import scala.jdk.CollectionConverters._

/**
  * Тестируем [[DealerTariffManager]].
  */
// scalastyle:off number.of.methods
class DealerTariffManagerSpec extends BaseSpec with MockitoSupport with OptionValues {

  implicit override def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1, Second)))

  private val salesmanClient = mock[SalesmanClient]
  private val cabinetApiClient = mock[CabinetApiClient]
  private val moishaClient = mock[MoishaClient]
  private val vosClient = mock[VosClient]
  private val dataService = mock[DataService]
  private val searcherClient = mock[SearcherClient]
  private val searcherManager = mock[SearcherManager]
  private val timeProvider = mock[TimeProvider]
  private val testTree = mock[Tree]
  private val featureManager = mock[FeatureManager]

  private val now = 1540470501161L // Thu Oct 25 15:28:21 MSK 2018
  private val todayFrom = OffsetDateTime.parse("2018-10-25T00:00:00+03:00")
  private val todayTo = OffsetDateTime.parse("2018-10-25T23:59:59.999+03:00")
  private val testClientId = 20101
  private val testClientIdStr = "20101"
  private val testDealer = AutoruDealer(testClientId)
  private val testBalanceClient = BalanceClient(accountId = Some(1L), BalanceId(100), balanceAgencyId = None)

  private val testBalanceClientWithAgency =
    BalanceClient(accountId = Some(1L), BalanceId(100), balanceAgencyId = Some(BalanceId(200)))

  private val testClientMarks = List(ClientMarkView("OPEL"), ClientMarkView("SKODA"))
  private val testClientMarksList = testClientMarks.map(_.mark)

  private val vosRegionId = 213
  private val regionId = 1L
  private val testCityId = 213L
  private val testRegion = RegionGen.next.copy(id = regionId)

  private val vosEkbRegionId = 54
  private val ekbRegionId = 11162
  private val ekbCityId = 54
  private val ekbRegion = RegionGen.next.copy(id = ekbRegionId)
  private val ekbCityRegion = RegionGen.next.copy(id = ekbCityId)

  private val callRegionIdPlacement = 1L
  private val vosCallRegionIdPlacement = 213
  private val callRegionPlacement = RegionGen.next.copy(id = callRegionIdPlacement)

  private val callRegionIdNoPlacement = ekbRegionId
  private val vosCallRegionIdNoPlacement = vosEkbRegionId
  private val callRegionNoPlacement = RegionGen.next.copy(id = callRegionIdNoPlacement)

  private val carsUsedCampaign = Campaign(Single, "cars:used", "cars", Nil, List("used"), Int.MaxValue, enabled = true)
  private val disabledCarsUsedCampaign = carsUsedCampaign.copy(enabled = false)

  private val motoCampaign =
    Campaign(Quota, "quota:placement:moto", "moto", Nil, List("new", "used"), 25, enabled = true)

  private val commercialCampaign =
    Campaign(Single, "commercial", "commercial", Nil, List("new", "used"), Int.MaxValue, enabled = true)

  private val quotaCommercialCampaign =
    Campaign(Quota, "quota:placement:commercial", "commercial", Nil, List("new", "used"), 100, enabled = true)

  private val callCampaign =
    Campaign(Calls, "call", "cars", Nil, List("new"), Int.MaxValue, enabled = true)

  private val callCarsUsedCampaign =
    Campaign(SingleWithCalls, "single_with_calls", "cars", Nil, List("used"), Int.MaxValue, enabled = true)

  private val callInfo =
    CallInfo(callCost = 120000, depositCoefficient = 3)

  private val billingCallCampaign = {
    val b = campaignHeaderGen.next.toBuilder
    b.getProductBuilder
      .clearGoods()
      .addGoodsBuilder()
      .setVersion(1)
      .getCustomBuilder
      .setId("call")
      .getCostBuilder
      .setVersion(1)
      .getPerCallBuilder
      .setUnits(120000)
    b.clearLimits()
      .getLimitsBuilder
      .setCurrentDaily(Limit.newBuilder().setFunds(10000).setEffectiveSince(1540933200000L))
      .setCurrentWeekly(Limit.newBuilder().setFunds(20000).setEffectiveSince(1550933200000L))
      .setCurrentMonthly(Limit.newBuilder().setFunds(30000).setEffectiveSince(1560933200000L))
      .setComingDaily(Limit.newBuilder().setFunds(40000).setEffectiveSince(1570933200000L))
      .setComingWeekly(Limit.newBuilder().setFunds(50000).setEffectiveSince(1580933200000L))
      .setComingMonthly(Limit.newBuilder().setFunds(60000).setEffectiveSince(1590933200000L))
    b.getSettingsBuilder.getDepositBuilder.setCoefficient(3)
    b.build()
  }

  private val dailyLimitCallCampaign = {
    val b = billingCallCampaign.toBuilder
    b.clearLimits().getLimitsBuilder.getCurrentDailyBuilder.setFunds(10000)
    b.build()
  }

  private val noLimitCallCampaign = billingCallCampaign.toBuilder.clearLimits().build()

  private val billingCallCarsUsedCampaign = {
    val b = campaignHeaderGen.next.toBuilder
    b.getProductBuilder
      .clearGoods()
      .addGoodsBuilder()
      .setVersion(1)
      .getCustomBuilder
      .setId("call:cars:used")
      .getCostBuilder
      .setVersion(1)
      .getPerCallBuilder
      .setUnits(100)
    b.clearLimits()
      .getLimitsBuilder
      .setCurrentDaily(Limit.newBuilder().setFunds(10000).setEffectiveSince(1540933200000L))
      .setCurrentWeekly(Limit.newBuilder().setFunds(20000).setEffectiveSince(1550933200000L))
      .setCurrentMonthly(Limit.newBuilder().setFunds(30000).setEffectiveSince(1560933200000L))
      .setComingDaily(Limit.newBuilder().setFunds(40000).setEffectiveSince(1570933200000L))
      .setComingWeekly(Limit.newBuilder().setFunds(50000).setEffectiveSince(1580933200000L))
      .setComingMonthly(Limit.newBuilder().setFunds(60000).setEffectiveSince(1590933200000L))
    b.getSettingsBuilder.getDepositBuilder.setCoefficient(3)
    b.build()
  }

  private val quotas = List(
    MoishaQuota("quota:placement:cars:used", Int.MaxValue, 10000),
    MoishaQuota("quota:placement:moto", 10, 7000),
    MoishaQuota("quota:placement:moto", 25, 10000),
    MoishaQuota("quota:placement:moto", 50, 15000),
    MoishaQuota("quota:placement:commercial", 100, 20000)
  )

  private val badgesOffers = Source(
    List(
      badgesOfferGen(List("badge1", "badge2", "badge3")).next,
      badgesOfferGen(List("badge4", "badge5")).next
    )
  )

  private val badgesCount = 5

  private def mockBadgesOffers(category: StrictCategory, params: SearcherFilter) = {
    when(
      searcherManager.searchOffers(
        eq(
          SearcherRequest(
            category,
            params ++ Map(
              "autoru_billing_service_type" -> Set("badge"),
              "client_id" -> Set(testClientIdStr),
              "with_autoru_expert" -> Set("BOTH")
            )
          )
        ),
        eq(NoSorting)
      )(?)
    ).thenReturn(badgesOffers)
  }

  private def mockOffersCount(
      category: StrictCategory,
      params: Map[String, Set[String]],
      offersCount: Int
  )(implicit request: Request): Unit = {
    when(
      searcherClient.offersCount(
        SearcherRequest(
          category,
          params + ("client_id" -> Set(testClientIdStr))
        )
      )
    ).thenReturnF(OfferCountResponse.newBuilder().setCount(offersCount).build())
  }

  private def mockOffersCount(
      category: StrictCategory,
      params: Map[String, Set[String]],
      offersCount: Int,
      state: String
  )(implicit request: Request): Unit = {
    mockOffersCount(category, params + ("state" -> Set(state)), offersCount)
  }

  private def mockCallPlacementFeature(featureValue: Boolean) = {
    when(featureManager.dealerCallPaymentModelWithPaidActivationSince).thenReturn {
      new Feature[String] {
        override def name: String = "dealer_call_payment_model_with_paid_activation_since"

        override def value: String = "2019-05-01"
      }
    }

    when(
      featureManager.sinceDateTimeFeatureIsWorking(
        eq(featureManager.dealerCallPaymentModelWithPaidActivationSince),
        eq(false)
      )
    ).thenReturn(featureValue)
  }

  implicit private val system = ActorSystem("test")
  implicit private val mat = Materializer.createMaterializer(system)

  lazy val dealerBadgeManager =
    new DefaultDealerBadgeManager(
      cabinetApiClient,
      moishaClient,
      timeProvider,
      vosClient,
      dataService,
      searcherManager
    )

  val manager =
    new DealerTariffManager(
      salesmanClient,
      cabinetApiClient,
      searcherClient,
      dealerBadgeManager,
      featureManager
    )

  private val clientRequest = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct("0.0.0.0"))
    r.setUser(ModelGenerators.DealerUserRefGen.next)
    r.setDealer(testDealer)
    r.setDealerRole(DealerUserRoles.Client)
    r
  }

  private val agencyRequest = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct("0.0.0.0"))
    r.setUser(ModelGenerators.DealerUserRefGen.next)
    r.setDealer(testDealer)
    r.setDealerRole(DealerUserRoles.Agency)
    r
  }

  private val companyRequest = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct("0.0.0.0"))
    r.setUser(ModelGenerators.DealerUserRefGen.next)
    r.setDealer(testDealer)
    r.setDealerRole(DealerUserRoles.Company)
    r
  }

  private val managerRequest = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct("0.0.0.0"))
    r.setUser(ModelGenerators.DealerUserRefGen.next)
    r.setDealer(testDealer)
    r.setDealerRole(DealerUserRoles.Manager)
    r
  }

  private val request: RequestImpl = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct("0.0.0.0"))
    r.setUser(ModelGenerators.DealerUserRefGen.next)
    r.setDealer(testDealer)
    r
  }

  private def protoPrice(basePrice: Long, finalPrice: Long, discountPercent: Int): ProtoPrice =
    ProtoPrice
      .newBuilder()
      .setBasePrice(KopeckPrice.newBuilder().setKopecks(basePrice))
      .setFinalPrice(KopeckPrice.newBuilder().setKopecks(finalPrice))
      .setDiscountPercent(discountPercent)
      .build()

  private def servicePrice(product: AutoruProduct,
                           basePrice: Long,
                           finalPrice: Long,
                           discountPercent: Int): ProductPrice = {
    val price = ProductPrice.ServicePrice
      .newBuilder()
      .setPrice(protoPrice(basePrice, finalPrice, discountPercent))
    ProductPrice
      .newBuilder()
      .setServicePrice(price)
      .setProductCode(product.name)
      .build()
  }

  private def rangePrice(range: Long,
                         placementPrice: ProtoPrice,
                         prolongPrice: ProtoPrice): ProductPrice.PlacementPrice = {
    ProductPrice.PlacementPrice
      .newBuilder()
      .setRangeFrom(KopeckPrice.newBuilder().setKopecks(toCents(range)))
      .setPlacementPrice(placementPrice)
      .setProlongPrice(prolongPrice)
      .build()
  }

  private def placementPrice(ranges: Seq[ProductPrice.PlacementPrice]): ProductPrice = {
    val prices = ProductPrice.PlacementPrices.newBuilder().addAllPriceRanges(ranges.asJava)
    ProductPrice
      .newBuilder()
      .setPlacementPrice(prices)
      .setProductCode(Placement.name)
      .build()
  }

  private def mockSalesmanPrices(
      category: Transport,
      section: Section,
      regionId: Long,
      services: List[ProductPrice],
      placementRanges: List[ProductPrice.PlacementPrice]
  ): Unit = {
    def mkRanges(ranges: Seq[Long]): List[Range] = {
      (ranges.map(range => Some(fromCents(range))) :+ None)
        .sliding(2, 1)
        .collect { case Seq(Some(from), to) => mkRange(from, to) }
        .toList
    }
    def mkRange(from: Long, to: Option[Long]): Range = {
      val rangeBuilder = Range.newBuilder()
      rangeBuilder.setFrom(from)
      to.foreach(rangeBuilder.setTo)
      rangeBuilder.build()
    }

    if (placementRanges.nonEmpty) {
      val requestRanges = mkRanges(placementRanges.map(_.getRangeFrom.getKopecks))
      val responsePlacement = ProductsPriceResponse
        .newBuilder()
        .addAllPrices(List(placementPrice(placementRanges)).asJava)
        .build()

      when(
        salesmanClient.productsPrices(
          eq(testDealer),
          eq(regionId),
          eq(category),
          eq(section),
          eq(Set.empty),
          eq(requestRanges)
        )(?)
      ).thenReturnF(responsePlacement)
    }

    if (services.nonEmpty) {
      val products = services.map(service => AutoruProduct.forName(service.getProductCode).get).toSet
      val responseServices = ProductsPriceResponse
        .newBuilder()
        .addAllPrices(services.asJava)
        .build()

      when(
        salesmanClient.productsPrices(
          eq(testDealer),
          eq(regionId),
          eq(category),
          eq(section),
          eq(products),
          eq(List.empty)
        )(?)
      ).thenReturnF(responseServices)
    }
  }

  before {
    reset(
      salesmanClient,
      cabinetApiClient,
      moishaClient,
      vosClient,
      dataService,
      searcherClient,
      timeProvider,
      testTree,
      featureManager
    )
    when(cabinetApiClient.getClient(?)(?))
      .thenReturnF(ClientView(ClientPropertiesView("active", regionId = 1)))
  }

  private def moishaProduct(price: Long) = {
    MoishaProduct(product = "", goods = Nil, price, Some(ProductDuration.days(1)))
  }

  "DealerTariffManager" should {
    implicit val r: Request = request

    "return enabled cars:used" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(carsUsedCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosEkbRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosEkbRegionId)).thenReturn(ekbRegion)
      when(testTree.city(vosEkbRegionId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List(ekbRegionId))
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        USED,
        ekbRegionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0),
          servicePrice(Reset, 95000, 95000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockOffersPriceFromToCount(priceFrom: String, priceTo: String, offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("price_from" -> Set(priceFrom), "price_to" -> Set(priceTo), "with_autoru_expert" -> Set("NONE")),
          offersCount,
          "USED"
        )

      def mockOffersPriceFromCount(priceFrom: String, offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("price_from" -> Set(priceFrom), "with_autoru_expert" -> Set("NONE")),
          offersCount,
          "USED"
        )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "USED"
        )

      mockOffersPriceFromToCount("1", "300000", 25)
      mockOffersPriceFromToCount("300001", "500000", 20)
      mockOffersPriceFromToCount("500001", "800000", 15)
      mockOffersPriceFromToCount("800001", "1500000", 10)
      mockOffersPriceFromCount("1500001", 5)
      mockProductOffersCount(Set("turbo"), 21)
      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockProductOffersCount(Set("reset"), 33)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("USED")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.getSectionList should contain only USED
      tariff.getType shouldBe SINGLE
      tariff.getEnabled shouldBe true
      tariff.getProduct shouldBe "cars:used"
      tariff.getQuotasList shouldBe empty
      tariff.hasCalls shouldBe false
      val placementPrices = tariff.getPlacementPricesInfoList.asScala.toList
      placementPrices should have size 5
      val List(
        resultPlacementPrice0,
        resultPlacementPrice300,
        resultPlacementPrice500,
        resultPlacementPrice800,
        resultPlacementPrice1500
      ) = placementPrices: @unchecked
      resultPlacementPrice0.getPlacementPrice shouldBe 200
      resultPlacementPrice0.getProlongPrice shouldBe 100
      resultPlacementPrice0.getOffersCount shouldBe 25
      resultPlacementPrice300.getPlacementPrice shouldBe 400
      resultPlacementPrice300.getProlongPrice shouldBe 300
      resultPlacementPrice300.getOffersCount shouldBe 20
      resultPlacementPrice500.getPlacementPrice shouldBe 600
      resultPlacementPrice500.getProlongPrice shouldBe 500
      resultPlacementPrice500.getOffersCount shouldBe 15
      resultPlacementPrice800.getPlacementPrice shouldBe 800
      resultPlacementPrice800.getProlongPrice shouldBe 700
      resultPlacementPrice800.getOffersCount shouldBe 10
      resultPlacementPrice1500.getPlacementPrice shouldBe 1000
      resultPlacementPrice1500.getProlongPrice shouldBe 900
      resultPlacementPrice1500.getOffersCount shouldBe 5
      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 6

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val turbo = getServicePrice("turbo-package")
      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      val reset = getServicePrice("reset")
      turbo.getPrice shouldBe 1000
      turbo.getOffersCount shouldBe 21
      turbo.hasBadgesCount shouldBe false
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 11 // 32 - 21
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 9 // 30 - 21
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount
      reset.getPrice shouldBe 950
      reset.getOffersCount shouldBe 33
      reset.hasBadgesCount shouldBe false

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosEkbRegionId)
      verify(testTree, times(1)).city(vosEkbRegionId)
      verify(timeProvider, times(1)).now()
    }

    "return disabled cars:used" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(disabledCarsUsedCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosEkbRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosEkbRegionId)).thenReturn(ekbRegion)
      when(testTree.city(vosEkbRegionId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List(ekbRegionId))
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        USED,
        ekbRegionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0),
          servicePrice(Reset, 95000, 95000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.getSectionList should contain only USED
      tariff.getType shouldBe SINGLE
      tariff.getEnabled shouldBe false
      tariff.getProduct shouldBe "cars:used"
      tariff.getQuotasList shouldBe empty
      tariff.hasCalls shouldBe false
      val placementPrices = tariff.getPlacementPricesInfoList.asScala.toList
      placementPrices should have size 5
      val List(
        resultPlacementPrice0,
        resultPlacementPrice300,
        resultPlacementPrice500,
        resultPlacementPrice800,
        resultPlacementPrice1500
      ) = placementPrices: @unchecked
      resultPlacementPrice0.getPlacementPrice shouldBe 200
      resultPlacementPrice0.getProlongPrice shouldBe 100
      resultPlacementPrice0.getOffersCount shouldBe 0
      resultPlacementPrice300.getPlacementPrice shouldBe 400
      resultPlacementPrice300.getProlongPrice shouldBe 300
      resultPlacementPrice300.getOffersCount shouldBe 0
      resultPlacementPrice500.getPlacementPrice shouldBe 600
      resultPlacementPrice500.getProlongPrice shouldBe 500
      resultPlacementPrice500.getOffersCount shouldBe 0
      resultPlacementPrice800.getPlacementPrice shouldBe 800
      resultPlacementPrice800.getProlongPrice shouldBe 700
      resultPlacementPrice800.getOffersCount shouldBe 0
      resultPlacementPrice1500.getPlacementPrice shouldBe 1000
      resultPlacementPrice1500.getProlongPrice shouldBe 900
      resultPlacementPrice1500.getOffersCount shouldBe 0
      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 6

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val turbo = getServicePrice("turbo-package")
      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      val reset = getServicePrice("reset")
      turbo.getPrice shouldBe 1000
      turbo.getOffersCount shouldBe 0
      turbo.hasBadgesCount shouldBe false
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 0
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 0
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 0
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 0
      badge.hasBadgesCount shouldBe true
      badge.getBadgesCount shouldBe 0
      reset.getPrice shouldBe 950
      reset.getOffersCount shouldBe 0
      reset.hasBadgesCount shouldBe false

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosEkbRegionId)
      verify(testTree, times(1)).city(vosEkbRegionId)
      verify(timeProvider, times(1)).now()
    }

    "return enabled cars:used if city is defined" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(carsUsedCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(Some(ekbCityId)))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosEkbRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosEkbRegionId)).thenReturn(ekbRegion)
      when(testTree.city(vosEkbRegionId)).thenReturn(Some(ekbCityRegion))
      when(timeProvider.now()).thenReturn(now)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        USED,
        ekbRegionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockOffersPriceFromToCount(priceFrom: String, priceTo: String, offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("price_from" -> Set(priceFrom), "price_to" -> Set(priceTo), "with_autoru_expert" -> Set("NONE")),
          offersCount,
          "USED"
        )

      def mockOffersPriceFromCount(priceFrom: String, offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("price_from" -> Set(priceFrom), "with_autoru_expert" -> Set("NONE")),
          offersCount,
          "USED"
        )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "USED"
        )

      mockOffersPriceFromToCount("1", "300000", 25)
      mockOffersPriceFromToCount("300001", "500000", 20)
      mockOffersPriceFromToCount("500001", "800000", 15)
      mockOffersPriceFromToCount("800001", "1500000", 10)
      mockOffersPriceFromCount("1500001", 5)
      mockProductOffersCount(Set("turbo"), 21)
      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("USED")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.getSectionList should contain only USED
      tariff.getType shouldBe SINGLE
      tariff.getEnabled shouldBe true
      tariff.getProduct shouldBe "cars:used"
      tariff.getQuotasList shouldBe empty
      tariff.hasCalls shouldBe false
      val placementPrices = tariff.getPlacementPricesInfoList.asScala.toList
      placementPrices should have size 5
      val List(
        resultPlacementPrice0,
        resultPlacementPrice300,
        resultPlacementPrice500,
        resultPlacementPrice800,
        resultPlacementPrice1500
      ) = placementPrices: @unchecked
      resultPlacementPrice0.getPlacementPrice shouldBe 200
      resultPlacementPrice0.getProlongPrice shouldBe 100
      resultPlacementPrice0.getOffersCount shouldBe 25
      resultPlacementPrice300.getPlacementPrice shouldBe 400
      resultPlacementPrice300.getProlongPrice shouldBe 300
      resultPlacementPrice300.getOffersCount shouldBe 20
      resultPlacementPrice500.getPlacementPrice shouldBe 600
      resultPlacementPrice500.getProlongPrice shouldBe 500
      resultPlacementPrice500.getOffersCount shouldBe 15
      resultPlacementPrice800.getPlacementPrice shouldBe 800
      resultPlacementPrice800.getProlongPrice shouldBe 700
      resultPlacementPrice800.getOffersCount shouldBe 10
      resultPlacementPrice1500.getPlacementPrice shouldBe 1000
      resultPlacementPrice1500.getProlongPrice shouldBe 900
      resultPlacementPrice1500.getOffersCount shouldBe 5
      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 5

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val turbo = getServicePrice("turbo-package")
      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      turbo.getPrice shouldBe 1000
      turbo.getOffersCount shouldBe 21
      turbo.hasBadgesCount shouldBe false
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 11 // 32 - 21
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 9 // 30 - 21
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(Some(ekbCityId)))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosEkbRegionId)
      verify(testTree, times(1)).city(vosEkbRegionId)
      verify(timeProvider, times(1)).now()
    }

    "return moto quotas" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(motoCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(regionId), eq(testClientMarksList), eq(None))(?)).thenReturnF(quotas)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosRegionId)).thenReturn(testRegion)
      when(testTree.city(vosRegionId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Moto,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Moto,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount
        )

      mockProductOffersCount(Set("turbo"), 21)
      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Moto, Map())

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe MOTO
      (tariff.getSectionList should contain).allOf(USED, NEW)
      tariff.getType shouldBe QUOTA
      tariff.getEnabled shouldBe true
      tariff.getProduct shouldBe "quota:placement:moto"
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.hasCalls shouldBe false

      val tariffQuotas = tariff.getQuotasList.asScala.toList
      tariffQuotas should have size 3
      val List(quota10, quota25, quota50) = tariffQuotas: @unchecked
      quota10.getSize shouldBe 10
      quota10.getPrice shouldBe 70
      quota10.getEnabled shouldBe false
      quota25.getSize shouldBe 25
      quota25.getPrice shouldBe 100
      quota25.getEnabled shouldBe true
      quota50.getSize shouldBe 50
      quota50.getPrice shouldBe 150
      quota50.getEnabled shouldBe false

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 5

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val turbo = getServicePrice("turbo-package")
      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      turbo.getPrice shouldBe 1000
      turbo.getOffersCount shouldBe 21
      turbo.hasBadgesCount shouldBe false
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 11 // 32 - 21
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 9 // 30 - 21
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(regionId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosRegionId)
      verify(testTree, times(1)).city(vosRegionId)
      verify(timeProvider, times(1)).now()
    }

    "return commercial quotas" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(quotaCommercialCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(regionId), eq(testClientMarksList), eq(None))(?)).thenReturnF(quotas)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosRegionId)).thenReturn(testRegion)
      when(testTree.city(vosRegionId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Commercial,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(500000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(1000000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(2000000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(4000000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Trucks,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount
        )

      mockProductOffersCount(Set("turbo"), 21)
      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Trucks, Map())

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe TRUCKS
      (tariff.getSectionList should contain).allOf(USED, NEW)
      tariff.getType shouldBe QUOTA
      tariff.hasTruckClass shouldBe false
      tariff.getEnabled shouldBe true
      tariff.getProduct shouldBe "quota:placement:commercial"
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.hasCalls shouldBe false

      val tariffQuotas = tariff.getQuotasList.asScala.toList
      tariffQuotas should have size 1
      val tariffQuota = tariffQuotas.head
      tariffQuota.getSize shouldBe 100
      tariffQuota.getPrice shouldBe 200
      tariffQuota.getEnabled shouldBe true

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 5

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val turbo = getServicePrice("turbo-package")
      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      turbo.getPrice shouldBe 1000
      turbo.getOffersCount shouldBe 21
      turbo.hasBadgesCount shouldBe false
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 11 // 32 - 21
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 9 // 30 - 21
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(regionId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosRegionId)
      verify(testTree, times(1)).city(vosRegionId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(2)).dealerVasProductsFeatures
    }

    "return enabled trucks in regions: commercial:used, commercial:new" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(commercialCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(Some(ekbCityId)))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosEkbRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosEkbRegionId)).thenReturn(ekbRegion)
      when(testTree.city(vosEkbRegionId)).thenReturn(Some(ekbCityRegion))
      when(timeProvider.now()).thenReturn(now)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Commercial,
        USED,
        ekbRegionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(500000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(2500000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0))
        )
      )

      mockSalesmanPrices(
        Transport.Commercial,
        NEW,
        ekbRegionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 250000, 250000, 0),
          servicePrice(Boost, 45000, 45000, 0),
          servicePrice(SpecialOffer, 65000, 65000, 0),
          servicePrice(Badge, 85000, 85000, 0)
        ),
        List(
          rangePrice(0, protoPrice(50000, 50000, 0), protoPrice(60000, 60000, 0))
        )
      )

      val truckCategories = Set(
        "TRAILER",
        "TRUCK",
        "ARTIC",
        "BUS",
        "SWAP_BODY",
        "LCV",
        "AGRICULTURAL",
        "CONSTRUCTION",
        "AUTOLOADER",
        "CRANE",
        "DREDGE",
        "BULLDOZERS",
        "CRANE_HYDRAULICS",
        "MUNICIPAL"
      )

      def mockSectionOffersPriceFromToCount(section: String,
                                            priceFrom: String,
                                            priceTo: String,
                                            offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Trucks,
          Map(
            "trucks_category" -> truckCategories,
            "price_from" -> Set(priceFrom),
            "price_to" -> Set(priceTo),
            "state" -> Set(section),
            "with_autoru_expert" -> Set("NONE")
          ),
          offersCount
        )

      def mockSectionOffersPriceFromCount(section: String, priceFrom: String, offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Trucks,
          Map(
            "trucks_category" -> truckCategories,
            "price_from" -> Set(priceFrom),
            "state" -> Set(section),
            "with_autoru_expert" -> Set("NONE")
          ),
          offersCount
        )

      def mockProductSectionOffersCount(section: String, products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Trucks,
          Map(
            "trucks_category" -> truckCategories,
            "autoru_billing_service_type" -> products,
            "with_autoru_expert" -> Set("BOTH"),
            "state" -> Set(section)
          ),
          offersCount
        )

      def mockCategoriesSectionBadgesOffers(section: String): Unit = {
        mockBadgesOffers(CategorySelector.Trucks, Map("trucks_category" -> truckCategories, "state" -> Set(section)))
      }

      mockSectionOffersPriceFromToCount("USED", "1", "500000", 25)
      mockSectionOffersPriceFromToCount("USED", "500001", "2500000", 20)
      mockSectionOffersPriceFromCount("USED", "2500001", 15)
      mockProductSectionOffersCount("USED", Set("turbo"), 21)
      mockProductSectionOffersCount("USED", Set("top", "extended"), 32)
      mockProductSectionOffersCount("USED", Set("fresh"), 31)
      mockProductSectionOffersCount("USED", Set("special"), 30)
      mockProductSectionOffersCount("USED", Set("badge"), 29)
      mockCategoriesSectionBadgesOffers("USED")

      mockSectionOffersPriceFromCount("NEW", "1", 45)
      mockProductSectionOffersCount("NEW", Set("turbo"), 21)
      mockProductSectionOffersCount("NEW", Set("top", "extended"), 52)
      mockProductSectionOffersCount("NEW", Set("fresh"), 51)
      mockProductSectionOffersCount("NEW", Set("special"), 50)
      mockProductSectionOffersCount("NEW", Set("badge"), 49)
      mockCategoriesSectionBadgesOffers("NEW")

      val tariffs = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala
      tariffs should have size 2 // commercial:new + commercial:used
      tariffs.map(_.getCategory).distinct should contain only TRUCKS
      tariffs.map(_.getEnabled).distinct should contain only true
      tariffs.map(_.getQuotasList.asScala.toList).distinct should contain only Nil
      tariffs.map(_.getType).distinct should contain only SINGLE

      def getTariff(section: Section): Tariff = {
        val filtered = tariffs.filter(t => t.getCategory == TRUCKS && t.getSectionList.asScala.toSet == Set(section))
        filtered should have size 1
        filtered.headOption.value
      }

      val usedTariff = getTariff(USED)
      usedTariff.hasTruckClass shouldBe false
      val usedPlacementPrices = usedTariff.getPlacementPricesInfoList.asScala.toList
      usedPlacementPrices should have size 3
      val List(
        usedPlacementPrice0,
        usedPlacementPrice500,
        usedPlacementPrice2500
      ) = usedPlacementPrices: @unchecked
      usedPlacementPrice0.getPlacementPrice shouldBe 200
      usedPlacementPrice0.getProlongPrice shouldBe 100
      usedPlacementPrice0.getOffersCount shouldBe 25
      usedPlacementPrice0.getRange.getFrom shouldBe 0
      usedPlacementPrice0.getRange.getTo shouldBe 500000
      usedPlacementPrice500.getPlacementPrice shouldBe 400
      usedPlacementPrice500.getProlongPrice shouldBe 300
      usedPlacementPrice500.getOffersCount shouldBe 20
      usedPlacementPrice500.getRange.getFrom shouldBe 500000
      usedPlacementPrice500.getRange.getTo shouldBe 2500000
      usedPlacementPrice2500.getPlacementPrice shouldBe 800
      usedPlacementPrice2500.getProlongPrice shouldBe 700
      usedPlacementPrice2500.getOffersCount shouldBe 15
      usedPlacementPrice2500.getRange.getFrom shouldBe 2500000
      usedPlacementPrice2500.getRange.hasTo shouldBe false
      val usedServicePrices = usedTariff.getServicePricesList.asScala.toList
      usedServicePrices should have size 5

      def getUsedServicePrice(service: String): ServicePrice =
        usedServicePrices.find(_.getService == service).value

      val usedTurbo = getUsedServicePrice("turbo-package")
      val usedPremium = getUsedServicePrice("premium")
      val usedBoost = getUsedServicePrice("boost")
      val usedSpecial = getUsedServicePrice("special-offer")
      val usedBadge = getUsedServicePrice("badge")
      usedTurbo.getPrice shouldBe 1000
      usedTurbo.getOffersCount shouldBe 21
      usedTurbo.hasBadgesCount shouldBe false
      usedPremium.getPrice shouldBe 1500
      usedPremium.getOffersCount shouldBe 11 // 32 - 21
      usedPremium.hasBadgesCount shouldBe false
      usedBoost.getPrice shouldBe 350
      usedBoost.getOffersCount shouldBe 31
      usedBoost.hasBadgesCount shouldBe false
      usedSpecial.getPrice shouldBe 550
      usedSpecial.getOffersCount shouldBe 9 // 30 - 21
      usedSpecial.hasBadgesCount shouldBe false
      usedBadge.getPrice shouldBe 750
      usedBadge.getOffersCount shouldBe 29
      usedBadge.getBadgesCount shouldBe badgesCount

      val newTariff = getTariff(NEW)
      newTariff.hasTruckClass shouldBe false
      val newPlacementPrices = newTariff.getPlacementPricesInfoList.asScala.toList
      newPlacementPrices should have size 1
      val newPlacementPrice0 = newPlacementPrices.head
      newPlacementPrice0.getPlacementPrice shouldBe 500
      newPlacementPrice0.getProlongPrice shouldBe 600
      newPlacementPrice0.getOffersCount shouldBe 45
      newPlacementPrice0.getRange.getFrom shouldBe 0
      newPlacementPrice0.getRange.hasTo shouldBe false
      val newServicePrices = newTariff.getServicePricesList.asScala.toList
      newServicePrices should have size 5

      def getNewServicePrice(service: String): ServicePrice =
        newServicePrices.find(_.getService == service).value

      val newTurbo = getNewServicePrice("turbo-package")
      val newPremium = getNewServicePrice("premium")
      val newBoost = getNewServicePrice("boost")
      val newSpecial = getNewServicePrice("special-offer")
      val newBadge = getNewServicePrice("badge")
      newTurbo.getPrice shouldBe 1000
      newTurbo.getOffersCount shouldBe 21
      newTurbo.hasBadgesCount shouldBe false
      newPremium.getPrice shouldBe 2500
      newPremium.getOffersCount shouldBe 31 // 52 - 21
      newPremium.hasBadgesCount shouldBe false
      newBoost.getPrice shouldBe 450
      newBoost.getOffersCount shouldBe 51
      newBoost.hasBadgesCount shouldBe false
      newSpecial.getPrice shouldBe 650
      newSpecial.getOffersCount shouldBe 29 // 50 - 21
      newSpecial.hasBadgesCount shouldBe false
      newBadge.getPrice shouldBe 850
      newBadge.getOffersCount shouldBe 49
      newBadge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(Some(ekbCityId)))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosEkbRegionId)
      verify(testTree, times(1)).city(vosEkbRegionId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(2)).dealerVasProductsFeatures
    }

    "return call cars:new tariff" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.getQuotasList shouldBe empty

      val calls = tariff.getCalls
      calls.getPrice shouldBe 1200
      calls.getDepositCoefficient shouldBe 3

      val limits = calls.getLimits
      limits.getCurrentDaily.getFunds shouldBe 100
      limits.getCurrentDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1540933200L).build()
      limits.getCurrentWeekly.getFunds shouldBe 200
      limits.getCurrentWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1550933200L).build()
      limits.getCurrentMonthly.getFunds shouldBe 300
      limits.getCurrentMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1560933200L).build()
      limits.getComingDaily.getFunds shouldBe 400
      limits.getComingDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1570933200L).build()
      limits.getComingWeekly.getFunds shouldBe 500
      limits.getComingWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1580933200L).build()
      limits.getComingMonthly.getFunds shouldBe 600
      limits.getComingMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1590933200L).build()

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 4

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 32
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 30
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return call cars:new tariff without priorityPlacement attribute if client`s region is not msk or spb" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(cabinetApiClient.getClient(?)(?))
        .thenReturnF(ClientView(ClientPropertiesView("active", regionId = 123)))
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.getQuotasList shouldBe empty
      tariff.getPriorityPlacement shouldBe PriorityPlacement.UNAVAILABLE

      val calls = tariff.getCalls
      calls.getPrice shouldBe 1200
      calls.getDepositCoefficient shouldBe 3

      val limits = calls.getLimits
      limits.getCurrentDaily.getFunds shouldBe 100
      limits.getCurrentDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1540933200L).build()
      limits.getCurrentWeekly.getFunds shouldBe 200
      limits.getCurrentWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1550933200L).build()
      limits.getCurrentMonthly.getFunds shouldBe 300
      limits.getCurrentMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1560933200L).build()
      limits.getComingDaily.getFunds shouldBe 400
      limits.getComingDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1570933200L).build()
      limits.getComingWeekly.getFunds shouldBe 500
      limits.getComingWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1580933200L).build()
      limits.getComingMonthly.getFunds shouldBe 600
      limits.getComingMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1590933200L).build()

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 4

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 32
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 30
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return cars:new tariff with priorityPlacement:unknown" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(cabinetApiClient.getClient(?)(?))
        .thenReturnF(ClientView(ClientPropertiesView("active", regionId = 1)))
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.getQuotasList shouldBe empty
      tariff.getPriorityPlacement shouldBe PriorityPlacement.UNAVAILABLE

      val calls = tariff.getCalls
      calls.getPrice shouldBe 1200
      calls.getDepositCoefficient shouldBe 3

      val limits = calls.getLimits
      limits.getCurrentDaily.getFunds shouldBe 100
      limits.getCurrentDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1540933200L).build()
      limits.getCurrentWeekly.getFunds shouldBe 200
      limits.getCurrentWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1550933200L).build()
      limits.getCurrentMonthly.getFunds shouldBe 300
      limits.getCurrentMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1560933200L).build()
      limits.getComingDaily.getFunds shouldBe 400
      limits.getComingDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1570933200L).build()
      limits.getComingWeekly.getFunds shouldBe 500
      limits.getComingWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1580933200L).build()
      limits.getComingMonthly.getFunds shouldBe 600
      limits.getComingMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1590933200L).build()

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 4

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 32
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 30
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return call cars:new tariff without special-offer by feature" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)

      val feature = new Feature[String] {
        override def name: String = "dealer_special_offer_for_cars_new_disabled"

        override def value: String = "1989-06-16"
      }

      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(feature),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.getQuotasList shouldBe empty

      val calls = tariff.getCalls
      calls.getPrice shouldBe 1200
      calls.getDepositCoefficient shouldBe 3

      val limits = calls.getLimits
      limits.getCurrentDaily.getFunds shouldBe 100
      limits.getCurrentDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1540933200L).build()
      limits.getCurrentWeekly.getFunds shouldBe 200
      limits.getCurrentWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1550933200L).build()
      limits.getCurrentMonthly.getFunds shouldBe 300
      limits.getCurrentMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1560933200L).build()
      limits.getComingDaily.getFunds shouldBe 400
      limits.getComingDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1570933200L).build()
      limits.getComingWeekly.getFunds shouldBe 500
      limits.getComingWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1580933200L).build()
      limits.getComingMonthly.getFunds shouldBe 600
      limits.getComingMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1590933200L).build()

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 3

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val badge = getServicePrice("badge")
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 32
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      val special = servicePrices.find(_.getService == "all_sale_special")
      special shouldBe None

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return call cars:new tariff with just daily limit" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(dailyLimitCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.getQuotasList shouldBe empty

      val calls = tariff.getCalls
      calls.getPrice shouldBe 1200
      calls.getDepositCoefficient shouldBe 3

      val limits = calls.getLimits
      limits.getCurrentDaily.getFunds shouldBe 100
      limits.getCurrentDaily.hasEffectiveSince shouldBe false
      limits.hasCurrentWeekly shouldBe false
      limits.hasCurrentMonthly shouldBe false
      limits.hasComingDaily shouldBe false
      limits.hasComingWeekly shouldBe false
      limits.hasComingMonthly shouldBe false

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 4

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 32
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 30
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return call cars:new tariff without limits" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(noLimitCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.getQuotasList shouldBe empty

      val calls = tariff.getCalls
      calls.getPrice shouldBe 1200
      calls.getDepositCoefficient shouldBe 3
      calls.hasLimits shouldBe false

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 4

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 32
      premium.hasBadgesCount shouldBe false
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      boost.hasBadgesCount shouldBe false
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 30
      special.hasBadgesCount shouldBe false
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return call cars:new tariff when billing campaign doesn't exist" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(None)
      when(salesmanClient.getCallInfo(testDealer))
        .thenReturnF(CallInfo(callCost = 50000, depositCoefficient = 3))
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getPlacementPricesInfoList shouldBe empty
      tariff.getQuotasList shouldBe empty

      val calls = tariff.getCalls
      calls.getPrice shouldBe 500
      calls.getDepositCoefficient shouldBe 3
      calls.hasLimits shouldBe false

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 4

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      premium.getPrice shouldBe 1500
      premium.getOffersCount shouldBe 32
      boost.getPrice shouldBe 350
      boost.getOffersCount shouldBe 31
      special.getPrice shouldBe 550
      special.getOffersCount shouldBe 30
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return call cars:used tariff" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCarsUsedCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCarsUsedCampaign(testDealer)).thenReturnF(Some(billingCallCarsUsedCampaign))
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        USED,
        geoId,
        List(
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockOffersPriceFromToCount(priceFrom: String, priceTo: String, offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("price_from" -> Set(priceFrom), "price_to" -> Set(priceTo), "with_autoru_expert" -> Set("NONE")),
          offersCount,
          "USED"
        )

      def mockOffersPriceFromCount(priceFrom: String, offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("price_from" -> Set(priceFrom), "with_autoru_expert" -> Set("NONE")),
          offersCount,
          "USED"
        )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "USED"
        )

      mockOffersPriceFromToCount("1", "300000", 25)
      mockOffersPriceFromToCount("300001", "500000", 20)
      mockOffersPriceFromToCount("500001", "800000", 15)
      mockOffersPriceFromToCount("800001", "1500000", 10)
      mockOffersPriceFromCount("1500001", 5)
      mockProductOffersCount(Set("special"), 29)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("USED")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only USED
      tariff.getType shouldBe SINGLE_WITH_CALLS
      tariff.getEnabled shouldBe true
      val placementPrices = tariff.getPlacementPricesInfoList.asScala.toList
      placementPrices should have size 5
      val List(
        resultPlacementPrice0,
        resultPlacementPrice300,
        resultPlacementPrice500,
        resultPlacementPrice800,
        resultPlacementPrice1500
      ) = placementPrices: @unchecked
      resultPlacementPrice0.getPlacementPrice shouldBe 200
      resultPlacementPrice0.getProlongPrice shouldBe 100
      resultPlacementPrice0.getOffersCount shouldBe 25
      resultPlacementPrice300.getPlacementPrice shouldBe 400
      resultPlacementPrice300.getProlongPrice shouldBe 300
      resultPlacementPrice300.getOffersCount shouldBe 20
      resultPlacementPrice500.getPlacementPrice shouldBe 600
      resultPlacementPrice500.getProlongPrice shouldBe 500
      resultPlacementPrice500.getOffersCount shouldBe 15
      resultPlacementPrice800.getPlacementPrice shouldBe 800
      resultPlacementPrice800.getProlongPrice shouldBe 700
      resultPlacementPrice800.getOffersCount shouldBe 10
      resultPlacementPrice1500.getPlacementPrice shouldBe 1000
      resultPlacementPrice1500.getProlongPrice shouldBe 900
      resultPlacementPrice1500.getOffersCount shouldBe 5
      tariff.getQuotasList shouldBe empty

      val calls = tariff.getCalls
      // цену звонка не надо заполнять, только лимиты
      calls.getPrice shouldBe 0
      calls.getDepositCoefficient shouldBe 0

      val limits = calls.getLimits
      limits.getCurrentDaily.getFunds shouldBe 100
      limits.getCurrentDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1540933200L).build()
      limits.getCurrentWeekly.getFunds shouldBe 200
      limits.getCurrentWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1550933200L).build()
      limits.getCurrentMonthly.getFunds shouldBe 300
      limits.getCurrentMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1560933200L).build()
      limits.getComingDaily.getFunds shouldBe 400
      limits.getComingDaily.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1570933200L).build()
      limits.getComingWeekly.getFunds shouldBe 500
      limits.getComingWeekly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1580933200L).build()
      limits.getComingMonthly.getFunds shouldBe 600
      limits.getComingMonthly.getEffectiveSince shouldBe Timestamp.newBuilder().setSeconds(1590933200L).build()

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 2

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val badge = getServicePrice("badge")
      badge.getPrice shouldBe 750
      badge.getOffersCount shouldBe 29
      badge.getBadgesCount shouldBe badgesCount

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return sorted tariffs" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true))
        .thenReturnF(Set(callCampaign, carsUsedCampaign, motoCampaign, commercialCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(regionId), eq(testClientMarksList), eq(None))(?)).thenReturnF(quotas)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosRegionId)).thenReturn(testRegion)
      when(testTree.city(vosRegionId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        regionId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1000000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0)),
          rangePrice(1500000, protoPrice(120000, 120000, 0), protoPrice(110000, 110000, 0)),
          rangePrice(3000000, protoPrice(140000, 140000, 0), protoPrice(130000, 130000, 0))
        )
      )
      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        regionId,
        List.empty,
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0))
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1000000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0)),
          rangePrice(1500000, protoPrice(120000, 120000, 0), protoPrice(110000, 110000, 0)),
          rangePrice(3000000, protoPrice(140000, 140000, 0), protoPrice(130000, 130000, 0))
        )
      )
      mockSalesmanPrices(
        Transport.Moto,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )
      mockSalesmanPrices(
        Transport.Commercial,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(500000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(1000000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(2000000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(4000000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )
      mockSalesmanPrices(
        Transport.Lcv,
        NEW,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )
      mockSalesmanPrices(
        Transport.Lcv,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )
      mockSalesmanPrices(
        Transport.Special,
        NEW,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0))
        )
      )
      mockSalesmanPrices(
        Transport.Special,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0))
        )
      )

      when(searcherClient.offersCount(?, ?)(?)).thenReturnF(
        OfferCountResponse.newBuilder().setCount(10).build()
      )
      when(searcherManager.searchOffers(?, ?)(?)).thenReturn(badgesOffers)

      mockCallPlacementFeature(featureValue = true)

      val tariffs = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.toList

      def getTariffIndex(category: Category, sections: Set[Section], truckClass: Option[TruckClass]) = {
        val index = tariffs.indexWhere { tariff =>
          tariff.getCategory == category &&
          tariff.getSectionList.asScala.toSet == sections &&
          truckClass.forall(_ == tariff.getTruckClass)
        }
        // Чтобы проще было найти баг с ненайденным элементом из expectedOrder в tariffs,
        // кидаем ошибку на -1 от indexWhere
        withClue(s"getTariffIndex($category, $sections, $truckClass)") {
          index should not be -1
        }
        index
      }

      val expectedOrder = List(
        (CARS, Set(USED), None),
        (CARS, Set(NEW), None),
        (TRUCKS, Set(USED), Some(TruckClass.LCV)),
        (TRUCKS, Set(NEW), Some(TruckClass.LCV)),
        (TRUCKS, Set(NEW, USED), Some(TruckClass.COMMERCIAL)),
        (TRUCKS, Set(NEW, USED), Some(TruckClass.SPECIAL)),
        (MOTO, Set(NEW, USED), None)
      )
      val actualOrder = expectedOrder.sortBy((getTariffIndex _).tupled)
      expectedOrder shouldBe actualOrder

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(regionId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosRegionId)
      verify(testTree, times(1)).city(vosRegionId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(10)).dealerVasProductsFeatures
    }

    "return sorted service_prices" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(carsUsedCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(regionId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosRegionId)).thenReturn(testRegion)
      when(testTree.city(vosRegionId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      mockSalesmanPrices(
        Transport.Cars,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1000000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0)),
          rangePrice(1500000, protoPrice(120000, 120000, 0), protoPrice(110000, 110000, 0)),
          rangePrice(3000000, protoPrice(140000, 140000, 0), protoPrice(130000, 130000, 0))
        )
      )
      when(searcherClient.offersCount(?, ?)(?))
        .thenReturnF(OfferCountResponse.newBuilder().setCount(1).build())

      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      val servicePrices = tariff.getServicePricesList.asScala.toList

      (servicePrices.map(_.getService) should contain)
        .theSameElementsInOrderAs(Seq("turbo-package", "premium", "boost", "special-offer", "badge"))
    }

    "return prolongable" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(carsUsedCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(regionId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosRegionId)).thenReturn(testRegion)
      when(testTree.city(vosRegionId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      mockSalesmanPrices(
        Transport.Cars,
        USED,
        regionId,
        List(
          servicePrice(PackageTurbo, 100000, 100000, 0),
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1000000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0)),
          rangePrice(1500000, protoPrice(120000, 120000, 0), protoPrice(110000, 110000, 0)),
          rangePrice(3000000, protoPrice(140000, 140000, 0), protoPrice(130000, 130000, 0))
        )
      )
      when(searcherClient.offersCount(?, ?)(?))
        .thenReturnF(OfferCountResponse.newBuilder().setCount(1).build())
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices.size shouldBe 5
      servicePrices.find(_.getService == "turbo-package").value.getProlongable shouldBe false
      servicePrices.find(_.getService == "premium").value.getProlongable shouldBe true
      servicePrices.find(_.getService == "boost").value.getProlongable shouldBe false
      servicePrices.find(_.getService == "special-offer").value.getProlongable shouldBe true
      servicePrices.find(_.getService == "badge").value.getProlongable shouldBe true
    }

    "return prices with discount" in {
      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(disabledCarsUsedCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosEkbRegionId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosEkbRegionId)).thenReturn(ekbRegion)
      when(testTree.city(vosEkbRegionId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        USED,
        ekbRegionId,
        List(
          servicePrice(PackageTurbo, 100000, 10000, 90),
          servicePrice(Premium, 150000, 135000, 10),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 10000, 50), protoPrice(10000, 2000, 80)),
          rangePrice(300000, protoPrice(40000, 30000, 25), protoPrice(30000, 27000, 10)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      val placementPrices = tariff.getPlacementPricesInfoList.asScala.toList
      placementPrices should have size 5
      val List(
        resultPlacementPrice0,
        resultPlacementPrice300,
        resultPlacementPrice500,
        resultPlacementPrice800,
        resultPlacementPrice1500
      ) = placementPrices: @unchecked
      resultPlacementPrice0.getPlacementPrice shouldBe 200
      resultPlacementPrice0.getPlacementFinalPrice shouldBe 100
      resultPlacementPrice0.getPlacementDiscountPercent shouldBe 50
      resultPlacementPrice0.getProlongPrice shouldBe 100
      resultPlacementPrice0.getProlongFinalPrice shouldBe 20
      resultPlacementPrice0.getProlongDiscountPercent shouldBe 80
      resultPlacementPrice300.getPlacementPrice shouldBe 400
      resultPlacementPrice300.getPlacementFinalPrice shouldBe 300
      resultPlacementPrice300.getPlacementDiscountPercent shouldBe 25
      resultPlacementPrice300.getProlongPrice shouldBe 300
      resultPlacementPrice300.getProlongFinalPrice shouldBe 270
      resultPlacementPrice300.getProlongDiscountPercent shouldBe 10
      resultPlacementPrice500.getPlacementPrice shouldBe 600
      resultPlacementPrice500.getPlacementFinalPrice shouldBe 600
      resultPlacementPrice500.getPlacementDiscountPercent shouldBe 0
      resultPlacementPrice500.getProlongPrice shouldBe 500
      resultPlacementPrice500.getProlongFinalPrice shouldBe 500
      resultPlacementPrice500.getProlongDiscountPercent shouldBe 0
      resultPlacementPrice800.getPlacementPrice shouldBe 800
      resultPlacementPrice800.getPlacementFinalPrice shouldBe 800
      resultPlacementPrice800.getPlacementDiscountPercent shouldBe 0
      resultPlacementPrice800.getProlongPrice shouldBe 700
      resultPlacementPrice800.getProlongFinalPrice shouldBe 700
      resultPlacementPrice800.getProlongDiscountPercent shouldBe 0
      resultPlacementPrice1500.getPlacementPrice shouldBe 1000
      resultPlacementPrice1500.getPlacementFinalPrice shouldBe 1000
      resultPlacementPrice1500.getPlacementDiscountPercent shouldBe 0
      resultPlacementPrice1500.getProlongPrice shouldBe 900
      resultPlacementPrice1500.getProlongFinalPrice shouldBe 900
      resultPlacementPrice1500.getProlongDiscountPercent shouldBe 0
      val servicePrices = tariff.getServicePricesList.asScala.toList
      servicePrices should have size 5

      def getServicePrice(service: String): ServicePrice =
        servicePrices.find(_.getService == service).value

      val turbo = getServicePrice("turbo-package")
      val premium = getServicePrice("premium")
      val boost = getServicePrice("boost")
      val special = getServicePrice("special-offer")
      val badge = getServicePrice("badge")
      turbo.getPrice shouldBe 1000
      turbo.getFinalPrice shouldBe 100
      turbo.getDiscountPercent shouldBe 90
      premium.getPrice shouldBe 1500
      premium.getFinalPrice shouldBe 1350
      premium.getDiscountPercent shouldBe 10
      boost.getPrice shouldBe 350
      boost.getFinalPrice shouldBe 350
      boost.getDiscountPercent shouldBe 0
      special.getPrice shouldBe 550
      special.getFinalPrice shouldBe 550
      special.getDiscountPercent shouldBe 0
      badge.getPrice shouldBe 750
      badge.getFinalPrice shouldBe 750
      badge.getDiscountPercent shouldBe 0

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(ekbRegionId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosEkbRegionId)
      verify(testTree, times(1)).city(vosEkbRegionId)
      verify(timeProvider, times(1)).now()
    }
  }

  "DealerTariffManager.tariffsForDealer() for client" should {
    implicit val r: Request = clientRequest

    "return editable = true for regular client" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue
      tariff.getEditable shouldBe true

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return editable = false for agency client" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClientWithAgency)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue
      tariff.getEditable shouldBe false

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "dont return call placement cars:new tariff on disabled feature" in {
      val geo = callRegionPlacement
      val geoId = callRegionIdPlacement
      val vosGeoId = vosCallRegionIdPlacement

      mockCallPlacementFeature(featureValue = false)

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1000000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0)),
          rangePrice(1500000, protoPrice(120000, 120000, 0), protoPrice(110000, 110000, 0)),
          rangePrice(3000000, protoPrice(140000, 140000, 0), protoPrice(130000, 130000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getQuotasList shouldBe empty

      tariff.getPlacementPricesInfoList.asScala shouldBe Nil

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return call placement cars:new tariff" in {
      val geo = callRegionPlacement
      val geoId = callRegionIdPlacement
      val vosGeoId = vosCallRegionIdPlacement

      mockCallPlacementFeature(featureValue = true)

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue.getTariffsList.asScala.head

      tariff.getCategory shouldBe CARS
      tariff.hasTruckClass shouldBe false
      tariff.getSectionList should contain only NEW
      tariff.getType shouldBe CALLS
      tariff.getEnabled shouldBe true
      tariff.getQuotasList shouldBe empty

      tariff.getPlacementPricesInfoList.asScala shouldBe List(
        PlacementPriceInfo
          .newBuilder()
          .setPlacementPrice(200L)
          .setPlacementFinalPrice(200L)
          .setPlacementDiscountPercent(0)
          .build()
      )

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }
  }

  "DealerTariffManager.tariffsForDealer() for agency" should {
    implicit val r: Request = agencyRequest

    "return editable = true for agency" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClientWithAgency)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue
      tariff.getEditable shouldBe true

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }
  }

  "DealerTariffManager.tariffsForDealer() for company" should {
    implicit val r: Request = companyRequest

    "return editable = true for company with regular client" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue
      tariff.getEditable shouldBe true

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }

    "return editable = false for company and agency client" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClientWithAgency)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue
      tariff.getEditable shouldBe false

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
      verify(featureManager, times(1)).dealerVasProductsFeatures
    }
  }

  "DealerTariffManager.tariffsForDealer() for manager" should {
    implicit val r: Request = managerRequest

    "return editable = true for manager with regular client" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClient)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue
      tariff.getEditable shouldBe true

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
    }

    "return editable = false for manager and agency client" in {
      val geo = callRegionNoPlacement
      val geoId = callRegionIdNoPlacement
      val vosGeoId = vosCallRegionIdNoPlacement

      when(salesmanClient.getCampaigns(testDealer, includeDisabled = true)).thenReturnF(Set(callCampaign))
      when(cabinetApiClient.getBalanceClient(eq(testDealer))(?)).thenReturnF(testBalanceClientWithAgency)
      when(cabinetApiClient.getClientMarks(eq(testDealer), ?, ?)(?)).thenReturnF(testClientMarks)
      when(moishaClient.getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)).thenReturnF(Nil)
      when(vosClient.getDealerRegionId(AutoruDealer(testClientId))).thenReturnF(vosGeoId)
      when(dataService.tree).thenReturn(testTree)
      when(testTree.unsafeFederalSubject(vosGeoId)).thenReturn(geo)
      when(testTree.city(vosGeoId)).thenReturn(None)
      when(timeProvider.now()).thenReturn(now)
      when(salesmanClient.getBillingCallCampaign(testDealer)).thenReturnF(Some(billingCallCampaign))
      when(salesmanClient.getCallInfo(testDealer)).thenReturnF(callInfo)
      when(featureManager.dealerVasProductsFeatures).thenReturn(
        DealerVasProductsFeatures(
          Seq(),
          Feature("", _ => true),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long]),
          Feature("", _ => List.empty[Long])
        )
      )

      mockSalesmanPrices(
        Transport.Cars,
        NEW,
        geoId,
        List(
          servicePrice(Premium, 150000, 150000, 0),
          servicePrice(Boost, 35000, 35000, 0),
          servicePrice(SpecialOffer, 55000, 55000, 0),
          servicePrice(Badge, 75000, 75000, 0)
        ),
        List(
          rangePrice(0, protoPrice(20000, 20000, 0), protoPrice(10000, 10000, 0)),
          rangePrice(300000, protoPrice(40000, 40000, 0), protoPrice(30000, 30000, 0)),
          rangePrice(500000, protoPrice(60000, 60000, 0), protoPrice(50000, 50000, 0)),
          rangePrice(800000, protoPrice(80000, 80000, 0), protoPrice(70000, 70000, 0)),
          rangePrice(1500000, protoPrice(100000, 100000, 0), protoPrice(90000, 90000, 0))
        )
      )

      def mockProductOffersCount(products: Set[String], offersCount: Int): Unit =
        mockOffersCount(
          CategorySelector.Cars,
          Map("autoru_billing_service_type" -> products, "with_autoru_expert" -> Set("BOTH")),
          offersCount,
          "NEW"
        )

      mockProductOffersCount(Set("top", "extended"), 32)
      mockProductOffersCount(Set("fresh"), 31)
      mockProductOffersCount(Set("special"), 30)
      mockProductOffersCount(Set("badge"), 29)
      mockBadgesOffers(CategorySelector.Cars, Map("state" -> Set("NEW")))

      val tariff = manager.tariffsForDealer(FetchSettings(true, true, true)).futureValue
      tariff.getEditable shouldBe true

      verify(salesmanClient, times(1)).getCampaigns(testDealer, includeDisabled = true)
      verify(cabinetApiClient, times(1)).getBalanceClient(testDealer)
      verify(cabinetApiClient, times(1)).getClientMarks(eq(testDealer), ?, ?)(?)
      verify(moishaClient, times(1)).getQuotas(eq(geoId), eq(testClientMarksList), eq(None))(?)
      verify(vosClient, times(1)).getDealerRegionId(AutoruDealer(testClientId))
      verify(dataService, times(1)).tree
      verify(testTree, times(1)).unsafeFederalSubject(vosGeoId)
      verify(testTree, times(1)).city(vosGeoId)
      verify(timeProvider, times(1)).now()
    }
  }
}
