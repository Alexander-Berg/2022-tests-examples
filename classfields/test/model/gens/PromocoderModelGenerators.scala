package ru.auto.salesman.test.model.gens

import org.scalacheck.Gen
import ru.auto.salesman.model.{
  FeatureConstraint,
  FeatureCount,
  FeatureDiscount,
  FeatureDiscountTypes,
  FeatureInstance,
  FeatureOrigin,
  FeaturePayload,
  FeatureTypes,
  FeatureUnits
}
import ru.auto.salesman.model.user.product.AutoruProduct

trait PromocoderModelGenerators extends BasicSalesmanGenerators {

  def featureDiscountTypeGen(): Gen[FeatureDiscountTypes.Value] =
    enumGen(FeatureDiscountTypes)

  def featureDiscountGen: Gen[FeatureDiscount] =
    featureDiscountGen(featureDiscountTypeGen(), Gen.chooseNum(0, 100))

  def featureDiscountGen(
      featureDiscountTypeGen: Gen[FeatureDiscountTypes.Value],
      valueGen: Gen[Int]
  ): Gen[FeatureDiscount] =
    for {
      discountType <- featureDiscountTypeGen
      value <- valueGen
    } yield FeatureDiscount(discountType, value)

  def featureConstraintGen: Gen[FeatureConstraint] =
    for {
      offerIdentity <- OfferIdentityGen
    } yield FeatureConstraint(offerIdentity)

  def loyaltyFeaturePayloadGen: Gen[FeaturePayload] =
    Gen.const(
      FeaturePayload(
        FeatureUnits.Money,
        FeatureTypes.Loyalty,
        constraint = None,
        discount = None
      )
    )

  def loyaltyDiscountFeaturePayloadGen(percent: Int): Gen[FeaturePayload] =
    Gen.const(
      FeaturePayload(
        FeatureUnits.Items,
        FeatureTypes.Loyalty,
        constraint = None,
        discount = Some(
          FeatureDiscount(
            FeatureDiscountTypes.Percent,
            percent
          )
        )
      )
    )

  def promocodeFeaturePayloadGen: Gen[FeaturePayload] =
    promocodeFeaturePayloadGen(
      enumGen(FeatureUnits),
      Gen.option(featureDiscountGen)
    )

  def promocodeFeaturePayloadGen(
      featureUnitGen: Gen[FeatureUnits.Value],
      featureDiscountOptGen: Gen[Option[FeatureDiscount]]
  ): Gen[FeaturePayload] =
    for {
      unit <- featureUnitGen
      discount <- unit match {
        case FeatureUnits.Money => Gen.const(None)
        case FeatureUnits.Items => featureDiscountOptGen
      }
    } yield
      FeaturePayload(
        unit,
        FeatureTypes.Promocode,
        constraint = None,
        discount = discount
      )

  def bundleFeaturePayloadGen: Gen[FeaturePayload] =
    for {
      constraint <- Gen.option(featureConstraintGen)
    } yield
      FeaturePayload(
        FeatureUnits.Items,
        FeatureTypes.Bundle,
        constraint = constraint,
        discount = None
      )

  def featurePayloadGen: Gen[FeaturePayload] =
    Gen.oneOf(
      loyaltyFeaturePayloadGen,
      promocodeFeaturePayloadGen,
      bundleFeaturePayloadGen
    )

  def featureTagGen: Gen[String] = readableString

  def featureInstanceGen: Gen[FeatureInstance] =
    featureInstanceGen(featureTagGen, featurePayloadGen)

  def featureInstanceCashbackGen(product: AutoruProduct): Gen[FeatureInstance] =
    featureInstanceGen(
      product.name,
      promocodeFeaturePayloadGen(Gen.const(FeatureUnits.Money), None)
    )

  def featureInstancePercentGen(product: AutoruProduct): Gen[FeatureInstance] =
    featureInstancePercentInRangeGen(product, min = 1, max = 99)

  def featureInstancePercentGen(
      product: AutoruProduct,
      percent: Int
  ): Gen[FeatureInstance] =
    featureInstancePercentInRangeGen(product, min = percent, max = percent)

  def featureInstancePercentInRangeGen(
      product: AutoruProduct,
      min: Int,
      max: Int
  ): Gen[FeatureInstance] = {
    def requireValidPercent(percent: Int, name: String): Unit =
      require(
        percent >= 0 && percent <= 100,
        s"$name should be inside range [0; 100], but equals to $percent"
      )
    requireValidPercent(min, "min")
    requireValidPercent(max, "max")
    featureInstanceGen(
      product.fullName,
      promocodeFeaturePayloadGen(
        Gen.const(FeatureUnits.Items),
        Gen.some(
          featureDiscountGen(
            FeatureDiscountTypes.Percent,
            Gen.chooseNum(min, max)
          )
        )
      )
    )
  }

  def featureInstanceLoyaltyDiscountGen(percent: Int): Gen[FeatureInstance] =
    featureInstanceGen(featureTagGen, loyaltyDiscountFeaturePayloadGen(percent))

  def featureInstanceFixedPriceGen(
      product: AutoruProduct
  ): Gen[FeatureInstance] =
    featureInstanceFixedPriceInRangeGen(product, 1, 500)

  def featureInstanceFixedPriceInRangeGen(
      product: AutoruProduct,
      min: Int,
      max: Int
  ): Gen[FeatureInstance] =
    featureInstanceGen(
      product.name,
      promocodeFeaturePayloadGen(
        Gen.const(FeatureUnits.Items),
        Gen.some(
          featureDiscountGen(
            FeatureDiscountTypes.FixedPrice,
            Gen.chooseNum(min, max)
          )
        )
      )
    )

  def featureInstance100PercentGen(
      product: AutoruProduct
  ): Gen[FeatureInstance] =
    featureInstanceGen(
      product.name,
      promocodeFeaturePayloadGen(
        Gen.const(FeatureUnits.Items),
        Gen.some(featureDiscountGen(FeatureDiscountTypes.Percent, 100))
      )
    )

  def featureInstanceZeroFixedPriceGen(
      product: AutoruProduct
  ): Gen[FeatureInstance] =
    featureInstanceGen(
      product.name,
      promocodeFeaturePayloadGen(
        Gen.const(FeatureUnits.Items),
        Gen.some(featureDiscountGen(FeatureDiscountTypes.FixedPrice, 0))
      )
    )

  def featureInstanceGen(
      tagGen: Gen[String],
      payloadGen: Gen[FeaturePayload],
      countGen: Gen[Int] = Gen.posNum[Int]
  ): Gen[FeatureInstance] =
    for {
      id <- readableString
      origin <- readableString.map(FeatureOrigin)
      tag <- tagGen
      user <- readableString
      countValue <- countGen
      createTs <- dateTimeInPast
      deadline <- dateTimeInFuture()
      payload <- payloadGen
      count = payload.unit match {
        case FeatureUnits.Items => FeatureCount.Items(countValue)
        case FeatureUnits.Money => FeatureCount.Money(countValue)
      }
    } yield FeatureInstance(id, origin, tag, user, count, createTs, deadline, payload)

}

object PromocoderModelGenerators extends PromocoderModelGenerators
