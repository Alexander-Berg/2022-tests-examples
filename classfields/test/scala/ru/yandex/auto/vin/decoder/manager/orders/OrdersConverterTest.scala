package ru.yandex.auto.vin.decoder.manager.orders

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.orders.OrdersApiModel.PublicOrderModel
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.Billing.BillingStatus
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.{Order, OrderStatus}

class OrdersConverterTest extends AnyWordSpecLike with Matchers {

  val converter = new OrdersConverter

  "status" should {
    "be NEW" when {
      "order is new" in {
        val order = Order.newBuilder().setStatus(OrderStatus.PREPARING).build()
        val converted = converter.convertStatus(order)
        converted shouldBe PublicOrderModel.Status.PREPARING
      }
    }
    "be PREPARING" when {
      "order is preparing" in {
        val order = Order.newBuilder().setStatus(OrderStatus.PREPARING).build()
        val converted = converter.convertStatus(order)
        converted shouldBe PublicOrderModel.Status.PREPARING
      }
    }
    "be UPDATING" when {
      "order data is updating" in {
        val order = Order.newBuilder().setStatus(OrderStatus.UPDATING).build()
        val converted = converter.convertStatus(order)
        converted shouldBe PublicOrderModel.Status.UPDATING
      }
    }
    "be FAILED" when {
      "order is finished and payment is failed" in {
        val builder = Order.newBuilder().setStatus(OrderStatus.FAILED)
        builder.getBillingBuilder.setStatus(BillingStatus.FAILED)
        val order = builder.build()
        val converted = converter.convertStatus(order)
        converted shouldBe PublicOrderModel.Status.FAILED
      }
    }
    "be SUCCESS" when {
      "order is finished and payment is success" in {
        val builder = Order.newBuilder().setStatus(OrderStatus.SUCCESS)
        builder.getBillingBuilder.setStatus(BillingStatus.SUCCESS)
        val order = builder.build()
        val converted = converter.convertStatus(order)
        converted shouldBe PublicOrderModel.Status.SUCCESS
      }
    }
  }

}
