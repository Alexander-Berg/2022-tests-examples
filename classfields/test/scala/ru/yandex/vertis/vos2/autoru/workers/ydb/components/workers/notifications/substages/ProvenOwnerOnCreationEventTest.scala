package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications.substages

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType.{CELEBRITY_SELLER, OFFER_ACTIVATION, PROVEN_OWNER_ON_CREATION}
import ru.yandex.vos2.AutoruModel.AutoruOffer.{CarDocuments, ChosenRecognizedLp, Notification, SourceInfo}
import ru.yandex.vos2.AutoruModel.AutoruOffer.SourceInfo.Platform
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.model.AutoruModelUtils.{AutoruModelRichOffer, AutoruRichOfferBuilder}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder

import scala.concurrent.duration.DurationInt

class ProvenOwnerOnCreationEventTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val mockedFeatureManager = mock[FeaturesManager]

    val worker = new ProvenOwnerOnCreationEvent(
      mockedFeatureManager
    )
  }

  private def buildDocuments: CarDocuments.Builder = {
    CarDocuments.newBuilder
      .setVin("QWERTY12345")
      .setLicensePlate("e123ee")
  }

  private def buildRecognizedLp: ChosenRecognizedLp.Builder = {
    ChosenRecognizedLp.newBuilder
      .setLicensePlate("e123ee")
      .setVersion(1)
      .setSourceHash("qwerty")
  }

  "Creation notify" should {
    "send" when {
      "created now" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
          )
          .build()

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
        assert(resultOffer.hasNotificationByType(PROVEN_OWNER_ON_CREATION))
        assert(resultOffer.getOfferAutoru.getNotifications(0).getMaxTries == 1)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasNumTries)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampSent)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampCancel)
      }

      "has another notification" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.ANDROID))
              .setDocuments(buildDocuments)
          )
          .putNotificationByType(OFFER_ACTIVATION, isCritical = false)
          .build()

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 2)
        assert(resultOffer.hasNotificationByType(OFFER_ACTIVATION))
        assert(resultOffer.hasNotificationByType(PROVEN_OWNER_ON_CREATION))
      }

      "has recognized lp" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(CarDocuments.newBuilder.setVin("QWERTY12345"))
              .setRecognizedLp(buildRecognizedLp)
          )
          .build()

        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
        assert(resultOffer.hasNotificationByType(PROVEN_OWNER_ON_CREATION))
        assert(resultOffer.getOfferAutoru.getNotifications(0).getMaxTries == 1)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasNumTries)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampSent)
        assert(!resultOffer.getOfferAutoru.getNotifications(0).hasTimestampCancel)
      }
    }

    "skip" when {
      "is old" in new Fixture {
        val offer = TestUtils
          .createOffer(getNow - 2.day.toMillis)
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "already sended" in new Fixture {
        val ts = getNow - 1.hour.toMillis
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
              .addNotifications(
                Notification
                  .newBuilder()
                  .setIsCritical(false)
                  .setMaxTries(1)
                  .setNumTries(1)
                  .setTimestampCreate(ts)
                  .setTimestampSent(ts)
                  .setType(PROVEN_OWNER_ON_CREATION)
              )
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)

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
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer(dealer = true)
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
          )
          .build()

        val result = worker.process(offer, None)
        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "seller is reseller" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
              .setReseller(true)
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "is not used" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
              .setSection(Section.NEW)
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "is not cars" in new Fixture {
        val offer = TestUtils
          .createOffer(category = Category.TRUCKS)
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer(category = Category.TRUCKS)
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "platform is not mobile" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setDocuments(buildDocuments)
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "has no vin" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setDocuments(CarDocuments.newBuilder.setLicensePlate("e123ee"))
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "has no lp" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setDocuments(CarDocuments.newBuilder.setVin("QWERTY12345"))
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "has flag of need activation" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer(dealer = true)
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
          )
          .putFlag(OfferFlag.OF_NEED_ACTIVATION)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "is draft" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer(dealer = true)
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
          )
          .putFlag(OfferFlag.OF_DRAFT)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "is not active" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer(dealer = true)
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments)
          )
          .putFlag(OfferFlag.OF_INACTIVE)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "not registered in Russia" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments.setNotRegisteredInRussia(true))
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "document photos are not empty" in new Fixture {
        val offer = TestUtils
          .createOffer()
          .setOfferAutoru(
            TestUtils
              .createAutoruOffer()
              .setSourceInfo(SourceInfo.newBuilder.setPlatform(Platform.IOS))
              .setDocuments(buildDocuments.setNotRegisteredInRussia(false))
              .addDocumentPhoto(TestUtils.createPhoto("xxx"))
          )
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }
    }
  }

}
