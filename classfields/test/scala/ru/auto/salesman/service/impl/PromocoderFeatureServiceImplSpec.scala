package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfter
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.cabinet.api_model.ExtraBonus
import ru.auto.cabinet.palma.proto.cashback_policies_palma_model.{
  CashbackPolicies,
  CashbackPolicy,
  CashbackUsageRule,
  ExtraBonuses
}
import ru.auto.salesman.environment._
import ru.auto.salesman.util.offer._
import ru.auto.salesman.model._
import ru.auto.salesman.model.ProductId._
import ru.auto.salesman.service.DealerFeatureService
import ru.auto.salesman.service.PromocoderFeatureService.LoyaltyArgs
import ru.auto.salesman.service.impl.DeciderUtilsSpec.{
  feature,
  features,
  loyaltyDiscountFeature,
  loyaltyFeature,
  loyaltyPlacementFeature,
  loyaltyVasFeature
}
import ru.auto.salesman.service.palma.PalmaService
import ru.auto.salesman.service.palma.domain.CallCashbackPoliciesIndex
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.util.GeoUtils.{RegMoscow, RegSPb}
import ru.yandex.vertis.generators.NetGenerators.asProducer
import zio.{Task, ZIO}

class PromocoderFeatureServiceImplSpec extends BaseSpec with BeforeAndAfter {
  import PromocoderFeatureServiceImplSpec._

  private val palmaService = new PalmaService {

    private val aggregatePolicy = CashbackPolicies(
      exclusivityPercent = 5,
      defaultCashbackPolicy = Some(
        CashbackPolicy(
          cashbackPercent = 10,
          cashbackPercentOnFullStock = 20,
          availableFor = List(CashbackUsageRule.VAS_CARS_USED),
          extraBonuses = Seq.empty[ExtraBonuses]
        )
      ),
      regionCashbackPolicies = List(
        CashbackPolicy(
          regionId = RegMoscow.toInt,
          cashbackPercent = 5,
          cashbackPercentOnFullStock = 5,
          availableFor = List(CashbackUsageRule.VAS_CARS_USED),
          extraBonuses = Seq(
            ExtraBonuses(ExtraBonus.UNDER_500_CARS, 3),
            ExtraBonuses(ExtraBonus.UNDER_1000_CARS, 5),
            ExtraBonuses(ExtraBonus.UNDER_2000_CARS, 7),
            ExtraBonuses(ExtraBonus.OVER_2000_CARS, 10)
          )
        ),
        CashbackPolicy(
          regionId = RegSPb.toInt,
          cashbackPercent = 5,
          cashbackPercentOnFullStock = 5,
          availableFor = List(CashbackUsageRule.ALL_CARS_USED),
          extraBonuses = Seq(
            ExtraBonuses(ExtraBonus.UNDER_500_CARS, 3),
            ExtraBonuses(ExtraBonus.UNDER_1000_CARS, 5),
            ExtraBonuses(ExtraBonus.UNDER_2000_CARS, 7),
            ExtraBonuses(ExtraBonus.OVER_2000_CARS, 10)
          )
        )
      )
    )

    def getCashbackPolicies: Task[CashbackPolicies] =
      ZIO.succeed(aggregatePolicy)

    def getCallCashbackPolicies: Task[CallCashbackPoliciesIndex] = ???
  }

  val dealerFeatureService = mock[DealerFeatureService]

  private val promocoderFeatureService =
    new PromocoderFeatureServiceImpl(palmaService, dealerFeatureService)

  "PromocoderFeatureServiceImpl.modifyPrice(chooseFeature logic)" should {

    "not apply loyalty feature for not used cars" in {
      promocoderFeatureService
        .modifyPrice(
          List(loyaltyFeature),
          Placement,
          defaultPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value shouldBe ModifiedPrice(defaultPrice, List.empty)
    }

    "apply generic feature for VAS product" in {
      val productId = ProductId.Premium
      val genericFeature = feature(productId)
      promocoderFeatureService
        .modifyPrice(
          features(genericFeature),
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.USED, RegMoscow))
        )
        .success
        .value shouldBe moneyFeatureExpectations(defaultPrice, genericFeature)
    }

    "not apply loyalty_vas feature for placement (non vas) product" in {
      val productId = ProductId.Placement
      val f = features(productId).filter(_.isLoyaltyVas)

      promocoderFeatureService
        .modifyPrice(
          f,
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.USED, RegSPb))
        )
        .success
        .value shouldBe ModifiedPrice(defaultPrice, List.empty)
    }

    "apply loyalty feature for placement product if cashback policy available for ALL_CARS_USED" in {
      val productId = ProductId.Placement
      val f = features(productId).filter(_.isLoyalty)

      promocoderFeatureService
        .modifyPrice(
          f,
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.USED, RegSPb))
        )
        .success
        .value shouldBe moneyFeatureExpectations(defaultPrice, loyaltyFeature)
    }

    "apply loyalty placement feature for placement product " in {
      val productId = ProductId.Placement
      val f = features(productId).filter(_.isLoyaltyPlacement)

      promocoderFeatureService
        .modifyPrice(
          f,
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.USED, RegSPb))
        )
        .success
        .value shouldBe moneyFeatureExpectations(
        defaultPrice,
        loyaltyPlacementFeature
      )
    }

    s"apply generic feature for $TradeInRequestCarsUsed" in {
      val productId = TradeInRequestCarsUsed
      val genericFeature = feature(productId)

      promocoderFeatureService
        .modifyPrice(
          features(genericFeature),
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          loyaltyArgs = Some(LoyaltyArgs(Category.CARS, Section.USED, RegMoscow))
        )
        .success
        .value shouldBe moneyFeatureExpectations(defaultPrice, genericFeature)
    }

    s"apply generic feature for non-offer $Certification" in {
      val productId = ProductId.Certification
      val genericFeature = feature(productId)

      promocoderFeatureService
        .modifyPrice(
          features(genericFeature),
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          loyaltyArgs = None
        )
        .success
        .value shouldBe moneyFeatureExpectations(defaultPrice, genericFeature)
    }

    s"not apply loyalty feature for $TradeInRequestCarsNew" in {

      promocoderFeatureService
        .modifyPrice(
          List(loyaltyFeature),
          TradeInRequestCarsNew,
          defaultPrice,
          itemsCount = 1,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.NEW, RegionId(-1L)))
        )
        .success
        .value shouldBe ModifiedPrice(defaultPrice, List.empty)
    }

    "not apply loyalty feature for not VAS product if VAS" in {
      val productId = ProductId.QuotaPlacementCarsNew

      val f = List(loyaltyFeature)
      promocoderFeatureService
        .modifyPrice(
          f,
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.USED, RegMoscow))
        )
        .success
        .value shouldBe ModifiedPrice(defaultPrice, List.empty)
    }

    "apply generic feature for any product if ALL" in {
      val productId = ProductId.Placement
      val genericFeature = feature(productId)

      promocoderFeatureService
        .modifyPrice(
          features(genericFeature),
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          Some(LoyaltyArgs(Category.CARS, Section.USED, RegSPb))
        )
        .success
        .value shouldBe moneyFeatureExpectations(defaultPrice, genericFeature)
    }

    "apply item feature if not enough money on money feature" in {
      val product = ProductId.Premium
      val feature1 =
        feature(product).copy(count = FeatureCount(10L, FeatureUnits.Money))
      val feature2 = feature1.copy(count = FeatureCount(1L, FeatureUnits.Items))
      val features = List(feature1, feature2)

      promocoderFeatureService
        .modifyPrice(
          features,
          product,
          defaultPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value shouldBe itemFeatureExpectations(
        itemsCount = 1,
        defaultPrice,
        feature2
      )
    }

    "apply generic feature instead money feature" in {
      val product = ProductId.Premium
      val moneyFeature = feature(product)
      val features = List(moneyFeature, loyaltyFeature)

      promocoderFeatureService
        .modifyPrice(
          features,
          product,
          defaultPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value shouldBe moneyFeatureExpectations(defaultPrice, moneyFeature)
    }

    "apply simple feature if VinHistory product" in {
      val product = ProductId.VinHistory
      val moneyFeature = feature(product)
      val features = List(moneyFeature, loyaltyFeature)

      promocoderFeatureService
        .modifyPrice(
          features,
          product,
          defaultPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value shouldBe moneyFeatureExpectations(defaultPrice, moneyFeature)
    }

    "apply simple feature if loyalty not enough funds" in {
      val product = ProductId.Premium
      val bigPrice = 10001L
      val moneyFeature =
        feature(product).copy(count = FeatureCount(10001L, FeatureUnits.Money))
      val features = List(moneyFeature, loyaltyFeature)

      promocoderFeatureService
        .modifyPrice(
          features,
          product,
          bigPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value shouldBe moneyFeatureExpectations(bigPrice, moneyFeature)
    }

    "not apply if features don`t have enough funds" in {
      val product = ProductId.Premium
      val moneyFeature =
        feature(product).copy(count = FeatureCount(10L, FeatureUnits.Money))

      promocoderFeatureService
        .modifyPrice(
          List(moneyFeature),
          product,
          defaultPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value shouldBe ModifiedPrice(defaultPrice, List.empty)
    }

    "not apply for loyalty features and placement product" in {
      val product = Placement

      promocoderFeatureService
        .modifyPrice(
          List(loyaltyFeature),
          product,
          defaultPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value shouldBe ModifiedPrice(defaultPrice, List.empty)
    }

    "use feature with offer constraint first" in {
      val offer = OfferModelGenerators.offerGen().next
      val featureWithOfferConstraint = FeatureInstance(
        id = offer.id.value,
        origin = FeatureOrigin("origin"),
        ProductId.alias(Placement),
        user = offer.getUserRef,
        count = FeatureCount(20L, FeatureUnits.Items),
        createTs = now(),
        deadline = now().plusDays(2),
        FeaturePayload(
          FeatureUnits.Items,
          FeatureTypes.Promocode,
          constraint = Some(FeatureConstraint(offer.id))
        )
      )

      promocoderFeatureService
        .modifyPrice(
          List(loyaltyFeature, featureWithOfferConstraint, feature(Placement)),
          Placement,
          defaultPrice,
          itemsCount = 1,
          Some(offer),
          defaultLoyaltyArgs
        )
        .success
        .value
        .shouldBe(
          itemFeatureExpectations(
            1,
            defaultPrice,
            featureWithOfferConstraint
          )
        )
    }

    "don't use use feature with offer constraint if ids don't match" in {
      val offer = OfferModelGenerators.offerGen().next
      val anotherOffer = OfferModelGenerators.offerGen().next
      val featureWithOfferConstraint = FeatureInstance(
        id = anotherOffer.id.value,
        origin = FeatureOrigin("origin"),
        ProductId.alias(Placement),
        user = offer.getUserRef,
        count = FeatureCount(20L, FeatureUnits.Items),
        createTs = now(),
        deadline = now().plusDays(2),
        FeaturePayload(
          FeatureUnits.Items,
          FeatureTypes.Promocode,
          constraint = Some(FeatureConstraint(anotherOffer.id))
        )
      )

      promocoderFeatureService
        .modifyPrice(
          List(loyaltyFeature, featureWithOfferConstraint),
          Placement,
          defaultPrice,
          itemsCount = 1,
          Some(offer),
          defaultLoyaltyArgs
        )
        .success
        .value shouldBe ModifiedPrice(defaultPrice, List.empty)
    }

  }

  "PromocoderFeatureServiceImpl.modifyPrice" should {

    "return originalPrice if no features found" in {
      val productId = ProductId.Placement
      val res = promocoderFeatureService
        .modifyPrice(
          List.empty,
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features shouldBe Nil
      res.price shouldBe defaultPrice
    }

    "return originalPrice if price <= 0" in {

      val productId = ProductId.Placement
      val price = 0L
      val res = promocoderFeatureService
        .modifyPrice(
          List.empty,
          productId,
          price,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features shouldBe Nil
      res.price shouldBe price
    }

    "use cashback if no discount" in {

      val productId = ProductId.Placement
      val expectedFeatures = List(
        PriceModifierFeature(
          loyaltyPlacementFeature,
          FeatureCount(defaultPrice, FeatureUnits.Money),
          defaultPrice
        )
      )
      val features = List(
        loyaltyFeature,
        loyaltyVasFeature,
        loyaltyPlacementFeature
      )
      val res = promocoderFeatureService
        .modifyPrice(
          features,
          productId,
          defaultPrice,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe 0L
    }

    "use only discount with ceil rounding to roubles" in {
      disableFeatureCalculatePriceWithKoopeks()
      val productId = ProductId.Placement
      val f = List(
        loyaltyFeature,
        loyaltyVasFeature,
        loyaltyPlacementFeature,
        loyaltyDiscountFeature
      )
      val price = 300L
      val priceWithDiscount = 200L
      val expectedFeatures = List(
        PriceModifierFeature(
          loyaltyDiscountFeature,
          FeatureCount(count = 1, FeatureUnits.Items),
          price - priceWithDiscount
        )
      )
      val res = promocoderFeatureService
        .modifyPrice(
          f,
          productId,
          price,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe priceWithDiscount
    }

    "use cashback if generic feature not suitable for product" in {

      val productId = ProductId.Certification
      val notSuitableGenericFeature = feature(ProductId.Placement)
      val price = 300L
      val priceWithDiscount = 0L
      val expectedFeatures = List(
        PriceModifierFeature(
          loyaltyFeature,
          FeatureCount(count = price, FeatureUnits.Money),
          price
        )
      )
      val res = promocoderFeatureService
        .modifyPrice(
          features(notSuitableGenericFeature),
          productId,
          price,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe priceWithDiscount
    }

    "use cashback if discount not suitable for product" in {

      val productId = ProductId.Certification
      val features = List(
        loyaltyDiscountFeature,
        loyaltyFeature
      )
      val price = 300L
      val priceWithDiscount = 0L
      val expectedFeatures = List(
        PriceModifierFeature(
          loyaltyFeature,
          FeatureCount(count = price, FeatureUnits.Money),
          price
        )
      )
      val res = promocoderFeatureService
        .modifyPrice(
          features,
          productId,
          price,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe priceWithDiscount
    }

    "use discount and then use money feature for price with discount" in {
      disableFeatureCalculatePriceWithKoopeks()

      val productId = ProductId.Placement
      val moneyFeature = feature(productId)
      val f = List(
        loyaltyPlacementFeature,
        loyaltyDiscountFeature,
        moneyFeature
      )
      val price = 300L
      val priceWithDiscount = 200L
      val expectedFeatures = List(
        PriceModifierFeature(
          loyaltyDiscountFeature,
          FeatureCount(count = 1, FeatureUnits.Items),
          price - priceWithDiscount
        ),
        PriceModifierFeature(
          moneyFeature,
          FeatureCount(count = priceWithDiscount, FeatureUnits.Money),
          priceWithDiscount
        )
      )
      val res = promocoderFeatureService
        .modifyPrice(
          f,
          productId,
          price,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe 0L
    }

    "use discount and then use item feature with right count" in {
      disableFeatureCalculatePriceWithKoopeks()

      val productId = ProductId.Placement
      val itemCount = 2
      val itemFeature = feature(productId).copy(
        count = FeatureCount(1000L, FeatureUnits.Items),
        payload = FeaturePayload(FeatureUnits.Items)
      )
      val f = List(
        loyaltyPlacementFeature,
        loyaltyDiscountFeature,
        itemFeature
      )
      val price = 300L
      val priceWithDiscount = 200L
      val expectedFeatures = List(
        PriceModifierFeature(
          loyaltyDiscountFeature,
          FeatureCount(count = 1, FeatureUnits.Items),
          price - priceWithDiscount
        ),
        PriceModifierFeature(
          itemFeature,
          FeatureCount(itemCount, FeatureUnits.Items),
          priceWithDiscount
        )
      )
      val res = promocoderFeatureService
        .modifyPrice(
          f,
          productId,
          price,
          itemCount,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe 0L
    }

    "use only one newest discount if have many" in {
      disableFeatureCalculatePriceWithKoopeks()

      val productId = ProductId.Placement
      val newestDiscountFeature = loyaltyDiscountFeature.copy(
        id = "discFeature2",
        createTs = loyaltyDiscountFeature.createTs.plusMinutes(1)
      )
      val f = List(
        loyaltyFeature,
        loyaltyVasFeature,
        loyaltyPlacementFeature,
        loyaltyDiscountFeature,
        newestDiscountFeature
      )
      val price = 300L
      val priceWithDiscount = 200L
      val expectedFeatures = List(
        PriceModifierFeature(
          newestDiscountFeature,
          FeatureCount(count = 1, FeatureUnits.Items),
          price - priceWithDiscount
        )
      )
      val res = promocoderFeatureService
        .modifyPrice(
          f,
          productId,
          price,
          itemsCount = 1,
          None,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe priceWithDiscount
    }

  }

  "PromocoderFeatureServiceImpl.priceWithLoyaltyDiscount" should {

    "return original price for empty features list" in {
      val productId = ProductId.Placement
      val res = promocoderFeatureService
        .modifyPriceWithLoyaltyDiscount(
          List.empty,
          productId,
          defaultPrice,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features shouldBe Nil
      res.price shouldBe defaultPrice
    }

    "use only discount with ceil rounding to roubles" in {
      disableFeatureCalculatePriceWithKoopeks()

      val productId = ProductId.Placement
      val moneyFeature = feature(productId)
      val f = List(
        loyaltyFeature,
        loyaltyVasFeature,
        loyaltyPlacementFeature,
        loyaltyDiscountFeature,
        moneyFeature
      )
      val price = 300L
      val priceWithDiscount = 200L
      val discountAmount = 100L
      val expectedFeatures = List(
        PriceModifierFeature(
          loyaltyDiscountFeature,
          FeatureCount(count = 1, FeatureUnits.Items),
          discountAmount
        )
      )
      val res = promocoderFeatureService
        .modifyPriceWithLoyaltyDiscount(
          f,
          productId,
          price,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe priceWithDiscount
    }

    "use only discount with ceil rounding to kopeks if feature recalculate with kopeks turn on" in {
      enableFeatureCalculatePriceWithKoopeks()
      val productId = ProductId.Placement
      val moneyFeature = feature(productId)
      val loyalityDiscount8Percent = loyaltyDiscountFeature.copy(payload =
        loyaltyDiscountFeature.payload.copy(
          discount = loyaltyDiscountFeature.payload.discount.map(fd => fd.copy(value = 8))
        )
      )
      val f = List(
        loyaltyFeature,
        loyaltyVasFeature,
        loyaltyPlacementFeature,
        loyalityDiscount8Percent,
        moneyFeature
      )
      val price = 300L
      val priceWithDiscount = 276L
      val discountAmount = 24L
      val expectedFeatures = List(
        PriceModifierFeature(
          loyalityDiscount8Percent,
          FeatureCount(count = 1, FeatureUnits.Items),
          discountAmount
        )
      )
      val res = promocoderFeatureService
        .modifyPriceWithLoyaltyDiscount(
          f,
          productId,
          price,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features should contain theSameElementsAs expectedFeatures
      res.price shouldBe priceWithDiscount
    }

    "don`t use other loyalty features if discount not exist" in {

      val productId = ProductId.Placement
      val f = List(
        loyaltyFeature,
        loyaltyVasFeature,
        loyaltyPlacementFeature
      )
      val res = promocoderFeatureService
        .modifyPriceWithLoyaltyDiscount(
          f,
          productId,
          defaultPrice,
          defaultLoyaltyArgs
        )
        .success
        .value
      res.features shouldBe Nil
      res.price shouldBe defaultPrice
    }

  }

  private def enableFeatureCalculatePriceWithKoopeks(): Unit =
    (dealerFeatureService.startTimeForCalculateDiscountWithKoopeks _)
      .expects()
      .returning(DateTime.now().minusDays(5))

  private def disableFeatureCalculatePriceWithKoopeks(): Unit =
    (dealerFeatureService.startTimeForCalculateDiscountWithKoopeks _)
      .expects()
      .returning(DateTime.now().plusDays(5))

}

object PromocoderFeatureServiceImplSpec {

  private val defaultLoyaltyArgs =
    Some(LoyaltyArgs(Category.CARS, Section.USED, RegionId(-1)))

  private val defaultPrice = 100L

  private def moneyFeatureExpectations(price: Funds, feature: FeatureInstance) =
    ModifiedPrice(
      price = 0L,
      List(
        PriceModifierFeature(
          feature,
          FeatureCount(price, FeatureUnits.Money),
          price
        )
      )
    )

  private def itemFeatureExpectations(
      itemsCount: Long,
      price: Funds,
      feature: FeatureInstance
  ) =
    ModifiedPrice(
      price = 0L,
      List(
        PriceModifierFeature(
          feature,
          FeatureCount(itemsCount, FeatureUnits.Items),
          price
        )
      )
    )

}
