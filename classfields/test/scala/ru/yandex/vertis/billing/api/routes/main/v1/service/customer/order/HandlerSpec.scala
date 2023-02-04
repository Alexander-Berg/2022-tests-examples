package ru.yandex.vertis.billing.api.routes.main.v1.service.customer.order

import akka.http.scaladsl.model.StatusCodes
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.Exceptions.{ArtificialInternalException, ArtificialNoSuchElementException}
import ru.yandex.vertis.billing.api.Exceptions.artificialAccessDenyException
import ru.yandex.vertis.billing.api.RootHandlerSpecBase
import ru.yandex.vertis.billing.api.routes.main.v1.service.customer.order.Handler
import ru.yandex.vertis.billing.api.routes.main.v1.view.OrderPaymentView
import ru.yandex.vertis.billing.api.routes.main.v1.view.PaymentRequestResultView
import ru.yandex.vertis.billing.balance.model.PaymentRequestResult
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens.{HoldRequestGen, OrderTransactionGen, Producer}
import ru.yandex.vertis.billing.service.OrderService
import ru.yandex.vertis.billing.service.OrderService.{ListFilter, TransactionFilter, TransactionTypeFilter}
import ru.yandex.vertis.billing.util._

import scala.concurrent.Future

/**
  * Specs on [[Handler]].
  *
  * @author ruslansd
  * @author alesavin
  * @author dimas
  */
class HandlerSpec extends AnyWordSpec with RootHandlerSpecBase {

  override def basePath: String = s"/api/1.x/service/autoru/customer/"

  private def requestUrl(customer: CustomerId, suffix: String = "/"): String = {
    val agency = customer.agencyId.map(a => s"agency/$a/").getOrElse("")
    basePath + agency + s"client/${customer.clientId}/order" + suffix
  }

  val agencyId = 1L
  val clientId = 2L
  val orderId = 3

  val directCustomerId = CustomerId(clientId, None)
  val agencyCustomerId = CustomerId(clientId, Some(agencyId))

  val properties = OrderProperties("properties", None)
  val balance = OrderBalance2(1, 2)

  val agencyOrder = Order(orderId, agencyCustomerId, properties, balance)

  val directOrder = Order(orderId, directCustomerId, properties, balance)

  val clientUrl = "www.client.com"
  val adminUrl = "www.admin.com"

  val paymentResult = PaymentRequestResult(clientUrl, adminUrl)

  "GET /agency/{agencyId}/client/{clientId}/order" should {
    testGetOrderRoute(agencyCustomerId, agencyOrder)
  }

  "GET /client/{clientId}/order" should {
    testGetOrderRoute(directCustomerId, directOrder)
  }

  private def testGetOrderRoute(customerId: CustomerId, order: Order): Unit = {
    import ru.yandex.vertis.billing.api.view.OrderView.slicedResultUnmarshaller

    val uri = requestUrl(customerId)

    "not provide customer orders without operator" in {
      Get(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    val request = Get(uri) ~> defaultHeaders

    "respond InternalServerError in case of backend problems" in {
      stub(backend.orderService.list(_: CustomerId, _: Slice, _: OrderService.ListFilter)(_: OperatorContext)) {
        case (`customerId`, _, ListFilter.NoFilter, `operator`) =>
          Future.failed(ArtificialInternalException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "not found customer orders" in {
      stub(backend.orderService.list(_: CustomerId, _: Slice, _: OrderService.ListFilter)(_: OperatorContext)) {
        case (`customerId`, _, ListFilter.NoFilter, `operator`) =>
          Future.failed(ArtificialNoSuchElementException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.NotFound)
      }
    }

    "get customer orders" in {
      stub(backend.orderService.list(_: CustomerId, _: Slice, _: OrderService.ListFilter)(_: OperatorContext)) {
        case (`customerId`, p, ListFilter.NoFilter, `operator`) =>
          Future.successful(SlicedResult(List(order), 1, p))
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
        val orders = responseAs[SlicedResult[Order]]
        orders.size should be(1)
        orders.head should be(order)
      }
    }
  }

  "POST /agency/{agencyId}/client/{clientId}/order" should {
    testPostOrderRoute(agencyCustomerId, agencyOrder)
  }

  "POST /client/{clientId}/order" should {
    testPostOrderRoute(directCustomerId, directOrder)
  }

  private def testPostOrderRoute(customer: CustomerId, order: Order): Unit = {
    import ru.yandex.vertis.billing.api.view.OrderPropertiesView.modelMarshaller
    import ru.yandex.vertis.billing.api.view.OrderView.modelUnmarshaller

    val uri = requestUrl(customer)

    "not create order without operator" in {
      Post(uri) ~>
        defaultHeaders ~>
        route ~>
        check {
          status should be(StatusCodes.BadRequest)
        }
    }

    val request = Post(uri, properties) ~> defaultHeaders

    "not create order by safety reasons" in {
      stub(backend.orderService.create(_: CustomerId, _: OrderProperties)(_: OperatorContext)) {
        case (`customer`, `properties`, `operator`) =>
          Future.failed(artificialAccessDenyException(operator.operator))
      }
      request ~> route ~> check {
        status should be(StatusCodes.Forbidden)
      }
    }

    "respond InternalServerError in case of backend problems" in {
      stub(backend.orderService.create(_: CustomerId, _: OrderProperties)(_: OperatorContext)) {
        case (`customer`, `properties`, `operator`) =>
          Future.failed(ArtificialInternalException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "successfully create order" in {
      stub(backend.orderService.create(_: CustomerId, _: OrderProperties)(_: OperatorContext)) {
        case (`customer`, `properties`, `operator`) =>
          Future.successful(order)
      }
      request ~> sealRoute(route) ~> check {
        status should be(StatusCodes.OK)
        val view = responseAs[Order]
        view should be(order)
      }
    }
  }

  "POST /agency/{agencyId}/client/{clientId}/order/{id}/payment" should {
    testPaymentRoute(agencyOrder.id, agencyCustomerId, OrderPayment(3))
  }

  "POST /client/{clientId}/order/{id}/payment" should {
    testPaymentRoute(directOrder.id, directCustomerId, OrderPayment(3))
  }

  private def testPaymentRoute(orderId: OrderId, customerId: CustomerId, payment: OrderPayment): Unit = {
    import OrderPaymentView.modelMarshaller
    import PaymentRequestResultView.modelUnmarshaller

    val uri = requestUrl(customerId, s"/$orderId/payment")
    val request = Post(uri, payment) ~> defaultHeaders

    "not create request for direct customer orders" in {
      Post(uri) ~> route ~> check {
        status should be(StatusCodes.BadRequest)
      }
    }

    "respond InternalServerError in case of backend problems" in {
      stub(
        backend.orderService.payment(_: CustomerId, _: OrderId, _: OrderPayment, _: Option[String])(_: OperatorContext)
      ) { case (`customerId`, `orderId`, `payment`, path, `operator`) =>
        Future.failed(ArtificialInternalException())
      }
      request ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "create payment request for direct customer orders" in {
      stub(
        backend.orderService.payment(_: CustomerId, _: OrderId, _: OrderPayment, _: Option[String])(_: OperatorContext)
      ) { case (`customerId`, `orderId`, `payment`, path, `operator`) =>
        Future.successful(paymentResult)
      }
      request ~> route ~> check {
        status should be(StatusCodes.OK)
        val view = responseAs[PaymentRequestResult]
        view should be(paymentResult)
      }
    }
  }

  "POST /client/{clientId}/order/{id}/hold" should {
    testHoldRoute(directOrder.id, directCustomerId)
  }

  "POST /agency/{agencyId}/client/{clientId}/order/{id}/hold" should {
    testHoldRoute(agencyOrder.id, agencyCustomerId)
  }

  "GET /client/{clientId}/order/{id}/transactions" should {
    testTransaction(orderId, directCustomerId)
  }

  private def testTransaction(orderId: OrderId, customer: CustomerId) = {
    import ru.yandex.vertis.billing.api.routes.main.v1.view.OrderTransactionView._
    import ru.yandex.vertis.billing.util.DateTimeUtils.IsoDateFormatter
    val interval = {
      val from = DateTimeInterval.currentDay.from
      DateTimeInterval(from, from.plusDays(1))
    }
    val from = IsoDateFormatter.print(interval.from)
    val to = IsoDateFormatter.print(interval.to)
    val uri = requestUrl(customer, s"/$orderId/transactions?from=$from&to=$to&pageSize=10")
    val transactions = OrderTransactionGen.next(5).toList

    "provide transaction without filter" in {
      val request = Get(uri) ~> defaultHeaders
      stub(
        backend.orderService
          .listTransactions(_: CustomerId, _: OrderId, _: DateTimeInterval, _: Option[TransactionFilter], _: Slice)(
            _: OperatorContext
          )
      ) { case (`customer`, `orderId`, `interval`, None, p, `operator`) =>
        Future.successful(SlicedResult(transactions, transactions.size, p))
      }
      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        val body = responseAs[SlicedResult[OrderTransaction]]
        body.values.map(_.id) should contain theSameElementsAs transactions.map(_.id)
      }
    }

    "provide transaction with few types filter" in {
      val types = List(OrderTransactions.Incoming, OrderTransactions.Withdraw)
      val withFilter = s"$uri${types.mkString("&transactionType=", "&transactionType=", "")}"
      val request = Get(withFilter) ~> defaultHeaders
      stub(
        backend.orderService
          .listTransactions(_: CustomerId, _: OrderId, _: DateTimeInterval, _: Option[TransactionFilter], _: Slice)(
            _: OperatorContext
          )
      ) { case (`customer`, `orderId`, `interval`, Some(filter), p, `operator`) =>
        Future.successful(SlicedResult(transactions, transactions.size, p))
      }
      request ~> route ~> check {
        status shouldBe StatusCodes.OK
        val body = responseAs[SlicedResult[OrderTransaction]]
        body.values.map(_.id) should contain theSameElementsAs transactions.map(_.id)
      }
    }

    "reject request with incorrect filter" in {
      val types = List(OrderTransactions.Incoming.toString, s"${OrderTransactions.Withdraw.toString}-incorrect")
      val withFilter = s"$uri${types.mkString("&transactionType=", "&transactionType=", "")}"
      val request = Get(withFilter) ~> defaultHeaders

      request ~> route ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }
  }

  private def testHoldRoute(orderId: OrderId, customerId: CustomerId): Unit = {
    import ru.yandex.vertis.billing.api.view.HoldRequestView.{modelMarshaller, modelUnmarshaller}

    val uri = requestUrl(customerId, s"/$orderId/hold")
    val holdRequest = HoldRequestGen.next
    val request = Post(uri, holdRequest) ~> defaultHeaders

    stub(backend.orderService.hold(_: CustomerId, _: OrderId, _: HoldRequest)(_: RequestContext)) {
      case (`customerId`, `orderId`, `holdRequest`, `operator`) =>
        Future.failed(ArtificialInternalException())
    }
    "respond InternalServerError in case of backend problems" in {
      request ~> route ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    "accept hold request" in {
      stub(backend.orderService.hold(_: CustomerId, _: OrderId, _: HoldRequest)(_: RequestContext)) {
        case (`customerId`, `orderId`, `holdRequest`, `operator`) =>
          Future.successful(HoldResponse.Ok(holdRequest, Some(DateTimeUtils.now())))
      }

      request ~> route ~> check {
        status should be(StatusCodes.OK)
        val body = responseAs[HoldRequest]
        body should be(holdRequest)
      }
    }

    "reject hold request in case of conflict" in {
      stub(backend.orderService.hold(_: CustomerId, _: OrderId, _: HoldRequest)(_: RequestContext)) {
        case (`customerId`, `orderId`, `holdRequest`, `operator`) =>
          Future.successful(HoldResponse.AlreadyExists(holdRequest, Some(DateTimeUtils.now())))
      }
      request ~> route ~> check {
        status should be(StatusCodes.Conflict)
        val body = responseAs[HoldRequest]
        body should be(holdRequest)
      }
    }

    "reject hold request in case of insufficient funds" in {
      stub(backend.orderService.hold(_: CustomerId, _: OrderId, _: HoldRequest)(_: RequestContext)) {
        case (`customerId`, `orderId`, `holdRequest`, `operator`) =>
          Future.successful(HoldResponse.NoEnoughFunds(holdRequest))
      }
      request ~> route ~> check {
        status should be(StatusCodes.PaymentRequired)
        val body = responseAs[HoldRequest]
        body should be(holdRequest)
      }
    }
  }
}
