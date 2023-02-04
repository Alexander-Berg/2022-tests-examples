package ru.yandex.auto.vin.decoder.manager.orders

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.enablers.Emptiness.emptinessOfGenTraversable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.orders.OrdersApiModel.{OrderIdentifierType, ReportType}
import ru.auto.api.vin.orders.RequestModel.GetOrdersListRequest.OrdersFilter
import ru.yandex.auto.vin.decoder.model.{UserRef, VinCode}
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.Order.AdditionalOrderContext
import ru.yandex.auto.vin.decoder.proto.OrdersSchema.{Order, OrderStatus}
import ru.yandex.auto.vin.decoder.report.ReportDefinition.EnumReportType
import ru.yandex.auto.vin.decoder.storage.orders.OrdersDao
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.utils.Paging
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OrdersManagerTest extends AnyWordSpecLike with Matchers with MockitoSupport {

  private val dao = mock[OrdersDao]
  private val manager = new OrdersManager(dao)
  private val orderContext = AdditionalOrderContext.getDefaultInstance

  implicit val t = Traced.empty

  "get order" should {
    "return None" when {
      "order id not matched" in {
        val orderId = UUID.randomUUID().toString

        when(dao.getOrder(?, ?)(?)).thenReturn(Future.successful(None))

        val optResult = manager.findOrder(orderId).await

        optResult shouldBe None
      }

    }
    "return order" when {
      "user and order id is matched" in {
        val orderId = UUID.randomUUID().toString
        val requestUser = UserRef.parseOrThrow("user:123")

        val order = Order.newBuilder().setOrderId(orderId).setUserId(requestUser.toPlain).build()
        when(dao.getOrder(?, ?)(?)).thenReturn(Future.successful(Some(order)))

        val optResult = manager.findOrder(orderId).await

        optResult shouldBe Some(order)
      }
    }
  }

  "create order" should {
    "return order" when {
      "successfully created" in {

        val requestUser = UserRef.parseOrThrow("user:123")
        val requestReportType = EnumReportType(ReportType.GIBDD_REPORT)
        val requestIdentifier = VinCode.apply("XWEPH81ABJ0014224")
        val requestIdentifierType = OrderIdentifierType.VIN

        when(dao.insert(?, ?)(?)).thenReturn(Future.successful(1))
        val createdOrder =
          manager
            .createOrder(
              requestUser,
              requestReportType,
              requestIdentifier,
              requestIdentifierType,
              skipBilling = false,
              orderContext
            )
            .await

        createdOrder.getUserId shouldBe requestUser.toPlain
        createdOrder.getIdentifier shouldBe requestIdentifier.toString
        createdOrder.getIdentifierType shouldBe requestIdentifierType
        createdOrder.getReportType shouldBe requestReportType.reportType
        createdOrder.hasCreated shouldBe true
        createdOrder.getStatus shouldBe OrderStatus.PREPARING
      }
    }
  }

  "get orders by vin" should {
    "return empty list" when {
      "where is no orders for such vin" in {

        when(dao.getOrders(?, ?, ?)(?)).thenReturn(Future.successful(List.empty))
        when(dao.count(?, ?)(?)).thenReturn(Future.successful(0))

        val filter = OrdersFilter.newBuilder().setVin("XWEPH81ABJ0014224").build()
        val paging = Paging.Default
        val result = manager.getOrders(filter, paging).await.filteredOrders

        result shouldBe empty
      }

    }
    "return orders" when {
      "there are some orders for such vin" in {

        val orderId1 = UUID.randomUUID().toString
        val requestUser1 = UserRef.parseOrThrow("user:123")
        val order1 = Order.newBuilder().setOrderId(orderId1).setUserId(requestUser1.toPlain).build()

        val orderId2 = UUID.randomUUID().toString
        val requestUser2 = UserRef.parseOrThrow("dealer:123")
        val order2 = Order.newBuilder().setOrderId(orderId2).setUserId(requestUser2.toPlain).build()

        when(dao.getOrders(?, ?, ?)(?)).thenReturn(Future.successful(List(order1, order2)))
        when(dao.count(?, ?)(?)).thenReturn(Future.successful(2))

        val filter = OrdersFilter.newBuilder().setVin("XWEPH81ABJ0014224").build()
        val paging = Paging.Default
        val result = manager.getOrders(filter, paging).await.filteredOrders

        (result should have).length(2)
        result should contain(order1)
        result should contain(order2)
      }
    }
  }

}
