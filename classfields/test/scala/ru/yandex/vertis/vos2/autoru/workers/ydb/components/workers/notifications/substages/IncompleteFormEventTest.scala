package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications.substages

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Equipment
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType.{CELEBRITY_SELLER, INCOMPLETE_FORM}
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.{Duration, DurationInt}

class IncompleteFormEventTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty
  val mockedFeatureManager = mock[FeaturesManager]

  val pushesForTesting = mock[Feature[Boolean]]

  when(pushesForTesting.value).thenReturn(false)
  when(mockedFeatureManager.PushesForTesting).thenReturn(pushesForTesting)

  abstract private class Fixture {

    val worker = new IncompleteFormEvent(
      mockedFeatureManager
    )
  }

  "Push" should {
    "send" when {
      "created 5 minutes ago" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val ts: Long = now - 5.minutes.toMillis

        val offer: Offer = createOffer(ts).build()
        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
        assert(worker.shouldProcess(offer, None).shouldProcess)
        assert(result.nextCheck.nonEmpty)
      }

      "created 1 day ago" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)

        val now: Long = getNow
        val ts: Long = now - 1.days.toMillis
        val tsPush: Seq[Long] = Seq(now - 1.days.toMillis)

        val offerBuilder: Offer.Builder = createOffer(ts)
        addSendPuh(offerBuilder, tsPush)

        val offer: Offer = offerBuilder.build()
        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 2)
        assert(worker.shouldProcess(offer, None).shouldProcess)

        assert(result.nextCheck.isEmpty)
      }

      "has no panoramas and feature flag is true" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(true)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)

        val now: Long = getNow
        val ts: Long = now - 1.days.toMillis
        val tsPush: Seq[Long] = Seq(now - 1.days.toMillis)

        val offerBuilder: Offer.Builder =
          createOffer(ts, withDescription = true, withEquipment = true, withPhoto = true)
        addSendPuh(offerBuilder, tsPush)

        val offer: Offer = offerBuilder.build()
        val result = worker.process(offer, None)
        val resultOffer = result.updateOfferFunc.get(offer)

        assert(resultOffer.getOfferAutoru.getNotificationsCount == 2)
        assert(worker.shouldProcess(offer, None).shouldProcess)

        assert(
          resultOffer.getOfferAutoru.getNotificationsList.asScala
            .filter(_.getType == INCOMPLETE_FORM)
            .filter(_.getExtraArgsList.contains("панорамы"))
            .isEmpty == false
        )
      }

    }

    "skip" when {
      "created now" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val offer: Offer = createOffer(getNow).build()
        val result = worker.process(offer, None)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)
        assert(result.nextCheck.nonEmpty)
      }

      "created 1 hour ago" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)

        val now: Long = getNow
        val tsCreate: Long = now - 1.hours.toMillis
        val tsPush: Seq[Long] = Seq(now - 1.hours.toMillis)
        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPush)

        val offer: Offer = offerBuilder.build()
        val result = worker.process(offer, None)
        assert(worker.shouldProcess(offer, None).shouldProcess)
        assert(result.updateOfferFunc.isEmpty)

        assert(offer.getOfferAutoru.getNotificationsCount == 1)
        assert(result.nextCheck.nonEmpty)
      }

      "created 6 days ago" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val tsCreate: Long = now - 6.days.toMillis
        val tsPushes: Seq[Long] = Seq(tsCreate + 3.days.toMillis, tsCreate + 5.days.toMillis)
        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPushes)
        val offer: Offer = offerBuilder.build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)

      }

      "created 8 days ago" in new Fixture {

        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val tsCreate: Long = now - 8.days.toMillis
        val tsPushes: Seq[Long] = Seq(now - 5.days.toMillis, now - 3.days.toMillis, now - 1.days.toMillis)

        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPushes)

        val offer: Offer = offerBuilder.build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)

      }
    }

    "not sent" when {
      "seller is dealer" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val offer: Offer = createOffer(dealer = true).build()

        val result = worker.process(offer, None)
        assert(!worker.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)
      }

      "has panoramas and feature flag is true" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(true)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val offer: Offer =
          createOffer(withDescription = true, withEquipment = true, withPhoto = true, withExternalPanorama = true)
            .build()
        val result = worker.process(offer, None)
        assert(worker.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)
      }

      "has panoramas and feature flag is false" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val ts: Long = now - 1.days.toMillis
        val tsPush: Seq[Long] = Seq(now - 1.days.toMillis)

        val offerBuilder: Offer.Builder =
          createOffer(ts, withDescription = true, withEquipment = true, withPhoto = true)
        addSendPuh(offerBuilder, tsPush)
        val offer: Offer = offerBuilder.build()
        assert(worker.shouldProcess(offer, None).shouldProcess)

        val result = worker.process(offer, None)

        assert(offer.getOfferAutoru.getNotificationsCount == 1)
        assert(result.updateOfferFunc.isEmpty)
        assert(
          offer.getOfferAutoru.getNotificationsList.asScala
            .filter(_.getType == INCOMPLETE_FORM)
            .filter(_.getExtraArgsList.contains("панорамы"))
            .isEmpty == true
        )
      }

      "already sent" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val tsCreate: Long = now - 3.days.toMillis
        val tsPush: Long = now

        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        offerBuilder.getOfferAutoruBuilder
          .addNotificationsBuilder()
          .setTimestampCreate(getNow)
          .setIsCritical(false)
          .setMaxTries(1)
          .setType(INCOMPLETE_FORM)
          .setTimestampCreate(tsPush)
          .setTimestampSent(tsPush)
        val offer: Offer = offerBuilder.build()
        assert(worker.shouldProcess(offer, None).shouldProcess)

        val result = worker.process(offer, None)
        assert(result.updateOfferFunc.isEmpty)

        assert(offer.getOfferAutoru.getNotificationsCount == 1)
        assert(result.nextCheck.nonEmpty)
      }

      "category is not CARS" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis
        val offerBuilder: Offer.Builder = createOffer(ts)
        offerBuilder.getOfferAutoruBuilder.setCategory(Category.TRUCKS)
        val offer: Offer = offerBuilder.build()

        val result = worker.process(offer, None)
        assert(!worker.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
      }

      "form is completed" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis
        val offer: Offer = createCompleteOffer(ts).build()

        val result = worker.process(offer, None)
        assert(result.updateOfferFunc.isEmpty)
        assert(worker.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.nextCheck.isEmpty)
      }

      "is draft" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis
        val offer: Offer = createOffer(ts)
          .putFlag(OfferFlag.OF_DRAFT)
          .build()

        assert(!worker.shouldProcess(offer, None).shouldProcess)
      }

      "already sent 3 pushes" in new Fixture {
        val mockedFeature = mock[Feature[Boolean]]
        when(mockedFeature.value).thenReturn(false)
        when(mockedFeatureManager.SendIncompletePushPanoramas).thenReturn(mockedFeature)
        val now: Long = getNow
        val tsCreate: Long = now - 12.days.toMillis
        val tsPushes: Seq[Long] = Seq(now - 9.days.toMillis, now - 7.days.toMillis, now - 5.days.toMillis)
        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPushes)
        val offer: Offer = offerBuilder.build()
        assert(!worker.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 3)
      }
    }
  }

  def createOffer(now: Long = System.currentTimeMillis(),
                  dealer: Boolean = false,
                  withPhoto: Boolean = false,
                  withEquipment: Boolean = false,
                  withDescription: Boolean = false,
                  withExternalPanorama: Boolean = false): Offer.Builder = {
    val offerBuilder = TestUtils.createOffer(
      now,
      dealer,
      withPhoto = withPhoto,
      withEquipment = withEquipment,
      withDescription = withDescription,
      withExternalPanorama = withExternalPanorama
    )
    offerBuilder
  }

  def createCompleteOffer(now: Long = System.currentTimeMillis(), dealer: Boolean = false): Offer.Builder = {
    val offerBuilder = createOffer(now, dealer)
    val offerAutoruBuilder = offerBuilder.getOfferAutoruBuilder
      .addPhoto(TestUtils.createPhoto("photo"))
    val equipmentBuilder = Equipment
      .newBuilder()
      .setName("equipment")
      .setEquipped(true)
    offerAutoruBuilder.getCarInfoBuilder
      .addEquipment(equipmentBuilder)
    offerBuilder.setDescription("description")
  }

  def addSendPuh(offerBuilder: Offer.Builder, pushes: Seq[Long]): Offer.Builder = {
    for (tsPush <- pushes) {
      offerBuilder.getOfferAutoruBuilder
        .addNotificationsBuilder()
        .setIsCritical(false)
        .setTimestampCreate(getNow)
        .setMaxTries(1)
        .setType(INCOMPLETE_FORM)
        .setTimestampCreate(tsPush)
        .setTimestampSent(tsPush)
    }
    offerBuilder
  }

}
