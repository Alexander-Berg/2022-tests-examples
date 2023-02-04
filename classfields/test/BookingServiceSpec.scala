package auto.dealers.booking.api

import java.time.{Instant, OffsetDateTime, ZoneId, _}
import java.util.UUID

import com.google.protobuf.timestamp.Timestamp
import ru.auto.api.api_offer_model.AdditionalInfo.Booking.State
import ru.auto.api.api_offer_model.{AdditionalInfo, Category, Documents, Offer}
import ru.auto.api.common_model.PriceInfo
import ru.auto.api.price_model.KopeckPrice
import auto.common.pagination.RequestPagination
import auto.dealers.booking.api.DefaultBookingService.BookingNotAllowed
import ru.auto.booking.api.api_model._
import ru.auto.booking.common_model.BookingStatus
import auto.dealers.booking.storage.BookingDao
import auto.dealers.booking.storage.memory.InMemoryBookingDao
import auto.dealers.booking.api.uuid.TestUUID
import auto.dealers.booking.api.{BookingService, DefaultBookingService, InvalidPagination}
import zio.test.Assertion._
import zio.test.TestAspect.{after, sequential}
import zio.test._
import zio.test.environment.{TestClock, TestRandom}

object BookingServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("BookingService")(
      suite("BookingService")(
        test("compute booking deadline") {
          val start = ZonedDateTime.of(2020, 5, 31, 13, 41, 11, 0, ZoneOffset.ofHours(3))
          assert(DefaultBookingService.bookingDeadline(start))(
            equalTo(ZonedDateTime.of(2020, 6, 3, 23, 59, 59, 0, ZoneOffset.ofHours(3)))
          )
        }
      ),
      testM("respond hard-coded values") {
        for {
          _ <- TestClock.setDateTime(
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(0), ZoneId.of("Europe/Moscow"))
          )
          res <- BookingService.getBookingTerms
        } yield assert(res)(
          equalTo(
            GetBookingTermsResponse(
              Some(KopeckPrice(1000000)),
              Some(Timestamp(4 * 24 * 60 * 60 - 1 - moscowSecondsGap, 0))
            )
          )
        )
      },
      testM("listBookings should fail on negative page number") {
        val pagination = RequestPagination(page = -1, pageSize = 10)
        val request = ListBookingsRequest(dealerId, Some(pagination))
        assertM(BookingService.listBookings(request).either)(isInvalidPagination)
      },
      testM("listBookings should fail on negative page size") {
        val pagination = RequestPagination(page = 1, pageSize = -10)
        val request = ListBookingsRequest(dealerId, Some(pagination))
        assertM(BookingService.listBookings(request).either)(isInvalidPagination)
      },
      testM("listBookings should fail on missing pagination") {
        val request = ListBookingsRequest(dealerId, None)
        assertM(BookingService.listBookings(request).either)(isInvalidPagination)
      },
      testM("create booking") {
        val bookingTransactionId = new UUID(1, 1)
        for {
          _ <- TestUUID.feedUUIDs(bookingTransactionId)
          b <- BookingService.createBooking(CreateBookingRequest(Some(bookingUser), Some(offer())))
        } yield assert(b)(equalTo(CreateBookingResponse(bookingTransactionId.toString, Some(KopeckPrice(1000000)))))
      },
      testM("not create booking if not allowed") {
        val bookingTransactionId = new UUID(1, 1)
        val createBookingReq =
          CreateBookingRequest(Some(bookingUser), Some(offer(AdditionalInfo.Booking(allowed = false))))
        for {
          _ <- TestUUID.feedUUIDs(bookingTransactionId)
          b <- BookingService.createBooking(createBookingReq).run
        } yield assert(b)(fails(isSubtype[BookingNotAllowed](anything)))
      },
      testM("not create booking on booked state") {
        val bookingTransactionId = new UUID(1, 1)

        val booked = State.Booked()
        val state: State = State(state = State.State.Booked(value = booked))
        val booking = AdditionalInfo.Booking(allowed = true, state = Some(state))

        val createBookingReq = CreateBookingRequest(Some(bookingUser), Some(offer(booking)))
        assertM((for {
          _ <- TestUUID.feedUUIDs(bookingTransactionId)
          b <- BookingService.createBooking(createBookingReq)
        } yield b).run)(fails(isSubtype[BookingNotAllowed](anything)))
      },
      testM("retry on code collision") {
        val bookingTransactionId = new UUID(1, 3)
        for {
          _ <- TestUUID.feedUUIDs(new UUID(1, 1), new UUID(1, 2), bookingTransactionId)
          _ <- TestRandom.feedInts(1, 1, 2)
          _ <- BookingService.createBooking(CreateBookingRequest(Some(bookingUser), Some(offer())))
          b <- BookingService.createBooking(CreateBookingRequest(Some(bookingUser), Some(offer())))
        } yield assert(b)(equalTo(CreateBookingResponse(bookingTransactionId.toString, Some(KopeckPrice(1000000)))))
      },
      testM("retry on booking_transaction_id collision") {
        val bookingTransactionId = new UUID(1, 1)
        val nextBookingTransactionId = new UUID(1, 2)
        for {
          _ <- TestUUID.feedUUIDs(bookingTransactionId, bookingTransactionId, nextBookingTransactionId)
          _ <- BookingService.createBooking(CreateBookingRequest(Some(bookingUser), Some(offer())))
          b <- BookingService.createBooking(CreateBookingRequest(Some(bookingUser), Some(offer())))
        } yield assert(b)(equalTo(CreateBookingResponse(nextBookingTransactionId.toString, Some(KopeckPrice(1000000)))))
      },
      testM("change booking status") {
        for {
          _ <- TestUUID.feedUUIDs(new UUID(1, 1))
          createResponse <- BookingService.createBooking(CreateBookingRequest(Some(bookingUser), Some(offer())))
          _ <- BookingDao.savePaymentInfo(OffsetDateTime.now(), createResponse.bookingTransactionId, "b:id")
          saved <- BookingDao.getBooking(createResponse.bookingTransactionId)
          _ <- BookingService.updateBookingStatus(
            UpdateBookingStatusRequest(saved.dealerId, saved.code, BookingStatus.CONFIRMED)
          )
          updated <- BookingDao.getBooking(createResponse.bookingTransactionId)
        } yield assert(saved.status)(equalTo(BookingStatus.PAID)) &&
          assert(updated.status)(equalTo(BookingStatus.CONFIRMED))
      }
    ) @@ after(InMemoryBookingDao.clean) @@ sequential
  }.provideCustomLayer(TestUUID.test ++ InMemoryBookingDao.live ++ (InMemoryBookingDao.live >>> BookingService.test))

  private val moscowSecondsGap = 3 * 60 * 60

  private val dealerId = "dealer:20101"

  private val bookingUser = BookingUser("123", "+79193334422", "Anton Antonovich Antonov")

  private def offer(
      booking: AdditionalInfo.Booking = AdditionalInfo.Booking(allowed = true)) =
    Offer(
      id = "offer_id",
      category = Category.CARS,
      userRef = "dealer:20101",
      priceInfo = Some(PriceInfo(rurPrice = 100.0f)),
      documents = Some(Documents(vin = "abcd1234"))
    ).withAdditionalInfo(AdditionalInfo().withBooking(booking))

  private def isInvalidPagination[A] =
    Assertion.assertion[Either[Throwable, A]]("isInvalidPagination")() {
      case Left(_: InvalidPagination) => true
      case _ => false
    }
}
