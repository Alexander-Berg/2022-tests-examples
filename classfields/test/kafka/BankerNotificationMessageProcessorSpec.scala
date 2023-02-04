package auto.dealers.booking.scheduler.kafka

import java.time.Instant

import auto.dealers.booking.scheduler.kafka.processors.BankerNotificationMessageProcessor
import auto.dealers.booking.storage.BookingDao
import auto.dealers.booking.storage.BookingDao.BookingNotFound
import auto.dealers.booking.storage.memory.InMemoryBookingDao
import auto.dealers.booking.testkit.BankingModelGens._
import auto.dealers.booking.testkit.BookingModelGens._
import auto.dealers.booking.testkit.gen._
import zio.test.Assertion._
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestEnvironment

object BankerNotificationMessageProcessorSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {

    (suite("Banker notifications processing")(
      testM("save booking purchase dt and banker transaction id") {
        val expectedDate = "2020-06-03T12:35:00Z"
        val bookingTransactionId = "fixed_booking_id"
        val bankerTransactionId = "fixed_banker_id"
        checkNM(1)(
          paymentNotificationGen(
            paidAtGen = expectedDate,
            bookingTransactionId = bookingTransactionId,
            bankerTransactionId = bankerTransactionId
          )(),
          createBookingGen(bookingTransactionIdGen = bookingTransactionId, codeGen = 101)
        ) { (notification, createBooking) =>
          for {
            initialBooking <- BookingDao.createBooking(createBooking)
            _ <- BankerNotificationMessageProcessor.processMessage(notification)
            updatedBooking <- BookingDao.getBooking(initialBooking.bookingTransactionId)
          } yield {
            assert(updatedBooking.paidAt.map(_.toInstant))(isSome(equalTo(Instant.parse(expectedDate)))) &&
            assert(updatedBooking.bankerTransactionId)(isSome(equalTo(bankerTransactionId)))
          }
        }
      },
      testM("fail with unknown bookingTransactionId") {
        val bookingTransactionId = "fixed_booking_id_2"
        checkNM(1)(paymentNotificationGen(bookingTransactionId = bookingTransactionId)()) { notification =>
          for {
            result <- BankerNotificationMessageProcessor.processMessage(notification).run
          } yield assert(result)(fails(isSubtype[BookingNotFound](Assertion.anything)))
        }
      },
      testM("filter string payload") {
        val bookingTransactionId = "fixed_booking_id_3"
        val expectedDate = "2020-06-03T12:35:00Z"
        checkNM(1)(
          paymentNotificationGen(paidAtGen = expectedDate, bookingTransactionId = bookingTransactionId)(
            stringPayloadGen
          ),
          createBookingGen(bookingTransactionIdGen = bookingTransactionId, codeGen = 103)
        ) { (notification, createBooking) =>
          for {
            initialBooking <- BookingDao.createBooking(createBooking)
            _ <- BankerNotificationMessageProcessor.processMessage(notification)
            updatedBooking <- BookingDao.getBooking(bookingTransactionId)
          } yield assert(initialBooking)(equalTo(updatedBooking))
        }
      }
    ) @@ before(InMemoryBookingDao.clean) @@ after(InMemoryBookingDao.clean))
      .provideCustomLayer(InMemoryBookingDao.live >+> BankerNotificationMessageProcessor.live)
  }
}
