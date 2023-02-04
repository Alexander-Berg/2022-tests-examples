package ru.yandex.vos2.model.notifications.builders

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.vos2.model.notifications.NotificationType._
import ru.yandex.vos2.model.ModelUtils._
import ru.yandex.vos2.realty.model.TestUtils
import ru.yandex.vos2.realty.services.notifications.builders.RealtyNotificationBuilders._
import ru.yandex.vos2.services.notifications.builders.NotificationBuilder

/**
  * Ensures generated id stay the same as code changes
  */
class BuildersSpec extends WordSpec with Matchers {

  "Builder" should {
    "create notification with proper id for simple email" in {
      check(SimpleEmail(NT_HELLO_OWNER), "NT_HELLO_OWNER")
    }

    "create notification with proper id for simple timed email" in {
      val time = System.currentTimeMillis()
      check(SimpleTimedEmail(NT_LONG_INACTIVE, time), s"NT_LONG_INACTIVE@$time")
    }

    "create notification with proper id for simple offer email" in {
      val offer = TestUtils.createOffer().build()
      check(SimpleOfferEmail(NT_PUBLISHED, offer), s"NT_PUBLISHED@${offer.externalId}")
    }

    "create notification with proper id for versioned offer email" in {
      val offer = TestUtils.createOffer().build()
      val timestamp = System.currentTimeMillis()
      check(TimedOfferEmail(NT_OFFER_FORGIVEN, offer, timestamp), s"NT_OFFER_FORGIVEN@${offer.externalId}@$timestamp")
    }

    "create notification with proper id for timed offer email" in {
      val offer = TestUtils.createOffer().build()
      val time = System.currentTimeMillis()
      check(TimedOfferEmail(NT_EXPIRING, offer, time), s"NT_EXPIRING@${offer.externalId}@$time")
    }

    "create notification with proper id for versioned offer sms" in {
      val offer = TestUtils.createOffer().build()
      val timestamp = System.currentTimeMillis()
      check(TimedOfferSms(NT_OFFER_FORGIVEN, offer, timestamp), s"sms@NT_OFFER_FORGIVEN@${offer.externalId}@$timestamp")
    }
  }

  private def check(builder: NotificationBuilder, expectedId: String): Unit = {
    builder.build().getId shouldBe expectedId
  }
}
