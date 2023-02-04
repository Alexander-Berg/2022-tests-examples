package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications.substages

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType._
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.concurrent.duration.DurationInt

class OfferExpirationEventTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with BeforeAndAfterAll
  with MockHttpClientHelper {
  implicit private val t = Traced.empty

  val featureValue: Boolean = false
  val mockedFeature = mock[Feature[Boolean]]
  val mockedFeatureManager = mock[FeaturesManager]

  when(mockedFeature.value).thenReturn(featureValue)
  when(mockedFeatureManager.PushesForTesting).thenReturn(mockedFeature)

  abstract private class Fixture {
    val offer: Offer
    val worker = new OfferExpirationEvent(mockedFeatureManager)

  }

  "Expiration notify" should {
    "send" when {
      "offer is soon expire" in new Fixture {
        val offer = TestUtils.createOffer().setTimestampWillExpire(getNow + 7.day.toMillis).build()

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.hasNotificationByType(OFFER_EXPIRATION))
        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
      }

      "offer expire today" in new Fixture {
        val offer = TestUtils.createOffer().setTimestampWillExpire(getNow + 1.day.toMillis + 12.hour.toMillis).build()

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.hasNotificationByType(OFFER_EXPIRATION))
        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
      }

      "offer expire now" in new Fixture {
        val offer = TestUtils.createOffer().setTimestampWillExpire(getNow + 10.second.toMillis).build()

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.hasNotificationByType(OFFER_EXPIRATION))
        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
      }
    }

    "skip" when {
      "offer expire yesterday" in new Fixture {
        val offer = TestUtils.createOffer().setTimestampWillExpire(getNow - 12.hour.toMillis).build()

        val result = worker.process(offer, None)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)
      }

      "offer created now" in new Fixture {
        val offer = TestUtils.createOffer().build()

        val result = worker.process(offer, None)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)
      }

      "seller id dealer" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setTimestampWillExpire(getNow + 7.day.toMillis)
          .setOfferAutoru(TestUtils.createAutoruOffer(dealer = true))
          .build()

        val result = worker.shouldProcess(offer, None).shouldProcess

        assert(!result)
      }

      "offer is not active" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .putFlag(OfferFlag.OF_INACTIVE)
          .build()

        val result = worker.process(offer, None)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)
      }

      "offer is draft" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .putFlag(OfferFlag.OF_DRAFT)
          .build()

        val result = worker.process(offer, None)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)
      }
    }

    "delay" when {
      "already sended" in new Fixture {
        val ts = getNow - 1.hour.toMillis
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .addNotifications(
                Notification
                  .newBuilder()
                  .setIsCritical(false)
                  .setMaxTries(1)
                  .setNumTries(1)
                  .setTimestampCreate(ts)
                  .setTimestampSent(ts)
                  .setType(OFFER_EXPIRATION)
              )
          )
          .build()

        val result = worker.process(offer, None)
        assert(result.updateOfferFunc.isEmpty)

        assert(offer.hasNotificationByType(OFFER_EXPIRATION))
        assert(offer.getOfferAutoru.getNotificationsCount == 1)
        assert(offer.getOfferAutoru.getNotifications(0).getMaxTries == 1)
        assert(offer.getOfferAutoru.getNotifications(0).getNumTries == 1)
        assert(offer.getOfferAutoru.getNotifications(0).getTimestampCreate == ts)
        assert(offer.getOfferAutoru.getNotifications(0).getTimestampSent == ts)
      }
    }
  }
}
