package ru.yandex.auto.vin.decoder.state

import com.google.protobuf.util.Timestamps
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.notifications.PurchaseSummaryNotify
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.PurchaseSummaryNotificationState
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.PurchaseSummaryNotificationState.{Purchase, PurchaseSummaryInfo}
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.StateUpdateHistory.StateUpdateSource

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters.ListHasAsScala

class PurchaseSummaryNotificationStateTest extends AnyWordSpecLike with Matchers {

  private val offerId = "1095852360-ae3f6b0b"

  "PartnerState[PurchaseSummaryNotificationState.Builder]" should {

    val partnerState = PurchaseSummaryNotify

    "not update state if StateUpdateSource is not BUY" in {
      val trigger = PartnerRequestTrigger(StateUpdateSource.BUY_GIBDD, Some("user-id"), Some(123L), Some(offerId), None)
      val notificationState = PurchaseSummaryNotificationState.newBuilder.build
      partnerState.schedule(notificationState)(trigger)
      notificationState.getNotificationsCount should be(0)
    }

    "not update state if offerId is not present" in {
      val trigger = PartnerRequestTrigger(
        StateUpdateSource.BUY,
        userId = Some("user-id"),
        clientId = Some(123L),
        offerId = None,
        optOrderId = None
      )
      val notificationState = PurchaseSummaryNotificationState.newBuilder.build
      partnerState.schedule(notificationState)(trigger)
      notificationState.getNotificationsCount should be(0)
    }

    "add purchase into existing notifications if present for today" in {
      val trigger =
        PartnerRequestTrigger(StateUpdateSource.BUY, Some("user-id"), Some(123L), Some(offerId), optOrderId = None)
      val scheduledFor = Instant
        .now()
        .truncatedTo(ChronoUnit.DAYS)
        .plus(24 + 10, ChronoUnit.HOURS)
      var notificationState = PurchaseSummaryNotificationState
        .newBuilder()
        .addNotifications(
          buildPush(
            scheduledFor,
            List(
              buildPurchase(Instant.now())
            )
          )
        )
        .build
      notificationState = partnerState.schedule(notificationState)(trigger)
      notificationState.getNotificationsCount shouldBe 1
      notificationState.getNotificationsList.asScala.foreach { info =>
        info.getCreateTimestamp shouldNot be(0)
        info.getSentTimestamp should be(0)
        info.getPurchasesCount should be(2)
        info.getOfferId should be(offerId)
        info.getPurchasesList.asScala.lastOption.foreach { purchase =>
          purchase.getUserId should be("user-id")
          purchase.getClientId should be(123L)
          purchase.getTimestamp shouldNot be(0)
        }
      }
    }

    "create new notification and save purchase history if none notification was present yet" in {
      val trigger =
        PartnerRequestTrigger(StateUpdateSource.BUY, Some("user-id"), Some(123L), Some(offerId), optOrderId = None)
      var notificationState = PurchaseSummaryNotificationState.newBuilder.build
      notificationState = partnerState.schedule(notificationState)(trigger)
      notificationState.getNotificationsCount shouldBe 1
      notificationState.getNotificationsList.asScala.foreach { info =>
        info.getCreateTimestamp shouldNot be(0)
        info.getSentTimestamp should be(0)
        info.getPurchasesCount should be(1)
        info.getOfferId should be(offerId)
        info.getPurchasesList.asScala.foreach { purchase =>
          purchase.getUserId should be("user-id")
          purchase.getClientId should be(123L)
          purchase.getTimestamp shouldNot be(0)
        }
      }
    }

    "create new notification if existing is not for today" in {
      val trigger = PartnerRequestTrigger(
        StateUpdateSource.BUY,
        Some("user-id-1234"),
        Some(1234L),
        Some(offerId),
        optOrderId = None
      )
      val yesterday = Instant
        .now()
        .truncatedTo(ChronoUnit.DAYS)
        .minus(1, ChronoUnit.DAYS)
      var notificationState = PurchaseSummaryNotificationState
        .newBuilder()
        .addNotifications(
          buildPush(
            Instant.ofEpochMilli(0),
            List(
              buildPurchase(Instant.now())
            )
          ).setCreateTimestamp(yesterday.toEpochMilli)
        )
        .build
      notificationState = partnerState.schedule(notificationState)(trigger)
      notificationState.getNotificationsCount shouldBe 2
      val notifications = notificationState.getNotificationsList.asScala
      notifications.foreach {
        _.getPurchasesCount should be(1)
      }
      notifications.lastOption.foreach { info =>
        info.getCreateTimestamp shouldNot be(0)
        info.getSentTimestamp should be(0)
        info.getPurchasesCount should be(1)
        info.getOfferId should be(offerId)
        info.getPurchasesList.asScala.lastOption.foreach { purchase =>
          purchase.getUserId should be("user-id-1234")
          purchase.getClientId should be(1234L)
          purchase.getTimestamp shouldNot be(0)
        }
      }
    }
  }

  private def buildPush(
      scheduledFor: Instant,
      purchases: List[Purchase.Builder] = List.empty): PurchaseSummaryInfo.Builder = {
    val info = PurchaseSummaryInfo
      .newBuilder()
      .setScheduledForTimestamp(Timestamps.fromMillis(scheduledFor.toEpochMilli))
      .setCreateTimestamp(System.currentTimeMillis())
      .setOfferId(offerId)
    purchases.foreach(info.addPurchases)
    info
  }

  private def buildPurchase(timestamp: Instant): Purchase.Builder = {
    Purchase
      .newBuilder()
      .setClientId(1L)
      .setUserId("user-id")
      .setTimestamp(timestamp.toEpochMilli)
  }
}
