package ru.auto.salesman.model

import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.Category._
import ru.auto.api.ApiOfferModel.{DeliveryInfo, DeliveryRegion, Location}
import ru.auto.api.ApiOfferModel.Section._
import ru.auto.api.MotoModel.MotoCategory.ATV
import ru.auto.api.TrucksModel.TruckCategory.BUS
import ru.auto.salesman.environment._
import ru.auto.salesman.model.OfferBillingBuilderSpec._
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.test.model.gens.OfferModelGenerators._
import ru.auto.salesman.test.model.gens.{
  balanceRecordGen,
  clientRecordGen,
  PromocoderModelGenerators
}
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.billing.Model.Cost.{Constraints, PerIndexing}
import ru.yandex.vertis.billing.Model.Good.Custom
import ru.yandex.vertis.billing.Model._
import ru.yandex.vertis.billing.Model
import ru.auto.salesman.test.model.gens.ProductGenerators._

import scala.collection.JavaConverters._

class OfferBillingBuilderSpec extends BaseSpec with PromocoderModelGenerators {

  "OfferBillingBuilder" should {
    val time = BillingTimestamp(now())
    val deadline = time.asDateTime.plusDays(1)
    val price = 333L
    val hold = "hold"
    val detailedFunds = OfferDetailedFunds(ModifiedPrice(price), price, 0L)

    "correctly set fields" in {
      val b = new OfferBillingBuilderImpl
      val offerBillingTry = for {
        _ <- b.withCampaignHeader(Header)
        _ = b.withTimestamp(time)
        _ <- b.withActiveDeadline(deadline)
        _ <- b.withCustomDynamicPrice(
          productId,
          detailedFunds,
          productTariff = None
        )
        _ <- b.withHold(hold)
        ob <- b.build
      } yield ob

      val offerBilling: OfferBilling = offerBillingTry.success.value

      offerBilling.getTimestamp should be(time.getMillis)

      offerBilling.hasKnownCampaign should be(true)

      val knownCampaign = offerBilling.getKnownCampaign
      knownCampaign.getActiveDeadline should be(deadline.getMillis)
      knownCampaign.getHold should be(hold)

      knownCampaign.hasCampaign should be(true)

      val header = knownCampaign.getCampaign
      header.getProduct.getTag should be(Tag)

      val goods = header.getProduct.getGoodsList
      goods.size() should be(1)

      val good = goods.asScala.head.getCustom
      good.getId should be(ProductId.alias(productId))
      val cost = good.getCost
      cost.getPerIndexing.getUnits should be(price)
    }

    "correctly use productTariff" in {
      forAll(defaultProductTariffGen) { productTariff =>
        val b = new OfferBillingBuilderImpl
        val offerBillingTry = for {
          _ <- b.withCampaignHeader(header(productByTariff(Tag, productTariff)))
          _ = b.withTimestamp(time)
          _ <- b.withActiveDeadline(deadline)
          _ <- b.withCustomDynamicPrice(
            productId,
            detailedFunds,
            productTariff = Some(productTariff)
          )
          _ <- b.withHold(hold)
          ob <- b.build
        } yield ob

        val offerBilling: OfferBilling = offerBillingTry.success.value

        val knownCampaign = offerBilling.getKnownCampaign
        knownCampaign.getActiveDeadline shouldBe deadline.getMillis
        knownCampaign.getHold shouldBe hold

        knownCampaign.hasCampaign shouldBe true

        val good =
          knownCampaign.getCampaign.getProduct.getGoodsList.asScala.head.getCustom

        good.getId shouldBe productTariff.entryName
      }
    }

    "set fields in any order" in {
      val obt1 = {
        val b = new OfferBillingBuilderImpl
        for {
          _ <- b.withCampaignHeader(Header)
          _ <- b.withActiveDeadline(deadline)
          _ <- b.withCustomDynamicPrice(
            productId,
            detailedFunds,
            productTariff = None
          )
          _ = b.withTimestamp(time)
          _ <- b.withHold(hold)
          ob <- b.build
        } yield ob
      }
      val obt2 = {
        val b = new OfferBillingBuilderImpl
        b.withTimestamp(time)
        for {
          _ <- b.withCampaignHeader(Header)
          _ <- b.withHold(hold)
          _ <- b.withCustomDynamicPrice(
            productId,
            detailedFunds,
            productTariff = None
          )
          _ <- b.withActiveDeadline(deadline)
          ob <- b.build
        } yield ob
      }
      val obt3 = {
        val b = new OfferBillingBuilderImpl
        b.withTimestamp(time)
        for {
          _ <- b.withCampaignHeader(Header)
          _ <- b.withCustomDynamicPrice(
            productId,
            detailedFunds,
            productTariff = None
          )
          _ <- b.withActiveDeadline(deadline)
          _ <- b.withHold(hold)
          ob <- b.build
        } yield ob
      }

      val ob1 = obt1.success.value
      val ob2 = obt2.success.value
      val ob3 = obt3.success.value
      ob1 shouldBe ob2
      ob2 shouldBe ob3
    }

    "modify goods by id" in {
      val otherProduct = ProductId.Fresh
      val otherPrice = 222
      val otherDetailedFunds =
        OfferDetailedFunds(ModifiedPrice(otherPrice), otherPrice, 0L)
      val offerBilling = {
        val b = new OfferBillingBuilderImpl
        for {
          _ <- b.withCampaignHeader(
            header(product(Tag, productId, otherProduct))
          )
          _ = b.withTimestamp(time)
          _ <- b.withActiveDeadline(deadline)
          _ <- b.withCustomDynamicPrice(
            productId,
            detailedFunds,
            productTariff = None
          )
          _ <- b.withCustomDynamicPrice(
            otherProduct,
            otherDetailedFunds,
            productTariff = None
          )
          ob <- b.build
        } yield ob
      }
      val ob = offerBilling.success.value
      val goods = ob.getKnownCampaign.getCampaign.getProduct.getGoodsList
      goods should have size 2
      goods.asScala
        .find(g => g.getCustom.getId == ProductId.alias(productId))
        .value
        .getCustom
        .getCost
        .getPerIndexing
        .getUnits shouldBe price
      goods.asScala
        .find(g => g.getCustom.getId == ProductId.alias(otherProduct))
        .value
        .getCustom
        .getCost
        .getPerIndexing
        .getUnits shouldBe otherPrice
    }

    "add offer id to payload" in {
      forAll(offerGen(offerIdGen = AutoruOfferId("1085995962-4ea98bf2"))) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("offerId") shouldBe "1085995962-4ea98bf2"
      }
    }

    "add cars offer category to payload" in {
      forAll(offerGen(offerCategoryGen = CARS)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("category") shouldBe "CARS"
      }
    }

    "add moto offer category to payload" in {
      forAll(offerGen(offerCategoryGen = MOTO)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("category") shouldBe "MOTO"
      }
    }

    "add trucks offer category to payload" in {
      forAll(offerGen(offerCategoryGen = TRUCKS)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("category") shouldBe "TRUCKS"
      }
    }

    "not add offer subcategory for cars offer to payload" in {
      forAll(offerGen(offerCategoryGen = CARS)) { offer =>
        val payload = payloadWithOffer(offer)
        Option(payload.get("subcategory")) shouldBe None
      }
    }

    "add moto offer subcategory to payload" in {
      forAll(offerGen(offerCategoryGen = MOTO, motoCategoryGen = ATV)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("subcategory") shouldBe "ATV"
      }
    }

    "add trucks offer subcategory to payload" in {
      forAll(offerGen(offerCategoryGen = TRUCKS, truckCategoryGen = BUS)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("subcategory") shouldBe "BUS"
      }
    }

    "add new offer section to payload" in {
      forAll(offerGen(offerSectionGen = NEW)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("section") shouldBe "NEW"
      }
    }

    "add used offer section to payload" in {
      forAll(offerGen(offerSectionGen = USED)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("section") shouldBe "USED"
      }
    }

    "add offer mark to payload" in {
      forAll(offerGenWithMarkModel(offerCategoryGen = CARS)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("offerMark") shouldBe offer.getCarInfo.getMark
      }
    }

    "add cars offer model to payload" in {
      forAll(offerGenWithMarkModel(offerCategoryGen = CARS)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("offerModel") shouldBe offer.getCarInfo.getModel
      }
    }

    "add cars offer superGenId to payload" in {
      forAll(offerGenWithMarkModel(offerCategoryGen = CARS)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get(
          "offerSuperGenId"
        ) shouldBe offer.getCarInfo.getSuperGenId.toString
      }
    }

    "add cars offer techParamId to payload" in {
      forAll(offerGenWithMarkModel(offerCategoryGen = CARS)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get(
          "offerTechParamId"
        ) shouldBe offer.getCarInfo.getTechParamId.toString
      }
    }

    "add offer vin to payload" in {
      forAll(offerGenWithMarkModel(offerCategoryGen = CARS)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("offerVin") shouldBe offer.getDocuments.getVin
      }
    }

    "add offer delivery region ids to payload" in {
      def makeRegion(regionId: RegionId) =
        DeliveryRegion
          .newBuilder()
          .setLocation(Location.newBuilder().setFederalSubjectId(regionId))
          .build()
      val deliveryInfo = DeliveryInfo
        .newBuilder()
        .addAllDeliveryRegions(
          List(
            makeRegion(RegionId(10174)),
            makeRegion(RegionId(10645))
          ).asJava
        )
        .build()
      forAll(offerGen(deliveryInfoGen = deliveryInfo)) { offer =>
        val payload = payloadWithOffer(offer)
        payload.get("offerDeliveryRegionIds") shouldBe "10174,10645"
      }
    }

    "add price info to payload" in {
      val deliveryPrice = 111L
      val discountAmount = detailedFunds.price.price
      val feature = featureInstanceGen.next
      val priceWithFeature = detailedFunds.price.copy(features =
        List(
          PriceModifierFeature(
            feature,
            FeatureCount(1, FeatureUnits.Items),
            discountAmount
          )
        )
      )
      val detailedFundsWithFeature = detailedFunds.copy(
        price = priceWithFeature,
        deliveryPrice = deliveryPrice
      )

      val b = new OfferBillingBuilderImpl

      val offerBillingTry = for {
        _ <- b.withCampaignHeader(Header)
        _ = b.withTimestamp(time)
        _ <- b.withActiveDeadline(deadline)
        _ <- b.withCustomDynamicPrice(
          productId,
          detailedFundsWithFeature,
          productTariff = None
        )
        _ <- b.withHold(hold)
        ob <- b.build
      } yield ob

      val offerBilling: OfferBilling = offerBillingTry.success.value

      offerBilling.getPayloadMap.get(
        "priceWithoutDiscount"
      ) shouldBe detailedFundsWithFeature.priceWithoutDiscount.toString
      offerBilling.getPayloadMap.get(
        "featureId"
      ) shouldBe feature.id
      offerBilling.getPayloadMap.get(
        "featureType"
      ) shouldBe feature.payload.featureType.toString
      offerBilling.getPayloadMap.get(
        "featureAmount"
      ) shouldBe discountAmount.toString
      offerBilling.getPayloadMap.get(
        "deliveryPrice"
      ) shouldBe detailedFundsWithFeature.deliveryPrice.toString
    }

    "add price info with discount to payload" in {
      val deliveryPrice = 111L
      val discountFeature = featureInstanceLoyaltyDiscountGen(percent = 10).next
      val otherFeature = featureInstanceGen.next
      val testPrice = 1000L
      val discountAmount = 100L
      val priceWithDiscount = testPrice - discountAmount
      val priceWithFeatures = ModifiedPrice(
        price,
        List(
          PriceModifierFeature(
            discountFeature,
            FeatureCount(1, FeatureUnits.Items),
            discountAmount
          ),
          PriceModifierFeature(
            otherFeature,
            FeatureCount(priceWithDiscount, FeatureUnits.Money),
            priceWithDiscount
          )
        )
      )
      val detailedFundsWithFeature = OfferDetailedFunds(
        priceWithFeatures,
        testPrice,
        deliveryPrice = deliveryPrice
      )

      val b = new OfferBillingBuilderImpl

      val offerBillingTry = for {
        _ <- b.withCampaignHeader(Header)
        _ = b.withTimestamp(time)
        _ <- b.withActiveDeadline(deadline)
        _ <- b.withCustomDynamicPrice(
          productId,
          detailedFundsWithFeature,
          productTariff = None
        )
        _ <- b.withHold(hold)
        ob <- b.build
      } yield ob

      val offerBilling: OfferBilling = offerBillingTry.success.value

      offerBilling.getPayloadMap.get(
        "priceWithoutDiscount"
      ) shouldBe testPrice.toString
      offerBilling.getPayloadMap.get(
        "featureId"
      ) shouldBe otherFeature.id
      offerBilling.getPayloadMap.get(
        "featureType"
      ) shouldBe otherFeature.payload.featureType.toString
      offerBilling.getPayloadMap.get(
        "featureAmount"
      ) shouldBe priceWithDiscount.toString
      offerBilling.getPayloadMap.get(
        "loyaltyDiscountId"
      ) shouldBe discountFeature.id
      offerBilling.getPayloadMap.get(
        "loyaltyDiscountAmount"
      ) shouldBe discountAmount.toString
      offerBilling.getPayloadMap.get(
        "deliveryPrice"
      ) shouldBe detailedFundsWithFeature.deliveryPrice.toString
    }

    "add client info to payload" in {
      forAll(clientRecordGen(), balanceRecordGen) { (clientRecord, balanceClient) =>
        val client = DetailedClient(clientRecord, balanceClient)
        val payload = payloadWithClient(client)

        payload("serviceClientId") shouldBe client.clientId.toString
        payload.get("serviceClientAgencyId") shouldBe client.agencyId.map(
          _.toString
        )
        payload.get("serviceClientCompanyId") shouldBe client.companyId.map(
          _.toString
        )
        payload("clientFederalSubjectId") shouldBe client.regionId.toString
        payload("clientCityId") shouldBe client.cityId.toString
      }
    }

    "fail when setting price without campaign" in {
      new OfferBillingBuilderImpl()
        .withCustomDynamicPrice(productId, detailedFunds, productTariff = None)
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }

    "fail when setting deadline without campaign" in {
      new OfferBillingBuilderImpl()
        .withActiveDeadline(deadline)
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }

    "fail when setting hold without campaign" in {
      new OfferBillingBuilderImpl()
        .withHold("")
        .failure
        .exception shouldBe an[IllegalArgumentException]
    }

    "fail on bad goodId" in {
      val builder = {
        val b = new OfferBillingBuilderImpl
        for {
          _ <- b.withCampaignHeader(Header)
          r <- b.withCustomDynamicPrice(
            ProductId.Premium,
            detailedFunds,
            productTariff = None
          )
        } yield r
      }

      builder.failure.exception shouldBe an[IllegalArgumentException]
    }
  }

  private def payloadWithOffer(offer: ApiOfferModel.Offer) =
    new OfferBillingBuilderImpl()
      .withOfferPayload(offer)
      .build
      .success
      .value
      .getPayloadMap

  private def payloadWithClient(client: DetailedClient) =
    new OfferBillingBuilderImpl()
      .withClientPayload(client)
      .build
      .success
      .value
      .getPayloadMap
      .asScala
}

object OfferBillingBuilderSpec {

  private val Tag = "product_tag"
  private val productId = ProductId.Color

  def product(tag: String, productIds: ProductId*): Model.Product = {
    val pb = Model.Product.newBuilder().setVersion(1).setTag(tag)
    productIds.foreach { p =>
      pb.addGoods(
        Good
          .newBuilder()
          .setVersion(1)
          .setCustom(
            Custom
              .newBuilder()
              .setId(ProductId.alias(p))
              .setCost(
                Cost
                  .newBuilder()
                  .setVersion(1)
                  .setPerIndexing(
                    PerIndexing
                      .newBuilder()
                      .setConstraints(
                        Constraints
                          .newBuilder()
                          .setCostType(CostType.COSTPERINDEXING)
                      )
                  )
              )
          )
      )
    }
    pb.build()
  }

  def productByTariff(
      tag: String,
      productTariff: ProductTariff
  ): Model.Product = {
    val pb = Model.Product.newBuilder().setVersion(1).setTag(tag)
    pb.addGoods(
      Good
        .newBuilder()
        .setVersion(1)
        .setCustom(
          Custom
            .newBuilder()
            .setId(productTariff.entryName)
            .setCost(
              Cost
                .newBuilder()
                .setVersion(1)
                .setPerIndexing(
                  PerIndexing
                    .newBuilder()
                    .setConstraints(
                      Constraints
                        .newBuilder()
                        .setCostType(CostType.COSTPERINDEXING)
                    )
                )
            )
        )
    )
    pb.build
  }

  def header(product: Model.Product): CampaignHeader = {
    val customerId = CustomerId.newBuilder().setVersion(1).setClientId(1)
    val order = Order.newBuilder
      .setVersion(1)
      .setId(1)
      .setOwner(customerId)
      .setText("")
      .setCommitAmount(0)
      .setApproximateAmount(0)
    val customer = CustomerHeader.newBuilder().setVersion(1).setId(customerId)
    val settings =
      CampaignSettings.newBuilder().setVersion(1).setIsEnabled(true)

    CampaignHeader
      .newBuilder()
      .setVersion(1)
      .setId("campaign_id")
      .setOwner(customer)
      .setOrder(order)
      .setProduct(product)
      .setSettings(settings)
      .build()
  }

  private val Header = header(product(Tag, productId))
}
