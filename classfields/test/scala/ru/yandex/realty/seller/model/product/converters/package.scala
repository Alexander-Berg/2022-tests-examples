package ru.yandex.realty.seller.model.product

import org.joda.time.DateTime
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.model.user.{PassportUser, UserRef}
import ru.yandex.realty.seller.model.{PersonPaymentTypes, ShardKeyEvaluator}
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.realty.seller.model.purchase.{Purchase, PurchaseDeliveryStatuses, PurchaseStatuses}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators

import scala.concurrent.duration.DurationInt

package object converters {
  object Generators extends SellerModelGenerators
  val Owner: UserRef = PassportUser(7723424)
  val PurchaseId: String = "d635a1d86c3fe953db03ac50dfc4c552"
  val Target: PurchaseTarget = OfferTarget("2425375873514278")
  val StartTime: DateTime = DateTimeUtil.now()
  val EndTime: DateTime = StartTime.plusDays(7)
  val Id: String = PurchasedProductUtils.generateRandomProductId
  val priceModifiersId = "asdasdafdsfdsf123"

  val RandomProductWithNoPurchase = (Generators.purchasedProductGen.sample.get, None)
  val RandomProductWithPurchase = (Generators.purchasedProductGen.sample.get, Generators.purchaseGen.sample)
  lazy val ActiveProductWithNoPurchase = (ActiveProduct, None)
  lazy val RefundedProductWithNoPurchase = (RefundedProduct, None)
  lazy val RefundedProductWithPurchase = (RefundedProduct, Some(RefundedPurchase))
  lazy val ActiveProductWithPurchase = (ActiveProduct, Some(ActivePurchase))

  val Modifiers = List(
    MoneyPromocodePriceModifier(priceModifiersId, 300),
    VasPromocodePriceModifier(priceModifiersId, ProductTypes.Premium, 1),
    DiscountPromocodePriceModifier(priceModifiersId, Some(ProductTypes.Premium), 80, 1),
    DiscountPromocodePriceModifier(priceModifiersId, None, 80, 1),
    DiscountBonusModifier(Some(ProductTypes.Premium), 70, "", 1)
  )

  val RefundedPurchase = Purchase(
    Id,
    PurchaseStatuses.Refunded,
    PurchaseDeliveryStatuses.Unknown,
    Owner,
    None,
    StartTime,
    None,
    None,
    None,
    Some("ANDROID"),
    None,
    ShardKeyEvaluator.evaluateShardKey(Owner)
  )

  val ActivePurchase = Purchase(
    Id,
    PurchaseStatuses.Pending,
    PurchaseDeliveryStatuses.Unknown,
    Owner,
    None,
    StartTime,
    None,
    None,
    None,
    Some("DESKTOP"),
    None,
    ShardKeyEvaluator.evaluateShardKey(Owner)
  )

  val RefundedProduct = PurchasedProduct(
    Id,
    Some(PurchaseId),
    Owner,
    ProductTypes.PackageRaising,
    Target,
    ManualSource,
    StartTime,
    Some(StartTime),
    Some(EndTime),
    PurchasedProductStatuses.Cancelled,
    PurchaseProductDeliveryStatuses.Pending,
    Some(PriceContext(200L, 200L, Modifiers, List())),
    None,
    ProductContext(7.days, Some(PaymentType.NATURAL_PERSON)),
    Stop,
    Some(StartTime),
    PersonPaymentTypes.Unknown,
    ShardKeyEvaluator.evaluateShardKey(Owner)
  )

  val ActiveProduct = PurchasedProduct(
    Id,
    Some(PurchaseId),
    Owner,
    ProductTypes.PackageRaising,
    Target,
    ManualSource,
    StartTime,
    Some(StartTime),
    Some(EndTime),
    PurchasedProductStatuses.Active,
    PurchaseProductDeliveryStatuses.Pending,
    Some(PriceContext(200L, 200L, Modifiers, List())),
    None,
    ProductContext(7.days, Some(PaymentType.NATURAL_PERSON)),
    Stop,
    Some(StartTime),
    PersonPaymentTypes.NaturalPerson,
    ShardKeyEvaluator.evaluateShardKey(Owner)
  )

}
