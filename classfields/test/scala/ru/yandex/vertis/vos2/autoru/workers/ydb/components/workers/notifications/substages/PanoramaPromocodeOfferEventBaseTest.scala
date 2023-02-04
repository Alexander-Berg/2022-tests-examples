package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications.substages

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType._
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.AutoruModelUtils.{AutoruModelRichOffer, AutoruRichOfferBuilder}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.{getNow, OfferModel}

import scala.concurrent.duration._

class PanoramaPromocodeOfferEventBaseTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val mockedFeatureManager = mock[FeaturesManager]

    val worker = new PanoramaPromocodeOfferEventBase(
      mockedFeatureManager
    )
  }

  "Promocode offer " should {
    "send" when {

      "user & no panoramas" in new Fixture {
        val offer: Offer =
          TestUtils.createOffer(withPhoto = true).setTimestampCreate(getNow - 2.hours.toMillis).build()

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.hasNotificationByType(PANORAMA_PROMO_SENT))
        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
      }

    }

    "not send" when {

      "has panorama" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(withPhoto = true, withExternalPanorama = true)
          .setTimestampCreate(getNow - 2.hours.toMillis)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "is dealer" in new Fixture {
        val offer: Offer =
          TestUtils.createOffer(dealer = true, withPhoto = true).setTimestampCreate(getNow - 2.hours.toMillis).build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "is moto" in new Fixture {
        val offer: Offer =
          TestUtils
            .createOffer(category = Category.MOTO, withPhoto = true)
            .setTimestampCreate(getNow - 2.hours.toMillis)
            .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "is trucks" in new Fixture {
        val offer: Offer =
          TestUtils
            .createOffer(category = Category.TRUCKS, withPhoto = true)
            .setTimestampCreate(getNow - 2.hours.toMillis)
            .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "offer inactive" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(withPhoto = true)
          .setTimestampCreate(getNow - 2.hours.toMillis)
          .addFlag(OfferModel.OfferFlag.OF_INACTIVE)
          .build()
        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "offer is active less than 1h" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(withPhoto = true)
          .setTimestampCreate(getNow - 2.minutes.toMillis)
          .build()
        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "already sent" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(withPhoto = true)
          .setTimestampCreate(getNow - 2.hours.toMillis)
          .addNotificationByType(PANORAMA_PROMO_SENT, isCritical = false)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
        assert(offer.hasNotificationByType(PANORAMA_PROMO_SENT))
        assert(offer.getOfferAutoru.getNotificationsCount == 1)
      }

    }
  }

}
