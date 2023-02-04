package ru.yandex.realty.rent.payments.backend.manager

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.payments.gen.EventGen
import ru.yandex.realty.rent.payments.model.OrderAggregator
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class OrderAggregatorSpec extends EventGen with AsyncSpecBase with ScalaFutures with Matchers {
  implicit val trace: Traced = Traced.empty

  "OrderAggregator" should {
    "build create rent order without items" in {
      val createOrderEvent1 = createRentOrderEventGen().next
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent1.idempotencyKey).next
      val createOrderEvent2 = createRentOrderEventGen(parent = Some(declineOrderEvent.idempotencyKey)).next
      val orderId = createOrderEvent2.orderId
      val order = OrderAggregator.buildOrder(orderId, Seq(createOrderEvent2, declineOrderEvent, createOrderEvent1))
      order.orderId shouldBe orderId
      order.getOrderType.order.isRentOrder shouldBe true
      order.isDeclined shouldBe false
      order.orderItems.size shouldBe 0
    }

    "build decline rent order without items" in {
      val createOrderEvent = createRentOrderEventGen().next
      val orderId = createOrderEvent.orderId
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent.idempotencyKey).next
      val order = OrderAggregator.buildOrder(orderId, Seq(declineOrderEvent, createOrderEvent))
      order.orderId shouldBe orderId
      order.getOrderType.order.isRentOrder shouldBe true
      order.isDeclined shouldBe true
      order.orderItems.size shouldBe 0
    }

    "build create house service order without items" in {
      val createOrderEvent1 = createHouseServiceOrderEventGen().next
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent1.idempotencyKey).next
      val createOrderEvent2 = createHouseServiceOrderEventGen(parent = Some(declineOrderEvent.idempotencyKey)).next
      val orderId = createOrderEvent2.orderId
      val order = OrderAggregator.buildOrder(orderId, Seq(createOrderEvent2, declineOrderEvent, createOrderEvent1))
      order.orderId shouldBe orderId
      order.getOrderType.order.isHouseServiceOrder shouldBe true
      order.isDeclined shouldBe false
      order.orderItems.size shouldBe 0
    }

    "build decline house service order without items" in {
      val createOrderEvent = createHouseServiceOrderEventGen().next
      val orderId = createOrderEvent.orderId
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent.idempotencyKey).next
      val order = OrderAggregator.buildOrder(orderId, Seq(declineOrderEvent, createOrderEvent))

      order.orderId shouldBe orderId
      order.getOrderType.order.isHouseServiceOrder shouldBe true
      order.isDeclined shouldBe true
      order.orderItems.size shouldBe 0
    }

    "build create rent order with items" in {
      val createOrderEvent1 = createRentOrderEventGen().next
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent1.idempotencyKey).next
      val createOrderEvent2 = createRentOrderEventGen(parent = Some(declineOrderEvent.idempotencyKey)).next
      val orderId = createOrderEvent2.orderId
      val orderItemId1 = readableString.next
      val orderItemId2 = readableString.next
      val orderItemId3 = readableString.next
      val item1 = createOrderItemEventGen(parent = createOrderEvent2.idempotencyKey, orderItemId1).next
      val item2 = createOrderItemEventGen(parent = item1.idempotencyKey, orderItemId2).next
      val item3 = createOrderItemEventGen(parent = item2.idempotencyKey, orderItemId3).next
      val declineItem2 = declineOrderItemEventGen(parent = item3.idempotencyKey, orderItemId2).next
      val order = OrderAggregator.buildOrder(
        orderId,
        Seq(item3, declineItem2, item2, createOrderEvent2, declineOrderEvent, item1, createOrderEvent1)
      )
      order.orderId shouldBe orderId
      order.getOrderType.order.isRentOrder shouldBe true
      order.isDeclined shouldBe false
      order.orderItems.size shouldBe 2
      order.orderItems.exists(_.orderItemId == orderItemId1) shouldBe true
      order.orderItems.exists(_.orderItemId == orderItemId3) shouldBe true
    }

    "build create rent order without items because all was declined" in {
      val createOrderEvent1 = createRentOrderEventGen().next
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent1.idempotencyKey).next
      val createOrderEvent2 = createRentOrderEventGen(parent = Some(declineOrderEvent.idempotencyKey)).next
      val orderId = createOrderEvent2.orderId
      val orderItemId1 = readableString.next
      val orderItemId2 = readableString.next
      val item1 = createOrderItemEventGen(parent = createOrderEvent2.idempotencyKey, orderItemId1).next
      val item2 = createOrderItemEventGen(parent = item1.idempotencyKey, orderItemId2).next
      val declineItem1 = declineOrderItemEventGen(parent = item2.idempotencyKey, orderItemId1).next
      val declineItem2 = declineOrderItemEventGen(parent = declineItem1.idempotencyKey, orderItemId2).next
      val order = OrderAggregator.buildOrder(
        orderId,
        Seq(declineItem2, declineItem1, item2, createOrderEvent2, declineOrderEvent, item1, createOrderEvent1)
      )

      order.orderId shouldBe orderId
      order.getOrderType.order.isRentOrder shouldBe true
      order.isDeclined shouldBe false
      order.orderItems.size shouldBe 0
    }

    "build create house service order with items" in {
      val createOrderEvent1 = createHouseServiceOrderEventGen().next
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent1.idempotencyKey).next
      val createOrderEvent2 = createHouseServiceOrderEventGen(parent = Some(declineOrderEvent.idempotencyKey)).next
      val orderId = createOrderEvent2.orderId
      val orderItemId1 = readableString.next
      val orderItemId2 = readableString.next
      val orderItemId3 = readableString.next
      val item1 = createOrderItemEventGen(parent = createOrderEvent2.orderId, orderItemId1).next
      val item2 = createOrderItemEventGen(parent = item1.idempotencyKey, orderItemId2).next
      val item3 = createOrderItemEventGen(parent = item2.idempotencyKey, orderItemId3).next
      val declineItem2 = declineOrderItemEventGen(parent = item3.idempotencyKey, orderItemId2).next
      val order = OrderAggregator.buildOrder(
        orderId,
        Seq(item3, declineItem2, item2, createOrderEvent2, declineOrderEvent, item1, createOrderEvent1)
      )
      order.orderId shouldBe orderId
      order.getOrderType.order.isHouseServiceOrder shouldBe true
      order.isDeclined shouldBe false
      order.orderItems.size shouldBe 2
      order.orderItems.exists(_.orderItemId == orderItemId1) shouldBe true
      order.orderItems.exists(_.orderItemId == orderItemId3) shouldBe true
    }

    "build create house service order without items because all was declined" in {
      val createOrderEvent1 = createHouseServiceOrderEventGen().next
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent1.idempotencyKey).next
      val createOrderEvent2 = createHouseServiceOrderEventGen(parent = Some(declineOrderEvent.idempotencyKey)).next
      val orderId = createOrderEvent2.orderId
      val orderItemId1 = readableString.next
      val orderItemId2 = readableString.next
      val item1 = createOrderItemEventGen(parent = createOrderEvent2.idempotencyKey, orderItemId1).next
      val item2 = createOrderItemEventGen(parent = item1.idempotencyKey, orderItemId2).next
      val declineItem1 = declineOrderItemEventGen(parent = item2.idempotencyKey, orderItemId1).next
      val declineItem2 = declineOrderItemEventGen(parent = declineItem1.idempotencyKey, orderItemId2).next
      val order = OrderAggregator.buildOrder(
        orderId,
        Seq(declineItem2, declineItem1, item2, createOrderEvent2, declineOrderEvent, item1, createOrderEvent1)
      )
      order.orderId shouldBe orderId
      order.getOrderType.order.isHouseServiceOrder shouldBe true
      order.isDeclined shouldBe false
      order.orderItems.size shouldBe 0
    }

    "not find cycle" in {
      val createOrderEvent1 = createRentOrderEventGen().next
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent1.idempotencyKey).next
      val createOrderEvent2 = createRentOrderEventGen(parent = Some(declineOrderEvent.idempotencyKey)).next
      val orderItemId2 = readableString.next
      val item1 = createOrderItemEventGen(parent = createOrderEvent2.idempotencyKey, readableString.next).next
      val item2 = createOrderItemEventGen(parent = item1.idempotencyKey, orderItemId2).next
      val item3 = createOrderItemEventGen(parent = item2.idempotencyKey, readableString.next).next
      val declineItem2 = declineOrderItemEventGen(parent = item3.idempotencyKey, orderItemId2).next
      OrderAggregator.checkForCycle(
        List(createOrderEvent2, createOrderEvent1, declineOrderEvent, item1, item2, item3, declineItem2)
      )
    }

    "find cycle in chain of multiple elements" in {
      val createOrderEvent1 = createRentOrderEventGen().next
      val declineOrderEvent = declineOrderEventGen(parent = createOrderEvent1.idempotencyKey).next
      val createOrderEvent2 = createRentOrderEventGen(parent = Some(declineOrderEvent.idempotencyKey)).next
      val orderItemId2 = readableString.next
      val item1 = createOrderItemEventGen(parent = createOrderEvent2.idempotencyKey, readableString.next).next
      val item2 = createOrderItemEventGen(parent = item1.idempotencyKey, orderItemId2).next
      val item3 = createOrderItemEventGen(parent = item2.idempotencyKey, readableString.next).next
      val declineItem2 = declineOrderItemEventGen(parent = item3.idempotencyKey, orderItemId2).next
      interceptCause[IllegalStateException] {
        OrderAggregator.checkForCycle(
          List(
            createOrderEvent2,
            createOrderEvent1,
            declineOrderEvent.copy(parent = Some(declineItem2.idempotencyKey)),
            item1,
            item2,
            item3,
            declineItem2
          )
        )
      }
    }

    "find cycle in chain of one element" in {
      val createOrderEvent1 = createRentOrderEventGen().next
      interceptCause[IllegalStateException] {
        OrderAggregator.checkForCycle(List(createOrderEvent1.copy(parent = Some(createOrderEvent1.idempotencyKey))))
      }
    }
  }
}
