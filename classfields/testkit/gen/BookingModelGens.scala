package auto.dealers.booking.testkit

import java.time.OffsetDateTime
import auto.dealers.booking.testkit.gen._
import ru.auto.booking.common_model.BookingStatus.NEED_PAYMENT

import java.time.temporal.ChronoUnit

object BookingModelGens {

  import ru.auto.api.api_offer_model.Category
  import ru.auto.booking.common_model.BookingStatus
  import auto.dealers.booking.model.CreateBooking

  val code = 101
  val dealerId = "dealer:20101"

  val dateTime = OffsetDateTime
    .now()
    .truncatedTo(ChronoUnit.MICROS) // JDK15 has nanotime resolution, while PG supports only microseconds

  val createBooking = CreateBooking(
    code = code,
    status = NEED_PAYMENT,
    createdAt = dateTime,
    validUntil = dateTime,
    userId = "user:12345",
    userFullName = "Test Testov",
    userPhone = "+79164352343",
    offerId = "123456-123",
    offerCategory = Category.CARS,
    dealerId = dealerId,
    offerPriceWhenBooked = 200000,
    vin = Some("vin"),
    paymentCost = 200000,
    bookingTransactionId = "transaction:id"
  )

  def anotherBooking(createBooking: CreateBooking) =
    createBooking.copy(
      code = createBooking.code + 1,
      bookingTransactionId = createBooking.bookingTransactionId + "1",
      offerId = createBooking.offerId + "1"
    )

  def createBookingGen(
      codeGen: RGen[Int] = createBooking.code,
      statusGen: RGen[BookingStatus] = createBooking.status,
      createdAtGen: RGen[OffsetDateTime] = createBooking.createdAt,
      validUntilGen: RGen[OffsetDateTime] = createBooking.validUntil,
      userIdGen: RGen[String] = createBooking.userId,
      userFullNameGen: RGen[String] = createBooking.userFullName,
      userPhoneGen: RGen[String] = createBooking.userPhone,
      offerIdGen: RGen[String] = createBooking.offerId,
      offerCategoryGen: RGen[Category] = createBooking.offerCategory,
      dealerIdGen: RGen[String] = createBooking.dealerId,
      offerPriceWhenBookedGen: RGen[Int] = createBooking.offerPriceWhenBooked,
      vinGen: RGen[Option[String]] = createBooking.vin,
      paymentCostGen: RGen[Int] = createBooking.paymentCost,
      bookingTransactionIdGen: RGen[String] = createBooking.bookingTransactionId) =
    for {
      code <- codeGen
      status <- statusGen
      createdAt <- createdAtGen
      validUntil <- validUntilGen
      userId <- userIdGen
      userFullName <- userFullNameGen
      userPhone <- userPhoneGen
      offerId <- offerIdGen
      offerCategory <- offerCategoryGen
      dealerId <- dealerIdGen
      offerPriceWhenBooked <- offerPriceWhenBookedGen
      vin <- vinGen
      paymentCost <- paymentCostGen
      bookingTransactionId <- bookingTransactionIdGen
    } yield CreateBooking(
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
      vin,
      paymentCost,
      bookingTransactionId
    )
}
