package ru.auto.salesman.service.tskv.dealer.format.impl

import org.joda.time.DateTime
import ru.auto.salesman.model.ProductId.TradeInRequestCarsNew
import ru.auto.salesman.model.{
  BalanceClient,
  CityId,
  Client,
  ClientStatuses,
  DetailedClient,
  RegionId
}
import ru.auto.salesman.service.BillingEventProcessor.BillingEventResponse
import ru.auto.salesman.service.PriceEstimateService.PriceRequest
import ru.auto.salesman.service.PriceEstimateService.PriceRequest.DefaultClientOffer
import ru.auto.salesman.service.tskv.tskvFormat
import ru.auto.salesman.tasks.trade_in.TradeInRequestsBilling
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.DateTimeInterval
import ru.auto.salesman.util.tskv.TskvByNameConverter
import scala.util.Success

class TradeInRequestsBillingTskvLogFormatSpec extends BaseSpec {

  val converter = new TradeInRequestsBillingTskvLogFormat()
  "TskvLoggedTradeInApplyService" should {
    "success all fields must be field" in {
      val args = converter.apply(
        generateTradeInRequest(),
        Success(generateTradeInResponse())
      )
      val resultLog = TskvByNameConverter.toTskv(args)(tskvFormat).get
      val resultMap = ResultLogConverter.convert(resultLog)

      resultMap.size shouldBe resultLog.split("\t").length
      resultMap.size shouldBe 17

      resultMap("tskv") shouldBe None
      resultMap("tskv_format") shouldBe Some("vertis-aggregated-log")
      resultMap("action") shouldBe Some("activate")
      resultMap.get("offerId") shouldBe None
      resultMap.get("offerCategory") shouldBe None
      resultMap.get("offerSection") shouldBe None
      resultMap.get("offerCategoryNumber") shouldBe None
      resultMap("product") shouldBe Some("trade-in-request:cars:new")
      resultMap("price") shouldBe Some("698")
      resultMap("actualPrice") shouldBe Some("3987")
      resultMap("hold") shouldBe Some("tradeInHoldId-223223")

      resultMap("clientId") shouldBe Some("123")
      resultMap("agencyId") shouldBe Some("223")
      resultMap("companyId") shouldBe Some("99")
      resultMap("regionId") shouldBe Some("66")

      resultMap("featureId") shouldBe Some("4542323")
      resultMap("featureType") shouldBe Some("promocode")
      resultMap("featureAmount") shouldBe Some("4")
      resultMap("loyaltyDiscountId") shouldBe Some("loyality-4542323")
      resultMap("loyaltyDiscountAmount") shouldBe Some("455554")

      resultMap.get("quotaSize") shouldBe None
      resultMap("isQuota") shouldBe Some("false")
      resultMap.get("vin") shouldBe None
      resultMap.get("deliveryRegionsIds") shouldBe None
      resultMap.get("deliveryRegionsPrice") shouldBe None
    }
  }

  private def generateTradeInRequest(): TradeInRequestsBilling.Request = {
    val priceRequest = new PriceRequest(
      offer = Some(DefaultClientOffer),
      context = PriceRequest.ClientContext(
        clientRegionId = RegionId(22),
        offerPlacementDay = Some(669),
        productTariff = None
      ),
      product = TradeInRequestCarsNew,
      interval = DateTimeInterval(DateTime.now().minusDays(4), DateTime.now()),
      priceRequestId = None
    )

    TradeInRequestsBilling.Request(
      product = TradeInRequestCarsNew,
      requestId = 1232,
      priceRequest = priceRequest,
      client = generateDetailedClient()
    )
  }

  private def generateDetailedClient(): DetailedClient = {
    val client = Client(
      clientId = 123,
      agencyId = Some(122),
      categorizedClientId = Some(3),
      companyId = Some(99),
      regionId = RegionId(66),
      cityId = CityId(89),
      status = ClientStatuses.New,
      singlePayment = Set.empty,
      firstModerated = false,
      name = None,
      paidCallsAvailable = false,
      priorityPlacement = false
    )
    DetailedClient(
      client = client,
      balanceClient = BalanceClient(
        clientId = client.clientId,
        balanceClientId = 696,
        balanceAgencyId = Some(223),
        accountId = 6985,
        amount = 32687
      )
    )
  }

  private def generateTradeInResponse(): BillingEventResponse =
    BillingEventResponse(
      deadline = DateTime.now(),
      price = 698,
      actualPrice = 3987,
      promocodeFeature = List(
        PriceModifierFeatureGenerator.generateNonLoaylityFeature(),
        PriceModifierFeatureGenerator.generateLoyalityFeature()
      ),
      holdId = "tradeInHoldId-223223",
      client = generateDetailedClient()
    )
}
