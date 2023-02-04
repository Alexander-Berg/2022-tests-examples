package ru.yandex.vos2.watching.stages.notifications

import java.time.ZonedDateTime.ofInstant
import java.time.temporal.ChronoField
import java.time.{Instant, ZoneId}
import java.util.concurrent.ThreadLocalRandom

import com.google.protobuf.Timestamp
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.proto.seller.ProductTypes
import ru.yandex.realty.vos.model.notification.NotificationType._
import ru.yandex.vertis.protobuf.BasicProtoFormats
import ru.yandex.vos2.OfferModel.{Offer, OfferService}
import ru.yandex.vos2.UserModel.User
import ru.yandex.vertis.vos2.model.notifications.{DigestKind, Notification, NotificationChannel, SentDigest}
import ru.yandex.vos2.config.TestRealtySchedulerComponents
import ru.yandex.vos2.model.notifications.SubscriptionUpdateBuilder
import ru.yandex.vos2.model.notifications.digests.Digest
import ru.yandex.vos2.realty.model.RealtyNotificationManager
import ru.yandex.vos2.watching.subscriptions.NotificationEngine
import ru.yandex.realty.vos.model.notification.{
  NotificationType,
  RenewalNotificationPayload,
  Notification => ProtoNotification
}
import ru.yandex.vos2.realty.services.notifications.builders.RenewalsNotificationBuilders

import scala.concurrent.duration._
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class RenewalDigestSpec extends WordSpec with Matchers with BasicProtoFormats {

  private val timeMoment = System.currentTimeMillis()

  private val user = User
    .newBuilder()
    .setUserRef("Some ref")
    .build()

  private val sampleOffer = {
    val id = ThreadLocalRandom.current().nextLong()
    Offer
      .newBuilder()
      .setUserRef("")
      .setUser(user)
      .setOfferID(s"i_$id")
      .setOfferIRef(id)
      .setOfferService(OfferService.OFFER_REALTY)
      .setTimestampUpdate(timeMoment)
      .setTimestampCreate(timeMoment)
      .build()
  }

  private val sampleSubscription = RealtyNotificationManager.initSubscription(user)

  private val engine = new NotificationEngine(new TestRealtySchedulerComponents)

  "Renewal digests" should {

    "be collected and sent" in {
      val notifications = createNotifications()
      val issueId = Digest
        .of(DigestKind.DK_RENEWALS)
        .issueOf(Instant.ofEpochMilli(timeMoment))
      val subscription = new SubscriptionUpdateBuilder(RealtyNotificationManager, user, sampleSubscription)
        .addAll(notifications, sampleOffer)
        .build()
        .get
        ._1

      subscription.getDoneList.asScala shouldBe empty
      subscription.getNotificationQueueList.asScala shouldNot be(empty)

      val now = ofInstant(Instant.now(), ZoneId.of("Europe/Moscow"))
        .`with`(ChronoField.HOUR_OF_DAY, 12)
        .toInstant
        .toEpochMilli
      val update = engine.process(subscription, user, now).getUpdate.get
      update.getDoneList.asScala shouldBe empty
      update.getNotificationQueueList.asScala should be(empty)
      val digests = update.getSentDigestList.asScala.filter(_.getKind == DigestKind.DK_RENEWALS)
      digests should have size 2
      assert(digests.forall(_.getIssueId == issueId))

      new SubscriptionUpdateBuilder(RealtyNotificationManager, user, update)
        .addAll(notifications, sampleOffer)
        .build() shouldBe empty
    }

    "retry partially failed issues" in {
      val notifications = createNotifications()
      val issueId = Digest
        .of(DigestKind.DK_RENEWALS)
        .issueOf(Instant.ofEpochMilli(timeMoment))
      val subscription = new SubscriptionUpdateBuilder(RealtyNotificationManager, user, sampleSubscription)
        .addAll(notifications, sampleOffer)
        .build()
        .get
        ._1
        .toBuilder
        .addSentDigest(
          SentDigest
            .newBuilder()
            .setChannel(NotificationChannel.NC_EMAIL)
            .setKind(DigestKind.DK_RENEWALS)
            .setIssueId(issueId)
        )
        .build()

      subscription.getDoneList.asScala shouldBe empty
      subscription.getNotificationQueueList.asScala shouldNot be(empty)

      val now = ofInstant(Instant.now(), ZoneId.of("Europe/Moscow"))
        .`with`(ChronoField.HOUR_OF_DAY, 12)
        .toInstant
        .toEpochMilli
      val update = engine.process(subscription, user, now).getUpdate.get
      update.getDoneList.asScala shouldBe empty
      update.getNotificationQueueList.asScala should be(empty)
      val digests = update.getSentDigestList.asScala.filter(_.getKind == DigestKind.DK_RENEWALS)
      digests should have size 2
      assert(digests.forall(_.getIssueId == issueId))

      new SubscriptionUpdateBuilder(RealtyNotificationManager, user, update)
        .addAll(notifications, sampleOffer)
        .build() shouldBe empty
    }

    "be sent only at appropriate moment" in {
      val notifications = createNotifications()
      val subscription = new SubscriptionUpdateBuilder(RealtyNotificationManager, user, sampleSubscription)
        .addAll(notifications, sampleOffer)
        .build()
        .get
        ._1

      subscription.getDoneList.asScala shouldBe empty
      subscription.getNotificationQueueList.asScala shouldNot be(empty)

      val now = ofInstant(Instant.now(), ZoneId.of("Europe/Moscow"))
        .`with`(ChronoField.HOUR_OF_DAY, 10)
        .toInstant
        .toEpochMilli
      val update = engine.process(subscription, user, now)
      update.getUpdate shouldBe empty
      update.getVisitDelay shouldBe defined
      update.getVisitDelay.get.isFinite shouldBe true
      update.getVisitDelay.get.toDuration(1.milli) shouldNot be(0)
    }
  }

  private def createNotifications(): List[Notification] =
    List(RENEWAL_FAILED, RENEWAL_STOPPED, RENEWAL_SUCCEEDED)
      .map(buildNotification)
      .map(RenewalsNotificationBuilders.RenewalEvent(_, sampleOffer))
      .map(_.build())

  private def buildNotification(`type`: NotificationType): ProtoNotification = {
    val renewalPayload = RenewalNotificationPayload
      .newBuilder()
      .setOfferId(sampleOffer.getOfferIRef.toString)
      .setProductId(ThreadLocalRandom.current().nextLong().toString)
      .setType(ProductTypes.PRODUCT_TYPE_PREMIUM)
      .setPrice(15000)

    ProtoNotification
      .newBuilder()
      .setType(`type`)
      .setEventTime(buildTimestamp(timeMoment))
      .setRenewal(renewalPayload)
      .build()
  }

  protected def buildTimestamp(time: Long): Timestamp =
    Timestamp.newBuilder().setSeconds(time / 1000).build()
}
