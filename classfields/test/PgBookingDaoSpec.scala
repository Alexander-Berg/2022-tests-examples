package auto.dealers.booking.storage

import java.time.OffsetDateTime
import common.zio.doobie.testkit.TestPostgresql
import doobie.implicits._
import doobie.util.invariant.NonNullableColumnRead
import doobie.util.meta.Meta
import doobie.util.transactor.Transactor
import ru.auto.api.api_offer_model.Category
import auto.common.pagination.RequestPagination
import auto.dealers.booking.api.DefaultBookingService
import ru.auto.booking.common_model.BookingStatus
import ru.auto.booking.common_model.BookingStatus._
import auto.dealers.booking.model._
import auto.dealers.booking.storage.BookingDao
import auto.dealers.booking.storage.BookingDao.{
  BookingNotFound,
  DuplicateBookingTransactionId,
  DuplicateCode,
  IllegalStatusTransition
}
import auto.dealers.booking.storage.postgresql.{PgBookingDao, PgBookingQueries}
import auto.dealers.booking.testkit.BookingModelGens._
import scalapb.{GeneratedEnum, GeneratedEnumCompanion}
import zio.interop.catz._
import zio.test.Assertion._
import zio.test.AssertionM.Render.param
import zio.test.TestAspect._
import zio.test._
import zio.{Has, Task, ZIO}

import java.time.temporal.ChronoUnit

object PgBookingDaoSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("PgBookingDao")(
      testM("insert booking") {
        for {
          _ <- PgBookingDao.clean
          inserted <- BookingDao.createBooking(createBooking)
        } yield assert(inserted)(equalTo(expectedBooking.copy(id = inserted.id)))
      },
      testM("get booking") {
        for {
          _ <- PgBookingDao.clean
          inserted <- BookingDao.createBooking(createBooking)
          booking <- BookingDao.getBooking(inserted.bookingTransactionId)
        } yield assert(booking)(equalTo(inserted))
      },
      testM("savePaymentInfo should update booking payment") {
        for {
          _ <- PgBookingDao.clean
          pgDao <- ZIO.access[Has[PgBookingDao]](_.get)
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          inserted <- BookingDao.createBooking(createBooking)
          offerBookedBeforeUpdate <- pgDao.bookedOfferExistsQuery(inserted.offerId).transact(xa)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          updated <- BookingDao.getBooking(inserted.bookingTransactionId)
          offerBookedAfterUpdate <- pgDao.bookedOfferExistsQuery(inserted.offerId).transact(xa)
        } yield assert(inserted.status)(equalTo(NEED_PAYMENT)) &&
          assert(inserted.paidAt)(isNone) &&
          assert(inserted.bankerTransactionId)(isNone) &&
          assert(updated.status)(equalTo(PAID)) &&
          assert(updated.paidAt)(equalTo(Some(paidAt))) &&
          assert(updated.bankerTransactionId)(equalTo(Some(bankerTransactionId))) &&
          assert(offerBookedBeforeUpdate)(equalTo(false)) &&
          assert(offerBookedAfterUpdate)(equalTo(true))
      },
      testM("savePaymentInfo should cancel booking and update its payment info if offer already booked") {
        val bankerTransactionId0 = "b:id0"
        val bankerTransactionId1 = "b:id1"
        for {
          _ <- PgBookingDao.clean
          insertedA <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, insertedA.bookingTransactionId, bankerTransactionId0)

          insertedB <- BookingDao.createBooking(createBooking.copy(code = 123, bookingTransactionId = "123"))
          _ <- BookingDao.savePaymentInfo(paidAt, insertedB.bookingTransactionId, bankerTransactionId1)
          updated <- BookingDao.getBooking(insertedB.bookingTransactionId)
        } yield assert(updated.status)(equalTo(CANCELLED)) &&
          assert(updated.paidAt)(equalTo(Some(paidAt))) &&
          assert(updated.bankerTransactionId)(equalTo(Some(bankerTransactionId1)))
      },
      testM("savePaymentInfo should not update booking if its status is not NEED_PAYMENT") {
        for {
          _ <- PgBookingDao.clean
          inserted <- BookingDao.createBooking(createBooking.copy(status = PAID))
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          updated <- BookingDao.getBooking(inserted.bookingTransactionId)
        } yield assert(updated.status)(equalTo(PAID)) &&
          assert(updated.paidAt)(isNone) &&
          assert(updated.bankerTransactionId)(isNone)
      },
      testM("getWaitingForPush should return nothing on empty db") {
        BookingDao.getWaitingForPush.map(assert(_)(isEmpty))
      },
      testM("getWaitingForPush should return one booking with no previous state on db with one booking") {
        for {
          inserted <- BookingDao.createBooking(createBooking)
          result <- BookingDao.getWaitingForPush
        } yield assert(result.map(event => (event.previousState, event.currentState)))(
          equalTo(List(None -> inserted))
        )
      },
      testM("getWaitingForPush should return id of event from db") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          _ <- BookingDao.createBooking(createBooking)
          expectedId <- sql"SELECT id FROM booking_event".query[Long].unique.transact(xa)
          result <- BookingDao.getWaitingForPush
        } yield assert(result.map(_.id))(equalTo(List(expectedId)))
      },
      testM("getWaitingForPush should return two bookings with no previous state on db with two bookings") {
        for {
          inserted0 <- BookingDao.createBooking(createBooking)
          inserted1 <- BookingDao.createBooking(anotherBooking(createBooking))
          result <- BookingDao.getWaitingForPush
        } yield assert(result.map(event => (event.previousState, event.currentState)))(
          equalTo(List(None -> inserted0, None -> inserted1))
        )
      },
      testM("get waitingForPush should return one booking with previous state on db with one updated booking") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          inserted <- BookingDao.createBooking(createBooking)
          insertedEvent <- getEventForBooking(inserted.id).transact(xa)
          _ <- BookingDao.markPushed(insertedEvent.id)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          updated <- BookingDao.getBooking(inserted.bookingTransactionId)
          result <- BookingDao.getWaitingForPush
        } yield assert(result.map(event => (event.previousState, event.currentState)))(
          equalTo(Vector(Some(inserted) -> updated))
        )
      },
      testM("get waitingForPush should return two events in proper order if booking updated twice after push") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          inserted <- BookingDao.createBooking(createBooking)
          insertedEvent <- getEventForBooking(inserted.id).transact(xa)
          _ <- BookingDao.markPushed(insertedEvent.id)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          updated0 <- BookingDao.getBooking(inserted.bookingTransactionId)
          // refactor to updateBookingStatus in https://st.yandex-team.ru/VSMONEY-1540
          updated1 = updated0.copy(status = CLOSED)
          _ <- PgBookingQueries.insertBookingEventQuery(updated1).run.transact(xa)
          result <- BookingDao.getWaitingForPush
        } yield {
          assert(result.map(event => (event.previousState, event.currentState)))(
            equalTo(Vector(Some(inserted) -> updated0, Some(updated0) -> updated1))
          )
        }
      },
      testM("markPushed should set pushed = true") {
        for {
          _ <- PgBookingDao.clean
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          inserted <- BookingDao.createBooking(createBooking)
          another <- BookingDao.createBooking(anotherBooking(createBooking))
          initialState <- getEventForBooking(inserted.id).transact(xa)
          initialStateB <- getEventForBooking(another.id).transact(xa)
          _ <- BookingDao.markPushed(initialState.id)
          resultState <- getEventForBooking(inserted.id).transact(xa)
          resultStateB <- getEventForBooking(another.id).transact(xa)
        } yield {
          assert(initialState.pushed)(isFalse) &&
          assert(resultState.pushed)(isTrue) &&
          assert(initialStateB.pushed)(isFalse) &&
          assert(resultStateB.pushed)(isFalse)
        }
      },
      testM("listBookings should return empty listing on empty db") {
        assertM(BookingDao.listBookings(dealerId, firstPage))(isEmptyListing)
      },
      testM("listBookings should return paid booking if it's created for dealer's offer") {
        for {
          inserted <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          updated <- BookingDao.getBooking(inserted.bookingTransactionId)
          result <- BookingDao.listBookings(dealerId, firstPage)
        } yield assert(result)(hasOnly(List(updated)))
      },
      testM("listBookings should not return paid booking if it's created for another dealer's offer") {
        for {
          inserted <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          result <- BookingDao.listBookings(anotherDealerId, firstPage)
        } yield assert(result)(isEmptyListing)
      },
      testM("listBookings should not return unpaid booking") {
        for {
          _ <- BookingDao.createBooking(createBooking)
          result <- BookingDao.listBookings(dealerId, firstPage)
        } yield assert(result)(isEmptyListing)
      },
      testM("listBookings should not return expired unpaid booking") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          _ <- BookingDao.createBooking(createBooking)
          _ <- sql"UPDATE booking SET status = 'EXPIRED'".update.run.transact(xa)
          result <- BookingDao.listBookings(dealerId, firstPage)
        } yield assert(result)(isEmptyListing)
      },
      testM("listBookings should return paid booking in any status") {
        val unpaidStatuses = Set(UNKNOWN, NEED_PAYMENT)
        val anyPaidStatus = Gen.elements(values.filterNot(unpaidStatuses): _*)
        checkM(anyPaidStatus)(status =>
          for {
            xa <- ZIO.access[Has[Transactor[Task]]](_.get)
            _ <- PgBookingDao.clean
            inserted <- BookingDao.createBooking(createBooking)
            _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
            _ <-
              sql"UPDATE booking SET status = ${fr"${status.toString().toUpperCase()}::enum_booking_status"}".update.run
                .transact(xa)
            updated <- BookingDao.getBooking(inserted.bookingTransactionId)
            result <- BookingDao.listBookings(dealerId, firstPage)
          } yield assert(result)(hasOnly(List(updated)))
        )
      },
      testM("listBookings should order bookings by paid_at desc, not by creation order or some transaction id") {
        val paidAt0 = OffsetDateTime.parse("2020-06-03T12:35:00+03:00")
        val paidAt1 = OffsetDateTime.parse("2020-06-04T12:15:00+03:00")
        val paidAt2 = OffsetDateTime.parse("2020-06-04T10:00:00+03:00")
        val bankerTransactionId0 = "b:id1"
        val bankerTransactionId1 = "b:id0"
        val bankerTransactionId2 = "b:id2"
        val anotherBooking1 = anotherBooking(createBooking)
        val anotherBooking2 = anotherBooking(anotherBooking1)
        for {
          inserted0 <- BookingDao.createBooking(createBooking)
          inserted1 <- BookingDao.createBooking(anotherBooking1)
          inserted2 <- BookingDao.createBooking(anotherBooking2)
          _ <- BookingDao.savePaymentInfo(paidAt0, inserted0.bookingTransactionId, bankerTransactionId0)
          _ <- BookingDao.savePaymentInfo(paidAt1, inserted1.bookingTransactionId, bankerTransactionId1)
          _ <- BookingDao.savePaymentInfo(paidAt2, inserted2.bookingTransactionId, bankerTransactionId2)
          updated0 <- BookingDao.getBooking(inserted0.bookingTransactionId)
          updated1 <- BookingDao.getBooking(inserted1.bookingTransactionId)
          updated2 <- BookingDao.getBooking(inserted2.bookingTransactionId)
          result <- BookingDao.listBookings(dealerId, firstPage)
        } yield assert(result)(hasOnly(List(updated1, updated2, updated0)))
      },
      testM("listBookings should return second page") {
        val totalCount = 8
        final case class PaymentData(paidAt: OffsetDateTime, bankerTransactionId: String)
        val paymentDataList =
          Iterable.iterate(PaymentData(OffsetDateTime.parse("2020-06-01T12:00:00+03:00"), "b:id"), len = totalCount)(
            data => data.copy(data.paidAt.minusHours(1), data.bankerTransactionId + "1")
          )
        val createBookings = Iterable.iterate(createBooking, len = totalCount)(anotherBooking)
        val pageSize = 5
        for {
          inserted <- ZIO.foreach(createBookings)(BookingDao.createBooking)
          updated <- ZIO.foreach(inserted.zip(paymentDataList)) { case (booking, paymentData) =>
            import booking.bookingTransactionId
            val paidAt = paymentData.paidAt
            val bankerTransactionId = paymentData.bankerTransactionId
            BookingDao.savePaymentInfo(paidAt, bookingTransactionId, bankerTransactionId) *>
              BookingDao.getBooking(bookingTransactionId)
          }
          expected = updated.drop(pageSize)
          result <- BookingDao.listBookings(dealerId, RequestPagination(page = 2, pageSize))
        } yield assert(result)(isPage(expected.toList, totalCount))
      },
      testM("createBooking should fail on duplicate booking transaction id") {
        for {
          _ <- BookingDao.createBooking(createBooking)
          res <- BookingDao.createBooking(createBooking.copy(code = createBooking.code + 1)).run
        } yield assert(res)(fails(equalTo(DuplicateBookingTransactionId())))
      },
      testM("createBooking should fail on duplicate booking code") {
        for {
          _ <- BookingDao.createBooking(createBooking)
          res <-
            BookingDao
              .createBooking(createBooking.copy(bookingTransactionId = createBooking.bookingTransactionId + "1"))
              .run
        } yield assert(res)(fails(equalTo(DuplicateCode())))
      },
      testM("getBankerTransactionId should return saved banker transaction id") {
        for {
          inserted <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          res <- BookingDao.getBankerTransactionId(BookingCode(code))
        } yield assert(res.raw)(equalTo(bankerTransactionId))
      },
      testM("getBankerTransactionId should return error if payment haven't occurred yet") {
        val res = BookingDao.createBooking(createBooking) *>
          BookingDao.getBankerTransactionId(BookingCode(code))
        assertM(res.either)(isLeft(isSubtype[NonNullableColumnRead](anything)))
      },
      testM("getBankerTransactionId should return banker transaction id of proper booking") {
        val bankerTransactionId0 = "b:id:0"
        val bankerTransactionId1 = "b:id:1"
        for {
          inserted0 <- BookingDao.createBooking(createBooking)
          inserted1 <- BookingDao.createBooking(anotherBooking(createBooking))
          _ <- BookingDao.savePaymentInfo(paidAt, inserted0.bookingTransactionId, bankerTransactionId0)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted1.bookingTransactionId, bankerTransactionId1)
          res0 <- BookingDao.getBankerTransactionId(BookingCode(inserted0.code))
          res1 <- BookingDao.getBankerTransactionId(BookingCode(inserted1.code))
        } yield assert(res0.raw)(equalTo(bankerTransactionId0)) &&
          assert(res1.raw)(equalTo(bankerTransactionId1))
      },
      testM("markRefunded should create event with booking as refunded") {
        for {
          inserted <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          eventsBeforeRefund <- BookingDao.getWaitingForPush
          _ = require(!eventsBeforeRefund.exists(_.currentState.paymentRefunded))
          _ <- ZIO.foreach_(eventsBeforeRefund)(event => BookingDao.markPushed(event.id))
          _ <- BookingDao.markRefunded(BankerTransactionId(bankerTransactionId))
          eventsAfterRefund <- BookingDao.getWaitingForPush
        } yield assert(eventsAfterRefund.map(_.currentState.paymentRefunded))(equalTo(Seq(true)))
      },
      testM("markRefunded should not fail on repeated invocations") {
        for {
          inserted <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, bankerTransactionId)
          eventsBeforeRefund <- BookingDao.getWaitingForPush
          _ = require(!eventsBeforeRefund.exists(_.currentState.paymentRefunded))
          _ <- ZIO.foreach_(eventsBeforeRefund)(event => BookingDao.markPushed(event.id))
          _ <- BookingDao.markRefunded(BankerTransactionId(bankerTransactionId))
          _ <- BookingDao.markRefunded(BankerTransactionId(bankerTransactionId))
          eventsAfterRefund <- BookingDao.getWaitingForPush
        } yield assert(eventsAfterRefund.map(_.currentState.paymentRefunded).distinct)(equalTo(Seq(true)))
      },
      testM("markRefunded should mark create event for proper booking") {
        val notRefunded = "b:id:0"
        val refunded = "b:id:1"
        for {
          inserted0 <- BookingDao.createBooking(createBooking)
          inserted1 <- BookingDao.createBooking(anotherBooking(createBooking))
          _ <- BookingDao.savePaymentInfo(paidAt, inserted0.bookingTransactionId, notRefunded)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted1.bookingTransactionId, refunded)
          eventsBeforeRefund <- BookingDao.getWaitingForPush
          _ <- ZIO.foreach_(eventsBeforeRefund)(event => BookingDao.markPushed(event.id))
          _ <- BookingDao.markRefunded(BankerTransactionId(refunded))
          eventsAfterRefund <- BookingDao.getWaitingForPush
        } yield assert(eventsAfterRefund.flatMap(_.currentState.bankerTransactionId))(equalTo(Seq(refunded)))
      },
      testM("updateBookingStatus should update status and create bookingEvent") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          inserted1 <- BookingDao.createBooking(createBooking)
          inserted2 <- BookingDao.createBooking(anotherBooking(createBooking))
          _ <- BookingDao.savePaymentInfo(paidAt, inserted1.bookingTransactionId, "b:id1")
          _ <- BookingDao.savePaymentInfo(paidAt, inserted2.bookingTransactionId, "b:id2")
          paid1 <- BookingDao.getBooking(inserted1.bookingTransactionId)
          paid2 <- BookingDao.getBooking(inserted2.bookingTransactionId)
          paid1BookedOfferId <- getBookedOfferId(paid1.bookingTransactionId).transact(xa)
          paid2BookedOfferId <- getBookedOfferId(paid2.bookingTransactionId).transact(xa)
          _ <- BookingDao.updateBookingStatus(
            inserted1.dealerId,
            BookingCode(inserted1.code.toLong),
            DefaultBookingService.allowedStatusFrom(CONFIRMED),
            CONFIRMED,
            dropBookedOfferId = false
          )
          _ <- BookingDao.updateBookingStatus(
            inserted2.dealerId,
            BookingCode(inserted2.code.toLong),
            DefaultBookingService.allowedStatusFrom(REJECTED),
            REJECTED,
            dropBookedOfferId = true
          )
          updated1 <- BookingDao.getBooking(inserted1.bookingTransactionId)
          updated2 <- BookingDao.getBooking(inserted2.bookingTransactionId)
          updated1BookedOfferId <- getBookedOfferId(updated1.bookingTransactionId).transact(xa)
          updated2BookedOfferId <- getBookedOfferId(updated2.bookingTransactionId).transact(xa)
        } yield assert(paid1.status)(equalTo(PAID)) && assert(paid1BookedOfferId)(isSome) &&
          assert(paid2.status)(equalTo(PAID)) && assert(paid2BookedOfferId)(isSome) &&
          assert(updated1.status)(equalTo(CONFIRMED)) && assert(updated1BookedOfferId)(equalTo(paid1BookedOfferId)) &&
          assert(updated2.status)(equalTo(REJECTED)) && assert(updated2BookedOfferId)(isNone)
      },
      testM("Do nothing if booking is already in required status") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          inserted <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, "b:id1")
          paid <- BookingDao.getBooking(inserted.bookingTransactionId)
          _ <- BookingDao.updateBookingStatus(
            inserted.dealerId,
            BookingCode(inserted.code.toLong),
            DefaultBookingService.allowedStatusFrom(CONFIRMED),
            CONFIRMED,
            dropBookedOfferId = false
          )
          updatedAfterFirstAttempt <- BookingDao.getBooking(inserted.bookingTransactionId)
          _ <- BookingDao.updateBookingStatus(
            inserted.dealerId,
            BookingCode(inserted.code.toLong),
            DefaultBookingService.allowedStatusFrom(CONFIRMED),
            CONFIRMED,
            dropBookedOfferId = false
          )
          updatedAfterSecondAttempt <- BookingDao.getBooking(inserted.bookingTransactionId)
          events <- getBookingEvents(inserted.id).transact(xa)
        } yield assert(paid.status)(equalTo(PAID)) &&
          assert(updatedAfterFirstAttempt)(equalTo(updatedAfterSecondAttempt)) &&
          assert(events.count(_ == CONFIRMED))(equalTo(1))
      },
      testM("fail when booking not found") {
        for {
          inserted <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, "b:id1")
          paid <- BookingDao.getBooking(inserted.bookingTransactionId)
          withWrongCode <-
            BookingDao
              .updateBookingStatus(
                paid.dealerId,
                BookingCode(paid.code.toLong + 1),
                DefaultBookingService.allowedStatusFrom(CONFIRMED),
                CONFIRMED,
                dropBookedOfferId = false
              )
              .run
          withWrongDealerId <-
            BookingDao
              .updateBookingStatus(
                paid.dealerId + "1",
                BookingCode(paid.code.toLong),
                DefaultBookingService.allowedStatusFrom(CONFIRMED),
                CONFIRMED,
                dropBookedOfferId = false
              )
              .run
        } yield assert(withWrongCode)(fails(equalTo(BookingNotFound()))) &&
          assert(withWrongDealerId)(fails(equalTo(BookingNotFound())))
      },
      testM("fail when transition not allowed") {
        for {
          inserted <- BookingDao.createBooking(createBooking)
          _ <- BookingDao.savePaymentInfo(paidAt, inserted.bookingTransactionId, "b:id1")
          paid <- BookingDao.getBooking(inserted.bookingTransactionId)
          withWrongDealerId <-
            BookingDao
              .updateBookingStatus(
                paid.dealerId,
                BookingCode(paid.code.toLong),
                DefaultBookingService.allowedStatusFrom(CLOSED),
                CONFIRMED,
                dropBookedOfferId = false
              )
              .run
        } yield assert(withWrongDealerId)(
          fails(equalTo(IllegalStatusTransition(paid.dealerId, BookingCode(paid.code.toLong), CONFIRMED)))
        )
      },
      testM("expire bookings and create booking events") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          needPaymentToSave = createBooking.copy(validUntil = dateTime.minusDays(7))
          confirmedToSave = anotherBooking(needPaymentToSave.copy(status = CONFIRMED))
          paidToSave = anotherBooking(confirmedToSave.copy(status = PAID))

          needPayment <- BookingDao.createBooking(needPaymentToSave)
          confirmed <- BookingDao.createBooking(confirmedToSave)
          paid <- BookingDao.createBooking(paidToSave)

          _ <- BookingDao.expireOutdatedBookings
          needPaymentExpired <- BookingDao.getBooking(needPayment.bookingTransactionId)
          confirmedExpired <- BookingDao.getBooking(confirmed.bookingTransactionId)
          paidExpired <- BookingDao.getBooking(paid.bookingTransactionId)
          all = Seq(needPaymentExpired, confirmedExpired, paidExpired)
          events <- ZIO.foreach(all.map(_.id))(getBookingEvents(_).transact(xa))
        } yield assert(all.map(_.status))(forall(equalTo(EXPIRED))) &&
          assert(events)(forall(contains(EXPIRED))) &&
          assert(events.flatten.size)(equalTo(6))
      },
      testM("don't touch non-expired bookings (status stays the same; there is no expired event)") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          needPaymentToSave = createBooking.copy(validUntil = dateTime.plusDays(7))
          confirmedToSave = anotherBooking(needPaymentToSave.copy(status = CONFIRMED))
          paidToSave = anotherBooking(confirmedToSave.copy(status = PAID))

          needPayment <- BookingDao.createBooking(needPaymentToSave)
          confirmed <- BookingDao.createBooking(confirmedToSave)
          paid <- BookingDao.createBooking(paidToSave)

          _ <- BookingDao.expireOutdatedBookings
          needPaymentExpired <- BookingDao.getBooking(needPayment.bookingTransactionId)
          confirmedExpired <- BookingDao.getBooking(confirmed.bookingTransactionId)
          paidExpired <- BookingDao.getBooking(paid.bookingTransactionId)
          all = Seq(needPaymentExpired, confirmedExpired, paidExpired)
          events <- ZIO.foreach(all.map(_.id))(getBookingEvents(_).transact(xa))
        } yield assert(all.map(_.status))(forall(not(equalTo(EXPIRED)))) &&
          assert(events)(forall(not(contains(EXPIRED)))) &&
          assert(events.flatten.size)(equalTo(3))
      },
      testM("ignore terminated booking") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          cancelledToSave = createBooking.copy(validUntil = dateTime.minusDays(7), status = CANCELLED)
          closedToSave = anotherBooking(cancelledToSave.copy(status = CLOSED))
          rejectedToSave = anotherBooking(closedToSave.copy(status = REJECTED))

          cancelled <- BookingDao.createBooking(cancelledToSave)
          closed <- BookingDao.createBooking(closedToSave)
          rejected <- BookingDao.createBooking(rejectedToSave)

          _ <- BookingDao.expireOutdatedBookings
          cancelledExpired <- BookingDao.getBooking(cancelled.bookingTransactionId)
          closedExpired <- BookingDao.getBooking(closed.bookingTransactionId)
          rejectedExpired <- BookingDao.getBooking(rejected.bookingTransactionId)
          all = Seq(cancelledExpired, closedExpired, rejectedExpired)
          events <- ZIO.foreach(all.map(_.id))(getBookingEvents(_).transact(xa))
        } yield assert(all.map(_.status))(forall(not(equalTo(EXPIRED)))) &&
          assert(events)(forall(not(contains(EXPIRED)))) &&
          assert(events.flatten.size)(equalTo(3))
      },
      testM("don't expire already expired events") {
        for {
          xa <- ZIO.access[Has[Transactor[Task]]](_.get)
          created <- BookingDao.createBooking(createBooking.copy(validUntil = dateTime.minusDays(7), status = EXPIRED))
          _ <- BookingDao.expireOutdatedBookings
          createdAfterExpiration <- BookingDao.getBooking(created.bookingTransactionId)
          events <- getBookingEvents(created.id).transact(xa)
        } yield assert(createdAfterExpiration)(equalTo(created)) &&
          assert(events)(equalTo(Seq(EXPIRED)))
      }
    ) @@ after(PgBookingDao.clean) @@ beforeAll(PgBookingDao.initSchema.orDie) @@ sequential
  }.provideCustomLayerShared(
    TestPostgresql.managedTransactor ++ (TestPostgresql.managedTransactor >>> PgBookingDao.live)
  )

  case class BookingEventLight(id: Long, pushed: Boolean)

  private def getEventForBooking(bookingId: Long) =
    sql"SELECT id, pushed FROM booking_event WHERE booking_id = $bookingId"
      .query[BookingEventLight]
      .unique

  implicit def protoEnumMeta[T <: GeneratedEnum](implicit T: GeneratedEnumCompanion[T]): Meta[T] =
    Meta[String].imap[T](v => T.fromName(v.toUpperCase).getOrElse(T.fromValue(0)))(_.name.toUpperCase)

  private def getBookingEvents(bookingId: Long): doobie.ConnectionIO[Seq[BookingStatus]] =
    sql"SELECT status FROM booking_event WHERE booking_id = $bookingId"
      .query[BookingStatus]
      .to[Seq]

  private def getBookedOfferId(bookingTransactionId: String): doobie.ConnectionIO[Option[String]] = {
    sql"SELECT booked_offer_id FROM booking WHERE booking_transaction_id = $bookingTransactionId"
      .query[Option[String]]
      .unique
  }

  val dateTime = createBooking.createdAt

  private val paidAt = OffsetDateTime
    .now()
    .truncatedTo(ChronoUnit.MICROS) // JDK15 has nanotime resolution, while PG supports only microseconds
  private val bankerTransactionId = "b:id"
  private val code = 101

  val dealerId = "dealer:20101"
  val anotherDealerId = "dealer:16453"

  val firstPage = RequestPagination(page = 1, pageSize = 5)

  val expectedBooking = Booking(
    id = 0,
    code = 101,
    status = NEED_PAYMENT,
    createdAt = dateTime,
    validUntil = dateTime,
    userId = "user:12345",
    userFullName = "Test Testov",
    userPhone = "+79164352343",
    offerId = "123456-123",
    offerCategory = Category.CARS,
    dealerId = "dealer:20101",
    offerPriceWhenBooked = 200000,
    vin = Some("vin"),
    paymentCost = 200000,
    paymentRefunded = false,
    bookingTransactionId = "transaction:id",
    bankerTransactionId = None,
    paidAt = None
  )

  private val isEmptyListing =
    Assertion.assertion[BookingListing]("isEmptyListing")() { actual =>
      actual.bookings.isEmpty && actual.totalCount == 0
    }

  // также проверяет, что бронирования упорядочены также, как в expected
  private def hasOnly(expected: List[Booking]) =
    Assertion.assertion[BookingListing]("hasOnly")(param(expected)) { actual =>
      actual.bookings == expected && actual.totalCount == expected.size
    }

  private def isPage(expected: List[Booking], totalCount: Int) =
    Assertion.assertion[BookingListing]("isPage")(param(expected), param(totalCount)) { actual =>
      actual.bookings == expected && actual.totalCount == totalCount
    }
}
