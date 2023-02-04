package ru.yandex.auto.vin.decoder.manager.orders

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.RequestModel.RequestPagination
import ru.auto.api.vin.orders.OrdersApiModel.{FailReason, OrderIdentifierType, PublicOrderModel, ReportType}
import ru.auto.api.vin.orders.RequestModel.{CreateOrderRequest, GetOrdersListRequest}
import ru.auto.api.vin.orders.ResponseModel.ErrorCode
import ru.yandex.auto.vin.decoder.api.exceptions._
import ru.yandex.auto.vin.decoder.exceptions.{InvalidIdentifierException, InvalidUser}
import ru.yandex.auto.vin.decoder.manager.orders.OrderParamsValidator.ValidatedOrderParams
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.OrdersSchema._
import ru.yandex.auto.vin.decoder.report.ReportDefinition.{EnumReportType, UnknownReportType}
import ru.yandex.auto.vin.decoder.report.processors.report.ReportDefinitionManager
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.auto.vin.decoder.utils.EmptyRequestInfo
import ru.yandex.vertis.mockito.MockitoSupport

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OrdersRequestsProcessorTest extends AnyWordSpecLike with Matchers with MockitoSupport {

  private val manager = mock[OrdersManager]
  private val converter = mock[OrdersConverter]
  private val validator = mock[OrderParamsValidator]
  private val definitionManager = mock[ReportDefinitionManager]
  private val processor = new OrdersRequestsProcessor(manager, converter, validator, definitionManager)

  implicit val r = EmptyRequestInfo

  val ordersFilter = (vin: String) => {
    val filter = GetOrdersListRequest.OrdersFilter.newBuilder.setVin(vin).build()
    val paging = RequestPagination.newBuilder.setPage(1).setPageSize(10)
    GetOrdersListRequest.newBuilder().setFilter(filter).setPaging(paging).build()
  }

  "get order" should {
    "throw Exception" when {
      "order is not found" in {
        val orderId = UUID.randomUUID().toString
        when(manager.findOrder(?)(?)).thenReturn(Future.successful(None))

        intercept[OrderNotFound] {
          processor.getOrder(orderId).await
        }
      }
    }
    "return order" when {
      "request is correct" in {
        val rawUser = "dealer:123"
        val orderId = UUID.randomUUID().toString

        val order = Order.newBuilder().setOrderId(orderId).setUserId(rawUser).build()
        val convertedOrder = PublicOrderModel
          .newBuilder()
          .setId(orderId)
          .setStatus(PublicOrderModel.Status.PREPARING)
          .build()

        when(manager.findOrder(?)(?)).thenReturn(Future.successful(Some(order)))
        when(converter.convert(?)).thenReturn(convertedOrder)

        val result = processor.getOrder(orderId).await

        result.getError shouldBe ErrorCode.UNKNOWN_ERROR_CODE
        result.getDetailedError.isEmpty shouldBe true
        result.getOrder.getId shouldBe orderId
        result.getOrder.getStatus shouldBe PublicOrderModel.Status.PREPARING
      }
    }
  }

  "create order" should {
    "throw Exception" when {
      "user is invalid" in {
        val request = CreateOrderRequest
          .newBuilder()
          .setUserId("laksg")
          .setReportType(ReportType.GIBDD_REPORT)
          .setIdentifierType(OrderIdentifierType.VIN)
          .setIdentifier(VinCode.apply("XWEPH81ABJ0014224").toString)
          .build()

        when(definitionManager.extractReportType(request)).thenReturn(Future.successful(UnknownReportType))
        intercept[InvalidUser] {
          processor.createOrder(request).await
        }
      }
      "validator return fail reason" in {
        val request = CreateOrderRequest
          .newBuilder()
          .setUserId("dealer:123")
          .build()

        when(validator.validate(?, ?, ?)).thenReturn(Left(FailReason.INVALID_IDENTIFIER))
        when(definitionManager.extractReportType(request)).thenReturn(Future.successful(UnknownReportType))
        intercept[InvalidCreateOrderRequest] {
          processor.createOrder(request).await
        }
      }
      "order is creating for non-dealer" in {
        val request = CreateOrderRequest.newBuilder
          .setUserId("user:123")
          .build
        when(definitionManager.extractReportType(request)).thenReturn(Future.successful(UnknownReportType))
        intercept[InvalidUser] {
          processor.createOrder(request).await
        }
      }
    }
    "return created order" when {
      "request is correct" in {
        val request = CreateOrderRequest
          .newBuilder()
          .setUserId("dealer:123")
          .setReportType(ReportType.GIBDD_REPORT)
          .build()
        val reportType = EnumReportType(request.getReportType)

        when(validator.validate(?, ?, ?)).thenReturn(
          Right(
            ValidatedOrderParams(
              VinCode.apply("XWEPH81ABJ0014224"),
              reportType,
              OrderIdentifierType.VIN
            )
          )
        )
        when(definitionManager.extractReportType(request)).thenReturn(Future.successful(reportType))

        val uuid = UUID.randomUUID().toString
        val order = Order.newBuilder().setOrderId(uuid).build()
        val convertedOrder =
          PublicOrderModel.newBuilder().setId(uuid).setStatus(PublicOrderModel.Status.PREPARING).build()

        when(manager.createOrder(?, ?, ?, ?, ?, ?)(?)).thenReturn(Future.successful(order))
        when(converter.convert(?)).thenReturn(convertedOrder)

        val res = processor.createOrder(request).await

        res.getError shouldBe ErrorCode.UNKNOWN_ERROR_CODE
        res.getDetailedError.isEmpty shouldBe true
        res.getOrder.getId shouldBe uuid
        res.getOrder.getStatus shouldBe PublicOrderModel.Status.PREPARING
      }
    }
  }

  "get orders" should {
    "throw Exception" when {
      "filter with empty vin" in {
        intercept[InvalidFilterException] {
          processor.getOrders(ordersFilter("")).await
        }
      }
      "filter with wrong vin" in {
        intercept[InvalidIdentifierException] {
          processor.getOrders(ordersFilter("wrong_vin_here")).await
        }
      }
    }
    "return orders list" when {
      "request is correct" in {
        val rawUser = "dealer:123"
        val orderId = UUID.randomUUID().toString

        val order = Order.newBuilder().setOrderId(orderId).setUserId(rawUser).build()
        val convertedOrder = PublicOrderModel
          .newBuilder()
          .setId(orderId)
          .setStatus(PublicOrderModel.Status.SUCCESS)
          .build()

        when(manager.getOrders(?, ?)(?)).thenReturn(Future.successful(FilteredOrderList(List(order), 1)))
        when(converter.convert(?)).thenReturn(convertedOrder)

        val result = processor.getOrders(ordersFilter("XWEPH81ABJ0014224")).await

        result.getError shouldBe ErrorCode.UNKNOWN_ERROR_CODE
        result.getDetailedError.isEmpty shouldBe true
        (result.getOrdersList should have).length(1)
        result.getOrdersList.get(0).getId shouldBe orderId
        result.getOrdersList.get(0).getStatus shouldBe PublicOrderModel.Status.SUCCESS
      }
    }
  }
}
