package auto.dealers.booking.scheduler

import java.time.OffsetDateTime

import com.google.protobuf.timestamp.Timestamp
import ru.auto.api.api_offer_model.Category
import ru.auto.api.price_model.{KopeckPrice, RublePrice}
import auto.dealers.booking.api.DefaultBookingService
import ru.auto.booking.broker.broker_model._
import ru.auto.booking.common_model.BookingStatus
import ru.auto.booking.common_model.BookingStatus._
import auto.dealers.booking.model.{BankerTransactionId, BookingCode, CreateBooking, UserId}
import auto.dealers.booking.scheduler.BookingRefundService._
import auto.dealers.booking.scheduler.BookingRefundServiceLive._
import auto.dealers.booking.scheduler.TestBanker.amountRefunded
import auto.dealers.booking.storage.BookingDao
import auto.dealers.booking.storage.memory.InMemoryBookingDao
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._
import zio.test.environment.TestEnvironment

object BookingRefundServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("BookingRefundService")(
      testM("fail on event without currentState") {
        val event = BookingChangeEvent()
        assertM(refundPaymentIfTerminal(event).either)(isParsingError)
      },
      testM("fail on event without user id") {
        val event = BookingChangeEvent(currentState = Some(Booking()))
        assertM(refundPaymentIfTerminal(event).either)(isParsingError)
      },
      testM("ignore if current status isn't terminal") {
        val event = eventOfPaidBooking(Some(PAID) -> CONFIRMED)
        val refunded = refundPaymentIfTerminal(event) *> amountRefunded(userId, bankerTransactionId)
        assertM(refunded)(equalTo(KopeckPrice(0)))
      },
      testM("ignore if booking isn't paid") {
        val event = eventOfUnpaidBooking(Some(NEED_PAYMENT) -> EXPIRED)
        val refunded = refundPaymentIfTerminal(event) *> amountRefunded(userId, bankerTransactionId)
        assertM(refunded)(equalTo(KopeckPrice(0)))
      },
      testM("refund payment in terminal state which isn't refunded yet") {
        for {
          _ <-
            BookingDao.createBooking(createBooking) *>
              BookingDao.savePaymentInfo(paidAt, bookingTransactionId, bankerTransactionId.raw) *>
              BookingDao
                .updateBookingStatus(
                  dealerId,
                  BookingCode(code),
                  DefaultBookingService.allowedStatusFrom(CONFIRMED),
                  toStatus = CONFIRMED,
                  dropBookedOfferId = false
                ) *>
              BookingDao.updateBookingStatus(
                dealerId,
                BookingCode(code),
                DefaultBookingService.allowedStatusFrom(CLOSED),
                toStatus = CLOSED,
                dropBookedOfferId = false
              )
          event = eventOfPaidBooking(Some(CONFIRMED) -> CLOSED)
          refunded <- refundPaymentIfTerminal(event) *> amountRefunded(userId, bankerTransactionId)
        } yield assert(refunded)(equalTo(KopeckPrice(1500000)))
      } @@ sequential
    ).provideCustomLayer(InMemoryBookingDao.live >+> TestBanker.live >+> BookingRefundService.live)

  private def isParsingError[A] =
    assertion[Either[Throwable, A]]("isParsingError")() {
      case Left(_: RefundableBookingParsingError) => true
      case _ => false
    }

  private val bankerTransactionId = BankerTransactionId("banker:12345-78943")
  private val code = 43725892
  private val dealerId = "dealer:12345"
  private val rawUserId = "user:47838412"
  private val userId = UserId(rawUserId)

  private val createdAt = OffsetDateTime.parse("2020-05-28T13:59:00+03:00")
  private val validUntil = OffsetDateTime.parse("2020-06-03T23:59:59+03:00")
  private val paidAt = OffsetDateTime.parse("2020-05-28T14:00:00+03:00")
  private val bookingTransactionId = "some-uuid"

  private val doesntMatter = "doesn't matter"

  private val createBooking = CreateBooking(
    code,
    status = NEED_PAYMENT,
    createdAt,
    validUntil,
    rawUserId,
    userFullName = doesntMatter,
    userPhone = doesntMatter,
    offerId = doesntMatter,
    offerCategory = Category.CARS,
    dealerId,
    offerPriceWhenBooked = 500,
    vin = Some(doesntMatter),
    paymentCost = 1500000,
    bookingTransactionId
  )

  private def unpaidState(status: BookingStatus) =
    Booking(
      code = code,
      user = Some(BookingUser(id = rawUserId)),
      status = status
    )

  private def paidState(status: BookingStatus) =
    unpaidState(status).copy(
      paidAt = Some(Timestamp.of(seconds = paidAt.toEpochSecond, nanos = 0)),
      payment = Some(BookingPayment(cost = Some(RublePrice(15000)), refunded = false))
    )

  private def eventOfUnpaidBooking(statusChange: (Option[BookingStatus], BookingStatus)) =
    eventOfBooking(statusChange, unpaidState)

  private def eventOfPaidBooking(statusChange: (Option[BookingStatus], BookingStatus)) =
    eventOfBooking(statusChange, paidState)

  private def eventOfBooking(statusChange: (Option[BookingStatus], BookingStatus), f: BookingStatus => Booking) =
    statusChange match {
      case (previousStatus, currentStatus) =>
        BookingChangeEvent(
          previousState = previousStatus.map(f),
          currentState = Some(f(currentStatus))
        )
    }
}
