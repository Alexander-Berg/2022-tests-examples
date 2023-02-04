package ru.yandex.auto.vin.decoder.api.handlers.orders

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.lemonlabs.uri.Url
import io.lemonlabs.uri.typesafe.dsl._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.vin.orders.OrdersApiModel.PublicOrderModel
import ru.auto.api.vin.orders.ResponseModel.{CreateOrderResponse, GetOrderStatusResponse, GetOrdersListResponse}
import ru.yandex.auto.vin.decoder.api.RequestDirectives
import ru.yandex.auto.vin.decoder.api.exceptions._
import ru.yandex.auto.vin.decoder.exceptions.{InvalidIdentifierException, InvalidUser}
import ru.yandex.auto.vin.decoder.manager.orders.OrdersRequestsProcessor
import ru.yandex.auto.vin.decoder.utils.{EmptyRequestInfo, RequestInfo}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class OrdersHandlerSpec
  extends AnyWordSpecLike
  with Matchers
  with ScalatestRouteTest
  with MockitoSupport
  with BeforeAndAfter {

  implicit val requestInfo: RequestInfo = EmptyRequestInfo

  private val processor = mock[OrdersRequestsProcessor]
  val handler = new OrdersHandler(processor)
  val route: Route = RequestDirectives.wrapRequest(handler.route)

  val GetOrderUrl: Url = "/status" ? ("user_id" -> "user:123") ? ("order_id" -> "abc")
  val CreateOrderUrl: Url = Url.parse("/create")
  val GetOrdersUrl: Url = Url.parse("/")
  val CreateOrderCorrectEntity: HttpEntity.Strict = HttpEntity.apply(ContentTypes.`application/json`, "{}")

  val OrdersFilterEntity: String => HttpEntity.Strict =
    vin => HttpEntity.apply(ContentTypes.`application/json`, s"""{"vin":"$vin"}""")

  before {
    reset(processor)
  }

  "get order" should {
    "return 400" when {
      "processor throw InvalidUser exception" in {
        when(processor.getOrder(?)(?)).thenReturn(Future.failed(InvalidUser("")))

        Get(GetOrderUrl.toStringRaw) ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[String] shouldBe """{"error":"INVALID_REQUEST"}"""
        }
      }
    }
    "return 404" when {
      "processor throw OrderNotFound exception" in {
        when(processor.getOrder(?)(?)).thenReturn(Future.failed(OrderNotFound("10")))

        Get(GetOrderUrl.toStringRaw) ~> route ~> check {
          status shouldBe StatusCodes.NotFound
          responseAs[String] shouldBe """{"error":"ORDER_NOT_FOUND","detailedError":"Order 10 not found"}"""
        }
      }
    }
    "return 200" in {
      val protoResponse = {
        val order = PublicOrderModel.newBuilder().setId("10").setStatus(PublicOrderModel.Status.FAILED).build()
        GetOrderStatusResponse.newBuilder().setOrder(order).build()
      }
      when(processor.getOrder(?)(?)).thenReturn(Future.successful(protoResponse))

      Get(GetOrderUrl.toStringRaw) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe """{"order":{"id":"10","status":"FAILED"}}"""
      }
    }
  }

  "create order" should {
    "return 400" when {
      "processor throw InvalidUser exception" in {
        when(processor.createOrder(?)(?)).thenReturn(Future.failed(InvalidUser("")))

        Post(CreateOrderUrl.toStringRaw, CreateOrderCorrectEntity) ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[String] shouldBe """{"error":"INVALID_REQUEST"}"""
        }
      }
      "processor throw InvalidCreateOrderRequest exception" in {
        when(processor.createOrder(?)(?)).thenReturn(Future.failed(InvalidCreateOrderRequest("")))

        Post(CreateOrderUrl.toStringRaw, CreateOrderCorrectEntity) ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[String] shouldBe """{"error":"INVALID_REQUEST"}"""
        }
      }
    }
    "return 200" in {
      val protoResponse: CreateOrderResponse = {
        val order = PublicOrderModel.newBuilder().setId("1").setStatus(PublicOrderModel.Status.PREPARING).build()
        CreateOrderResponse.newBuilder().setOrder(order).build()
      }
      when(processor.createOrder(?)(?)).thenReturn(Future.successful(protoResponse))

      Post(CreateOrderUrl.toStringRaw, CreateOrderCorrectEntity) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe """{"order":{"id":"1","status":"PREPARING"}}"""
      }
    }
  }

  "get orders" should {
    "return 400" when {
      "filter with empty vin" in {
        when(processor.getOrders(?)(?))
          .thenReturn(Future.failed(InvalidFilterException("Filter cannot be empty")))

        Post(GetOrdersUrl.toStringRaw, OrdersFilterEntity("")) ~> route ~> check {
          status shouldBe StatusCodes.BadRequest
          responseAs[String] shouldBe """{"error":"INVALID_REQUEST","detailedError":"Filter cannot be empty"}"""
        }
      }
    }

    "return 422" when {
      "filter with wrong vin" in {
        when(processor.getOrders(?)(?)).thenReturn(Future.failed(InvalidIdentifierException("wrong_vin")))

        Post(GetOrdersUrl.toStringRaw, OrdersFilterEntity("wrong_vin")) ~> route ~> check {
          status shouldBe StatusCodes.UnprocessableEntity
          responseAs[String] shouldBe """{"error":"INVALID_REQUEST","detailedError":"Invalid identifier wrong_vin"}"""
        }
      }
    }

    "return 200" in {
      val protoResponse = {
        val order = PublicOrderModel.newBuilder().setId("10").setStatus(PublicOrderModel.Status.SUCCESS).build()
        GetOrdersListResponse.newBuilder().addOrders(order).build()
      }
      when(processor.getOrders(?)(?)).thenReturn(Future.successful(protoResponse))

      Post(GetOrdersUrl.toStringRaw, OrdersFilterEntity("good_vin_here")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        responseAs[String] shouldBe """{"orders":[{"id":"10","status":"SUCCESS"}]}"""
      }
    }
  }

}
