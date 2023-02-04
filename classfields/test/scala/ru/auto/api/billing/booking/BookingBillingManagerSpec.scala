package ru.auto.api.billing.booking

import org.mockito.ArgumentMatcher
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{reset, verify}
import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.PriceModel.KopeckPrice
import ru.auto.api.managers.offers.OfferLoader
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.BankerModelGenerators._
import ru.auto.api.model.gen.BookingModelGenerators.initBookingPaymentRequestGen
import ru.auto.api.model.{AutoruUser, CategorySelector, RequestParams}
import ru.auto.api.services.billing.BankerClient
import ru.auto.api.services.booking.BookingClient
import ru.auto.api.services.keys.TokenServiceImpl
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.auto.booking.api.ApiModel.{BookingUser, CreateBookingRequest, CreateBookingResponse, GetBookingCostRequest, GetBookingCostResponse}
import ru.yandex.vertis.banker.model.ApiModel.{PaymentRequest, Target}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

class BookingBillingManagerSpec extends BaseSpec with MockitoSupport {

  private val offerLoader = mock[OfferLoader]
  private val bookingClient = mock[BookingClient]
  private val bankerClient = mock[BankerClient]

  private val bookingTransactionId = readableString.next
  private val rawBookingPrice = 1000000
  private val bookingPrice = KopeckPrice.newBuilder().setKopecks(rawBookingPrice).build()

  private val createBookingResponse = CreateBookingResponse
    .newBuilder()
    .setBookingTransactionId(bookingTransactionId)
    .setPrice(bookingPrice)
    .build()
  private val getBookingCostResponse = GetBookingCostResponse.newBuilder().setBookingCost(bookingPrice).build()
  private val paymentMethods = Gen.nonEmptyListOf(PaymentMethodGen).next

  private val offerId = OfferIDGen.next
  private val offerCategory = Category.CARS

  private val categorizedOfferId =
    ApiOfferModel.OfferId.newBuilder().setId(offerId.toPlain).setCategory(offerCategory).build()

  private val offer =
    offerGen(category = CategorySelector.Cars, id = offerId.toPlain, userRefGen = DealerUserRefGen).next

  private val phone = "+79161234567"
  private val fullName = "Иван Петров"

  private val bookingUser =
    BookingUser.newBuilder().setPhone(phone).setFullName(fullName).build()

  private val initBookingPaymentRequest =
    initBookingPaymentRequestGen(categorizedOfferId, bookingUser).next

  private val bookingUserId = "user:33808912"
  private val bookingAutoruUser = AutoruUser(33808912)

  implicit private val request: Request = {
    for {
      token <- TestTokenGen
      deviceUid <- DeviceUidGen
    } yield {
      val request = new RequestImpl
      request.setTrace(Traced.empty)
      request.setRequestParams(RequestParams.construct("1.1.1.1"))
      request.setToken(token)
      request.setNewDeviceUid(deviceUid)
      request.setUser(bookingAutoruUser)
      TokenServiceImpl.getStaticApplication(token).foreach(request.setApplication)
      request
    }
  }.next

  before {
    reset(offerLoader, bookingClient, bankerClient)
    when(offerLoader.findRawOffer(?, ?, ?, ?)(?)).thenReturnF(offer)
    when(bookingClient.createBooking(?)(?)).thenReturnF(createBookingResponse)
    when(bookingClient.getBookingCost(?)(?)).thenReturnF(getBookingCostResponse)
    when(bankerClient.requestPaymentMethods(?, ?)(?)).thenReturnF(paymentMethods)
  }

  private val manager = new BookingBillingManager(offerLoader, bookingClient, bankerClient)

  private def initPayment() =
    manager.initPayment(initBookingPaymentRequest).futureValue

  private def verifyCreateBookingRequest(matcher: ArgumentMatcher[CreateBookingRequest]) =
    verify(bookingClient).createBooking(argThat(matcher))(eq(request))

  private def verifyPaymentMethodsRequest(matcher: ArgumentMatcher[PaymentRequest.Source]) =
    verify(bankerClient).requestPaymentMethods(?, argThat(matcher))(eq(request))

  "BookingBillingManager.initPayment" should {

    "request searcher/vos for offer with id and category from request" in {
      initPayment()
      verify(offerLoader).findRawOffer(CategorySelector.Cars, offerId)
    }

    "create booking for offer from searcher/vos" in {
      initPayment()
      verifyCreateBookingRequest(_.getOffer == offer)
    }

    "create booking with user id from session" in {
      initPayment()
      verifyCreateBookingRequest(_.getUser.getId == bookingUserId)
    }

    "create booking with user phone from request" in {
      initPayment()
      verifyCreateBookingRequest(_.getUser.getPhone == phone)
    }

    "create booking with user full name from request" in {
      initPayment()
      verifyCreateBookingRequest(_.getUser.getFullName == fullName)
    }

    "request payment methods for user from session (to show this user's payment methods, not another one's)" in {
      initPayment()
      verify(bankerClient).requestPaymentMethods(eq(bookingAutoruUser), ?)(eq(request))
    }

    // Следующие тесты "pass any..." проверяют, что мы передаём какое-то значение в поле в банкир.
    // Однако фактически ответ банкира не зависит от того, что именно мы передадим.
    // При этом, если не передать ничего, банкир ответит 400.
    "pass any user account while requesting payment methods" in {
      initPayment()
      verifyPaymentMethodsRequest(_.getAccount.nonEmpty)
    }

    "pass any amount while requesting payment methods" in {
      initPayment()
      verifyPaymentMethodsRequest(_.getAmount > 0)
    }

    "pass any payload while requesting payment methods" in {
      initPayment()
      verifyPaymentMethodsRequest(_.hasPayload)
    }

    "pass any options while requesting payment methods" in {
      initPayment()
      verifyPaymentMethodsRequest(_.hasOptions)
    }

    // Чтобы банкир не кидал 400 с "Empty goods" (goods -- объект внутри receipt).
    "not pass any receipt while requesting payment methods" in {
      initPayment()
      verifyPaymentMethodsRequest(!_.hasReceipt)
    }

    // Чтобы банкир ответил только refundable методами, подходящими для бронирования.
    "request payment methods for target = SECURITY_DEPOSIT" in {
      initPayment()
      verifyPaymentMethodsRequest(_.getContext.getTarget == Target.SECURITY_DEPOSIT)
    }

    // Чтобы банкир ответил всеми подходящими для бронирования refundable методами,
    // а не только отфильтрованными в pay gate context.
    "request payment methods without pay gate context" in {
      initPayment()
      verifyPaymentMethodsRequest(!_.hasPayGateContext)
    }

    // Чтобы фронт мог отправить booking_transaction_id в /payment/process
    "respond with ticketId = bookingTransactionId" in {
      initPayment().getTicketId shouldBe bookingTransactionId
    }

    // Чтобы фронт показал корректную цену бронирования в попапе оплаты
    "respond with cost = bookingPrice" in {
      initPayment().getCost shouldBe rawBookingPrice
    }

    // Явно показываем, что скидок нет
    "respond with baseCost = bookingPrice" in {
      initPayment().getBaseCost shouldBe rawBookingPrice
    }

    "respond with salesmanDomain = booking" in {
      initPayment().getSalesmanDomain shouldBe "booking"
    }

    // Фронт не показывает баланс кошелька, т.к. с кошелька нельзя оплатить бронирование
    "not respond with accountBalance" in {
      // фактически hasAccountBalance shouldBe false
      initPayment().getAccountBalance shouldBe 0
    }

    "respond with license agreement" in {
      initPayment().getOfferUrl shouldBe "https://yandex.ru/legal/autoru_licenseagreement"
    }

    // Чтобы фронт показал доступные методы оплаты в попапе оплаты
    "respond with payment methods" in {
      (initPayment().getPaymentMethodsList should contain).theSameElementsInOrderAs(paymentMethods)
    }

    "respond with service and name" in {
      val payment = initPayment()
      payment.getDetailedProductInfosCount shouldBe 1
      val info = payment.getDetailedProductInfos(0)

      info.getService shouldBe "booking"
      info.getName shouldBe "Бронирование автомобиля"
    }
  }

  "BookingBillingManager.getPaymentCost" should {

    "request booking-api with requested bookingTransactionId" in {
      manager.getPaymentCost(bookingTransactionId).futureValue
      verify(bookingClient).getBookingCost(
        argThat[GetBookingCostRequest](_.getBookingTransactionId == bookingTransactionId)
      )(?)
    }

    "respond with cost from booking-api" in {
      manager.getPaymentCost(bookingTransactionId).futureValue.getKopecks shouldBe rawBookingPrice
    }
  }
}
