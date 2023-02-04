package ru.yandex.vos2.autoru.booking

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.time.temporal.ChronoUnit._
import org.scalatest.Suite
import ru.auto.booking.CommonModel.BookingStatus
import ru.auto.booking.broker.BrokerModel
import ru.yandex.vos2.AutoruModel.AutoruOffer.Booking
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.booking.impl.BookingDeciderImpl
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.proto.ProtoMacro._
import ru.yandex.vos2.util.Dates._

class BookigDeciderSpec extends AnyWordSpec with Matchers with Suite {

  private val bookingDecider = new BookingDeciderImpl

  private case class TestCase(description: String,
                              event: BrokerModel.BookingChangeEvent,
                              offer: OfferModel.Offer,
                              expected: Option[Booking])

  private val someUserRef: String = "a_123"
  private val someOfferId: String = "123-hash"

  private def offerWithoutBookingBuilder: Offer.Builder =
    TestUtils.createOffer(Instant.now, dealer = true).setOfferID(someOfferId)
  private val now: Instant = Instant.now

  private def bookingEvent(id: Long,
                           timestamp: Instant,
                           code: Long,
                           userRef: String,
                           offerId: String,
                           status: BookingStatus,
                           paidAd: Option[Instant] = None,
                           validUntil: Option[Instant] = None): BrokerModel.BookingChangeEvent = {
    val builder = BrokerModel.BookingChangeEvent.newBuilder
      .setId(id)
      .setTimestamp(timestamp)
    val offer = BrokerModel.BookingOffer.newBuilder.setId(offerId)
    val user = BrokerModel.BookingUser.newBuilder.setId(userRef)
    val currentStateBuilder = builder.getCurrentStateBuilder
      .setCode(code)
      .setOffer(offer)
      .setUser(user)
      .setStatus(status)
    paidAd.foreach(ts => currentStateBuilder.setPaidAt(ts))
    validUntil.foreach(ts => currentStateBuilder.setValidUntil(ts))
    builder.build
  }

  private val testCases: Seq[TestCase] = Seq(
    TestCase(
      description = "Not booked to booked",
      event =
        bookingEvent(100, now, 6, someUserRef, someOfferId, BookingStatus.PAID, Some(now), Some(now.plus(7, DAYS))),
      offer = offerWithoutBookingBuilder.build,
      expected = Some {
        val builder = Booking.newBuilder
        val booked = Booking.State.Booked.newBuilder
          .setBookingId("6")
          .setUserRef(someUserRef)
          .setPeriod(TimeRange(now, now.plus(7, DAYS)).toProto)
        builder.getStateBuilder
          .setUpdated(now)
          .setEventId(100)
          .setBooked(booked)
        builder.build
      }
    ),
    TestCase(
      description = "Not booked to not booked",
      event = bookingEvent(
        100,
        now,
        5,
        someUserRef,
        someOfferId,
        BookingStatus.NEED_PAYMENT,
        Some(now),
        Some(now.plus(7, DAYS))
      ),
      offer = offerWithoutBookingBuilder.build,
      expected = None
    ),
    TestCase(
      description = "Booked to not booked",
      event = bookingEvent(
        100,
        now,
        5,
        someUserRef,
        someOfferId,
        BookingStatus.NEED_PAYMENT,
        Some(now),
        Some(now.plus(7, DAYS))
      ),
      offer = {
        val builder = offerWithoutBookingBuilder
        val booked = Booking.State.Booked.newBuilder
          .setBookingId("5")
          .setUserRef(someUserRef)
        val state = Booking.State.newBuilder
          .setUpdated(now.minus(1, DAYS))
          .setEventId(99)
          .setBooked(booked)
        builder.getOfferAutoruBuilder.getBookingBuilder.setState(state)
        builder.build
      },
      expected = Some {
        val builder = Booking.newBuilder
        val notBooked = Booking.State.NotBooked.newBuilder
        builder.getStateBuilder
          .setUpdated(now)
          .setEventId(100)
          .setNotBooked(notBooked)
        builder.build
      }
    ),
    TestCase(
      description = "Booked to not booked with older eventId",
      event = bookingEvent(
        99,
        now,
        5,
        someUserRef,
        someOfferId,
        BookingStatus.NEED_PAYMENT,
        Some(now),
        Some(now.plus(7, DAYS))
      ),
      offer = {
        val builder = offerWithoutBookingBuilder
        val booked = Booking.State.Booked.newBuilder
          .setBookingId("5")
          .setUserRef(someUserRef)
        val state = Booking.State.newBuilder
          .setUpdated(now.minus(1, DAYS))
          .setEventId(100)
          .setBooked(booked)
        builder.getOfferAutoruBuilder.getBookingBuilder.setState(state)
        builder.build
      },
      expected = None
    ),
    TestCase(
      description = "Booked to not booked with other bookingId",
      event = bookingEvent(
        100,
        now,
        5,
        someUserRef,
        someOfferId,
        BookingStatus.NEED_PAYMENT,
        Some(now),
        Some(now.plus(7, DAYS))
      ),
      offer = {
        val builder = offerWithoutBookingBuilder
        val booked = Booking.State.Booked.newBuilder
          .setBookingId("6")
          .setUserRef(someUserRef)
        val state = Booking.State.newBuilder
          .setUpdated(now.minus(1, DAYS))
          .setEventId(99)
          .setBooked(booked)
        builder.getOfferAutoruBuilder.getBookingBuilder.setState(state)
        builder.build
      },
      expected = None
    ),
    TestCase(
      description = "Booked to booked with other bookingId",
      event = bookingEvent(
        100,
        now,
        5,
        someUserRef,
        someOfferId,
        BookingStatus.CONFIRMED,
        Some(now),
        Some(now.plus(7, DAYS))
      ),
      offer = {
        val builder = offerWithoutBookingBuilder
        val booked = Booking.State.Booked.newBuilder
          .setBookingId("6")
          .setUserRef(someUserRef)
        val state = Booking.State.newBuilder
          .setUpdated(now.minus(1, DAYS))
          .setEventId(99)
          .setBooked(booked)
        builder.getOfferAutoruBuilder.getBookingBuilder.setState(state)
        builder.build
      },
      expected = Some {
        val builder = Booking.newBuilder
        val booked = Booking.State.Booked.newBuilder
          .setBookingId("5")
          .setUserRef(someUserRef)
          .setPeriod(TimeRange(now, now.plus(7, DAYS)).toProto)
        builder.getStateBuilder
          .setUpdated(now)
          .setEventId(100)
          .setBooked(booked)
        builder.build
      }
    ),
    TestCase(
      description = "Booked to booked",
      event = bookingEvent(
        100,
        now,
        5,
        someUserRef,
        someOfferId,
        BookingStatus.CONFIRMED,
        Some(now),
        Some(now.plus(14, DAYS))
      ),
      offer = {
        val builder = offerWithoutBookingBuilder
        val booked = Booking.State.Booked.newBuilder
          .setBookingId("5")
          .setUserRef(someUserRef)
        val state = Booking.State.newBuilder
          .setUpdated(now.minus(2, DAYS))
          .setEventId(99)
          .setBooked(booked)
        builder.getOfferAutoruBuilder.getBookingBuilder.setState(state)
        builder.build
      },
      expected = Some {
        val builder = Booking.newBuilder
        val booked = Booking.State.Booked.newBuilder
          .setBookingId("5")
          .setUserRef(someUserRef)
          .setPeriod(TimeRange(now, now.plus(14, DAYS)).toProto)
        builder.getStateBuilder
          .setUpdated(now)
          .setEventId(100)
          .setBooked(booked)
        builder.build
      }
    )
  )

  "BookigDeciderImpl" should {
    testCases.foreach {
      case TestCase(description, event, offer, expected) =>
        description in {
          val result = bookingDecider.decide(event, offer)
          val actual = for {
            update <- result.getUpdate
            offer <- ?(update.getOfferAutoru)
            booking <- ?(offer.getBooking)
          } yield booking
          actual shouldBe expected
        }
    }
  }
}
