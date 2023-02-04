package ru.auto.salesman.service.tskv.dealer.format.impl

import cats.data.NonEmptyList
import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Section
import ru.auto.salesman.dao.GoodsDao
import ru.auto.salesman.model.{
  ActivateDate,
  FirstActivateDate,
  GoodStatuses,
  OfferCategories,
  ProductId
}
import ru.auto.salesman.service.GoodsDecider.{Action, Request, Response}
import ru.auto.salesman.service.tskv.tskvFormat
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.tskv.TskvByNameConverter
import ru.yandex.vertis.billing.Model.OfferBilling

class GoodsDeciderTskvLogFormatSpec extends BaseSpec {

  "GoodsDeciderTskvDealerLogFormat" should {

    val tskvMapConverter = new GoodsDeciderTskvLogFormat()
    "success generate default tskv log" in {
      val response = generateDefaultResponse()
      val args = tskvMapConverter.apply(
        generateDefaultRequest(),
        scala.util.Success(response)
      )
      val resultLog = TskvByNameConverter.toTskv(args)(tskvFormat).get
      val resultMap = ResultLogConverter.convert(resultLog)
      resultMap.size shouldBe resultLog.split("\t").size
      resultMap.size shouldBe 9
      resultMap("tskv") shouldBe None
      resultMap("tskv_format") shouldBe Some("vertis-aggregated-log")
      resultMap("offerCategoryNumber") shouldBe Some("15")
      resultMap("offerId") shouldBe Some("123")
      resultMap("offerCategory") shouldBe Some("cars")
      resultMap("isQuota") shouldBe Some("false")
      resultMap("offerSection") shouldBe Some("section_unknown")
      resultMap("action") shouldBe Some("activate")
      resultMap("product") shouldBe Some("vip-package")

      tskvMapConverter.skipRow(response) shouldBe false
    }

    "success generate full tskv log" in {
      val response = generateFullResponse()
      val args = tskvMapConverter.apply(
        generateFullRequest(),
        scala.util.Success(generateFullResponse())
      )
      val resultLog = TskvByNameConverter.toTskv(args)(tskvFormat).get
      val resultMap = ResultLogConverter.convert(resultLog)
      resultMap.size shouldBe resultLog.split("\t").size
      resultMap.size shouldBe 23
      resultMap("tskv") shouldBe None
      resultMap("tskv_format") shouldBe Some("vertis-aggregated-log")
      resultMap("action") shouldBe Some("activate")

      resultMap("offerId") shouldBe Some("123")
      resultMap("offerCategory") shouldBe Some("cars")
      resultMap("offerSection") shouldBe Some("new")
      resultMap("offerCategoryNumber") shouldBe Some("15")

      resultMap("product") shouldBe Some("vip-package")
      resultMap("price") shouldBe Some("356")
      resultMap("actualPrice") shouldBe Some("355")
      resultMap("hold") shouldBe Some("holdId-12233")

      resultMap("clientId") shouldBe Some("22")
      resultMap("agencyId") shouldBe Some("44")
      resultMap("companyId") shouldBe Some("45")
      resultMap("regionId") shouldBe Some("444")

      resultMap("featureId") shouldBe Some("4542323")
      resultMap("featureType") shouldBe Some("promocode")
      resultMap("featureAmount") shouldBe Some("4")

      resultMap("loyaltyDiscountId") shouldBe Some("loyality-4542323")
      resultMap("loyaltyDiscountAmount") shouldBe Some("455554")

      resultMap.get("quotaSize") shouldBe None
      resultMap("isQuota") shouldBe Some("false")
      resultMap.get("vin") shouldBe None
      resultMap("deliveryRegionsIds") shouldBe Some("42,77,99")
      resultMap("deliveryRegionsPrice") shouldBe Some("556")

      tskvMapConverter.skipRow(response) shouldBe false
    }

    "check skip tskv log if action == NoAction" in {
      val response =
        generateFullResponse().copy(action = Action.NoAction(null))

      tskvMapConverter.skipRow(response) shouldBe true

    }
  }

  private def generateFullRequest(): Request = {
    val goods = GoodsDao.Record(
      primaryKeyId = 1L,
      offerId = 2L,
      offerHash = "342",
      category = OfferCategories.Cars,
      section = Section.NEW,
      clientId = 345L,
      product = ProductId.Vip,
      status = GoodStatuses.Active,
      createDate = DateTime.now(),
      extraData = "extradata",
      expireDate = None,
      firstActivateDate = FirstActivateDate(DateTime.now()),
      offerBilling = None,
      offerBillingDeadline = None,
      holdTransactionId = None,
      epoch = None
    )

    Request(
      goods = goods,
      offer = ApiOfferModel.Offer
        .newBuilder()
        .setId("123-asdfas")
        .setCategory(ApiOfferModel.Category.CARS)
        .setSection(ApiOfferModel.Section.NEW)
        .setAdditionalInfo(
          ApiOfferModel.AdditionalInfo
            .newBuilder()
            .build()
        )
        .build(),
      customPrice = None
    )
  }

  private def generateFullResponse(): Response =
    Response(
      action = Action.Activate(
        activateDate = ActivateDate(DateTime.now()),
        offerBilling = OfferBilling.newBuilder().setVersion(1).build(),
        features = List(
          PriceModifierFeatureGenerator.generateLoyalityFeature(),
          PriceModifierFeatureGenerator.generateNonLoaylityFeature()
        )
      ),
      price = Option(356),
      actualPrice = Option(355),
      holdId = Option("holdId-12233"),
      clientId = Option(22L),
      agencyId = Option(44L),
      companyId = Option(45L),
      regionId = Option(ru.auto.salesman.model.RegionId(444)),
      deliveryRegionsIds = Some(
        NonEmptyList(
          ru.auto.salesman.model.RegionId(42),
          List(
            ru.auto.salesman.model.RegionId(77),
            ru.auto.salesman.model.RegionId(99)
          )
        )
      ),
      deliveryRegionsPrice = Some(
        556
      )
    )

  private def generateDefaultRequest(): Request = {
    val goods = GoodsDao.Record(
      primaryKeyId = 1L,
      offerId = 2L,
      offerHash = "342",
      category = OfferCategories.Cars,
      section = Section.NEW,
      clientId = 345L,
      product = ProductId.Vip,
      status = GoodStatuses.Active,
      createDate = DateTime.now(),
      extraData = "extradata",
      expireDate = None,
      firstActivateDate = FirstActivateDate(DateTime.now()),
      offerBilling = None,
      offerBillingDeadline = None,
      holdTransactionId = None,
      epoch = None
    )
    Request(
      goods = goods,
      offer = ApiOfferModel.Offer
        .newBuilder()
        .setId("123-asdfas")
        .setCategory(ApiOfferModel.Category.CARS)
        .build(),
      customPrice = None
    )
  }

  private def generateDefaultResponse(): Response =
    Response(
      action = Action.Activate(
        activateDate = ActivateDate(DateTime.now()),
        offerBilling = OfferBilling.newBuilder().setVersion(1).build(),
        features = Nil
      )
    )
}
