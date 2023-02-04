package ru.auto.salesman.service.tskv.dealer.format.impl

import org.joda.time.DateTime
import ru.auto.salesman.controller.QuotaController
import ru.auto.salesman.controller.QuotaController.{NoAction, NoCampaign, Response}
import ru.auto.salesman.model.{
  FeatureCount,
  Quota,
  QuotaEntities,
  QuotaRequest,
  QuotaTypes,
  RegionId
}
import ru.auto.salesman.service.tskv.tskvFormat
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.tskv.TskvByNameConverter
import ru.yandex.vertis.billing.Model.OfferBilling

import scala.util.Success

class QuotaControllerTskvLogFormatSpec extends BaseSpec {

  "TskvLoggedQuotaApplyAction" should {
    val converter = new QuotaControllerTskvLogFormat()

    "success all fields must be field" in {
      val response = generateQuoutaResponse()
      val args = converter.apply(
        generateQuotaRequest(),
        Success(response)
      )
      val resultLog = TskvByNameConverter.toTskv(args)(tskvFormat).get
      val resultMap = ResultLogConverter.convert(resultLog)

      resultMap.size shouldBe resultLog.split("\t").size
      resultMap.size shouldBe 16
      resultMap("tskv") shouldBe None
      resultMap("tskv_format") shouldBe Some("vertis-aggregated-log")
      resultMap("action") shouldBe Some("activate")
      resultMap.get("offerId") shouldBe None
      resultMap.get("offerCategory") shouldBe None
      resultMap.get("offerSection") shouldBe None
      resultMap.get("offerCategoryNumber") shouldBe None
      resultMap("product") shouldBe Some("quota:placement:cars:new")
      resultMap("price") shouldBe Some("600")
      resultMap("actualPrice") shouldBe Some("549")
      resultMap("hold") shouldBe Some("quota-hold-id")
      resultMap("clientId") shouldBe Some("123")
      resultMap("agencyId") shouldBe Some("899")
      resultMap("companyId") shouldBe Some("898")
      resultMap("regionId") shouldBe Some("66")
      resultMap("featureId") shouldBe Some("4542323")
      resultMap("featureType") shouldBe Some("promocode")
      resultMap("featureAmount") shouldBe Some("549")
      resultMap.get("loyaltyDiscountId") shouldBe None
      resultMap.get("loyaltyDiscountAmount") shouldBe None
      resultMap("quotaSize") shouldBe Some("10")
      resultMap("isQuota") shouldBe Some("true")
      resultMap.get("vin") shouldBe None
      resultMap.get("deliveryRegionsIds") shouldBe None
      resultMap.get("deliveryRegionsPrice") shouldBe None

      converter.skipRow(response) shouldBe false
    }

    "skip row if action === NoAction" in {
      val response = generateQuoutaResponse().copy(
        action = NoAction()
      )
      converter.skipRow(response) shouldBe true
    }

    "didn't skip row if action === NoCampaign" in {
      val response = generateQuoutaResponse().copy(
        action = NoCampaign(null)
      )
      converter.skipRow(response) shouldBe false
    }
  }

  private def generateQuotaRequest(): QuotaRequest =
    QuotaRequest(
      clientId = 123,
      quotaType = QuotaTypes.withName("quota:placement:cars:new"),
      settings = QuotaRequest.Settings(
        size = 10,
        days = 4,
        price = Some(42),
        entity = QuotaEntities.Dealer
      ),
      from = DateTime.now().minusDays(3),
      regionId = None
    )

  private def generateQuoutaResponse(): Response = {
    val quota = Quota(
      clientId = 123,
      quotaType = QuotaTypes.withName("quota:placement:cars:new"),
      size = 10,
      revenue = 10,
      price = 10,
      from = DateTime.now().minusDays(3),
      to = DateTime.now(),
      offerBilling = OfferBilling.newBuilder().setVersion(134).build(),
      regionId = Some(RegionId(33)),
      entity = QuotaEntities.Dealer
    )
    Response(
      action = QuotaController.Activate(
        quota = quota,
        clientId = 123
      ),
      price = Some(600),
      actualPrice = Some(549),
      holdId = Some("quota-hold-id"),
      clientId = Some(123),
      agencyId = Some(899),
      companyId = Some(898),
      regionId = Some(RegionId(66)),
      feature = Some(
        PriceModifierFeatureGenerator.generateNonLoaylityFeature().feature
      ),
      featureCount = Some(FeatureCount.Items(72))
    )
  }
}
