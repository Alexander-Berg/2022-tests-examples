package ru.auto.salesman.test.model.gens.user

import org.joda.time.DateTime
import org.scalacheck.Gen
import org.scalacheck.Gen._
import ru.auto.salesman.model.offer.OfferIdentity
import ru.auto.salesman.model.user.PriceModifier.ProlongInterval
import ru.auto.salesman.model.user.ProductContext.{
  BundleContext,
  GoodsContext,
  SubscriptionContext
}
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Vip
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.user.product.ProductProvider._
import ru.auto.salesman.model.user.product.ProductSource.{AutoApply, AutoProlong}
import ru.auto.salesman.model.user.product.Products.{
  Bundle => _,
  Goods => _,
  Subscription => _,
  _
}
import ru.auto.salesman.model.user.product.{Products, _}
import ru.auto.salesman.model.user.{PaymentPayloadRaw, _}
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  DomainAware,
  ExperimentId,
  FeatureConstraint,
  FeaturePayload,
  FeatureTypes,
  FeatureUnits,
  Funds,
  PaymentAction,
  PaymentActions,
  PaymentSystem,
  PaymentSystems,
  ProductDuration,
  ProductStatus,
  ProductStatuses,
  TransactionId,
  TransactionStatus,
  TransactionStatuses,
  UserId
}
import ru.auto.salesman.test.model.gens.{
  AutoruOfferIdGen,
  BasicSalesmanGenerators,
  PromocoderModelGenerators
}
import ru.yandex.passport.model.common.CommonModel.UserModerationStatus
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.vertis.moderation.proto.Model.Reason
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.collection.JavaConverters._
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.reflect.ClassTag
import ru.auto.salesman.model.user._

trait UserModelGenerators
    extends BasicSalesmanGenerators
    with PromocoderModelGenerators
    with DomainAware {

  val OfferIdentityGen: Gen[OfferIdentity] =
    domain match {
      case DeprecatedDomains.AutoRu =>
        AutoruOfferIdGen
    }

  val AutoruUserGen: Gen[AutoruUser] = for {
    id <- Gen.posNum[Long]
  } yield AutoruUser(id)

  val OptionAutoruUserGen: Gen[Option[AutoruUser]] = Gen.option(AutoruUserGen)

  val AutoruUserIdGen: Gen[UserId] = AutoruUserGen.map(_.toString)

  val ProductGen: Gen[AutoruProduct] = Gen.oneOf(Products.all.toSeq)

  def productGen[A <: AutoruProduct: ClassTag]: Gen[A] =
    Gen.oneOf(Products.allOfType[A].toSeq)

  def prolongableProductGen[A <: AutoruProduct: ClassTag]: Gen[A] =
    Gen.oneOf(
      filterAutoProlongable(Products.allOfType[A], shouldBe = true).toSeq
    )

  def productNotPlacementGen[A <: AutoruProduct: ClassTag]: Gen[A] =
    Gen.oneOf(Products.allOfType[A].filterNot(_ == Placement).toSeq)

  val ProductNotPlacementGen: Gen[AutoruProduct] =
    Gen.oneOf(Products.all.filter(_ != Placement).toSeq)

  lazy val OfferProductGen: Gen[OfferProduct] = productGen[OfferProduct]

  lazy val UserProductGen: Gen[UserProduct] =
    Gen.oneOf(OffersHistoryReports(1), OffersHistoryReports(10))

  lazy val AutoProlongableOfferProductGen: Gen[OfferProduct] =
    Gen.oneOf(
      filterAutoProlongable(
        Products.allOfType[OfferProduct],
        shouldBe = true
      ).toSeq
    )

  lazy val NonAutoProlongableOfferProductGen: Gen[OfferProduct] =
    Gen.oneOf(
      filterAutoProlongable(
        Products.allOfType[OfferProduct],
        shouldBe = false
      ).toSeq
    )

  lazy val NonAutoProlongableUserProductGen: Gen[UserProduct] =
    Gen.oneOf(
      filterAutoProlongable(
        Products.allOfType[UserProduct],
        shouldBe = false
      ).toSeq
    )

  private def filterAutoProlongable[A <: AutoruProduct: ClassTag](
      products: Set[A],
      shouldBe: Boolean
  ) =
    products.filter {
      case _: AutoProlongable => shouldBe
      case _ => !shouldBe
    }

  def bindedOfferProductGen(
      autoruUserGen: Gen[AutoruUser] = AutoruUserGen
  ): Gen[BindedOfferProduct] =
    Gen
      .zip(autoruUserGen, OfferIdentityGen, OfferProductGen)
      .map(BindedOfferProduct.tupled)

  lazy val BindedUserProductGen: Gen[BindedUserProduct] =
    Gen.zip(AutoruUserGen, UserProductGen).map(BindedUserProduct.tupled)

  lazy val BindedProductGen: Gen[BindedProduct] =
    Gen.oneOf(bindedOfferProductGen(), BindedUserProductGen)

  lazy val BindedOfferProductGen: Gen[BindedOfferProduct] =
    bindedOfferProductGen()

  val ProductSourceGen: Gen[ProductSource] =
    Gen.oneOf(readableString.map(AutoProlong), Gen.posNum[Long].map(AutoApply))

  val PaymentReasonGen: Gen[PaymentReason] = enumGen(PaymentReasons)

  val FeatureGen: Gen[PriceModifier.Feature] = for {
    instanceId <- readableString
    deadline <- dateTimeInPast()
    payload = FeaturePayload(FeatureUnits.Items, FeatureTypes.Promocode)
    count <- Gen.posNum[Int]
  } yield PriceModifier.Feature(instanceId, deadline, payload, count)

  val ExperimentIdGen: Gen[ExperimentId] = readableString

  val ExperimentInfoGen: Gen[ExperimentInfo] = for {
    id <- ExperimentIdGen
    boxes <- readableString
  } yield ExperimentInfo(Some(id), boxes)

  val ProlongIntervalGen: Gen[ProlongInterval] = for {
    prolongPrice <- Gen.posNum[Long]
    willExpire <- Gen.oneOf(dateTimeInPast, dateTimeInFuture())
  } yield ProlongInterval(prolongPrice, willExpire)

  val PeriodicalDiscountModifierGen: Gen[PriceModifier.PeriodicalDiscount] =
    for {
      discountId <- readableString
      discount <- Gen.choose(0, 100)
      deadline <- dateTimeInFuture()
    } yield PriceModifier.PeriodicalDiscount(discountId, discount, deadline)

  val PeriodicalDiscountContextGen: Gen[PeriodicalDiscountContext] = for {
    productCount <- Gen.choose(1, 3)
    products <- Gen.listOfN(productCount, ProductGen)
  } yield PeriodicalDiscountContext(Some(products.map(_.name)))

  val PeriodicalDiscountGen: Gen[PeriodicalDiscount] = for {
    id <- readableString
    start <- dateTimeInPast()
    deadline <- dateTimeInFuture()
    discount <- Gen.choose(1, 99)
    context <- PeriodicalDiscountContextGen
  } yield PeriodicalDiscount(id, start, deadline, discount, Some(context))

  val PriceModifierGen: Gen[PriceModifier] = for {
    feature <- Gen.option(FeatureGen)
    bundleId <- Gen.option(readableString)
    experiment <- Gen.option(ExperimentInfoGen)
    periodicalDiscount <- Gen.option(PeriodicalDiscountModifierGen)
  } yield
    PriceModifier(
      feature,
      bundleId,
      experiment,
      experiment.flatMap(_.activeExperimentId),
      periodicalDiscount
    )

  def priceModifierGen(
      prolongInterval: Gen[Option[ProlongInterval]] = Gen.option(ProlongIntervalGen),
      bundleId: Gen[Option[String]] = Gen.option(readableString),
      periodicalDiscount: Gen[Option[PriceModifier.PeriodicalDiscount]] =
        Gen.option(PeriodicalDiscountModifierGen),
      feature: Gen[Option[PriceModifier.Feature]] = Gen.option(FeatureGen)
  ): Gen[PriceModifier] =
    for {
      feature <- feature
      bundleId <- bundleId
      experiment <- Gen.option(ExperimentInfoGen)
      periodicalDiscount <- periodicalDiscount
      prolongInterval <- prolongInterval
    } yield
      PriceModifier(
        feature,
        bundleId,
        experiment,
        experiment.flatMap(_.activeExperimentId),
        periodicalDiscount,
        prolongInterval
      )

  def constPriceGen(
      basePrice: Gen[Funds] = FundsGen,
      prolongPrice: Gen[Option[Funds]] = Gen.option(FundsGen),
      modifier: Gen[Option[PriceModifier]] = Gen.option(PriceModifierGen)
  ): Gen[Price] =
    for {
      basePrice <- basePrice
      prolongPrice <- prolongPrice
      effectivePrice <- Gen.posNum[Long]
      modifier <- modifier
      policyId <- readableString
    } yield Price(basePrice, effectivePrice, prolongPrice, modifier, Some(policyId))

  type BasePrice = Funds

  def priceGen(
      effectivePrice: BasePrice => Gen[Funds] = basePrice => Gen.choose(0, basePrice),
      modifier: Gen[Option[PriceModifier]] = Gen.option(PriceModifierGen)
  ): Gen[Price] =
    for {
      basePrice <- Gen.posNum[Long]
      effectivePrice <- effectivePrice(basePrice)
      prolongPrice <- Gen.option(Gen.posNum[Long])
      modifier <- modifier
      policyId <- readableString
    } yield Price(basePrice, effectivePrice, prolongPrice, modifier, Some(policyId))

  val PriceGen: Gen[Price] =
    priceGen()

  val positivePriceGen: Gen[Price] =
    priceGen(effectivePrice = basePrice => Gen.choose(1, basePrice))

  val ProductPriceInfoGen: Gen[ProductPriceInfo] = productPriceInfoGen(bool)

  val ProductPriceGen: Gen[ProductPrice] = productPriceGen(
    Gen.option(ProductPriceInfoGen)
  )

  val positiveProductPriceGen: Gen[ProductPrice] =
    productPriceGen(price = positivePriceGen)

  val ProductPriceWithAutoAppliedPriceGen: Gen[ProductPrice] = productPriceGen(
    Gen.some(productPriceInfoGen(Gen.const(true)))
  )

  def productPriceGen(
      productPriceInfo: Gen[Option[ProductPriceInfo]] = Gen.option(ProductPriceInfoGen),
      duration: Gen[ProductDuration] = productDurationGen,
      price: Gen[Price] = PriceGen,
      product: Gen[AutoruProduct] = ProductGen,
      analytics: Gen[Option[Analytics]] = Gen.option(analyticsGen),
      paymentReason: Gen[Option[PaymentReason]] = Gen.option(PaymentReasonGen),
      scheduleFreeBoost: Gen[Boolean] = bool
  ): Gen[ProductPrice] =
    for {
      product <- product
      duration <- duration
      paymentReason <- paymentReason
      price <- price
      prolongationAvailable <- bool
      prolongationForced <- bool
      prolongationForcedNotTogglable <- bool
      productPriceInfo <- productPriceInfo
      analytics <- analytics
      scheduleFreeBoost <- scheduleFreeBoost
    } yield
      ProductPrice(
        product,
        duration,
        paymentReason,
        price,
        prolongationAvailable,
        prolongationForced,
        prolongationForcedNotTogglable,
        productPriceInfo,
        analytics,
        scheduleFreeBoost
      )

  def analyticsGen: Gen[Analytics] =
    for {
      analyticsUserExcludedFromDiscount <- Gen.option(
        analyticsUserExcludedFromDiscountGen
      )
    } yield Analytics(analyticsUserExcludedFromDiscount)

  def analyticsUserExcludedFromDiscountGen: Gen[Analytics.UserExcludedFromDiscount] =
    for {
      discountId <- readableString
    } yield Analytics.UserExcludedFromDiscount(discountId)

  val ProductPriceWithInfoGen: Gen[ProductPrice] = for {
    product <- ProductGen
    duration <- productDurationGen
    paymentReason <- Gen.option(PaymentReasonGen)
    price <- PriceGen
    prolongationAvailable <- bool
    prolongationForced <- bool
    prolongationForcedNotTogglable <- bool
    productPriceInfo <- Gen.some(ProductPriceInfoGen)
    periodicalDiscountExclusion <- Gen.option(analyticsGen)
    scheduleFreeBoost <- bool
  } yield
    ProductPrice(
      product,
      duration,
      paymentReason,
      price,
      prolongationAvailable,
      prolongationForced,
      prolongationForcedNotTogglable,
      productPriceInfo,
      periodicalDiscountExclusion,
      scheduleFreeBoost
    )

  val PackageContentGen: Gen[PackageContent] = for {
    alias <- readableString
    name <- Gen.option(readableString)
    duration <- productDurationGen
  } yield PackageContent(alias, name, duration)

  def productPriceInfoGen(
      withAutoAppliedPriceGen: Gen[Boolean]
  ): Gen[ProductPriceInfo] =
    for {
      name <- Gen.option(readableString)
      title <- Gen.option(readableString)
      description <- Gen.option(readableString)
      multiplier <- Gen.option(Gen.posNum[Int])
      aliases <- Gen.listOf(Gen.alphaNumStr)
      withAutoAppliedPrice <- withAutoAppliedPriceGen
      autoApplyPrice <-
        if (withAutoAppliedPrice) Gen.some(Gen.posNum[Long])
        else Gen.const(None)
      packageContent <- Gen.listOf(PackageContentGen)
      purchaseForbidden <- bool
      quotaLeft <- Gen.option(Gen.posNum[Int])
    } yield
      ProductPriceInfo(
        name,
        title,
        description,
        multiplier,
        aliases,
        autoApplyPrice,
        packageContent,
        purchaseForbidden,
        quotaLeft.map(_.toLong)
      )

  val ProductPriceInfoOptGen: Gen[Option[ProductPriceInfo]] =
    Gen.option(ProductPriceInfoGen)

  val ProductPricesGen: Gen[ProductPrices] = for {
    offerId <- readableString
    prices <- Gen.nonEmptyListOf(ProductPriceGen)
  } yield ProductPrices(offerId, prices)

  val GoodsContextGen: Gen[GoodsContext] = goodsContextGen()

  def goodsContextGen(
      productPrice: Gen[ProductPrice] = ProductPriceGen
  ): Gen[GoodsContext] =
    for {
      price <- productPrice
    } yield GoodsContext(price)

  def bundleContextGen(
      productPrice: Gen[ProductPrice] = ProductPriceGen
  ): Gen[BundleContext] =
    for {
      price <- productPrice
    } yield BundleContext(price)

  val BundleContextGen: Gen[BundleContext] = bundleContextGen()

  def subscriptionContextGen(
      productPrice: Gen[ProductPrice] = ProductPriceGen
  ): Gen[SubscriptionContext] =
    for {
      price <- productPrice
    } yield SubscriptionContext(price, vinOrPlate = None, garageId = None)

  val SubscriptionContextGen: Gen[SubscriptionContext] =
    subscriptionContextGen()

  def productContextGen(
      productType: Gen[ProductType],
      productPrice: Gen[ProductPrice] = productPriceGen()
  ): Gen[ProductContext] =
    for {
      productType <- productType
      context <- productType match {
        case ProductType.Goods => goodsContextGen(productPrice)
        case ProductType.Bundle => bundleContextGen(productPrice)
        case ProductType.Subscription => subscriptionContextGen(productPrice)
      }
    } yield context

  val ProlongableGen: Gen[Prolongable] = bool.map(Prolongable)

  // Самостоятельно активированный продукт, т.е. активированный не в рамках пакета.
  val selfActivatedProductPriceGen: Gen[ProductPrice] =
    productPriceGen(price =
      priceGen(modifier = Gen.option(priceModifierGen(bundleId = None)))
    )

  def productRequestGen(
      product: Gen[AutoruProduct],
      offerId: Gen[Option[OfferIdentity]] = Gen.some(OfferIdentityGen),
      productPrice: Gen[ProductPrice] = productPriceGen(),
      prolongable: Gen[Prolongable] = ProlongableGen
  ): Gen[ProductRequest] =
    for {
      product <- product
      offerId <- offerId
      amount <- Gen.posNum[Long]
      context <- productContextGen(product.productType, productPrice)
      prolongable <- prolongable
    } yield ProductRequest(product, offerId, amount, context, prolongable)

  val ProductRequestGen: Gen[ProductRequest] = productRequestGen(ProductGen)

  val FieldGen: Gen[(String, String)] = for {
    first <- Gen.alphaNumStr
    second <- Gen.alphaNumStr
  } yield (first, second)

  val bankerTransactionIdGen: Gen[TransactionId] =
    // contains domain id after @
    readableString.map(_ + "@1")

  def paidAtGen(createdAt: DateTime): Gen[DateTime] =
    dateTimeInPast((DateTimeUtil.now().getMillis - createdAt.getMillis).millis)

  def createTransactionResultGen(
      createdAt: DateTime = DateTime.now()
  ): Gen[CreateTransactionResult] =
    readableString.map(CreateTransactionResult(_, createdAt))

  def transactionGen(
      user: Gen[UserId] = AutoruUserIdGen,
      bankerTransactionId: Gen[Option[TransactionId]] =
        Gen.option(bankerTransactionIdGen),
      paidAt: DateTime => Gen[Option[DateTime]] = createdAt =>
        Gen.option(paidAtGen(createdAt)),
      statusGen: Gen[TransactionStatus] = enumGen(TransactionStatuses),
      productGen: Gen[AutoruProduct] = ProductGen
  ): Gen[Transaction] =
    for {
      id <- posNum[Long]
      transactionId <- readableString
      user <- user
      amount <- Gen.posNum[Long]
      status <- statusGen
      payload <- listUnique(2, 3, productRequestGen(productGen))(_.product)
      createdAt <- dateTimeInPast()
      bankerTransactionId <- bankerTransactionId
      paidAt <- paidAt(createdAt)
      epoch <- posNum[Long]
      fields <- Gen.listOf(FieldGen)
    } yield
      Transaction(
        id,
        transactionId,
        user,
        amount,
        status,
        payload,
        createdAt,
        bankerTransactionId,
        paidAt,
        fields,
        epoch
      )

  val TransactionGen: Gen[Transaction] = transactionGen()

  private def dateTimeInPastBefore(base: DateTime) =
    dateTimeInPast((DateTimeUtil.now().getMillis - base.getMillis).millis)

  // with all defined fields
  def finalTransactionGen(
      statusGen: Gen[TransactionStatus] = enumGen(TransactionStatuses)
  ): Gen[Transaction] = transactionGen(
    bankerTransactionId = Gen.some(bankerTransactionIdGen),
    paidAt = createdAt => {
      dateTimeInPastBefore(createdAt).map(Some(_))
    },
    statusGen = statusGen
  )

  def paidTransactionGen(
      user: Gen[String] = AutoruUserIdGen
  ): Gen[PaidTransaction] =
    for {
      origin <- transactionGen(user = user)
      bankerTransactionId <- bankerTransactionIdGen
      paidAt <- dateTimeInPastBefore(origin.createdAt)
    } yield PaidTransaction(origin, bankerTransactionId, paidAt)

  val FundsGen: Gen[Funds] = posNum[Funds]

  val TransactionRequestGen: Gen[TransactionRequest] = for {
    user <- AutoruUserGen
    payload <- Gen.nonEmptyListOf(ProductRequestGen)
  } yield TransactionRequest(user.toString, payload.map(_.amount).sum, payload)

  def goodsGen(
      goodsProduct: Gen[Products.Goods] = productGen[Products.Goods],
      prolongable: Gen[Prolongable] = ProlongableGen,
      activated: Gen[DateTime] = dateTimeInPast,
      status: Gen[ProductStatus] = enumGen(ProductStatuses),
      offerIdentity: Gen[OfferIdentity] = OfferIdentityGen,
      context: Gen[GoodsContext] = goodsContextGen(),
      transactionId: Gen[String] = readableString
  ): Gen[Goods] =
    for {
      id <- readableString
      offer <- offerIdentity
      user <- AutoruUserIdGen
      product <- goodsProduct
      amount <- Gen.posNum[Long]
      status <- status
      transactionId <- transactionId
      context <- context
      activated <- activated
      deadline <- status match {
        case ProductStatuses.Active =>
          dateTimeInFuture()
        case _ =>
          dateTimeInPast(
            minDistance = 0.seconds,
            maxDistance = (DateTimeUtil.now().getMillis - activated.getMillis).millis
          )
      }
      epoch <- Gen.posNum[Long]
      prolongable <- prolongable
    } yield
      Goods(
        id,
        offer,
        user,
        product,
        amount,
        status,
        transactionId,
        context.copy(
          productPrice = context.productPrice.copy(product = product)
        ),
        activated,
        deadline,
        epoch,
        prolongable
      )

  lazy val GoodsGen: Gen[Goods] =
    goodsGen()

  lazy val GoodsWithDifferentUserGen: Gen[(Goods, Goods)] = for {
    userId1 <- Gen.posNum[Long]
    userId2 <- userId1 + 1
    good1 <- goodsGen(productGen[Products.Goods], ProlongableGen)
      .map(_.copy(user = AutoruUser(userId1).toString))
    good2 <- goodsGen(productGen[Products.Goods], ProlongableGen)
      .map(_.copy(user = AutoruUser(userId2).toString))
  } yield (good1, good2)

  lazy val ProlongableGoodsGen: Gen[Goods] =
    goodsGen(
      Gen.oneOf(
        filterAutoProlongable(
          Products.allOfType[Products.Goods],
          shouldBe = true
        ).toSeq
      ),
      Prolongable(true)
    )

  def concreteProlongableGoodsGen(
      productTypes: Set[Products.Goods]
  ): Gen[Goods] =
    goodsGen(
      Gen.oneOf(filterAutoProlongable(productTypes, shouldBe = true).toSeq),
      Prolongable(true)
    )

  def goodsBundleGen(
      productGen: Gen[Products.GoodsBundle],
      prolongableGen: Gen[Prolongable]
  ): Gen[Bundle] =
    for {
      id <- readableString
      offer <- AutoruOfferIdGen
      user <- AutoruUserIdGen
      product <- productGen
      amount <- Gen.posNum[Long]
      status <- enumGen(ProductStatuses)
      transactionId <- readableString
      context <- BundleContextGen
      activated <- dateTimeInPast
      deadline <- status match {
        case ProductStatuses.Active =>
          dateTimeInFuture()
        case _ =>
          dateTimeInPast(
            (DateTimeUtil.now().getMillis - activated.getMillis).millis
          )
      }
      epoch <- BasicGenerators.posNum[Long]
      prolongable <- prolongableGen
    } yield
      Bundle(
        id,
        offer,
        user,
        product,
        amount,
        status,
        transactionId,
        context,
        activated,
        deadline,
        epoch,
        prolongable
      )

  def goodsBundleActivatedGen(activatedGen: Gen[DateTime]): Gen[Bundle] =
    for {
      id <- readableString
      offer <- AutoruOfferIdGen
      user <- AutoruUserIdGen
      product <- productGen[Products.GoodsBundle]
      amount <- Gen.posNum[Long]
      status <- enumGen(ProductStatuses)
      transactionId <- readableString
      context <- BundleContextGen
      activated <- activatedGen
      deadline <- status match {
        case ProductStatuses.Active =>
          dateTimeInFuture()
        case _ =>
          dateTimeInPast(
            (DateTimeUtil.now().getMillis - activated.getMillis).millis
          )
      }
      epoch <- BasicGenerators.posNum[Long]
      prolongable <- ProlongableGen
    } yield
      Bundle(
        id,
        offer,
        user,
        product,
        amount,
        status,
        transactionId,
        context,
        activated,
        deadline,
        epoch,
        prolongable
      )

  def bundleWithTypeAndStatusGen(
      productGen: Gen[AutoruBundles],
      statusGen: Gen[ProductStatus]
  ): Gen[Bundle] =
    for {
      id <- readableString
      offer <- AutoruOfferIdGen
      user <- AutoruUserIdGen
      product <- productGen
      amount <- Gen.posNum[Long]
      status <- statusGen
      transactionId <- readableString
      context <- BundleContextGen
      activated <- dateTimeInPast
      deadline <- dateTimeInFuture()
      epoch <- BasicGenerators.posNum[Long]
      prolongable <- ProlongableGen
    } yield
      Bundle(
        id,
        offer,
        user,
        product,
        amount,
        status,
        transactionId,
        context,
        activated,
        deadline,
        epoch,
        prolongable
      )

  lazy val VipBundleActiveGen: Gen[Bundle] =
    bundleWithTypeAndStatusGen(Vip, ProductStatuses.Active)

  lazy val NotVipBundleActiveGen: Gen[Bundle] =
    bundleWithTypeAndStatusGen(
      Gen.oneOf(AutoruBundles.Express, AutoruBundles.Turbo),
      enumGen(ProductStatuses)
    )

  lazy val NotActiveVipBundleGen: Gen[Bundle] =
    bundleWithTypeAndStatusGen(
      Vip,
      Gen.oneOf(ProductStatuses.Inactive, ProductStatuses.Canceled)
    )

  lazy val NotVipBundleNotActiveGen: Gen[Bundle] =
    bundleWithTypeAndStatusGen(
      Gen.oneOf(AutoruBundles.Express, AutoruBundles.Turbo),
      Gen.oneOf(ProductStatuses.Inactive, ProductStatuses.Canceled)
    )

  lazy val GoodsBundleGen: Gen[Bundle] =
    goodsBundleGen(productGen[Products.GoodsBundle], ProlongableGen)

  lazy val ProlongableGoodsBundleGen: Gen[Bundle] = goodsBundleGen(
    prolongableProductGen[Products.GoodsBundle],
    Prolongable(true)
  )

  val SubscriptionProductGen: Gen[Products.Subscription] =
    Gen.posNum[Long].map(OffersHistoryReports.apply)

  val SubscriptionOffersHistoryReportsGen: Gen[Products.Subscription] =
    Gen.posNum[Long].map(OffersHistoryReports)

  def subscriptionGen(
      userIdGen: Gen[UserId] = AutoruUserIdGen,
      prolongableGen: Gen[Prolongable] = ProlongableGen,
      productGen: Gen[Products.Subscription] = SubscriptionProductGen
  ): Gen[Subscription] =
    for {
      id <- readableString
      user <- userIdGen
      product <- productGen
      counter = product.counter
      amount <- FundsGen
      status <- productStatusGen
      transactionId <- readableString
      context <- SubscriptionContextGen
      activated <- dateTimeInPast
      deadline <- status match {
        case ProductStatuses.Active =>
          dateTimeInFuture()
        case _ =>
          dateTimeInPast(
            (DateTimeUtil.now().getMillis - activated.getMillis).millis
          )
      }
      epoch <- Gen.posNum[Long]
      prolongable <- prolongableGen
    } yield
      Subscription(
        id,
        user,
        product,
        counter,
        amount,
        status,
        transactionId,
        context,
        activated,
        deadline,
        epoch,
        prolongable
      )

  val ProlongableSubscriptionGen: Gen[Subscription] = subscriptionGen(
    prolongableGen = Prolongable(true)
  )

  val ProlongableSubscriptionListGen: Gen[List[Subscription]] = Nil

  lazy val PaidOfferProductGen: Gen[PaidOfferProduct] =
    Gen.oneOf(GoodsGen, GoodsBundleGen)

  lazy val vosProductSourceGen: Gen[VosProductSource] =
    for {
      id <- readableString
      offer <- OfferIdentityGen
      user <- AutoruUserIdGen
      product <- Gen.oneOf(
        productGen[Products.Goods],
        productGen[Products.GoodsBundle]
      )
      status <- enumGen(ProductStatuses)
      activated <- dateTimeInPast
      deadline <- status match {
        case ProductStatuses.Active =>
          dateTimeInFuture()
        case _ =>
          dateTimeInPast(
            minDistance = 0.seconds,
            maxDistance = (DateTimeUtil.now().getMillis - activated.getMillis).millis
          )
      }
      epoch <- Gen.posNum[Long]
    } yield
      VosProductSource(
        id,
        offer,
        user,
        product,
        status,
        activated,
        deadline,
        epoch
      )

  lazy val PaidOfferProductNotPlacementGen: Gen[PaidOfferProduct] =
    Gen.oneOf(goodsGen(productNotPlacementGen[Products.Goods]), GoodsBundleGen)

  /** Use domain-aware OfferIdentityGen */
  override val featureConstraintGen: Gen[FeatureConstraint] = for {
    offerIdentity <- OfferIdentityGen
  } yield FeatureConstraint(offerIdentity)

  override val featureTagGen: Gen[String] =
    ProductGen.map(_.name)

  def paymentRequestPayloadGen(
      domain: Option[DeprecatedDomain]
  ): Gen[PaymentPayload] =
    for {
      transactionId <- readableString
    } yield PaymentPayload(transactionId)

  def paymentRequestGen(
      payloadGen: Option[Gen[PaymentPayload]] = None,
      action: Option[PaymentAction] = None
  ): Gen[PaymentRequest] =
    for {
      bankerTransactionId <- readableString
      payload <- payloadGen.getOrElse(paymentRequestPayloadGen(Some(domain)))
      time <- dateTimeInPast
      action <- action.map(Gen.const).getOrElse(enumGen(PaymentActions))
    } yield PaymentRequest(bankerTransactionId, payload, time, action)

  lazy val GoodsBundleUnitGen: Gen[GoodsBundleUnit] = for {
    product <- productGen[AutoruGoods]
    count <- Gen.posNum[Long]
    duration <- productDurationGen
  } yield GoodsBundleUnit(product, count, duration)

  lazy val BundleUnitGen: Gen[BundleUnit] = GoodsBundleUnitGen

  lazy val BundleUnitListGen: Gen[List[BundleUnit]] =
    Gen.nonEmptyListOf(BundleUnitGen)

  def autoAppliedProductGen[A <: AutoruProduct: ClassTag]: Gen[A] =
    Gen.oneOf(filterAutoApplied(Products.allOfType[A], shouldBe = true).toSeq)

  def notAutoAppliedProductGen[A <: AutoruProduct: ClassTag]: Gen[A] =
    Gen.oneOf(filterAutoApplied(Products.allOfType[A], shouldBe = false).toSeq)

  private def filterAutoApplied[A <: AutoruProduct: ClassTag](
      products: Set[A],
      shouldBe: Boolean
  ) =
    products.filter {
      case _: AutoApplied => shouldBe
      case _ => !shouldBe
    }

  lazy val AutoAppliedProductGen: Gen[AutoruProduct] =
    autoAppliedProductGen[AutoruProduct]

  lazy val NotAutoAppliedProductGen: Gen[AutoruProduct] =
    notAutoAppliedProductGen[AutoruProduct]

  val paymentSystemGen: Gen[PaymentSystem] = enumGen(PaymentSystems)

  val moderationStatusStringGen: Gen[String] =
    Gen.oneOf(Reason.values().map(_.toString).toSeq)

  val userModerationStatusGen: Gen[UserModerationStatus] = {
    for {
      moderationStatus <- moderationStatusStringGen
      timestamp <- timestampGen
      m <- Gen.mapOf(moderationStatus, timestamp)
    } yield
      UserModerationStatus.newBuilder
        .putAllResellerFlagUpdatedByDomain(m.asJava)
        .build
  }

  def productPriceWithNonEmptyProlongPriceGen(
      duration: Gen[ProductDuration] = productDurationGen,
      basePrice: Gen[Funds] = Gen.posNum[Funds]
  ): Gen[ProductPrice] =
    productPriceGen(
      price = constPriceGen(prolongPrice = Gen.some(FundsGen), basePrice = basePrice),
      duration = duration
    )

  def prolongIntervalInfoGen(
      productPrice: Gen[ProductPrice] = productPriceWithNonEmptyProlongPriceGen()
  ): Gen[ProlongIntervalInfo] =
    for {
      productPrice <- productPrice
      willExpire <- Gen.oneOf(dateTimeInPast, dateTimeInFuture())
    } yield
      ProlongIntervalInfo(
        productPrice.price.prolongPrice.get,
        willExpire,
        productPrice
      )

  val paymentPayloadRawGen: Gen[PaymentPayloadRaw] =
    zip(alphaStr, alphaStr).map(PaymentPayloadRaw.tupled)

  val paymentRequestRawGen: Gen[PaymentRequestRaw] = {
    for {
      bankerTransactionId <- alphaStr
      payload <- paymentPayloadRawGen
      time <- dateTime()
      action <- enumGen(PaymentActions)
    } yield PaymentRequestRaw(bankerTransactionId, payload, time, action)
  }

  def userPeriodicalDiscountGen(): Gen[periodical_discount_exclusion.User.UserPeriodicalDiscount] = {
    import ru.auto.salesman.model.user.periodical_discount_exclusion.User._
    for {
      periodicalDiscount <- PeriodicalDiscountGen
      res <- Gen.oneOf(
        UserExcludedFromDiscount(periodicalDiscount),
        UserInPeriodicalDiscount(periodicalDiscount),
        NoActiveDiscount
      )
    } yield res
  }

  def productPeriodicalDiscountGen(): Gen[periodical_discount_exclusion.Product.ProductPeriodicalDiscount] = {
    import ru.auto.salesman.model.user.periodical_discount_exclusion.Product._
    for {
      periodicalDiscount <- PeriodicalDiscountGen
      res <- Gen.oneOf(
        UserExcludedFromDiscount(periodicalDiscount),
        InDiscount(periodicalDiscount),
        NoActiveDiscount
      )
    } yield res
  }

}
