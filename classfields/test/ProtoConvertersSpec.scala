package auto.dealers.booking.converters.test

import java.time.OffsetDateTime

import ru.auto.api.api_offer_model.Category
import ru.auto.api.price_model.RublePrice
import auto.common.pagination.{RequestPagination, ResponsePagination}
import ru.auto.booking.common_model.BookingStatus
import auto.dealers.booking.converters.ProtoConverters._
import auto.dealers.booking.model.{Booking, BookingListing}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object ProtoConvertersSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("ProtoConverters")(
      test("listBookingsResponse should return converted bookings") {
        // сложно протестировать внутренности прямо здесь, toProto(booking) полноценно тестируется ниже
        assert(listBookingsResponse(listing, requestPagination).bookings)(equalTo(apiBookings))
      },
      test("listBookingsResponse should return pagination") {
        assert(listBookingsResponse(listing, requestPagination).pagination)(
          isSome(
            equalTo(
              ResponsePagination(pageNum = 4, pageSize = 5, totalCount = 18, totalPageCount = 4)
            )
          )
        )
      },
      test("toProto(booking) should return code as is") {
        assert(toProto(booking).code)(equalTo(code.toLong))
      },
      test("toProto(booking) should return user id as is") {
        assert(toProto(booking).user.map(_.id))(isSome(equalTo(userId)))
      },
      test("toProto(booking) should return offer id as is") {
        assert(toProto(booking).offerData.offerId.map(_.id))(isSome(equalTo(offerId)))
      },
      test("toProto(booking) should return offer category as is") {
        assert(toProto(booking).offerData.offerId.map(_.category))(isSome(equalTo(offerCategory)))
      },
      // id оффера будет конвертироваться в оффер в public-api
      test("toProto(booking) should not return offer") {
        assert(toProto(booking).offerData.offer)(isNone)
      },
      test("toProto(booking) should return createdAt") {
        assert(toProto(booking).createdAt.map(_.seconds))(isSome(equalTo(createdAt.toEpochSecond)))
      },
      test("toProto(booking) should return paidAt") {
        assert(toProto(booking).paidAt.map(_.seconds))(isSome(equalTo(paidAt.toEpochSecond)))
      },
      test("toProto(booking) should return validUntil") {
        assert(toProto(booking).validUntil.map(_.seconds))(isSome(equalTo(validUntil.toEpochSecond)))
      },
      test("toProto(booking) should return status") {
        assert(toProto(booking).status)(equalTo(status))
      },
      test("toProto(booking) should return offerPriceWhenBooked") {
        assert(toProto(booking).offerPriceWhenBooked)(isSome(equalTo(RublePrice(offerPriceWhenBooked))))
      },
      test("responsePagination should return empty pagination on totalCount = 0") {
        val requestPagination = RequestPagination(page = 1, pageSize = 10)
        val totalCount = 0
        assert(responsePagination(requestPagination, totalCount))(
          equalTo(
            ResponsePagination(pageNum = 1, pageSize = 10, totalCount = 0, totalPageCount = 0)
          )
        )
      },
      test("responsePagination should return page 4 of 8") {
        val requestPagination = RequestPagination(page = 4, pageSize = 5)
        val totalCount = 39
        assert(responsePagination(requestPagination, totalCount))(
          equalTo(
            ResponsePagination(pageNum = 4, pageSize = 5, totalCount = 39, totalPageCount = 8)
          )
        )
      },
      test("responsePagination should return 8 pages if there are 8 full pages") {
        val requestPagination = RequestPagination(page = 4, pageSize = 5)
        val totalCount = 40
        assert(responsePagination(requestPagination, totalCount))(
          equalTo(
            ResponsePagination(pageNum = 4, pageSize = 5, totalCount = 40, totalPageCount = 8)
          )
        )
      }
    )
  }

  private val id = 5
  private val code = 868323
  private val status = BookingStatus.CONFIRMED
  private val createdAt = OffsetDateTime.parse("2020-06-05T08:00:00+03:00")
  private val validUntil = OffsetDateTime.parse("2020-06-10T23:59:59+03:00")
  private val userId = "user:33158932"
  private val userFullName = "Иван Петров"
  private val userPhone = "+79161234567"
  private val offerId = "123458892-def8"
  private val offerCategory = Category.CARS
  private val dealerId = "dealer:16453"
  private val offerPriceWhenBooked = 1500000
  private val vin = "12345678901243FJIOABC"
  private val paymentCost = 1500000
  private val paymentRefunded = true
  private val bookingTransactionId = "1243-56748"
  private val bankerTransactionId = "473824-437289"
  private val paidAt = OffsetDateTime.parse("2020-06-05T08:03:00+03:00")

  private val booking = Booking(
    id,
    code,
    status,
    createdAt,
    validUntil,
    userId,
    userFullName,
    userPhone,
    offerId,
    offerCategory,
    dealerId,
    offerPriceWhenBooked,
    Some(vin),
    paymentCost,
    paymentRefunded,
    bookingTransactionId,
    Some(bankerTransactionId),
    Some(paidAt)
  )

  private val bookings = Seq(
    booking,
    booking.copy(code = booking.code + 1),
    booking.copy(code = booking.code + 2)
  )

  private val apiBookings = Seq(
    toProto(booking),
    toProto(booking.copy(code = booking.code + 1)),
    toProto(booking.copy(code = booking.code + 2))
  )

  private val totalCount = 18

  private val listing = BookingListing(bookings, totalCount)

  private val requestPagination = RequestPagination(page = 4, pageSize = 5)
}
