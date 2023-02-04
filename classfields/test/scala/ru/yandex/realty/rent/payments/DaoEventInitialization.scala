package ru.yandex.realty.rent.payments

import com.google.protobuf.timestamp.Timestamp
import org.scalacheck.Gen
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatest.BeforeAndAfterEach
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, TestContainerDatasource}
import ru.yandex.realty.doobie.DoobieTestDatabase
import ru.yandex.realty.rent.payments.dao.EventDao
import ru.yandex.realty.rent.payments.gen.EventGen
import ru.yandex.realty.rent.payments.manager.{EventManager, OrderManager}
import ru.yandex.realty.rent.payments.model.OrderAggregator
import ru.yandex.realty.rent.payments.proto.model.event.{
  CreateOrderEvent,
  CreateOrderItemEvent,
  DeclineOrderItemEvent,
  UpdateOrderEvent
}
import ru.yandex.realty.rent.payments.proto.model.order.{HouseServiceOrder, Order, OrderType, RentOrder}
import ru.yandex.realty.rent.payments.proto.model.order.RentOrder.ShortnessType
import ru.yandex.realty.rent.payments.proto.model.order_item.{
  Commission,
  Insurance,
  MoneyTransfer,
  OrderItemType,
  Penalty
}
import ru.yandex.realty.rent.payments.proto.model.transaction.{FullPayment, PaymentFormat}
import ru.yandex.realty.tracing.Traced

import java.time.Instant

trait DaoEventInitialization
  extends AsyncSpecBase
  with EventGen
  with BeforeAndAfterEach
  with MySQLTestContainer.V8_0
  with TestContainerDatasource
  with DoobieTestDatabase {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  override def beforeAll(): Unit = {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/schema.sql")).futureValue
  }

  override def afterAll(): Unit = {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/drop_tables.sql")).futureValue
  }

  override def beforeEach(): Unit = {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/truncate_events.sql")).futureValue
  }

  lazy val eventDao = new EventDao
  lazy val orderManager: OrderManager = new OrderManager(doobieDatabase, eventDao)
  implicit val trace: Traced = Traced.empty

  val orderId: String = readableString.next
  val entityId: String = readableString.next

  val now = Instant.now()

  val periodStartDate = Some(Timestamp(now.getEpochSecond))
  val periodEndDate = Some(Timestamp(now.getEpochSecond))

  val moneyTransferOrderItemId: String = readableString.next
  val moneyTransferAmount: Long = posNum[Long].next

  val commissionOrderItemId: String = readableString.next
  val commissionAmount: Long = posNum[Long].next

  val insuranceOrderItemId: String = readableString.next
  val insuranceAmount: Long = posNum[Long].next

  val penaltyOrderItemId: String = readableString.next
  val penaltyAmount: Long = posNum[Long].next

  protected def getEvents() =
    OrderAggregator.buildEventChain(doobieDatabase.masterTransaction {
      eventDao.findByOrderId(orderId, forUpdate = true)(_)
    }.futureValue)

  protected def createOrder(
    createOrderEvent: CreateOrderEvent,
    entityId: String = entityId,
    itemEvents: Seq[CreateOrderItemEvent] = Seq.empty
  ): Unit = orderManager.createOrderWithItems(orderId, entityId, createOrderEvent, itemEvents).futureValue

  protected def declineOrder(): Order = orderManager.declineOrder(orderId).futureValue

  protected def createRentOrderEvent(): CreateOrderEvent = {
    val shortnessType = Gen.oneOf(RentOrder.ShortnessType.values).next
    val paymentFormat = PaymentFormat(PaymentFormat.Format.FullPayment(FullPayment()))
    val orderType = OrderType(OrderType.Order.RentOrder(RentOrder(shortnessType, 13)))
    CreateOrderEvent(Some(orderType), Some(paymentFormat))
  }

  protected def updateRentOrderEvent(shortnessType: ShortnessType, paymentDayOfMonth: Int): UpdateOrderEvent = {
    val rentOrder = OrderType.Order.RentOrder(RentOrder(shortnessType, paymentDayOfMonth))
    val paymentFormat = EventManager.defaultPaymentFormat
    UpdateOrderEvent(Some(OrderType(rentOrder)), Some(paymentFormat))
  }

  protected def updateHouseServiceOrderEvent(): UpdateOrderEvent = {
    val rentOrder = OrderType.Order.HouseServiceOrder(HouseServiceOrder())
    val paymentFormat = EventManager.defaultPaymentFormat
    UpdateOrderEvent(Some(OrderType(rentOrder)), Some(paymentFormat))
  }

  protected def createHouseServiceOrderEvent(): CreateOrderEvent =
    CreateOrderEvent(Some(OrderType(OrderType.Order.HouseServiceOrder(HouseServiceOrder()))))

  protected def declineOrderItemEvent(orderItemId: String): DeclineOrderItemEvent =
    DeclineOrderItemEvent(orderItemId)

  protected def moneyTransferEvent(
    shouldGenerateNewId: Boolean = false,
    startDate: Option[Timestamp] = periodStartDate
  ): CreateOrderItemEvent = {
    CreateOrderItemEvent(
      if (shouldGenerateNewId) readableString.next else moneyTransferOrderItemId,
      moneyTransferAmount,
      startDate,
      startDate,
      Some(OrderItemType(OrderItemType.Item.MoneyTransfer(MoneyTransfer())))
    )
  }

  protected def commissionEvent(shouldGenerateNewId: Boolean = false): CreateOrderItemEvent =
    CreateOrderItemEvent(
      if (shouldGenerateNewId) readableString.next else commissionOrderItemId,
      commissionAmount,
      periodStartDate,
      periodEndDate,
      Some(OrderItemType(OrderItemType.Item.Commission(Commission())))
    )

  protected def insuranceEvent(shouldGenerateNewId: Boolean = false): CreateOrderItemEvent =
    CreateOrderItemEvent(
      if (shouldGenerateNewId) readableString.next else insuranceOrderItemId,
      insuranceAmount,
      periodStartDate,
      periodEndDate,
      Some(OrderItemType(OrderItemType.Item.Insurance(Insurance())))
    )

  protected def penaltyEvent(shouldGenerateNewId: Boolean = false): CreateOrderItemEvent =
    CreateOrderItemEvent(
      if (shouldGenerateNewId) readableString.next else penaltyOrderItemId,
      penaltyAmount,
      periodStartDate,
      periodEndDate,
      Some(OrderItemType(OrderItemType.Item.Penalty(Penalty())))
    )

}
