package ru.yandex.realty.rent.payments.gen

import java.time.Instant
import org.scalacheck.Gen
import ru.yandex.vertis.generators.BasicGenerators
import ru.yandex.realty.rent.payments.model.Event
import ru.yandex.realty.rent.payments.proto.model.transaction.PayoutTransactionStatusNamespace.PayoutTransactionStatus
import ru.yandex.realty.rent.payments.proto.model.event.{
  CreateOrderEvent,
  CreateOrderItemEvent,
  DeclineOrderEvent,
  DeclineOrderItemEvent,
  EventData,
  InitPaymentEvent,
  InitPayoutEvent,
  UpdateOrderEvent
}
import ru.yandex.realty.rent.payments.proto.model.transaction.{FullPayment, PaymentFormat}
import ru.yandex.realty.rent.payments.proto.model.order.{HouseServiceOrder, OrderType, RentOrder}
import ru.yandex.realty.rent.proto.api.common.platform_type.PlatformNamespace.Platform
import ru.yandex.realty.rent.proto.api.payment.payment_method_type.PaymentMethodNamespace.PaymentMethod
import ru.yandex.realty.util.TimeUtils

trait EventGen extends BasicGenerators {

  def eventGen(parent: Option[String] = None, dataOpt: Option[EventData] = None): Gen[Event] =
    for {
      idempotencyKey <- readableString
      entityId <- readableString
      orderId <- readableString
      data = dataOpt.getOrElse(eventDataGen().next)
      createTime <- posNum[Long].map(Instant.ofEpochMilli)
    } yield Event(
      parent = parent,
      idempotencyKey = idempotencyKey,
      entityId = entityId,
      orderId = orderId,
      data = data,
      createTime = createTime
    )

  def createRentOrderEventGen(parent: Option[String] = None): Gen[Event] =
    for {
      shortnessType <- Gen.oneOf(RentOrder.ShortnessType.values)
      orderType = OrderType.Order.RentOrder(RentOrder(shortnessType, 9))
      paymentFormat = PaymentFormat(PaymentFormat.Format.FullPayment(FullPayment()))
      data = EventData(
        EventData.Event.CreateOrderEvent(
          CreateOrderEvent(orderType = Some(OrderType(orderType)), paymentFormat = Some(paymentFormat))
        )
      )
    } yield eventGen(parent, Some(data)).next

  def updateRentOrderEventGen(parent: Option[String] = None): Gen[Event] =
    for {
      shortnessType <- Gen.oneOf(RentOrder.ShortnessType.values)
      orderType = OrderType.Order.RentOrder(RentOrder(shortnessType, 9))
      paymentFormat = PaymentFormat(PaymentFormat.Format.FullPayment(FullPayment()))
      data = EventData(
        EventData.Event.UpdateOrderEvent(
          UpdateOrderEvent(orderType = Some(OrderType(orderType)), paymentFormat = Some(paymentFormat))
        )
      )
    } yield eventGen(parent, Some(data)).next

  def createHouseServiceOrderEventGen(parent: Option[String] = None): Gen[Event] = {
    val orderType = OrderType.Order.HouseServiceOrder(HouseServiceOrder())
    val data = EventData(EventData.Event.CreateOrderEvent(CreateOrderEvent(Some(OrderType(orderType)))))
    eventGen(parent, Some(data))
  }

  def declineOrderEventGen(parent: String): Gen[Event] = {
    val data = EventData(EventData.Event.DeclineOrderEvent(DeclineOrderEvent()))
    eventGen(Some(parent), Some(data))
  }

  def createOrderItemEventGen(parent: String, orderItemId: String): Gen[Event] = {
    val data = EventData(EventData.Event.CreateOrderItemEvent(CreateOrderItemEvent(orderItemId = orderItemId)))
    eventGen(Some(parent), Some(data))
  }

  def declineOrderItemEventGen(parent: String, orderItemId: String): Gen[Event] = {
    val data = EventData(EventData.Event.DeclineOrderItemEvent(DeclineOrderItemEvent(orderItemId)))
    eventGen(Some(parent), Some(data))
  }

  def initPaymentEventGen(): Gen[Event] =
    for {
      transactionId <- readableString
      amount <- posNum[Long]
      paymentUrl <- readableString
      platform <- Gen.oneOf(Platform.values)
      paymentMethod <- Gen.oneOf(PaymentMethod.values)
      initiationTime <- posNum[Long].map(Instant.ofEpochMilli)
      userId <- readableString
    } yield {
      val initPaymentEvent = InitPaymentEvent(
        transactionId = transactionId,
        amount = amount,
        paymentUrl = paymentUrl,
        platform = platform,
        paymentMethod = paymentMethod,
        initiationTime = Some(TimeUtils.instantToScalaPbTimestamp(initiationTime)),
        userId = userId
      )
      eventGen(dataOpt = Some(EventData(EventData.Event.InitPaymentEvent(initPaymentEvent)))).next
    }

  def initPayoutEventGen(): Gen[Event] =
    for {
      transactionId <- readableString
      cardId <- readableString
      panMask <- readableString
      status <- Gen.oneOf(PayoutTransactionStatus.values)
      errorCode <- readableString
      errorMessage <- readableString
      initiationTime <- posNum[Long].map(Instant.ofEpochMilli)
      userId <- readableString
    } yield {
      val initPayoutEvent = InitPayoutEvent(
        transactionId = transactionId,
        cardId = cardId,
        panMask = panMask,
        status = status,
        errorCode = errorCode,
        errorMessage = errorMessage,
        initiationTime = Some(TimeUtils.instantToScalaPbTimestamp(initiationTime)),
        userId = userId
      )
      eventGen(dataOpt = Some(EventData(EventData.Event.InitPayoutEvent(initPayoutEvent)))).next
    }

  private def eventDataGen(): Gen[EventData] = {
    for {
      orderItemId <- readableString
      shortnessType <- Gen.oneOf(RentOrder.ShortnessType.values)
      orderType <- Gen.oneOf(
        Seq(
          OrderType.Order.RentOrder(RentOrder(shortnessType, 9)),
          OrderType.Order.HouseServiceOrder(HouseServiceOrder())
        )
      )
      data <- Gen.oneOf(
        Seq(
          EventData(EventData.Event.CreateOrderEvent(CreateOrderEvent(Some(OrderType(orderType))))),
          EventData(EventData.Event.DeclineOrderEvent(DeclineOrderEvent())),
          EventData(EventData.Event.CreateOrderItemEvent(CreateOrderItemEvent(orderItemId = orderItemId))),
          EventData(EventData.Event.DeclineOrderItemEvent(DeclineOrderItemEvent(orderItemId))),
          EventData(EventData.Event.UpdateOrderEvent(UpdateOrderEvent()))
        )
      )
    } yield data
  }
}
