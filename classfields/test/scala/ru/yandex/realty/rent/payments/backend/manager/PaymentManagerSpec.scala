package ru.yandex.realty.rent.payments.backend.manager

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.clients.tinkoff.eacq.TinkoffEACQClient
import ru.yandex.realty.clients.tinkoff.eacq.customer.{
  AddCustomerRequest,
  AddCustomerResponse,
  GetCustomerRequest,
  GetCustomerResponse
}
import ru.yandex.realty.clients.tinkoff.eacq.init.{InitRequest, InitResponse}
import ru.yandex.realty.errors.ConflictApiException
import ru.yandex.realty.payments.proto.api.internal.payment_service.InitPayment
import ru.yandex.realty.rent.payments.DaoEventInitialization
import ru.yandex.realty.rent.payments.dao.EventDao
import ru.yandex.realty.rent.payments.model.Event
import ru.yandex.realty.rent.payments.proto.model.event.{CreateOrderEvent, CreateOrderItemEvent}
import ru.yandex.realty.rent.proto.api.common.platform_type.PlatformNamespace
import ru.yandex.realty.rent.proto.api.payment.payment_method_type.PaymentMethodNamespace
import ru.yandex.realty.rent.proto.api.payment.transaction.InitiatedTransactionInfo
import ru.yandex.realty.tracing.Traced

import scala.collection.immutable
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PaymentManagerSpec extends DaoEventInitialization with ScalaFutures with Matchers {

  "PaymentManager init payment" should {
    "with new init full payment event for existing customer" in new InitPayment {
      mockExistingCustomer()
      mockInit(Some(paymentUrl), Some(orderId))
      val event: CreateOrderEvent = createRentOrderEvent()
      val item: CreateOrderItemEvent = moneyTransferEvent()
      val amount: Long = item.amount
      createOrder(createOrderEvent = event, itemEvents = Seq(item))
      val initResponse: InitPayment.Response = paymentManager.initPayment(request).futureValue
      checkInitPaymentResponse(amount, initResponse)
      val Seq(initPayment, _, _) = getEvents()
      checkInitPaymentEvent(amount, initPayment)
    }

    "with new init full payment event for not existing customer" in new InitPayment {
      mockNotExistingCustomer()
      mockAddCustomer()
      mockInit(Some(paymentUrl), Some(orderId))
      val event: CreateOrderEvent = createRentOrderEvent()
      val item: CreateOrderItemEvent = moneyTransferEvent()
      val amount: Long = item.amount
      createOrder(createOrderEvent = event, itemEvents = Seq(item))
      val initResponse: InitPayment.Response = paymentManager.initPayment(request).futureValue
      checkInitPaymentResponse(amount, initResponse)
      val Seq(initPayment, _, _) = getEvents()
      checkInitPaymentEvent(amount, initPayment)
    }

    "fail because order is declined" in new InitPayment {
      mockExistingCustomer()
      val event: CreateOrderEvent = createRentOrderEvent()
      val item: CreateOrderItemEvent = moneyTransferEvent()
      createOrder(createOrderEvent = event, itemEvents = Seq(item))
      declineOrder()
      val events: immutable.Seq[Event] = getEvents()
      interceptCause[ConflictApiException] {
        paymentManager.initPayment(request).futureValue
      }
      getEvents() shouldBe events
    }

    "without new events because failed init response" in new InitPayment {
      mockExistingCustomer()
      mockInit(Some(paymentUrl), Some(orderId), success = false)
      val event: CreateOrderEvent = createRentOrderEvent()
      val item: CreateOrderItemEvent = moneyTransferEvent()
      createOrder(createOrderEvent = event, itemEvents = Seq(item))
      val events: Seq[Event] = getEvents()
      interceptCause[IllegalStateException] {
        paymentManager.initPayment(request).futureValue
      }
      getEvents() shouldBe events
    }

    "without new events because empty paymentId in the init response" in new InitPayment {
      mockExistingCustomer()
      mockInit(Some(paymentUrl), orderId = None)
      val event: CreateOrderEvent = createRentOrderEvent()
      val item: CreateOrderItemEvent = moneyTransferEvent()
      createOrder(createOrderEvent = event, itemEvents = Seq(item))
      val events: Seq[Event] = getEvents()
      interceptCause[IllegalStateException] {
        paymentManager.initPayment(request).futureValue
      }
      getEvents() shouldBe events
    }

    "without new events because empty paymentUrl in the init response for desktop" in new InitPayment {
      mockExistingCustomer()
      mockInit(paymentUrl = None, Some(orderId))
      val desktop: PlatformNamespace.Platform = PlatformNamespace.Platform.DESKTOP
      val event: CreateOrderEvent = createRentOrderEvent()
      val item: CreateOrderItemEvent = moneyTransferEvent()
      createOrder(createOrderEvent = event, itemEvents = Seq(item))
      val events: Seq[Event] = getEvents()
      interceptCause[IllegalStateException] {
        paymentManager.initPayment(request.copy(platform = desktop)).futureValue
      }
      getEvents() shouldBe events
    }

    "with new init full payment event for mobile with empty paymentUrl" in new InitPayment {
      mockExistingCustomer()
      mockInit(paymentUrl = None, Some(orderId))
      val event: CreateOrderEvent = createRentOrderEvent()
      val item: CreateOrderItemEvent = moneyTransferEvent()
      val amount: Long = item.amount
      val ios: PlatformNamespace.Platform = PlatformNamespace.Platform.IOS
      createOrder(createOrderEvent = event, itemEvents = Seq(item))
      val initResponse: InitPayment.Response = paymentManager.initPayment(request.copy(platform = ios)).futureValue
      checkInitPaymentResponse(amount, initResponse, paymentUrl = "")
      val Seq(initPayment, _, _) = getEvents()
      checkInitPaymentEvent(amount, initPayment, platform = ios, paymentUrl = "")
    }

  }

  trait InitPayment {
    val platform: PlatformNamespace.Platform = Gen.oneOf(PlatformNamespace.Platform.values).next

    val paymentMethod: PaymentMethodNamespace.PaymentMethod =
      Gen.oneOf(PaymentMethodNamespace.PaymentMethod.values).next

    val userId: String = readableString.next
    val notificationUrl: String = readableString.next
    val paymentUrl: String = readableString.next
    val successUrl: String = readableString.next
    val failUrl: String = readableString.next
    lazy val eventDao = new EventDao
    val mockTinkoffClient: TinkoffEACQClient = mock[TinkoffEACQClient]
    lazy val paymentManager: PaymentManager =
      new PaymentManager(doobieDatabase, eventDao, notificationUrl, mockTinkoffClient, mockTinkoffClient)

    val request: InitPayment.Request = InitPayment.Request(
      orderId = orderId,
      userId = userId,
      redirectDueDate = None,
      successUrl = successUrl,
      failUrl = failUrl,
      platform = platform,
      paymentMethod = paymentMethod
    )

    protected def mockExistingCustomer() =
      (mockTinkoffClient
        .getCustomer(_: GetCustomerRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(GetCustomerResponse(Success = true, "")))

    protected def mockAddCustomer() =
      (mockTinkoffClient
        .addCustomer(_: AddCustomerRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(AddCustomerResponse(Success = true, "")))

    protected def mockNotExistingCustomer() =
      (mockTinkoffClient
        .getCustomer(_: GetCustomerRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(GetCustomerResponse(Success = false, "")))

    protected def mockInit(
      paymentUrl: Option[String],
      orderId: Option[String],
      success: Boolean = true
    ) =
      (mockTinkoffClient
        .init(_: InitRequest)(_: Traced))
        .expects(*, *)
        .once()
        .returning(Future.successful(InitResponse(Success = success, "", PaymentURL = paymentUrl, PaymentId = orderId)))

    protected def checkInitPaymentResponse(
      amount: Long,
      initResponse: InitPayment.Response,
      paymentUrl: String = paymentUrl
    ): Unit = {
      initResponse.paymentUrl shouldBe paymentUrl
      initResponse.transactionInfo.nonEmpty shouldBe true
      val transactionInfo: InitiatedTransactionInfo = initResponse.transactionInfo.get
      transactionInfo.amount shouldBe amount
      transactionInfo.tinkoffPaymentId shouldBe orderId
      transactionInfo.tinkoffOrderId shouldBe orderId
      transactionInfo.tinkoffCustomerKey shouldBe userId
    }

    protected def checkInitPaymentEvent(
      amount: Long,
      event: Event,
      paymentUrl: String = paymentUrl,
      platform: PlatformNamespace.Platform = platform,
      num: Int = 0
    ): Unit = {
      event.orderId shouldBe orderId
      event.entityId shouldBe entityId
      val initPaymentEvent = event.data.getInitPaymentEvent
      initPaymentEvent.userId shouldBe userId
      initPaymentEvent.amount shouldBe amount
      initPaymentEvent.paymentUrl shouldBe paymentUrl
      initPaymentEvent.paymentMethod shouldBe paymentMethod
      initPaymentEvent.platform shouldBe platform
      event.idempotencyKey shouldBe s"init_payment_$num"
    }
  }
}
