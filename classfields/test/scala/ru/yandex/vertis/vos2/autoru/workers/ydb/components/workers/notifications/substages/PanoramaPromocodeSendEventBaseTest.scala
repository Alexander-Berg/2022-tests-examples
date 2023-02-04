package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications.substages

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType._
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.model.AutoruModelUtils.{AutoruModelRichOffer, AutoruRichOfferBuilder}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.{getNow, OfferModel}

import scala.concurrent.duration._

class PanoramaPromocodeSendEventBaseTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val mockedFeatureManager = mock[FeaturesManager]

    val worker = new PanoramaPromocodeSendEventBase(
      mockedFeatureManager
    )
  }

  "Promocode" should {
    "send" when {
      "all OK" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(withPhoto = true, withExternalPanorama = true)
          .setTimestampCreate(getNow - 2.hours.toMillis)
          .addNotificationByType(PANORAMA_PROMO_SENT, false)
          .build()

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.hasNotificationByType(PANORAMA_PUSHUP_CODE_SENT))
        assert(resultOffer.getOfferAutoru.getNotificationsCount == 2)
      }
    }

    "not send" when {
      "has no notification" in new Fixture {
        val offer: Offer =
          TestUtils
            .createOffer(withPhoto = true, withExternalPanorama = true)
            .setTimestampCreate(getNow - 2.hours.toMillis)
            .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "offer inactive" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(withPhoto = true, withExternalPanorama = true)
          .setTimestampCreate(getNow - 2.hours.toMillis)
          .addNotificationByType(PANORAMA_PROMO_SENT, false)
          .addFlag(OfferModel.OfferFlag.OF_INACTIVE)
          .build()
        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "no panorama" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(withPhoto = true)
          .setTimestampCreate(getNow - 2.hours.toMillis)
          .addNotificationByType(PANORAMA_PROMO_SENT, false)
          .build()
        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "already sent" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(withPhoto = true, withExternalPanorama = true)
          .setTimestampCreate(getNow - 2.hours.toMillis)
          .addNotificationByType(PANORAMA_PROMO_SENT, isCritical = false)
          .addNotificationByType(PANORAMA_PUSHUP_CODE_SENT, isCritical = false)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
        assert(offer.hasNotificationByType(PANORAMA_PUSHUP_CODE_SENT))
        assert(offer.getOfferAutoru.getNotificationsCount == 2)
      }

    }
  }

}
