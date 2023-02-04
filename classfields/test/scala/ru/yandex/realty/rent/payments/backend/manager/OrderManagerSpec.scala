package ru.yandex.realty.rent.payments.backend.manager

import com.google.protobuf.timestamp.Timestamp
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.errors.{ConflictApiException, InvalidParamsApiException, NotFoundApiException}
import ru.yandex.realty.rent.payments.proto.model.event.{
  CreateOrderEvent,
  CreateOrderItemEvent,
  DeclineOrderEvent,
  EventData,
  UpdateOrderEvent
}
import ru.yandex.realty.rent.payments.DaoEventInitialization
import ru.yandex.realty.rent.payments.manager.OrderManager
import ru.yandex.realty.rent.payments.model.{Event, OrderAggregator}
import ru.yandex.realty.rent.payments.proto.model.order.{HouseServiceOrder, Order, OrderType, RentOrder}
import ru.yandex.realty.rent.payments.proto.model.order_item.{
  Commission,
  Insurance,
  MoneyTransfer,
  OrderItemType,
  Penalty
}
import ru.yandex.realty.rent.payments.proto.model.event.DeclineOrderItemEvent
import ru.yandex.realty.rent.payments.proto.model.order.RentOrder.ShortnessType

import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}

@RunWith(classOf[JUnitRunner])
class OrderManagerSpec extends DaoEventInitialization with ScalaFutures with Matchers {

  "OrderManager restore order" should {
    "without new event because it has been created before as rent order" in new InitOrder {
      val event = createRentOrderEvent()
      createOrder(event)
      val order = orderManager.restoreOrder(orderId).futureValue
      val events = getEvents()
      events.size shouldBe 1
      checkOrder(events.head, EventData.Event.CreateOrderEvent(event))
      checkRentOrder(order)
    }

    "without new event because it has been created before as house service order" in new InitOrder {
      val event = createHouseServiceOrderEvent()
      createOrder(event)
      val order = orderManager.restoreOrder(orderId).futureValue
      val events = getEvents()
      events.size shouldBe 1
      checkOrder(events.head, EventData.Event.CreateOrderEvent(event))
      checkHouseServiceOrder(order)
    }

    "with new event as rent order because it has been declined before" in new InitOrder {
      val event = createRentOrderEvent()
      createOrder(event)
      declineOrder()
      val order = orderManager.restoreOrder(orderId).futureValue
      val events = getEvents()
      events.size shouldBe 3
      checkOrder(events.head, EventData.Event.CreateOrderEvent(event), 1)
      checkRentOrder(order)
    }

    "with new event as house service order because it has been declined before" in new InitOrder {
      val event = createHouseServiceOrderEvent()
      createOrder(event)
      declineOrder()
      val order = orderManager.restoreOrder(orderId).futureValue
      val events = getEvents()
      events.size shouldBe 3
      checkOrder(events.head, EventData.Event.CreateOrderEvent(event), 1)
      checkHouseServiceOrder(order)
    }

    "fail because no such order" in new InitOrder {
      interceptCause[NotFoundApiException] {
        orderManager.restoreOrder(orderId).futureValue
      }
      val events = getEvents()
      events.isEmpty shouldBe true
    }
  }

  "OrderManager create order with items" should {
    "with new event as rent order because it hasn't been created before" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      val order = orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      val Seq(commission, penalty, insurance, moneyTransfer, createOrder) = getEvents()
      checkOrder(createOrder, EventData.Event.CreateOrderEvent(createOrderEvent))
      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 4
    }

    "without new event because it has been created before as rent order with the same items" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      val order = orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event2, event4, event3, event1))
        .futureValue
      val Seq(commission, penalty, insurance, moneyTransfer, createOrder) = getEvents()
      checkOrder(createOrder, EventData.Event.CreateOrderEvent(createOrderEvent))
      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 4
    }

    "fail because it has been created before as house service order" in {
      createOrder(createHouseServiceOrderEvent())
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      interceptCause[ConflictApiException] {
        orderManager
          .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
          .futureValue
      }
    }

    "with new event as house service order because it hasn't been created before" in new InitOrder {
      val createOrderEvent = createHouseServiceOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      val order = orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      val Seq(commission, penalty, insurance, moneyTransfer, createOrder) = getEvents()
      checkOrder(createOrder, EventData.Event.CreateOrderEvent(createOrderEvent))
      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
      checkHouseServiceOrder(order)
      order.orderItems.size shouldBe 4
    }

    "without new event because it has been created before as house service order with the same items" in new InitOrder {
      val createOrderEvent = createHouseServiceOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      val order = orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event2, event1, event4, event3))
        .futureValue
      val Seq(commission, penalty, insurance, moneyTransfer, createOrder) = getEvents()
      checkOrder(createOrder, EventData.Event.CreateOrderEvent(createOrderEvent))
      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
      checkHouseServiceOrder(order)
      order.orderItems.size shouldBe 4
    }

    "fail because it has been declined before" in new InitOrder {
      createOrder(createRentOrderEvent())
      declineOrder()
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      interceptCause[ConflictApiException] {
        orderManager
          .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
          .futureValue
      }
      val events = getEvents()
      events.size shouldBe 2
    }

    "fail because it has been created before with different entity id" in new InitOrder {
      createOrder(createRentOrderEvent(), readableString.next)
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      interceptCause[ConflictApiException] {
        orderManager
          .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
          .futureValue
      }
      val events = getEvents()
      events.size shouldBe 1
    }

    "fail because it has been duplicates in order item ids" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent().copy(orderItemId = event1.orderItemId)
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      interceptCause[InvalidParamsApiException] {
        orderManager
          .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
          .futureValue
      }
      val events = getEvents()
      events.isEmpty shouldBe true
    }

    "fail because it has been created before with smaller number of the same items" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      val order = orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event4))
        .futureValue
      interceptCause[ConflictApiException] {
        orderManager
          .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
          .futureValue
      }
      val events = getEvents()
      checkRentOrder(order)
      order.orderItems.size shouldBe 3
    }

    "fail because it has been created before with bigger number of the same items" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      val order = orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      interceptCause[ConflictApiException] {
        orderManager
          .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3))
          .futureValue
      }
      checkRentOrder(order)
      order.orderItems.size shouldBe 4
    }

    "fail because it has been created before with different items" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      val order = orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      val event5 = penaltyEvent(true)
      val event6 = commissionEvent(true)
      interceptCause[ConflictApiException] {
        orderManager
          .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event6, event3, event5))
          .futureValue
      }
      checkRentOrder(order)
      order.orderItems.size shouldBe 4
    }

    "without new event because it has been created before with the same items and some declined" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      val event5 = declineOrderItemEvent(commissionOrderItemId)
      val event6 = declineOrderItemEvent(insuranceOrderItemId)
      orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      orderManager.updateOrder(orderId, None, Seq(event5, event6), Seq.empty).futureValue
      val order = orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event3))
        .futureValue

      val Seq(declineInsurance, declineCommission, commission, penalty, insurance, moneyTransfer, createOrder) =
        getEvents()
      checkOrder(createOrder, EventData.Event.CreateOrderEvent(createOrderEvent))
      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
      checkDeclineItem(declineCommission, event5, commissionOrderItemId)
      checkDeclineItem(declineInsurance, event6, insuranceOrderItemId)
      checkRentOrder(order)
      order.orderItems.size shouldBe 2
    }

    "fail because it has been created before with different items and some declined" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      val event5 = declineOrderItemEvent(event2.orderItemId)
      orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      val order = orderManager.updateOrder(orderId, None, Seq(event5), Seq.empty).futureValue
      val event6 = penaltyEvent(true)
      interceptCause[ConflictApiException] {
        orderManager
          .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event3, event4, event6))
          .futureValue
      }
      checkRentOrder(order)
    }
  }

  "OrderManager decline order" should {
    "with new event because this rent order hasn't been declined before" in new InitOrder {
      createOrder(createRentOrderEvent())
      val order = declineOrder()
      val events = getEvents()
      events.size shouldBe 2
      checkDeclineOrder(events.head)
      checkRentOrder(order, isDeclined = true)
    }

    "with new event because this house service order hasn't been declined before" in new InitOrder {
      createOrder(createHouseServiceOrderEvent())
      val order = declineOrder()
      val events = getEvents()
      events.size shouldBe 2
      checkDeclineOrder(events.head)
      checkHouseServiceOrder(order, isDeclined = true)
    }

    "without new event because this rent order has been declined before" in new InitOrder {
      createOrder(createRentOrderEvent())
      declineOrder()
      val order = declineOrder()
      val events = getEvents()
      events.size shouldBe 2
      checkDeclineOrder(events.head)
      checkRentOrder(order, isDeclined = true)
    }

    "without new event because this house service order hasn't been declined before" in new InitOrder {
      createOrder(createHouseServiceOrderEvent())
      declineOrder()
      val order = declineOrder()
      val events = getEvents()
      events.size shouldBe 2
      checkDeclineOrder(events.head)
      checkHouseServiceOrder(order, isDeclined = true)
    }

    "with new event because it has been created before again" in new InitOrder {
      createOrder(createRentOrderEvent())
      declineOrder()
      orderManager.restoreOrder(orderId).futureValue
      val order = declineOrder()
      val events = getEvents()
      events.size shouldBe 4
      checkDeclineOrder(events.head, 1)
      checkRentOrder(order, isDeclined = true)
    }

    "with new event as house service order because it has been created before again" in new InitOrder {
      createOrder(createHouseServiceOrderEvent())
      declineOrder()
      orderManager.restoreOrder(orderId).futureValue
      val order = declineOrder()
      val events = getEvents()
      events.size shouldBe 4
      checkDeclineOrder(events.head, 1)
      checkHouseServiceOrder(order, isDeclined = true)
    }

  }

  "OrderManager update order" should {

    "with update rent order" in new InitOrder {
      createOrder(createRentOrderEvent())
      val shortnessType = ShortnessType.TERMINATION
      val paymentDayOfMonth = 14
      val updateOrderEvent = updateRentOrderEvent(shortnessType, paymentDayOfMonth).copy()
      val order = orderManager.updateOrder(orderId, Some(updateOrderEvent), Seq.empty, Seq.empty).futureValue
      val events = getEvents()
      events.size shouldBe 2
      checkUpdateOrder(events.head, updateOrderEvent)
      checkRentOrder(order)
    }

    "with update house service order" in new InitOrder {
      createOrder(createHouseServiceOrderEvent())
      val updateOrderEvent = updateHouseServiceOrderEvent()
      val order = orderManager.updateOrder(orderId, Some(updateOrderEvent), Seq.empty, Seq.empty).futureValue
      val events = getEvents()
      events.size shouldBe 2
      checkUpdateOrder(events.head, updateOrderEvent)
      checkHouseServiceOrder(order)
    }

    "with new event as money transfer" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = moneyTransferEvent()
      val order = orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val events = getEvents()
      events.size shouldBe 2
      checkCreateItem(events.head, item, moneyTransferOrderItemId, moneyTransferAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 1
    }

    "with new event as commission" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = commissionEvent()
      val order = orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val events = getEvents()
      events.size shouldBe 2
      checkCreateItem(events.head, item, commissionOrderItemId, commissionAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 1
    }

    "with new event as insurance" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = insuranceEvent()
      val order = orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val events = getEvents()
      events.size shouldBe 2
      checkCreateItem(events.head, item, insuranceOrderItemId, insuranceAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 1
    }

    "with new event as penalty" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = penaltyEvent()
      val order = orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val events = getEvents()
      events.size shouldBe 2
      checkCreateItem(events.head, item, penaltyOrderItemId, penaltyAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 1
    }

    "without new event because item has been created" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = penaltyEvent()
      orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val order = orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val events = getEvents()
      events.size shouldBe 2
      checkCreateItem(events.head, item, penaltyOrderItemId, penaltyAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 1
    }

    "fail because order not found" in new InitOrder {
      val item = moneyTransferEvent()
      interceptCause[NotFoundApiException] {
        orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      }
    }

    "fail because order was declined" in new InitOrder {
      createOrder(createRentOrderEvent())
      declineOrder()
      val item = moneyTransferEvent()
      interceptCause[ConflictApiException] {
        orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      }
    }

    "fail because of negative amount" in new InitOrder {
      createOrder(createRentOrderEvent())
      val negativeAmount = -posNum[Long].next
      val item = moneyTransferEvent().copy(amount = negativeAmount)
      interceptCause[InvalidParamsApiException] {
        orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      }
    }

    "fail because update inconsistent rent order type" in new InitOrder {
      createOrder(createHouseServiceOrderEvent())
      val shortnessType = ShortnessType.TERMINATION
      val paymentDayOfMonth = 14
      val updateOrderEvent = updateRentOrderEvent(shortnessType, paymentDayOfMonth).copy()
      interceptCause[ConflictApiException] {
        orderManager.updateOrder(orderId, Some(updateOrderEvent), Seq.empty, Seq.empty).futureValue
      }
    }

    "fail because update inconsistent house service order type" in new InitOrder {
      createOrder(createRentOrderEvent())
      val updateOrderEvent = updateHouseServiceOrderEvent().copy()
      interceptCause[ConflictApiException] {
        orderManager.updateOrder(orderId, Some(updateOrderEvent), Seq.empty, Seq.empty).futureValue
      }
    }

    "with new multiple events" in new InitOrder {
      createOrder(createRentOrderEvent())
      val event1 = moneyTransferEvent()
      val event2 = moneyTransferEvent(true)
      val event3 = insuranceEvent()
      val event4 = penaltyEvent()
      val event5 = commissionEvent()
      val order = orderManager
        .updateOrder(orderId, None, Seq.empty, Seq(event1, event2, event3, event4, event5))
        .futureValue
      val Seq(commission, penalty, insurance, moneyTransfer2, moneyTransfer1, _) = getEvents()

      checkCreateItem(moneyTransfer1, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(moneyTransfer2, event2, event2.orderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event3, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event4, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event5, commissionOrderItemId, commissionAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 5
    }

    "with new multiple events and some of them exists" in new InitOrder {
      createOrder(createRentOrderEvent())
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      orderManager
        .updateOrder(orderId, None, Seq.empty, Seq(event1, event2))
        .futureValue
      val order = orderManager
        .updateOrder(orderId, None, Seq.empty, Seq(event1, event2, event3, event4))
        .futureValue

      val Seq(commission, penalty, insurance, moneyTransfer, _) = getEvents()

      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 4
    }

    "without new multiple events because of duplicates" in new InitOrder {
      createOrder(createRentOrderEvent())
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      orderManager
        .updateOrder(orderId, None, Seq.empty, Seq(event1, event2, event3, event4))
        .futureValue
      val order = orderManager
        .updateOrder(orderId, None, Seq.empty, Seq(event1, event2, event3, event4))
        .futureValue

      val Seq(commission, penalty, insurance, moneyTransfer, _) = getEvents()

      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
      checkRentOrder(order)
      order.orderItems.size shouldBe 4

    }

    "fail because it has been duplicates in items events" in new InitOrder {
      createOrder(createRentOrderEvent())
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      interceptCause[InvalidParamsApiException] {
        orderManager
          .updateOrder(orderId, None, Seq.empty, Seq(event1, event2, event3, event2, event4))
          .futureValue
      }
    }

    "with new decline event of money transfer" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = moneyTransferEvent()
      val decline = declineOrderItemEvent(moneyTransferOrderItemId)
      orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val order = orderManager.updateOrder(orderId, None, Seq(decline), Seq.empty).futureValue
      val events = getEvents()
      events.size shouldBe 3
      checkDeclineItem(events.head, decline, moneyTransferOrderItemId)
      checkRentOrder(order)
      order.orderItems.isEmpty shouldBe true
    }

    "with new decline event of commission" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = commissionEvent()
      val decline = declineOrderItemEvent(commissionOrderItemId)
      orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val order = orderManager.updateOrder(orderId, None, Seq(decline), Seq.empty).futureValue
      val events = getEvents()
      events.size shouldBe 3
      checkDeclineItem(events.head, decline, commissionOrderItemId)
      checkRentOrder(order)
      order.orderItems.isEmpty shouldBe true
    }

    "with new decline event of insurance" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = insuranceEvent()
      val decline = declineOrderItemEvent(insuranceOrderItemId)
      orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val order = orderManager.updateOrder(orderId, None, Seq(decline), Seq.empty).futureValue
      val events = getEvents()
      events.size shouldBe 3
      checkDeclineItem(events.head, decline, insuranceOrderItemId)
      checkRentOrder(order)
      order.orderItems.isEmpty shouldBe true
    }

    "with new decline event of penalty" in new InitOrder {
      createOrder(createRentOrderEvent())
      val item = penaltyEvent()
      val decline = declineOrderItemEvent(penaltyOrderItemId)
      orderManager.updateOrder(orderId, None, Seq.empty, Seq(item)).futureValue
      val order = orderManager.updateOrder(orderId, None, Seq(decline), Seq.empty).futureValue
      val events = getEvents()
      events.size shouldBe 3
      checkDeclineItem(events.head, decline, penaltyOrderItemId)
      checkRentOrder(order)
      order.orderItems.isEmpty shouldBe true
    }

    "with multiple decline events" in new InitOrder {
      createOrder(createRentOrderEvent())
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()

      val declineEvent1 = declineOrderItemEvent(moneyTransferOrderItemId)
      val declineEvent2 = declineOrderItemEvent(insuranceOrderItemId)
      val declineEvent3 = declineOrderItemEvent(penaltyOrderItemId)
      val declineEvent4 = declineOrderItemEvent(commissionOrderItemId)
      orderManager
        .updateOrder(orderId, None, Seq.empty, Seq(event1, event2, event3, event4))
        .futureValue
      val order = orderManager
        .updateOrder(orderId, None, Seq(declineEvent1, declineEvent3, declineEvent4, declineEvent2), Seq.empty)
        .futureValue
      val Seq(declineInsurance, declineCommission, declinePenalty, declineMoneyTransfer, _, _, _, _, _) = getEvents()

      checkDeclineItem(declineMoneyTransfer, declineEvent1, moneyTransferOrderItemId)
      checkDeclineItem(declineCommission, declineEvent4, commissionOrderItemId)
      checkDeclineItem(declineInsurance, declineEvent2, insuranceOrderItemId)
      checkDeclineItem(declinePenalty, declineEvent3, penaltyOrderItemId)
      checkRentOrder(order)
      order.orderItems.isEmpty shouldBe true
    }

    "fail because it has duplicates decline events" in new InitOrder {
      createOrder(createRentOrderEvent())
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()

      val declineEvent1 = declineOrderItemEvent(moneyTransferOrderItemId)
      val declineEvent2 = declineOrderItemEvent(insuranceOrderItemId)
      val declineEvent3 = declineOrderItemEvent(penaltyOrderItemId)
      val declineEvent4 = declineOrderItemEvent(commissionOrderItemId)
      orderManager
        .updateOrder(orderId, None, Seq.empty, Seq(event1, event2, event3, event4))
        .futureValue
      interceptCause[InvalidParamsApiException] {
        orderManager
          .updateOrder(
            orderId,
            None,
            Seq(declineEvent1, declineEvent3, declineEvent4, declineEvent3, declineEvent2),
            Seq.empty
          )
          .futureValue
      }
      val Seq(commission, penalty, insurance, moneyTransfer, _) = getEvents()

      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
    }

    "fail because there is nothing to decline" in new InitOrder {
      createOrder(createRentOrderEvent())
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()

      val declineEvent1 = declineOrderItemEvent(moneyTransferOrderItemId)
      val declineEvent2 = declineOrderItemEvent(insuranceOrderItemId)
      val declineEvent3 = declineOrderItemEvent(penaltyOrderItemId)
      val declineEvent4 = declineOrderItemEvent(commissionOrderItemId)
      orderManager
        .updateOrder(orderId, None, Seq.empty, Seq(event1, event2))
        .futureValue
      interceptCause[NotFoundApiException] {
        orderManager
          .updateOrder(orderId, None, Seq(declineEvent1, declineEvent3, declineEvent4, declineEvent2), Seq.empty)
          .futureValue
      }

      val Seq(insurance, moneyTransfer, _) = getEvents()
      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
    }

    "with multiple items and decline events" in new InitOrder {
      createOrder(createRentOrderEvent())
      val updateEvent = updateRentOrderEvent(ShortnessType.TERMINATION, 7)
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()

      val declineEvent2 = declineOrderItemEvent(insuranceOrderItemId)
      val declineEvent3 = declineOrderItemEvent(penaltyOrderItemId)
      orderManager
        .updateOrder(orderId, Some(updateEvent), Seq.empty, Seq(event1, event2, event3))
        .futureValue
      val order = orderManager
        .updateOrder(orderId, None, Seq(declineEvent2, declineEvent3), Seq(event1, event4))
        .futureValue
      val Seq(commission, declinePenalty, declineInsurance, penalty, insurance, moneyTransfer, _, _) = getEvents()

      checkCreateItem(moneyTransfer, event1, moneyTransferOrderItemId, moneyTransferAmount)
      checkCreateItem(insurance, event2, insuranceOrderItemId, insuranceAmount)
      checkCreateItem(penalty, event3, penaltyOrderItemId, penaltyAmount)
      checkCreateItem(commission, event4, commissionOrderItemId, commissionAmount)
      checkDeclineItem(declineInsurance, declineEvent2, insuranceOrderItemId)
      checkDeclineItem(declinePenalty, declineEvent3, penaltyOrderItemId)
      checkRentOrder(order)
      order.orderItems.size shouldBe 2
    }
  }

  "OrderManager getOrdersByEntityId" should {
    "return single order" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue
      val orders = orderManager.getOrdersByEntityId(entityId).futureValue
      orders.size shouldBe 1
      checkRentOrder(orders.head)
      orders.head.orderItems.size shouldBe 4
    }

    "return multiple orders" in new InitOrder {
      val createOrderEvent = createRentOrderEvent()
      val event1 = moneyTransferEvent()
      val event2 = insuranceEvent()
      val event3 = penaltyEvent()
      val event4 = commissionEvent()
      orderManager
        .createOrderWithItems(orderId, entityId, createOrderEvent, Seq(event1, event2, event3, event4))
        .futureValue

      val newOrderId = readableString.next
      val houseServiceOrderEvent = createHouseServiceOrderEvent()
      val startDate = Some(Timestamp(now.plus(30, ChronoUnit.DAYS).getEpochSecond))
      val event5 = moneyTransferEvent(startDate = startDate)
      orderManager
        .createOrderWithItems(newOrderId, entityId, houseServiceOrderEvent, Seq(event5))
        .futureValue
      val orders = orderManager.getOrdersByEntityId(entityId).futureValue
      orders.size shouldBe 2
      checkRentOrder(orders.head)
      checkHouseServiceOrder(orders.last, orderId = newOrderId)
      orders.head.orderItems.size shouldBe 4
      orders.last.orderItems.size shouldBe 1
    }
  }

  trait InitOrder {

    protected def checkOrder(event: Event, data: EventData.Event, num: Int = 0): Unit = {
      event.orderId shouldBe orderId
      event.entityId shouldBe entityId
      event.data shouldBe EventData(data)
      event.idempotencyKey shouldBe s"create_$num"
    }

    protected def checkUpdateOrder(event: Event, item: UpdateOrderEvent, num: Int = 0): Unit = {
      event.orderId shouldBe orderId
      event.entityId shouldBe entityId
      event.data shouldBe EventData(EventData.Event.UpdateOrderEvent(item))
      event.idempotencyKey shouldBe s"update_$num"
    }

    protected def checkCreateItem(event: Event, item: CreateOrderItemEvent, itemId: String, amount: Long): Unit = {
      event.orderId shouldBe orderId
      event.entityId shouldBe entityId
      event.data shouldBe EventData(EventData.Event.CreateOrderItemEvent(item))
      event.idempotencyKey shouldBe s"create_item_${item.orderItemId}"
      event.data.getCreateOrderItemEvent.orderItemId shouldBe itemId
      event.data.getCreateOrderItemEvent.amount shouldBe amount
      event.data.getDeclineOrderItemEvent
    }

    protected def checkDeclineOrder(event: Event, num: Int = 0): Unit = {
      event.orderId shouldBe orderId
      event.entityId shouldBe entityId
      event.data shouldBe EventData(EventData.Event.DeclineOrderEvent(DeclineOrderEvent()))
      event.idempotencyKey shouldBe s"decline_$num"
    }

    protected def checkDeclineItem(event: Event, item: DeclineOrderItemEvent, itemId: String): Unit = {
      event.orderId shouldBe orderId
      event.entityId shouldBe entityId
      event.data shouldBe EventData(EventData.Event.DeclineOrderItemEvent(item))
      event.idempotencyKey shouldBe s"decline_item_${item.orderItemId}"
      event.data.getDeclineOrderItemEvent.orderItemId shouldBe itemId
    }

    protected def checkRentOrder(order: Order, isDeclined: Boolean = false): Unit = {
      order.orderId shouldBe orderId
      order.entityId shouldBe entityId
      order.isDeclined shouldBe isDeclined
      order.getOrderType.order.isRentOrder shouldBe true
    }

    protected def checkHouseServiceOrder(order: Order, isDeclined: Boolean = false, orderId: String = orderId): Unit = {
      order.orderId shouldBe orderId
      order.entityId shouldBe entityId
      order.isDeclined shouldBe isDeclined
      order.getOrderType.order.isHouseServiceOrder shouldBe true
    }
  }
}
