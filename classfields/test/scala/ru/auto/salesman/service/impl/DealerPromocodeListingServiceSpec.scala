package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.api.PromocodeModel.{Promocode, PromocodeListing}
import ru.auto.salesman.environment.RichDateTime
import ru.auto.salesman.model.ProductId.{Badge, Placement}
import ru.auto.salesman.model.{
  AutoruDealer,
  AutoruUser,
  FeatureCount,
  FeatureDiscount,
  FeatureDiscountType,
  FeatureDiscountTypes,
  FeatureInstance,
  FeatureOrigin,
  FeaturePayload,
  FeatureType,
  FeatureTypes,
  FeatureUnit,
  FeatureUnits,
  ProductId,
  PromocoderUser,
  UserTypes
}
import ru.auto.salesman.service.PromocoderSource
import ru.auto.salesman.test.BaseSpec

import scala.collection.JavaConverters._

class DealerPromocodeListingServiceSpec extends BaseSpec {
  import DealerPromocodeListingServiceSpec._

  private val promoSource = mock[PromocoderSource]

  private val service = new DealerPromocodeListingService(promoSource)

  "fail if user is not dealer" in {
    service
      .getListing(AutoruUser(1))
      .failure
      .exception shouldBe an[IllegalArgumentException]
  }

  "return empty list if no features exists" in {
    mockFeatures(Nil)
    val listing = service.getListing(testUser).success.value
    listing.getPromocodesList shouldBe empty
  }

  "return right promocode listing for item promo" in {
    val testFeature = makeFeature(
      ProductId.alias(Badge),
      FeatureUnits.Items,
      FeatureTypes.Promocode,
      discountType = None
    )

    val expectedPromo = Promocode
      .newBuilder()
      .setProduct(
        Promocode.Product
          .newBuilder()
          .setHumanName(ProductId.alias(Badge))
          .setName(ProductId.alias(Badge))
      )
      .setCommon(Promocode.SourceCommon.newBuilder())
      .setItem(Promocode.Item.newBuilder().setCount(testCount))
      .setDeadline(deadline.asTimestamp)
      .build()

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(List(expectedPromo).asJava)
      .build()

    mockFeatures(List(testFeature))
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  "return right promocode listing for money promo" in {
    val testFeature = makeFeature(
      ProductId.alias(Badge),
      FeatureUnits.Money,
      FeatureTypes.Promocode,
      discountType = None
    )

    val expectedPromo = Promocode
      .newBuilder()
      .setProduct(
        Promocode.Product
          .newBuilder()
          .setHumanName(ProductId.alias(Badge))
          .setName(ProductId.alias(Badge))
      )
      .setCommon(Promocode.SourceCommon.newBuilder())
      .setMoney(Promocode.Money.newBuilder().setKopecks(testCount))
      .setDeadline(deadline.asTimestamp)
      .build()

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(List(expectedPromo).asJava)
      .build()

    mockFeatures(List(testFeature))
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  "return right promocode listing for discount promo" in {
    val testFeature = makeFeature(
      ProductId.alias(Badge),
      FeatureUnits.Items,
      FeatureTypes.Promocode,
      discountType = Some(FeatureDiscountTypes.Percent)
    )

    val expectedPromo = Promocode
      .newBuilder()
      .setProduct(
        Promocode.Product
          .newBuilder()
          .setHumanName(ProductId.alias(Badge))
          .setName(ProductId.alias(Badge))
      )
      .setCommon(Promocode.SourceCommon.newBuilder())
      .setPercent(
        Promocode.Percent
          .newBuilder()
          .setPercent(testPercent)
          .setCount(testCount)
      )
      .setDeadline(deadline.asTimestamp)
      .build()

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(List(expectedPromo).asJava)
      .build()

    mockFeatures(List(testFeature))
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  "return all proper promocodes even ones with constraints" in {
    val feature1 = makeFeature(
      ProductId.alias(Badge),
      FeatureUnits.Items,
      FeatureTypes.Promocode,
      discountType = Some(FeatureDiscountTypes.Percent)
    )

    val feature2 = makeFeature(
      ProductId.alias(Badge),
      FeatureUnits.Items,
      FeatureTypes.Promocode,
      discountType = None
    )

    val feature3 = makeFeature(
      ProductId.alias(Placement),
      FeatureUnits.Money,
      FeatureTypes.Promocode,
      discountType = None
    )

    val featureWithConstraint = FeatureInstance(
      id = "1",
      origin = FeatureOrigin("11"),
      tag = ProductId.alias(Placement),
      user = "1",
      count = FeatureCount(20, FeatureUnits.Items),
      createTs = createTime,
      deadline = deadline,
      payload = FeaturePayload(
        FeatureUnits.Items,
        FeatureTypes.Promocode,
        constraint = None,
        discount = None
      )
    )

    mockFeatures(List(feature1, feature2, feature3, featureWithConstraint))

    service.getListing(testUser).success.value.getPromocodesList.size() shouldBe 4
  }

  "ignore bundle promocodes" in {
    val testFeature = makeFeature(
      ProductId.alias(Badge),
      FeatureUnits.Items,
      FeatureTypes.Bundle,
      discountType = None
    )

    mockFeatures(List(testFeature))
    val result = service.getListing(testUser).success.value
    result.getPromocodesList shouldBe empty
  }

  "return right promocode listing for cashback common loyalty" in {
    val testFeature = makeFeature(
      FeatureInstance.LoyaltyTag,
      FeatureUnits.Money,
      FeatureTypes.Loyalty,
      discountType = None
    )

    val expectedPromo = Promocode
      .newBuilder()
      .setLoyalty(
        Promocode.SourceLoyalty
          .newBuilder()
          .setApplyRules(Promocode.SourceLoyalty.ApplyRules.REGION_POLICY)
      )
      .setMoney(Promocode.Money.newBuilder().setKopecks(testCount))
      .setDeadline(deadline.asTimestamp)
      .build()

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(List(expectedPromo).asJava)
      .build()

    mockFeatures(List(testFeature))
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  "return right promocode listing for cashback vas loyalty" in {
    val testFeature = makeFeature(
      FeatureInstance.LoyaltyVasTag,
      FeatureUnits.Money,
      FeatureTypes.Loyalty,
      discountType = None
    )

    val expectedPromo = Promocode
      .newBuilder()
      .setLoyalty(
        Promocode.SourceLoyalty
          .newBuilder()
          .setApplyRules(Promocode.SourceLoyalty.ApplyRules.VAS)
      )
      .setMoney(Promocode.Money.newBuilder().setKopecks(testCount))
      .setDeadline(deadline.asTimestamp)
      .build()

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(List(expectedPromo).asJava)
      .build()

    mockFeatures(List(testFeature))
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  "return right promocode listing for cashback placement loyalty" in {
    val testFeature = makeFeature(
      FeatureInstance.LoyaltyPlacementTag,
      FeatureUnits.Money,
      FeatureTypes.Loyalty,
      discountType = None
    )

    val expectedPromo = Promocode
      .newBuilder()
      .setLoyalty(
        Promocode.SourceLoyalty
          .newBuilder()
          .setApplyRules(Promocode.SourceLoyalty.ApplyRules.PLACEMENT)
      )
      .setMoney(Promocode.Money.newBuilder().setKopecks(testCount))
      .setDeadline(deadline.asTimestamp)
      .build()

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(List(expectedPromo).asJava)
      .build()

    mockFeatures(List(testFeature))
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  "return right promocode listing for loyalty discount" in {
    val testFeature = makeFeature(
      FeatureInstance.LoyaltyPlacementTag,
      FeatureUnits.Items,
      FeatureTypes.Loyalty,
      discountType = Some(FeatureDiscountTypes.Percent)
    )

    val expectedPromo = Promocode
      .newBuilder()
      .setLoyalty(
        Promocode.SourceLoyalty
          .newBuilder()
          .setApplyRules(Promocode.SourceLoyalty.ApplyRules.PLACEMENT)
      )
      .setPercent(
        Promocode.Percent
          .newBuilder()
          .setPercent(testPercent)
          .setCount(testCount)
      )
      .setDeadline(deadline.asTimestamp)
      .build()

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(List(expectedPromo).asJava)
      .build()

    mockFeatures(List(testFeature))
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  "return right result for many features" in {
    val testFeatures = List(
      makeFeature(
        ProductId.alias(Badge),
        FeatureUnits.Items,
        FeatureTypes.Promocode,
        discountType = None
      ),
      makeFeature(
        FeatureInstance.LoyaltyPlacementTag,
        FeatureUnits.Items,
        FeatureTypes.Loyalty,
        discountType = Some(FeatureDiscountTypes.Percent)
      )
    )

    val expectedPromos = List(
      Promocode
        .newBuilder()
        .setProduct(
          Promocode.Product
            .newBuilder()
            .setHumanName(ProductId.alias(Badge))
            .setName(ProductId.alias(Badge))
        )
        .setCommon(Promocode.SourceCommon.newBuilder())
        .setItem(Promocode.Item.newBuilder().setCount(testCount))
        .setDeadline(deadline.asTimestamp)
        .build(),
      Promocode
        .newBuilder()
        .setLoyalty(
          Promocode.SourceLoyalty
            .newBuilder()
            .setApplyRules(Promocode.SourceLoyalty.ApplyRules.PLACEMENT)
        )
        .setPercent(
          Promocode.Percent
            .newBuilder()
            .setPercent(testPercent)
            .setCount(testCount)
        )
        .setDeadline(deadline.asTimestamp)
        .build()
    )

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(expectedPromos.asJava)
      .build()

    mockFeatures(testFeatures)
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  "not show features with invalid tag" in {
    val testFeatures = List(
      makeFeature(
        FeatureInstance.LoyaltyPlacementTag,
        FeatureUnits.Items,
        FeatureTypes.Loyalty,
        discountType = Some(FeatureDiscountTypes.Percent)
      ),
      makeFeature(
        tag = "some shit",
        FeatureUnits.Items,
        FeatureTypes.Promocode,
        discountType = None
      )
    )

    val expectedPromos = List(
      Promocode
        .newBuilder()
        .setLoyalty(
          Promocode.SourceLoyalty
            .newBuilder()
            .setApplyRules(Promocode.SourceLoyalty.ApplyRules.PLACEMENT)
        )
        .setPercent(
          Promocode.Percent
            .newBuilder()
            .setPercent(testPercent)
            .setCount(testCount)
        )
        .setDeadline(deadline.asTimestamp)
        .build()
    )

    val expected = PromocodeListing
      .newBuilder()
      .addAllPromocodes(expectedPromos.asJava)
      .build()

    mockFeatures(testFeatures)
    val result = service.getListing(testUser).success.value
    result shouldBe expected
  }

  private def mockFeatures(features: List[FeatureInstance]): Unit =
    (promoSource.getFeaturesForUser _)
      .expects(PromocoderUser(testUser.id, UserTypes.ClientUser))
      .returningZ(features)
}

object DealerPromocodeListingServiceSpec {
  private val testUser = AutoruDealer(1)

  private val createTime = DateTime.parse("2021-10-18T10:00:00.000")
  private val deadline = DateTime.parse("2021-10-28T10:00:00.000")
  private val testCount = 100
  private val testPercent = 50

  private def makeFeature(
      tag: String,
      unit: FeatureUnit,
      featureType: FeatureType,
      discountType: Option[FeatureDiscountType]
  ) =
    FeatureInstance(
      id = "1",
      origin = FeatureOrigin("11"),
      tag = tag,
      user = "1",
      count = FeatureCount(testCount, unit),
      createTs = createTime,
      deadline = deadline,
      payload = FeaturePayload(
        unit,
        featureType,
        constraint = None,
        discount = discountType.map(dt => FeatureDiscount(dt, testPercent))
      )
    )
}
