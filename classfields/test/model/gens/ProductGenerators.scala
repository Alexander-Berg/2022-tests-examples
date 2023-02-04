package ru.auto.salesman.test.model.gens

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.model.{
  ActiveProductNaturalKey,
  Product,
  ProductTariff,
  UniqueProductType
}
import ru.auto.salesman.model.Product.ProductPaymentStatus
import ru.auto.salesman.model.Product.ProductPaymentStatus.{Active, Inactive, NeedPayment}
import ru.auto.salesman.model.UniqueProductType.ApplicationCreditSingle
import ru.auto.salesman.test.model.gens.ProductGenerators._
import ru.yandex.vertis.generators.{BasicGenerators, DateTimeGenerators}

trait ProductGenerators extends BasicGenerators with DateTimeGenerators {

  def productKeyGen(
      payerGen: Gen[String] = dealerGen(),
      targetGen: Gen[String] = defaultTargetGen,
      uniqueProductTypeGen: Gen[UniqueProductType] = uniqueProductTypeGen
  ): Gen[ActiveProductNaturalKey] =
    for {
      payer <- payerGen
      target <- targetGen
      uniqueProductType <- uniqueProductTypeGen
    } yield
      ActiveProductNaturalKey(
        payer,
        target,
        uniqueProductType
      )

  def basicProductGen(
      idGen: Gen[Long] = Gen.posNum[Long],
      keyGen: Gen[ActiveProductNaturalKey] = productKeyGen(),
      statusGen: Gen[ProductPaymentStatus] = defaultProductStatusGen,
      createDateGen: Gen[DateTime] = dateTimeInPast,
      inactiveReasonGen: Gen[String] = defaultInactiveReasonGen,
      prolongableGen: Gen[Boolean] = bool,
      pushedGen: Gen[Boolean] = Gen.const(false),
      productTariffGen: Gen[ProductTariff] = defaultProductTariffGen
  ): Gen[Product] =
    for {
      id <- idGen
      key <- keyGen
      status <- statusGen
      createDate <- createDateGen
      expireDate <- status match {
        case Active => Gen.some(dateTimeInFuture())
        case _ => Gen.const(None)
      }
      inactiveReason <- status match {
        case Inactive => Gen.some(inactiveReasonGen)
        case _ => Gen.const(None)
      }
      prolongable <- prolongableGen
      pushed <- pushedGen
      productTariff <- key.uniqueProductType match {
        case ApplicationCreditSingle => Gen.some(productTariffGen)
        case _ => Gen.const(None)
      }
    } yield
      Product(
        id,
        key,
        status,
        createDate,
        expireDate,
        None,
        inactiveReason,
        prolongable,
        pushed,
        productTariff
      )

  val needPaymentProductGen: Gen[Product] =
    basicProductGen(
      statusGen = Gen.const(NeedPayment)
    )

  val activeProductGen: Gen[Product] =
    basicProductGen(
      statusGen = Gen.const(Active)
    )

  val applicationCreditSingleGen: Gen[Product] =
    basicProductGen(
      statusGen = Gen.const(NeedPayment),
      keyGen = productKeyGen(
        uniqueProductTypeGen = Gen.const(UniqueProductType.ApplicationCreditSingle)
      )
    )

  val applicationCreditAccessGen: Gen[Product] =
    basicProductGen(
      statusGen = Gen.const(NeedPayment),
      keyGen = productKeyGen(
        uniqueProductTypeGen = Gen.const(UniqueProductType.ApplicationCreditAccess)
      )
    )

  val fullHistoryReportGen: Gen[Product] =
    basicProductGen(
      statusGen = Gen.const(NeedPayment),
      keyGen = productKeyGen(
        uniqueProductTypeGen = Gen.const(UniqueProductType.FullHistoryReport)
      )
    )

  def expiredProductGen(
      productGen: Gen[Product] = basicProductGen(),
      expireDate: DateTime = DateTime.now().minusDays(1)
  ): Gen[Product] =
    productGen
      .map(
        _.copy(
          createDate = expireDate.minusDays(1),
          expireDate = Some(expireDate)
        )
      )

  def dealerGen(idGen: Gen[Long] = Gen.posNum[Long]): Gen[String] =
    idGen.map(id => s"dealer:$id")

  def productForDealer(
      idGen: Gen[Long] = Gen.posNum[Long],
      productGen: Gen[Product] = basicProductGen()
  ): Gen[Product] =
    for {
      dealerKey <- productKeyGen(payerGen = dealerGen(idGen))
      product <- productGen
    } yield product.copy(key = product.key.copy(payer = dealerKey.payer))

}

object ProductGenerators {
  val categories = List("cars")
  val sections = List("new", "used")
  val inactiveReasons = List("expired")

  val defaultTargetGen: Gen[String] =
    for {
      category <- Gen.oneOf(categories)
      section <- Gen.oneOf(sections)
    } yield s"$category:$section"

  val uniqueProductTypeGen: Gen[UniqueProductType] =
    Gen.oneOf(UniqueProductType.values)

  val defaultProductStatusGen: Gen[ProductPaymentStatus] =
    Gen.oneOf(ProductPaymentStatus.values)

  val defaultInactiveReasonGen: Gen[String] =
    Gen.oneOf(inactiveReasons)

  val defaultProductTariffGen: Gen[ProductTariff] =
    Gen.oneOf(ProductTariff.values)
}
