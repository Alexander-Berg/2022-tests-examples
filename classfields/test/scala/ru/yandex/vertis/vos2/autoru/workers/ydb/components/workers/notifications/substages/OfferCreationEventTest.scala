package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications.substages

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType.{LOW_RATING, OFFER_ACTIVATION, OFFER_CREATION}
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.model.AutoruModelUtils.{AutoruModelRichOffer, AutoruRichOfferBuilder}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.concurrent.duration.DurationInt

class OfferCreationEventTest
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
    val worker = new OfferCreationEvent(mockedFeatureManager)

  }

  "Creation notify" should {
    "send" when {
      "created now" in new Fixture {
        val offer = TestUtils.createOffer().build()

        assert(worker.shouldProcess(offer, None).shouldProcess)
        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
        assert(resultOffer.hasNotificationByType(OFFER_CREATION))
        assert(resultOffer.getOfferAutoru.getNotifications(0).getMaxTries == 1)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasNumTries)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampSent)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampCancel)
      }

      //      "created half day ago" in new Fixture {
      //        val offer = TestUtils.createOffer(getNow - 12.hour.toMillis).build()
      //
      //                val result = worker.process(offer, None)
//      val resultOffer = result.updateOfferFunc.get(offer)

      //
      //        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
      //        assert(resultOffer.hasNotificationByType(OFFER_CREATION))
      //      }

      "created half hour ago" in new Fixture {
        val offer = TestUtils.createOffer(getNow - 30.minute.toMillis).build()
        assert(worker.shouldProcess(offer, None).shouldProcess)

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
        assert(resultOffer.hasNotificationByType(OFFER_CREATION))
      }

      "has another notification" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .putNotificationByType(OFFER_ACTIVATION, isCritical = false)
          .build()
        assert(worker.shouldProcess(offer, None).shouldProcess)

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 2)
        assert(resultOffer.hasNotificationByType(OFFER_ACTIVATION))
        assert(resultOffer.hasNotificationByType(OFFER_CREATION))
      }

      "created now for truck" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(TestUtils.createAutoruOffer().setCategory(Category.TRUCKS))
          .build()
        assert(worker.shouldProcess(offer, None).shouldProcess)

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
        assert(resultOffer.hasNotificationByType(OFFER_CREATION))
        assert(resultOffer.getOfferAutoru.getNotifications(0).getMaxTries == 1)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasNumTries)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampSent)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampCancel)
      }
    }

    "skip" when {
      "is old" in new Fixture {
        val offer = TestUtils.createOffer(getNow - 2.day.toMillis).build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)

        val result = worker.process(offer, None)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)
      }

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
                  .setType(OFFER_CREATION)
              )
          )
          .build()
        assert(!worker.shouldProcess(offer, None).shouldProcess)

        val result = worker.process(offer, None)
        assert(result.updateOfferFunc.isEmpty)

        assert(offer.getOfferAutoru.getNotificationsCount == 1)
        assert(offer.getOfferAutoru.getNotifications(0).getMaxTries == 1)
        assert(offer.getOfferAutoru.getNotifications(0).getNumTries == 1)
        assert(offer.getOfferAutoru.getNotifications(0).getTimestampCreate == ts)
        assert(offer.getOfferAutoru.getNotifications(0).getTimestampSent == ts)
      }
    }

    "not send" when {
      "seller is dealer" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(TestUtils.createAutoruOffer(dealer = true))
          .build()
        assert(!worker.shouldProcess(offer, None).shouldProcess)

      }

      "has flag of need activation" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .putFlag(OfferFlag.OF_NEED_ACTIVATION)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)

      }

      "is draft" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .putFlag(OfferFlag.OF_DRAFT)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)

      }

      "is not active" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .putFlag(OfferFlag.OF_INACTIVE)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)

      }
    }
  }
}
