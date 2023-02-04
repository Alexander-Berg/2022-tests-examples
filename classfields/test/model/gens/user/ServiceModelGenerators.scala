package ru.auto.salesman.test.model.gens.user

import cats.data.NonEmptySet
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel
import ru.auto.salesman.model._
import ru.auto.salesman.model.user._
import ru.auto.salesman.model.user.product.{AutoruProduct, BindedProduct, Products}
import ru.auto.salesman.service.PriceEstimateService.PriceRequest.{UserContext, UserOffer}
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.service.ProductApplyService.Request
import ru.auto.salesman.service.impl.UserPromocodesServiceImpl.CashbackFeatureTag
import ru.auto.salesman.service.impl.user.AutoruPriceService.GoodsPriceRequest
import ru.auto.salesman.service.user.ModifyPriceService.PatchedPrice
import ru.auto.salesman.service.user.PriceService
import ru.auto.salesman.service.user.PriceService._
import ru.auto.salesman.test.model.gens
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.util.money.Money.Kopecks
import ru.auto.salesman.util.{PriceRequestContext, PriceRequestContextOffers}
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus

import java.util.concurrent.TimeUnit
import scala.collection.immutable.SortedSet
import scala.concurrent.duration.Duration

trait ServiceModelGenerators extends UserModelGenerators with OfferModelGenerators {

  def bindedOfferProductRequestGen(
      autoruUserGen: Gen[AutoruUser] = AutoruUserGen,
      productPriceGen: Gen[ProductPrice] = ProductPriceGen
  ): Gen[Request] =
    productApplyServiceRequestGen(
      bindedOfferProductGen(autoruUserGen),
      productPriceGen
    )

  lazy val BindedUserProductRequestGen: Gen[Request] =
    productApplyServiceRequestGen(BindedUserProductGen, ProductPriceGen)

  // PositivePrice генераторы полезны для оплат с карты, т.к. невозможно снять
  // 0 рублей с карты, только с кошелька.
  def bindedOfferProductPositivePriceRequestGen(
      autoruUserGen: Gen[AutoruUser] = AutoruUserGen
  ): Gen[Request] =
    productApplyServiceRequestGen(
      bindedOfferProductGen(autoruUserGen),
      positiveProductPriceGen
    )

  lazy val bindedUserProductPositivePriceRequestGen: Gen[Request] =
    productApplyServiceRequestGen(BindedUserProductGen, positiveProductPriceGen)

  lazy val BindedProductRequestGen: Gen[Request] =
    productApplyServiceRequestGen(BindedProductGen, ProductPriceGen)

  lazy val BindedOfferProductRequestGen: Gen[Request] =
    productApplyServiceRequestGen(BindedOfferProductGen, ProductPriceGen)

  def bindedProductRequestZeroPriceGen(
      bindedProductGen: Gen[BindedProduct],
      productPriceGen: Gen[ProductPrice] = ProductPriceGen
  ): Gen[Request] =
    productApplyServiceRequestGen(
      bindedProductGen,
      productPriceGen.map { pp =>
        val withZeroPrice = pp.price.copy(effectivePrice = 0)
        pp.copy(price = withZeroPrice)
      }
    )

  def bindedProductRequestNotZeroPriceGen(
      bindedProductGen: Gen[BindedProduct]
  ): Gen[Request] =
    productApplyServiceRequestGen(
      bindedProductGen,
      ProductPriceGen.map { pp =>
        val price = pp.price
        val withoutZeroPrice = setOneWhenZeroPrice(price)
        pp.copy(price = withoutZeroPrice)
      }
    )

  private def setOneWhenZeroPrice(price: Price): Price = {
    val newEffectivePrice =
      if (price.effectivePrice == 0) price.effectivePrice + 1
      else price.effectivePrice
    price.copy(effectivePrice = newEffectivePrice)
  }

  lazy val UserSellerTypeGen: Gen[UserSellerType] =
    Gen.oneOf(UserSellerType.values)

  lazy val RegionIdGen: Gen[RegionId] = Gen.posNum[Long].map(RegionId.apply)

  lazy val ContentQualityGen: Gen[ContentQuality] = Gen.choose(0, 100)

  lazy val productIdGen: Gen[AutoruProduct] = Gen.oneOf(Products.all.toSeq)

  lazy val experimentGen: Gen[Experiment] = for {
    experimentId <- Gen.alphaNumStr
    regionId <- Gen.option(Gen.nonEmptyListOf(RegionIdGen))
    regionIdNonEmptySet = regionId.flatMap(regions =>
      NonEmptySet.fromSet(SortedSet[RegionId]() ++ regions)
    )
    productsId <- Gen.option(Gen.nonEmptyListOf(productIdGen))
    productsNonEmptySet = productsId.flatMap(r =>
      NonEmptySet.fromSet(SortedSet[AutoruProduct]() ++ r)
    )
  } yield
    Experiment(
      experimentId,
      geoIds = regionIdNonEmptySet,
      experimentProducts = productsNonEmptySet
    )

  lazy val PriceServiceUserContextGen: Gen[PriceService.UserContext] = for {
    geoIds <- Gen.listOf(RegionIdGen)
    features <- Gen.listOf(featureInstanceGen)
    moneyFeature <- Gen.listOf(moneyFeatureInstanceGen())
    allUserExperiment <- Gen.nonEmptyListOf(experimentGen)
    expBoxes <- Gen.alphaNumStr
  } yield
    PriceService.UserContext(
      Some(Experiments(expBoxes, allUserExperiment)),
      geoIds,
      userModerationStatus = Some(UserModerationStatus.newBuilder.build),
      features,
      moneyFeature
    )

  lazy val PriceRequestContextOffersGen: Gen[PriceRequestContextOffers] = for {
    offerSource <- Gen.listOfN(1, offerGen())
    applyMoneyFeature <- bool
    applyProlongInterval <- bool
    user <- Gen.some(AutoruUserGen)
  } yield
    PriceRequestContextOffers(
      offerSource,
      applyMoneyFeature,
      applyProlongInterval,
      user
    )

  lazy val PriceRequestContextOffersOneOfferGen: Gen[PriceRequestContextOffers] =
    for {
      offerSource <- Gen.listOfN(1, offerGen())
      applyMoneyFeature <- bool
      applyProlongInterval <- bool
      user <- Gen.some(AutoruUserGen)
    } yield
      PriceRequestContextOffers(
        offerSource,
        applyMoneyFeature,
        applyProlongInterval,
        user
      )

  val priceRequestContextGen: Gen[PriceRequestContext] = for {
    user <- Gen.option(AutoruUserGen)
    moderationStatus <- Gen.option(userModerationStatusGen)
    offerId <- Gen.option(OfferIdentityGen)
    category <- Gen.option(gens.OfferCategoryGen)
    section <- Gen.option(gens.OfferSectionGen)
    geoId <- Gen.option(RegionIdGen)
    vin <- Gen.option(Gen.alphaStr)
    contentQuality <- Gen.option(ContentQualityGen)
    applyMoneyFeature <- bool
  } yield
    PriceRequestContext(
      contextType = None,
      moderationStatus,
      user,
      offerId,
      None,
      category,
      section,
      geoId,
      vin,
      vinReportParams = None,
      licensePlate = None,
      contentQuality,
      applyMoneyFeature,
      applyProlongInterval = true
    )

  private def productApplyServiceRequestGen(
      bindedProductGen: Gen[BindedProduct],
      productPriceGen: Gen[ProductPrice]
  ) =
    Gen
      .zip(
        bindedProductGen,
        productPriceGen,
        ProductSourceGen,
        Gen.some(readableString)
      )
      .map(Request.tupled)

  lazy val EnrichedPriceRequestContextGen: Gen[EnrichedPriceRequestContext] =
    for {
      hasOffer <- bool
      hasGoodsPriceRequest <- bool
      context <- enrichedPriceRequestContext(hasOffer, hasGoodsPriceRequest)
    } yield context

  def enrichedPriceRequestContext(
      hasOffer: Boolean = true,
      hasUser: Boolean = true,
      hasGoodsPriceRequest: Boolean = false,
      applyProlongInterval: Gen[Boolean] = bool
  ): Gen[EnrichedPriceRequestContext] =
    for {
      priceRequestContext <- priceRequestContextGen

      user <- if (hasUser) Gen.some(AutoruUserGen) else Gen.const(None)
      userType <- UserSellerTypeGen

      offer <- if (hasOffer) Gen.some(EnrichedOfferGen) else Gen.const(None)

      geoId <- Gen.listOf(RegionIdGen)
      contentQuality <- Gen.option(ContentQualityGen)
      category <- Gen.option(gens.OfferCategoryGen)
      section <- Gen.option(gens.OfferSectionGen)
      product <- ProductGen
      features <- Gen.listOf(featureInstanceGen)
      promocoderFeatures <- Gen.mapOf(product, features)
      allUserExperiment <- Gen.listOf(experimentGen)
      expBoxes <- Gen.alphaNumStr
      goodsPriceRequest <-
        if (hasGoodsPriceRequest)
          Gen.some(GoodsPriceRequestGen)
        else Gen.const(None)
      userPeriodicalDiscount <- userPeriodicalDiscountGen()
      applyProlongInterval <- applyProlongInterval
      applyMoneyFeature <- bool
      bonusMoneyFeature <- Gen.listOf(moneyFeatureInstanceGen())
      quotaLeft <- Gen.mapOf(for {
        product <- ProductGen
        quotaLeft <- Gen.posNum[Long]
      } yield product -> quotaLeft)
    } yield
      EnrichedPriceRequestContext(
        priceRequestContext,
        contextType = None,
        user,
        userType,
        offer,
        geoId,
        contentQuality,
        category,
        section,
        Some(Experiments(expBoxes, allUserExperiment)),
        goodsPriceRequest,
        userPeriodicalDiscount,
        promocoderFeatures,
        bonusMoneyFeature,
        applyMoneyFeature,
        applyProlongInterval,
        quotaLeft,
        userContextOpt = None
      )

  lazy val EnrichedOfferGen: Gen[EnrichedOffer] = enrichedOfferGen(
    ActiveOfferGen
  )

  def enrichedOfferGen(offerGen: Gen[ApiOfferModel.Offer]): Gen[EnrichedOffer] =
    for {
      offerId <- OfferIdentityGen
      source <- offerGen
      user <- Gen.some(AutoruUserGen)
      info <- OfferInfoModelGen
      year <- Gen.posNum[Int]
      geoId <- Gen.listOf(RegionIdGen)
      price <- Gen.posNum[Long]
      createDate <- dateTimeInPast
    } yield EnrichedOffer(offerId, source, user, info, year, geoId, price, createDate)

  lazy val OfferInfoModelGen: Gen[OfferInfoModel] = for {
    category <- gens.OfferCategoryGen
    section <- gens.OfferSectionGen
    mark <- Gen.option(Gen.alphaNumStr)
    model <- Gen.option(Gen.alphaNumStr)
    generationId <- Gen.option(Gen.alphaNumStr)
  } yield OfferInfoModel(category, section, mark, model, generationId)

  lazy val GoodsPriceRequestGen: Gen[GoodsPriceRequest] = for {
    offer <- UserOfferGen
    context <- UserContextGen
  } yield GoodsPriceRequest(offer, context)

  lazy val UserOfferGen: Gen[UserOffer] = for {
    category <- gens.OfferCategoryGen
    section <- gens.OfferSectionGen
    mark <- Gen.alphaNumStr
    model <- Gen.alphaNumStr
    generationId <- Gen.option(Gen.alphaNumStr)
    year <- Gen.posNum[Int]
    geoId <- Gen.listOf(RegionIdGen)
    price <- Gen.posNum[Long]
    creationTs <- dateTimeInPast
  } yield
    UserOffer(
      category,
      section,
      mark,
      model,
      generationId,
      year,
      geoId,
      price,
      creationTs
    )

  lazy val UserContextGen: Gen[UserContext] = for {
    offerType <- enumGen(OfferTypes)
    userType <- UserSellerTypeGen
    canAddFree <- bool
    numByMark <- Gen.option(Gen.posNum[Int])
    numByModel <- Gen.option(Gen.posNum[Int])
    invalidVin <- bool
    experiment <- Gen.alphaNumStr
    autoApply <- bool
    paymentReason <- Gen.option(PaymentReasonGen)
  } yield
    UserContext(
      offerType,
      userType,
      canAddFree,
      numByMark,
      numByModel,
      invalidVin,
      experiment,
      autoApply,
      paymentReason
    )

  lazy val FeatureCountGen: Gen[FeatureCount] = for {
    count <- Gen.posNum[Long]
    units <- enumGen(FeatureUnits)
  } yield FeatureCount(count, units)

  lazy val FeatureOriginGen: Gen[FeatureOrigin] = for {
    id <- Gen.alphaNumStr
  } yield FeatureOrigin(id)

  lazy val FeaturePayloadGen: Gen[FeaturePayload] = for {
    unit <- enumGen(FeatureUnits)
    featureType <-
      if (unit == FeatureUnits.Money)
        Gen.oneOf(FeatureTypes.Promocode, FeatureTypes.Loyalty)
      else Gen.oneOf(FeatureTypes.Bundle, FeatureTypes.Promocode)
    constraint = Option.empty[FeatureConstraint]
    discount = Option.empty[FeatureDiscount]
  } yield FeaturePayload(unit, featureType, constraint, discount)

  lazy val FeatureConstraintGen: Gen[FeatureConstraint] = for {
    offer <- OfferIdentityGen
  } yield FeatureConstraint(offer)

  lazy val FeatureDiscountGen: Gen[FeatureDiscount] = for {
    discountType <- Gen.oneOf(
      FeatureDiscountTypes.FixedPrice,
      FeatureDiscountTypes.Percent
    )
    value <- Gen.posNum[Int]
  } yield FeatureDiscount(discountType, value)

  def moneyFeatureInstanceGen(
      countGen: Gen[Int] = Gen.posNum[Int]
  ): Gen[MoneyFeatureInstance] =
    for {
      source <- featureInstanceGen(readableString, featurePayloadGen, countGen)
    } yield MoneyFeatureInstance(source)

  def cashbackGen(countGen: Gen[Int] = Gen.posNum[Int]): Gen[MoneyFeatureInstance] =
    featureInstanceGen(
      CashbackFeatureTag,
      loyaltyFeaturePayloadGen,
      countGen
    ).map(MoneyFeatureInstance)

  def userTariffGen(): Gen[UserTariff] =
    Gen.oneOf(UserTariff.values)

  lazy val ProductInfoGen: Gen[ProductInfo] = for {
    price <- FundsGen
    prolongPrice <- Gen.option(FundsGen)
    productId = ProductId.Fresh
    tariff <- Gen.option(userTariffGen())
    duration <- Gen.option(productDurationGen)
  } yield
    ProductInfo(productId, Kopecks(price), prolongPrice, duration, tariff, None, None)

  lazy val PromoFeatureGen: Gen[PromoFeature] = for {
    zeroPriceInstance <- Gen.option(featureInstanceGen)
    featureInstance <- Gen.option(featureInstanceGen)
    percent <- Gen.option(featureInstanceGen)
  } yield PromoFeature(zeroPriceInstance, featureInstance, percent)

  lazy val EnrichedProductGen: Gen[EnrichedProduct] = for {
    product <- ProductGen
    enrichedProduct <- enrichedProduct(product)
  } yield enrichedProduct

  def enrichedProduct(product: AutoruProduct): Gen[EnrichedProduct] =
    for {
      productId <- Gen.oneOf(ProductId.values.toList)
      isFreeByQuota <- bool
      baseQuota <- Gen.posNum[Int]
      duration <- Gen.option(productDurationGen)
      limitReason <- Gen.option(PaymentReasonGen)
      features <- Gen.listOf(featureInstanceGen)
      productPeriodicalDiscount <- productPeriodicalDiscountGen()
      offerType <- Gen.option(Gen.oneOf(OfferTypes.values.toList))
    } yield
      EnrichedProduct(
        product,
        productId,
        isFreeByQuota,
        baseQuota,
        duration,
        limitReason,
        features,
        productPeriodicalDiscount,
        offerType
      )

  lazy val DurationGen: Gen[Duration] = for {
    duration <- Gen.posNum[Long]
  } yield Duration(duration, TimeUnit.SECONDS)

  lazy val ProductQuotaGen: Gen[ProductQuota] = for {
    size <- Gen.choose(0, 2)
    baseQuota <- Gen.choose(0, 2)
    reason <- Gen.option(PaymentReasonGen)
    duration <- Gen.option(productDurationGen)
    offerType <- Gen.option(Gen.oneOf(OfferTypes.values.toList))
  } yield ProductQuota(size, baseQuota, reason, duration, offerType)

  def patchedPriceGen(
      periodicalDiscountExclusion: Gen[
        Option[Analytics.UserExcludedFromDiscount]
      ] = Gen.option(analyticsUserExcludedFromDiscountGen)
  ): Gen[PatchedPrice] =
    for {
      basePrice <- Gen.posNum[Long]
      effectivePrice <- Gen.posNum[Long]
      modifier <- Gen.option(PriceModifierGen)
      periodicalDiscountExclusion <- periodicalDiscountExclusion
    } yield
      PatchedPrice(
        basePrice,
        effectivePrice,
        modifier,
        periodicalDiscountExclusion
      )
}
