package auto.dealers.booking.scheduler.notifications

import java.time.OffsetDateTime

import auto.common.clients.cabinet.Cabinet.Cabinet
import auto.common.clients.cabinet.model.ClientSubscription
import auto.common.clients.cabinet.testkit.{CabinetEmptyTest, CabinetTest}
import auto.common.clients.vos.Vos.Vos
import auto.common.clients.vos.testkit.VosTest
import common.clients.email.EmailSender.EmailSender
import common.clients.email.testkit.EmailSenderTest
import common.clients.sms.SmsSender.SmsSender
import common.clients.sms.testkit.SmsSenderTest
import ru.auto.api.api_offer_model.Category
import ru.auto.booking.common_model.BookingStatus
import auto.dealers.booking.model.{Booking, BookingEvent}
import ru.auto.comeback.model.testkit.VosOfferGen
import common.zio.app.Environments
import zio.{ULayer, ZLayer}
import zio.test.Assertion._
import zio.test._
import zio.test.mock.Expectation._

object NotificationsManagerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("NotificationsManager")(
      testM("send email to dealer, managers and sms to user when booking paid") {
        checkM(VosOfferGen.offer()) { offer =>
          val cabinet: ULayer[Cabinet] = CabinetTest.GetClientSubscriptionsByCategory(
            equalTo(20101L -> "booking"),
            value(Seq(ClientSubscription(20101, "booking", "test@test.ru")))
          )

          val vos = VosTest.GetOfferOrFail(equalTo(Category.CARS -> offer.id), value(offer))
          val email = EmailSenderTest.SendEmail(anything, unit).repeats(2 to 2)
          val sms = SmsSenderTest.SendSms(anything, value(""))
          val event = BookingEvent(
            1L,
            OffsetDateTime.now(),
            Some(booking.copy(status = BookingStatus.NEED_PAYMENT, offerId = offer.id)),
            booking.copy(status = BookingStatus.PAID, offerId = offer.id)
          )

          assertM(NotificationsManager.notifyOnStatusChange(event))(isUnit)
            .provideLayer(
              (cabinet ++ vos ++ email ++ sms ++ ZLayer.succeed(Environments.Stable)) >>> NotificationsManagerLive.live
            )
        }
      },
      testM("fail if unable to send email to dealer when booking paid") {
        checkM(VosOfferGen.offer()) { offer =>
          val cabinet: ULayer[Cabinet] = CabinetTest.GetClientSubscriptionsByCategory(
            equalTo(20101L -> "booking"),
            value(Seq(ClientSubscription(20101, "booking", "test@test.ru")))
          )
          val vos: ULayer[Vos] = VosTest.GetOfferOrFail(equalTo(Category.CARS -> offer.id), value(offer))
          val email = EmailSenderTest.SendEmail(anything, failure(EmailSendingException))
          val sms = SmsSenderTest.SendSms(anything, value("")).atMost(1)

          val event = BookingEvent(
            1L,
            OffsetDateTime.now(),
            Some(booking.copy(status = BookingStatus.NEED_PAYMENT, offerId = offer.id)),
            booking.copy(status = BookingStatus.PAID, offerId = offer.id)
          )

          assertM(NotificationsManager.notifyOnStatusChange(event).flip)(equalTo(EmailSendingException))
            .provideCustomLayer(
              (cabinet ++ vos ++ email ++ sms ++ ZLayer.succeed(Environments.Stable)) >>> NotificationsManagerLive.live
            )
        }
      },
      testM("fail if unable to send sms to user when booking paid") {
        checkM(VosOfferGen.offer()) { offer =>
          val cabinet: ULayer[Cabinet] = CabinetTest
            .GetClientSubscriptionsByCategory(
              equalTo((20101L, "booking")),
              value(Seq(ClientSubscription(20101, "booking", "test@test.ru")))
            )
            .atMost(1)

          val vos = VosTest.GetOfferOrFail(equalTo(Category.CARS -> offer.id), value(offer))
          val email = EmailSenderTest.SendEmail(anything, unit).atMost(2)
          val sms = SmsSenderTest.SendSms(anything, failure(SmsSendingException))

          val event = BookingEvent(
            1L,
            OffsetDateTime.now(),
            Some(booking.copy(status = BookingStatus.NEED_PAYMENT, offerId = offer.id)),
            booking.copy(status = BookingStatus.PAID, offerId = offer.id)
          )

          assertM(NotificationsManager.notifyOnStatusChange(event).flip)(equalTo(SmsSendingException))
            .provideCustomLayer(
              (cabinet ++ vos ++ email ++ sms ++ ZLayer.succeed(Environments.Stable)) >>> NotificationsManagerLive.live
            )
        }
      },
      testM("dont send any notification if status not changed") {
        checkM(VosOfferGen.offer()) { offer =>
          val cabinet = CabinetEmptyTest.empty
          val vos = VosTest.empty
          val email = EmailSenderTest.empty
          val sms = SmsSenderTest.empty

          val event = BookingEvent(
            1L,
            OffsetDateTime.now(),
            Some(booking.copy(status = BookingStatus.PAID, offerId = offer.id)),
            booking.copy(status = BookingStatus.PAID, offerId = offer.id)
          )

          assertM(NotificationsManager.notifyOnStatusChange(event))(isUnit)
            .provideCustomLayer(
              (cabinet ++ vos ++ email ++ sms ++ ZLayer.succeed(Environments.Stable)) >>> NotificationsManagerLive.live
            )
        }
      },
      testM("send email to inner email even if no booking subscriptions") {
        checkM(VosOfferGen.offer()) { offer =>
          val cabinet: ULayer[Cabinet] = CabinetTest.GetClientSubscriptionsByCategory(
            equalTo(20101L -> "booking"),
            value(Nil)
          )

          val vos: ULayer[Vos] = VosTest.GetOfferOrFail(equalTo(Category.CARS -> offer.id), value(offer))
          val email: ULayer[EmailSender] = EmailSenderTest.SendEmail(anything, unit)
          val sms: ULayer[SmsSender] = SmsSenderTest.SendSms(anything, value(""))

          val event = BookingEvent(
            1L,
            OffsetDateTime.now(),
            Some(booking.copy(status = BookingStatus.NEED_PAYMENT, offerId = offer.id)),
            booking.copy(status = BookingStatus.PAID, offerId = offer.id)
          )

          assertM(NotificationsManager.notifyOnStatusChange(event))(isUnit)
            .provideCustomLayer(
              (cabinet ++ vos ++ email ++ sms ++ ZLayer.succeed(Environments.Stable)) >>> NotificationsManagerLive.live
            )
        }
      },
      testM("don't send inner email on testing") {
        checkM(VosOfferGen.offer()) { offer =>
          val cabinet: ULayer[Cabinet] = CabinetTest.GetClientSubscriptionsByCategory(
            equalTo(20101L -> "booking"),
            value(Nil)
          )

          val vos: ULayer[Vos] = VosTest.GetOfferOrFail(equalTo(Category.CARS -> offer.id), value(offer))
          val email: ULayer[EmailSender] = EmailSenderTest.empty
          val sms: ULayer[SmsSender] = SmsSenderTest.SendSms(anything, value(""))

          val event = BookingEvent(
            1L,
            OffsetDateTime.now(),
            Some(booking.copy(status = BookingStatus.NEED_PAYMENT, offerId = offer.id)),
            booking.copy(status = BookingStatus.PAID, offerId = offer.id)
          )

          assertM(NotificationsManager.notifyOnStatusChange(event))(isUnit)
            .provideCustomLayer(
              (cabinet ++ vos ++ email ++ sms ++ ZLayer.succeed(Environments.Testing)) >>> NotificationsManagerLive.live
            )
        }
      }
    )
  }

  case object EmailSendingException extends Throwable
  case object SmsSendingException extends Throwable

  private val id = 5
  private val code = 868323
  private val status = BookingStatus.CONFIRMED
  private val createdAt = OffsetDateTime.parse("2020-06-05T08:00:00+03:00")
  private val validUntil = OffsetDateTime.parse("2020-06-10T23:59:59+03:00")
  private val userId = "user:33158932"
  private val userFullName = "Иван Петров"
  private val userPhone = "+79161234567"
  private val userEmail = "test@yandex-team.ru"
  private val offerId = "123458892-def8"
  private val offerCategory = Category.CARS
  private val dealerId = "dealer:20101"
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
}
