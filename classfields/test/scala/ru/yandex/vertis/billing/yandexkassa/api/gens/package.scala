package ru.yandex.vertis.billing.yandexkassa.api

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.yandex.vertis.billing.yandexkassa.api.model.notifications.Notification
import ru.yandex.vertis.billing.yandexkassa.api.model._

import scala.jdk.CollectionConverters._

/**
  * Gens for api testing
  *
  * @author alex-kovalenko
  */
package object gens {

  def alphaStrN(n: Int): Gen[String] =
    Gen.nonEmptyListOf(Gen.alphaChar).map(_.mkString).suchThat(_.forall(_.isLetter)).map(_.take(n))

  def getOr[A](opt: Option[A], gen: Gen[A]): Gen[A] =
    opt.map(Gen.const).getOrElse(gen)

  val OrderStatusGen: Gen[OrderStatus] = for {
    status <- Gen.oneOf(OrderStatuses.values.toSeq)
  } yield status

  val RecipientGen: Gen[Recipient] = for {
    shopId <- Gen.posNum[Long]
    shopArticleId <- Gen.posNum[Long]
  } yield Recipient(shopId, shopArticleId)

  val AmountValueGen: Gen[AmountValue] = for {
    amount <- Gen.posNum[Long]
    currency <- Gen.oneOf(Currencies.values.toSeq)
  } yield AmountValue(amount, currency)

  val PairGen: Gen[(String, String)] = for {
    k <- alphaStrN(5)
    v <- alphaStrN(10)
  } yield k -> v

  val OrderParamsGen: Gen[Map[String, String]] = for {
    n <- Gen.chooseNum(0, 3)
    pGens = Iterator.continually(PairGen).take(n).iterator.to(Iterable)
    params <- Gen.sequence(pGens)
  } yield params.asScala.toMap

  val OrderInfoGen: Gen[OrderInfo] = for {
    clientOrderId <- alphaStrN(10)
    customerId <- alphaStrN(10)
    amount <- AmountValueGen
    params <- OrderParamsGen
  } yield OrderInfo(clientOrderId, customerId, amount, params)

  val CardAuthorizeResultGen: Gen[CardAuthorizeResult] = for {
    responseCode <- alphaStrN(2)
    rrn <- Gen.option(alphaStrN(2))
    authId <- Gen.option(alphaStrN(2))
    eci <- Gen.option(alphaStrN(2))
    mpiResult <- Gen.option(alphaStrN(2))
  } yield CardAuthorizeResult(responseCode, rrn, authId, eci, mpiResult)

  val InstrumentGen: Gen[Instrument] = for {
    source <- Gen.oneOf(PaymentSources.values.toSeq)
    method <- alphaStrN(5)
    title <- alphaStrN(5)
    reference <- alphaStrN(5)
  } yield Instrument(source, method, title, reference)

  def notificationGen(p: NotificationGenParams): Gen[Notification] =
    for {
      t <- Gen.oneOf(NotificationTypes.values.toSeq)
      orderId <- alphaStrN(5)
      status <- OrderStatusGen
      createdDt = DateTime.now()
      recipient <- getOr(p.recipient, RecipientGen)
      order <- OrderInfoGen
      source <- Gen.option(Gen.oneOf(PaymentSources.values.toSeq))
      method <- Gen.option(alphaStrN(5))
      authorizedDt <- Gen.option(Gen.const(DateTime.now()))
      cardAuthorizedDt <- Gen.option(Gen.const(DateTime.now()))
      cardAuthorizeResult <- Gen.option(CardAuthorizeResultGen)
      charge <- Gen.option(AmountValueGen)
      income <- Gen.option(AmountValueGen)
      instrument <- Gen.option(InstrumentGen)
    } yield Notification(
      t,
      orderId,
      status,
      createdDt,
      recipient,
      order,
      source,
      method,
      authorizedDt,
      cardAuthorizedDt,
      cardAuthorizeResult,
      charge,
      income,
      instrument
    )
}
