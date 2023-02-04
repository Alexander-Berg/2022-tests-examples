package ru.auto.salesman.service.tskv.dealer.format.impl

import org.joda.time.DateTime
import ru.auto.salesman.model.Product.ProductPaymentStatus
import ru.auto.salesman.model.ProductId.VinHistory
import ru.auto.salesman.model.{
  ActiveProductNaturalKey,
  BalanceClient,
  CityId,
  Client,
  ClientStatuses,
  DetailedClient,
  PriceModifierFeature,
  Product,
  RegionId,
  UniqueProductType
}
import ru.auto.salesman.service.BillingEventProcessor.BillingEventResponse
import ru.auto.salesman.service.tskv.tskvFormat
import ru.auto.salesman.tasks.credit.ProductsBilling.ProductBillingRequest
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.tskv.TskvByNameConverter

import scala.util.Success

class ProductsBillingTskvLogFormatSpec extends BaseSpec {

  val converter = new ProductsBillingTskvLogFormat()

  "TskvLogFormatterProductsBilling" should {

    "success all fields must be field" in {
      val args = converter.apply(
        generateProductBillingRequest(),
        Success(generateBillingEventResponse())
      )
      val resultLog = TskvByNameConverter.toTskv(args)(tskvFormat).get
      val resultMap = ResultLogConverter.convert(resultLog)
      resultMap.size shouldBe resultLog.split("\t").size
      println(resultLog)
      resultMap("tskv") shouldBe None
      resultMap("tskv_format") shouldBe Some("vertis-aggregated-log")
      resultMap("action") shouldBe Some("activate")

      resultMap.get("offerId") shouldBe None
      resultMap("offerCategory") shouldBe Some("cars")
      resultMap("offerSection") shouldBe Some("new")
      resultMap("offerCategoryNumber") shouldBe Some("15")
      resultMap("product") shouldBe Some("vin-history")
      resultMap("price") shouldBe Some("223")
      resultMap("actualPrice") shouldBe Some("221")
      resultMap("hold") shouldBe Some("holdId-BillingEventResponse")
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

  private def generateProductBillingRequest(): ProductBillingRequest =
    ProductBillingRequest(
      client = generateDetailedClient(),
      priceRequest = null,
      product = Product(
        id = 23,
        key = new ActiveProductNaturalKey(
          payer = "payer",
          target = "cars:new",
          uniqueProductType = UniqueProductType.ApplicationCreditAccess
        ),
        status = ProductPaymentStatus.Active,
        createDate = DateTime.now().minusDays(4),
        // Заполняется при обиливании
        expireDate = None,
        // Заполняется при обиливании
        context = None,
        // Заполняется при выключении
        inactiveReason = None,
        prolongable = true,
        pushed = true,
        tariff = None
      ),
      productId = VinHistory
    )

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

  private def generateBillingEventResponse(): BillingEventResponse =
    BillingEventResponse(
      deadline = DateTime.now(),
      price = 223,
      actualPrice = 221,
      promocodeFeature = List[PriceModifierFeature](
        PriceModifierFeatureGenerator.generateLoyalityFeature(),
        PriceModifierFeatureGenerator.generateNonLoaylityFeature()
      ),
      holdId = "holdId-BillingEventResponse",
      client = generateDetailedClient()
    )
}
