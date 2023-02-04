package ru.yandex.realty.seller.model

import org.joda.time.DateTime
import ru.yandex.realty.model.offer.PaymentType
import ru.yandex.realty.model.user.{PassportUser, UserRef}
import ru.yandex.realty.seller.model.product._
import ru.yandex.vertis.util.time.DateTimeUtil

import scala.concurrent.duration.DurationInt

/**
  * @author Vsevolod Levin
  */
package object packages {
  val Owner: UserRef = PassportUser(7723424)
  val PurchaseId: String = "d635a1d86c3fe953db03ac50dfc4c552"
  val Target: PurchaseTarget = OfferTarget("2425375873514278")
  val StartTime: DateTime = DateTimeUtil.now()
  val EndTime: DateTime = StartTime.plusDays(7)
  val Id: String = PurchasedProductUtils.generateRandomProductId

  val TurboProduct = PurchasedProduct(
    Id,
    Some(PurchaseId),
    Owner,
    ProductTypes.PackageTurbo,
    Target,
    ManualSource,
    StartTime,
    Some(StartTime),
    Some(EndTime),
    PurchasedProductStatuses.Pending,
    PurchaseProductDeliveryStatuses.Pending,
    None,
    None,
    ProductContext(7.days, Some(PaymentType.NATURAL_PERSON)),
    Stop,
    Some(StartTime),
    PersonPaymentTypes.NaturalPerson,
    ShardKeyEvaluator.evaluateShardKey(Owner)
  )

  val RaisingProduct = PurchasedProduct(
    Id,
    Some(PurchaseId),
    Owner,
    ProductTypes.PackageRaising,
    Target,
    ManualSource,
    StartTime,
    Some(StartTime),
    Some(EndTime),
    PurchasedProductStatuses.Pending,
    PurchaseProductDeliveryStatuses.Pending,
    None,
    None,
    ProductContext(7.days, Some(PaymentType.NATURAL_PERSON)),
    Stop,
    Some(StartTime),
    PersonPaymentTypes.NaturalPerson,
    ShardKeyEvaluator.evaluateShardKey(Owner)
  )
}
