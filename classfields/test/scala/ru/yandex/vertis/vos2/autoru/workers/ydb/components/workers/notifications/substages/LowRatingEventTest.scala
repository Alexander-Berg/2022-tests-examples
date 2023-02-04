package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications.substages

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType.{INCOMPLETE_FORM, LOW_RATING}
import ru.yandex.vos2.AutoruModel.AutoruOffer.PaidService.ServiceType
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.searcher.HttpSearcherClient
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder
import ru.yandex.vos2.util.HttpBlockingPool.Instance
import ru.yandex.vos2.util.http.MockHttpClientHelper

import scala.concurrent.duration.{Duration, DurationInt}

class LowRatingEventTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with BeforeAndAfterAll
  with MockHttpClientHelper {
  implicit private val t = Traced.empty

  abstract private class Fixture {
    val offer: Offer

    val featureValue: Boolean = false
    val mockedFeature = mock[Feature[Boolean]]
    val mockedFeatureManager = mock[FeaturesManager]

    when(mockedFeature.value).thenReturn(featureValue)
    when(mockedFeatureManager.PushesForTesting).thenReturn(mockedFeature)

    def createWorker(status: Int, position: Int): LowRatingEvent = {
      new LowRatingEvent(new HttpSearcherClient("back-rt-01-sas.test.vertis.yandex.net", 34389, 10) {
        override val client =
          new Instance(mockHttpClient(status, generateSearcherResponseById(position)))
      }, mockedFeatureManager)
    }

    def create200LowRatingStage: LowRatingEvent = {
      createWorker(200, LOW_POSITION)
    }
  }

  "Push" should {
    "send" when {
      "created 3 days ago" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis

        val offer: Offer = createOffer(ts).build()
        val stage: LowRatingEvent = create200LowRatingStage

        val dateStart: Long = new DateTime(now)
          .withZone(MOSCOW_TIME_ZONE)
          .withHourOfDay(PUSH_TIME_START)
          .getMillis
        val dateFinish: Long = new DateTime(now)
          .withZone(MOSCOW_TIME_ZONE)
          .withHourOfDay(PUSH_TIME_FINISH)
          .getMillis

        val result = stage.process(offer, None)
        if (now >= dateStart && now <= dateFinish) {
          val resultOffer = result.updateOfferFunc.get(offer)
          assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
        } else {
          val resultOffer = offer
          assert(resultOffer.getOfferAutoru.getNotificationsCount == 0)
        }
        assert(result.nextCheck.nonEmpty)
      }

      "created 5 days ago" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 5.days.toMillis
        val tsPush: Seq[Long] = Seq(now - 2.days.toMillis)

        val offerBuilder: Offer.Builder = createOffer(ts)
        addSendPuh(offerBuilder, tsPush)

        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage

        val dateStart: Long = new DateTime(now)
          .withZone(MOSCOW_TIME_ZONE)
          .withHourOfDay(PUSH_TIME_START)
          .getMillis
        val dateFinish: Long = new DateTime(now)
          .withZone(MOSCOW_TIME_ZONE)
          .withHourOfDay(PUSH_TIME_FINISH)
          .getMillis
        val result = stage.process(offer, None)
        if (now >= dateStart && now <= dateFinish) {
          val resultOffer = result.updateOfferFunc.get(offer)
          assert(resultOffer.getOfferAutoru.getNotificationsCount == 2)
        } else {
          val resultOffer = offer
          assert(resultOffer.getOfferAutoru.getNotificationsCount == 1)
        }
        assert(result.nextCheck.nonEmpty)
      }

      "created 7 days ago" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 7.days.toMillis
        val tsPush: Seq[Long] = Seq(now - 4.days.toMillis, now - 2.days.toMillis)

        val offerBuilder: Offer.Builder = createOffer(ts)

        addSendPuh(offerBuilder, tsPush)

        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage

        val dateStart: Long = new DateTime(now)
          .withZone(MOSCOW_TIME_ZONE)
          .withHourOfDay(PUSH_TIME_START)
          .getMillis
        val dateFinish: Long = new DateTime(now)
          .withZone(MOSCOW_TIME_ZONE)
          .withHourOfDay(PUSH_TIME_FINISH)
          .getMillis
        val result = stage.process(offer, None)
        if (now >= dateStart && now <= dateFinish) {
          val resultOffer = result.updateOfferFunc.get(offer)
          assert(resultOffer.getOfferAutoru.getNotificationsCount == 3)
        } else {
          val resultOffer = offer
          assert(resultOffer.getOfferAutoru.getNotificationsCount == 2)
        }
        assert(result.nextCheck.nonEmpty)
      }

      "created 14 days ago" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 14.days.toMillis
        val tsPush: Seq[Long] = Seq(now - 11.days.toMillis, now - 9.days.toMillis, now - 7.days.toMillis)

        val offerBuilder: Offer.Builder = createOffer(ts)

        addSendPuh(offerBuilder, tsPush)

        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage

        val dateStart: Long = new DateTime(now)
          .withZone(MOSCOW_TIME_ZONE)
          .withHourOfDay(PUSH_TIME_START)
          .getMillis
        val dateFinish: Long = new DateTime(now)
          .withZone(MOSCOW_TIME_ZONE)
          .withHourOfDay(PUSH_TIME_FINISH)
          .getMillis
        val result = stage.process(offer, None)
        if (now >= dateStart && now <= dateFinish) {
          val resultOffer = result.updateOfferFunc.get(offer)
          assert(resultOffer.getOfferAutoru.getNotificationsCount == 4)
          assert(result.nextCheck.isEmpty)
        } else {
          val resultOffer = offer
          assert(resultOffer.getOfferAutoru.getNotificationsCount == 3)
          assert(result.nextCheck.nonEmpty)
        }
      }
    }

    "skip" when {
      "created now" in new Fixture {
        val offer: Offer = createOffer(getNow).build()
        val stage: LowRatingEvent = create200LowRatingStage
        val result = stage.process(offer, None)
        assert(stage.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.nextCheck.nonEmpty)
        assert(result.updateOfferFunc.isEmpty)
      }

      "created 3 days ago but position is high" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis

        val offer: Offer = createOffer(ts).build()
        val stage: LowRatingEvent = createWorker(200, HIGH_POSITION)
        val result = stage.process(offer, None)
        assert(stage.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.nextCheck.nonEmpty)
        assert(result.updateOfferFunc.isEmpty)
      }

      "created 4 days ago" in new Fixture {
        val now: Long = getNow
        val tsCreate: Long = now - 4.days.toMillis
        val tsPush: Seq[Long] = Seq(now - 1.days.toMillis)
        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPush)

        val offer: Offer = offerBuilder.build()

        val stage: LowRatingEvent = create200LowRatingStage
        val result = stage.process(offer, None)
        assert(stage.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 1)
        assert(result.nextCheck.nonEmpty)
        assert(result.updateOfferFunc.isEmpty)
      }

      "created 6 days ago" in new Fixture {
        val now: Long = getNow
        val tsCreate: Long = now - 6.days.toMillis
        val tsPushes: Seq[Long] = Seq(tsCreate + 3.days.toMillis, tsCreate + 5.days.toMillis)
        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPushes)
        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage
        val result = stage.process(offer, None)
        assert(stage.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 2)
        assert(result.nextCheck.nonEmpty)
        assert(result.updateOfferFunc.isEmpty)
      }

      "created 8 days ago" in new Fixture {
        val now: Long = getNow
        val tsCreate: Long = now - 8.days.toMillis
        val tsPushes: Seq[Long] = Seq(now - 5.days.toMillis, now - 3.days.toMillis, now - 1.days.toMillis)
        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPushes)

        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage
        val result = stage.process(offer, None)
        assert(stage.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 3)
        assert(result.nextCheck.nonEmpty)
        assert(result.updateOfferFunc.isEmpty)
      }

      "created 15 days ago" in new Fixture {
        val now: Long = getNow
        val tsCreate: Long = now - 15.days.toMillis
        val tsPushes: Seq[Long] =
          Seq(now - 12.days.toMillis, now - 10.days.toMillis, now - 8.days.toMillis, now - 1.day.toMillis)
        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPushes)

        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage
        assert(!stage.shouldProcess(offer, None).shouldProcess)
        assert(offer.getOfferAutoru.getNotificationsCount == 4)

      }
    }

    "not send" when {
      "seller is dealer" in new Fixture {
        val offer: Offer = createOffer(dealer = true).build()
        val stage: LowRatingEvent = create200LowRatingStage
        val result = stage.process(offer, None)
        assert(!stage.shouldProcess(offer, None).shouldProcess)
      }

      "already sended" in new Fixture {
        val now: Long = getNow
        val tsCreate: Long = now - 3.days.toMillis
        val tsPush: Long = now

        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        offerBuilder.getOfferAutoruBuilder
          .addNotificationsBuilder()
          .setTimestampCreate(getNow)
          .setIsCritical(false)
          .setMaxTries(1)
          .setType(LOW_RATING)
          .setTimestampCreate(tsPush)
          .setTimestampSent(tsPush)

        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage
        val result = stage.process(offer, None)
        assert(stage.shouldProcess(offer, None).shouldProcess)
        assert(offer.getOfferAutoru.getNotificationsCount == 1)
        assert(result.updateOfferFunc.isEmpty)
        assert(result.nextCheck.nonEmpty)
      }

      "searcher return not 200 response" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis
        val offer: Offer = createOffer(ts).build()
        val stage: LowRatingEvent = createWorker(404, -1)
        val result = stage.process(offer, None)
        assert(stage.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.updateOfferFunc.isEmpty)

        assert(result.nextCheck.nonEmpty)
      }

      "category is not CARS" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis
        val offerBuilder: Offer.Builder = createOffer(ts)
        offerBuilder.getOfferAutoruBuilder.setCategory(Category.TRUCKS)

        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage
        val result = stage.process(offer, None)
        assert(!stage.shouldProcess(offer, None).shouldProcess)
      }

      "rating is high" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis
        val offer: Offer = createOffer(ts).build()
        val stage: LowRatingEvent = createWorker(200, HIGH_POSITION)
        val result = stage.process(offer, None)
        assert(stage.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 0)
        assert(result.nextCheck.nonEmpty)
        assert(result.updateOfferFunc.isEmpty)
      }

      "is draft" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis
        val offer: Offer = createOffer(ts)
          .putFlag(OfferFlag.OF_DRAFT)
          .build()
        val stage: LowRatingEvent = create200LowRatingStage
        assert(!stage.shouldProcess(offer, None).shouldProcess)

      }

      "already send 4 puhsed" in new Fixture {
        val now: Long = getNow
        val tsCreate: Long = now - 20.days.toMillis

        val tsPushes: Seq[Long] =
          Seq(now - 17.days.toMillis, now - 15.days.toMillis, now - 13.days.toMillis, now - 6.days.toMillis)
        val offerBuilder: Offer.Builder = createOffer(tsCreate)
        addSendPuh(offerBuilder, tsPushes)

        val offer: Offer = offerBuilder.build()
        val stage: LowRatingEvent = create200LowRatingStage
        assert(!stage.shouldProcess(offer, None).shouldProcess)

        assert(offer.getOfferAutoru.getNotificationsCount == 4)
      }

      "has service top" in new Fixture {
        val now: Long = getNow
        val ts: Long = now - 3.days.toMillis

        val builder: Offer.Builder = createOffer(ts)
        builder.getOfferAutoruBuilder
          .addServicesBuilder()
          .setServiceType(ServiceType.TOP)
          .setCreated(ts)
          .setIsActive(true)

        val offer: Offer = builder.build()
        val stage: LowRatingEvent = create200LowRatingStage
        assert(!stage.shouldProcess(offer, None).shouldProcess)

      }
    }
  }

  val PUSH_TIME_START = 12
  val PUSH_TIME_FINISH = 21
  val MOSCOW_TIME_ZONE: DateTimeZone = DateTimeZone.forID("Europe/Moscow")

  val LOW_POSITION = 40807
  val HIGH_POSITION = 1

  def createOffer(now: Long = System.currentTimeMillis(), dealer: Boolean = false): Offer.Builder = {
    val offerBuilder = TestUtils.createOffer(now, dealer)
    offerBuilder
  }

  def addSendPuh(offerBuilder: Offer.Builder, pushes: Seq[Long]): Offer.Builder = {
    for (tsPush <- pushes) {
      offerBuilder.getOfferAutoruBuilder
        .addNotificationsBuilder()
        .setIsCritical(false)
        .setTimestampCreate(getNow)
        .setMaxTries(1)
        .setType(LOW_RATING)
        .setTimestampCreate(tsPush)
        .setTimestampSent(tsPush)
    }
    offerBuilder
  }

  def generateSearcherResponseById(position: Int): String = {
    s"""{"data": [{"position":$position,"id":"autoru-1054412552"}], "errors": []}"""
  }

}
