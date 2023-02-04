package ru.yandex.auto.vin.decoder.scheduler.stage.notifications

import auto.carfax.common.clients.vos.VosClient
import com.google.protobuf.Timestamp
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.extdata.region.{GeoRegion, Tree}
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.scheduler.stage.CompoundStageSupport
import auto.carfax.common.utils.misc.DateTimeUtils.RichProtoTimestamp
import ru.yandex.vertis.mockito.MockitoSupport

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NotificationEnrichStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with PurchaseSummaryNotificationsSupport
  with CompoundStageSupport[VinCode, NotificationEnrichStage] {

  private val vosClient = mock[VosClient]
  private val tree = mock[Tree]

  "NotificationEnrichStage" should {
    "schedule correct timestamp for offer timezone (positive offset)" in {
      val createdTimestamp = Instant.parse("2007-01-01T23:00:00.00Z") // 3.00 по tz оффера
      val expectedScheduleTimestamp = Instant.parse("2007-01-03T06:00:00.00Z") // 10.00 по tz оффера
      when(vosClient.getOffer(?)(?)).thenReturn(Future.successful {
        val offer = buildVosOffer
        offer.getPrivateSellerBuilder.getLocationBuilder
          .setGeobaseId(20167)
        Some(offer.build())
      })

      val region = GeoRegion(20167, "Ахтубинск", 0, "", "", "", 0, "", "", 14400) // UTC+04:00
      when(tree.findRegion(eq(20167))).thenReturn(Some(region))

      val push = buildPurchaseSummaryNotification(Instant.now)
        .clearScheduledForTimestamp()
        .setCreateTimestamp(createdTimestamp.toEpochMilli)
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder.addNotifications(push)
      val stage = createProcessingStage()
      val update = stage.processWithAsync(vin, state).state.getPurchaseSummaryNotificationState

      checkTimestampBetween(update.getNotifications(0).getScheduledForTimestamp, expectedScheduleTimestamp)
    }

    "schedule correct timestamp for offer timezone (negative offset)" in {
      val createdTimestamp = Instant.parse("2007-01-02T01:00:00.00Z") // 21.00 по tz оффера
      val expectedScheduleTimestamp = Instant.parse("2007-01-02T14:00:00.00Z") // 10.00 по tz оффера
      when(vosClient.getOffer(?)(?)).thenReturn(Future.successful {
        val offer = buildVosOffer
        offer.getPrivateSellerBuilder.getLocationBuilder
          .setGeobaseId(110712)
        Some(offer.build())
      })

      val region = GeoRegion(110712, "Саут-Лейк-Тахо", 0, "", "", "", 0, "", "", -14400) // UTC-04:00
      when(tree.findRegion(eq(110712))).thenReturn(Some(region))

      val push = buildPurchaseSummaryNotification(Instant.now)
        .clearScheduledForTimestamp()
        .setCreateTimestamp(createdTimestamp.toEpochMilli)
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder.addNotifications(push)
      val stage = createProcessingStage()
      val update = stage.processWithAsync(vin, state).state.getPurchaseSummaryNotificationState

      checkTimestampBetween(update.getNotifications(0).getScheduledForTimestamp, expectedScheduleTimestamp)
    }

    "schedule with default timezone if offer wasn't found" in {
      val createdTimestamp = Instant.parse("2007-01-01T10:00:00.00Z") // 13.00 по tz оффера
      val expectedScheduleTimestamp = Instant.parse("2007-01-02T07:00:00.00Z") // 10.00 по tz оффера
      when(vosClient.getOffer(?)(?)).thenReturn(Future.successful(None))

      val push = buildPurchaseSummaryNotification(Instant.now)
        .clearScheduledForTimestamp()
        .setCreateTimestamp(createdTimestamp.toEpochMilli)
      val state = CompoundState.newBuilder()
      state.getPurchaseSummaryNotificationStateBuilder.addNotifications(push)
      val stage = createProcessingStage()
      val update = stage.processWithAsync(vin, state).state.getPurchaseSummaryNotificationState

      checkTimestampBetween(update.getNotifications(0).getScheduledForTimestamp, expectedScheduleTimestamp)
    }
  }

  private def checkTimestampBetween(ts: Timestamp, expected: Instant) = {
    val min = expected.toEpochMilli
    val max = expected.plus(1, ChronoUnit.HOURS).toEpochMilli // т.к. размазываем на час
    val beInRange = be >= min and be <= max
    ts.getMillis should beInRange
  }

  override def createProcessingStage(): NotificationEnrichStage = new NotificationEnrichStage(vosClient, tree)
}
