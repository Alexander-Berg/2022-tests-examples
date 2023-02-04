package ru.yandex.realty.rent.payments.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.payments.gen.EventGen
import ru.yandex.realty.tracing.Traced

@RunWith(classOf[JUnitRunner])
class IdempotencyKeyGeneratorSpec extends AsyncSpecBase with EventGen {

  implicit val trace: Traced = Traced.empty

  "IdempotencyKeyGenerator" should {
    "generate idempotency key for CreateOrderEvent for new order" in {
      val event = createRentOrderEventGen().next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(event.data, List.empty)
      idempotencyKey shouldBe "create_0"
    }

    "generate the same idempotency key for CreateOrderEvent for created order" in {
      val event = createRentOrderEventGen().next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(event.data, List(event))
      idempotencyKey shouldBe "create_0"
    }

    "generate idempotency key for CreateOrderEvent for declined order" in {
      val event = createRentOrderEventGen().next
      val declineEvent = declineOrderEventGen(event.idempotencyKey).next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(event.data, List(event, declineEvent))
      idempotencyKey shouldBe "create_1"
    }

    "generate idempotency key for DeclineOrderEvent for created order" in {
      val declineEvent = declineOrderEventGen(readableString.next).next
      val event = createRentOrderEventGen().next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(declineEvent.data, List(event))
      idempotencyKey shouldBe "decline_0"
    }

    "generate idempotency key for DeclineOrderEvent for declined order" in {
      val event = createRentOrderEventGen().next
      val declineEvent = declineOrderEventGen(event.idempotencyKey).next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(declineEvent.data, List(event, declineEvent))
      idempotencyKey shouldBe "decline_0"
    }

    "generate idempotency key for DeclineOrderEvent for declined and created order" in {
      val event = createRentOrderEventGen().next
      val declineEvent = declineOrderEventGen(event.idempotencyKey).next
      val idempotencyKey =
        IdempotencyKeyGenerator.generateIdempotencyKey(declineEvent.data, List(event, declineEvent, event))
      idempotencyKey shouldBe "decline_1"
    }

    "generate idempotency key for CreateOrderItemEvent" in {
      val createOrder = createRentOrderEventGen().next
      val orderItemId = readableString.next
      val event = createOrderItemEventGen(createOrder.idempotencyKey, orderItemId).next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(event.data, List(createOrder))
      idempotencyKey shouldBe s"create_item_$orderItemId"
    }

    "generate idempotency key for DeclineOrderItemEvent" in {
      val orderItemId = readableString.next
      val createOrder = createRentOrderEventGen().next
      val createItem = createOrderItemEventGen(createOrder.idempotencyKey, orderItemId).next
      val event = declineOrderItemEventGen(createItem.idempotencyKey, orderItemId).next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(event.data, List(event, createOrder))
      idempotencyKey shouldBe s"decline_item_$orderItemId"
    }

    "generate idempotency key for UpdateOrderItemEvent for the first time" in {
      val createOrder = createRentOrderEventGen().next
      val event = updateRentOrderEventGen(Some(createOrder.idempotencyKey)).next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(event.data, List(createOrder))
      idempotencyKey shouldBe "update_0"
    }

    "generate idempotency key for UpdateOrderItemEvent for multiple times" in {
      val createOrder = createRentOrderEventGen().next
      val event = updateRentOrderEventGen(Some(createOrder.idempotencyKey)).next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(event.data, List(event, event, createOrder))
      idempotencyKey shouldBe "update_2"
    }

    "generate idempotency key for InitPaymentEvent for the first time" in {
      val event = createRentOrderEventGen().next
      val initPaymentEvent = initPaymentEventGen().next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(initPaymentEvent.data, List(event))
      idempotencyKey shouldBe "init_payment_0"
    }

    "generate idempotency key for InitPaymentEvent" in {
      val event = createRentOrderEventGen().next
      val initEvent = initPaymentEventGen().next
      val idempotencyKey =
        IdempotencyKeyGenerator.generateIdempotencyKey(initEvent.data, List(initEvent, initEvent, event))
      idempotencyKey shouldBe "init_payment_2"
    }

    "generate idempotency key for InitPayoutEvent for the first time" in {
      val event = createRentOrderEventGen().next
      val initPayoutEvent = initPayoutEventGen().next
      val idempotencyKey = IdempotencyKeyGenerator.generateIdempotencyKey(initPayoutEvent.data, List(event))
      idempotencyKey shouldBe "init_payout_0"
    }

    "generate idempotency key for InitPayoutEvent" in {
      val event = createRentOrderEventGen().next
      val initPayoutEvent = initPayoutEventGen().next
      val idempotencyKey =
        IdempotencyKeyGenerator.generateIdempotencyKey(
          initPayoutEvent.data,
          List(initPayoutEvent, initPayoutEvent, event)
        )
      idempotencyKey shouldBe "init_payout_2"
    }
  }

}
