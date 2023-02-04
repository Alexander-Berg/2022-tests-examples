package ru.auto.salesman.service.tskv.dealer.format.impl

import org.joda.time.DateTime
import ru.auto.api.ResponseModel.VinHistoryApplyResponse.PaymentStatus
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.{
  AdsRequestType,
  BalanceClient,
  CityId,
  Client,
  ClientStatuses,
  DetailedClient,
  PriceModifierFeature,
  RegionId
}
import ru.auto.salesman.service.BillingEventProcessor.BillingEventResponse
import ru.auto.salesman.service.VinHistoryService.{Request, Response}
import ru.auto.salesman.service.tskv.tskvFormat
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.tskv.TskvByNameConverter

import scala.util.Success

class DealerVinHistoryApplyServiceTskvLogFormatSpec extends BaseSpec {
  "VinHistoryTskvDealerLogFormat" should {
    val converter = new DealerVinHistoryApplyServiceTskvLogFormat()

    "success all fields must be filled" in {
      val request = requestGenerator()
      val response = responseGenerator()
      val args = converter.apply(request, Success(response))
      val logString = TskvByNameConverter.toTskv(args, Nil)(tskvFormat).get
      val resultMap = ResultLogConverter.convert(logString)

      resultMap.size shouldBe logString.split("\t").size
      resultMap.size shouldBe 19

      resultMap("tskv") shouldBe None
      resultMap("tskv_format") shouldBe Some("vertis-aggregated-log")
      resultMap("action") shouldBe Some("activate")
      resultMap("offerId") shouldBe Some("2344423")
      resultMap.get("offerCategory") shouldBe None
      resultMap.get("offerSection") shouldBe None
      resultMap.get("offerCategoryNumber") shouldBe None
      resultMap("product") shouldBe Some("vin-history")
      resultMap("price") shouldBe Some("456")
      resultMap("actualPrice") shouldBe Some("459")
      resultMap("hold") shouldBe Some("TransactionId")
      resultMap("clientId") shouldBe Some("11")
      resultMap("agencyId") shouldBe Some("423")
      resultMap("companyId") shouldBe Some("14")
      resultMap("regionId") shouldBe Some("22")

      resultMap("featureId") shouldBe Some("4542323")
      resultMap("featureType") shouldBe Some("promocode")
      resultMap("featureAmount") shouldBe Some("4")

      resultMap("loyaltyDiscountId") shouldBe Some("loyality-4542323")
      resultMap("loyaltyDiscountAmount") shouldBe Some("455554")

      resultMap.get("quotaSize") shouldBe None
      resultMap("isQuota") shouldBe Some("false")

      resultMap("vin") shouldBe Some("VINTest")

      resultMap.get("deliveryRegionsIds") shouldBe None
      resultMap.get("deliveryRegionsPrice") shouldBe None

      converter.skipRow(response) shouldBe false
    }
  }

  private def requestGenerator(): Request =
    Request(
      clientId = 123456,
      vin = "VINTest",
      offerId = Some(AutoruOfferId("2344423-kdlkskj"))
    )

  private def responseGenerator(): Response =
    Response(
      paymentStatus = PaymentStatus.OK,
      billingEventResponse = BillingEventResponse(
        deadline = DateTime.now(),
        price = 456,
        actualPrice = 459,
        promocodeFeature = List[PriceModifierFeature](
          PriceModifierFeatureGenerator.generateNonLoaylityFeature(),
          PriceModifierFeatureGenerator.generateLoyalityFeature()
        ),
        holdId = "TransactionId",
        client = DetailedClient(
          client = Client(
            clientId = 11L,
            agencyId = Some(12L),
            categorizedClientId = Some(13L),
            companyId = Some(14L),
            regionId = RegionId(22),
            cityId = CityId(332),
            status = ClientStatuses.New,
            singlePayment = Set[AdsRequestType](),
            firstModerated = false,
            name = Option("name"),
            paidCallsAvailable = false,
            priorityPlacement = false
          ),
          balanceClient = BalanceClient(
            clientId = 11L,
            balanceClientId = 222L,
            balanceAgencyId = Some(423),
            accountId = 422,
            amount = 66.2
          )
        )
      )
    )

}
