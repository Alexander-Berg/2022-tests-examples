package ru.yandex.auto.garage.scheduler.stage.notifications

import auto.carfax.common.utils.tracing.Traced
import com.google.protobuf.util.Timestamps
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.garage.scheduler.stage.GarageCardStageSupport
import ru.yandex.auto.garage.scheduler.stage.notifications.NotificationsStage.NotificationWithIndex
import ru.yandex.auto.garage.scheduler.stage.notifications.NotificationsTestUtils._
import ru.yandex.auto.garage.utils.NotificationUtils._
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.Notification.NotificationType
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, ExactDelay, WatchingStateUpdate}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.mockito.MockitoSupport

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class NotificationStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with GarageCardStageSupport[NotificationsStage] {

  private val processor = mock[NotificationProcessor]
  private val stage = createProcessingStage()

  implicit val t: Traced = Traced.empty

  before {
    reset(processor)
  }

  private val emptyCard = GarageCard.newBuilder().build()

  "processNotification" should {
    "cancel notification" when {
      "deadline exceeded" in {
        val notification = notificationTemplate(
          deadline = Timestamps.fromSeconds(LocalDateTime.now().minusDays(1).toEpochSecond(ZoneOffset.UTC))
        )
        val res = stage.processNotification(0, emptyCard, notification).await
        res.hasTimestampCancel shouldBe true
        res.hasTimestampSent shouldBe false
      }
      "numTries > maxRetries" in {
        val notification = notificationTemplate(numTries = 5)
        val res = stage.processNotification(0, emptyCard, notification).await
        res.hasTimestampCancel shouldBe true
        res.hasTimestampSent shouldBe false
      }
    }
    "return processor result" when {
      "notifications need to sent" in {
        val notification = notificationTemplate()
        val updatedNotification = notification.markAsSent
        when(processor.process(?, ?, ?)(?)).thenReturn(Future.successful(updatedNotification))
        val res = stage.processNotification(0, emptyCard, notification).await
        res shouldBe updatedNotification
      }
    }
  }

  "processNotificationsBatch" should {
    "handle errors" in {
      val n1 = notificationTemplate(NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)
      val n2 = notificationTemplate(NotificationType.CREATE_CARD_FOR_PREDICTED_BUYER)

      val updatedN1 = n1.markAsCancelled

      when(processor.process(?, ?, eq(n1))(?)).thenReturn(Future.successful(updatedN1))
      when(processor.process(?, ?, eq(n2))(?)).thenReturn(Future.failed(new RuntimeException("")))

      val res = stage
        .processNotificationsBatch(
          0,
          emptyCard,
          List(
            NotificationWithIndex(n1, 1),
            NotificationWithIndex(n2, 2)
          )
        )
        .await

      res shouldBe List(NotificationWithIndex(updatedN1, 1))
    }
  }

  "stage.process" should {
    "apply partial updates and reschedule" in {
      val n1 = notificationTemplate(NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)
      val n2 = notificationTemplate(NotificationType.CREATE_CARD_FOR_PREDICTED_BUYER)

      val updatedN1 = n1.markAsCancelled

      when(processor.process(?, ?, eq(n1))(?)).thenReturn(Future.successful(updatedN1))
      when(processor.process(?, ?, eq(n2))(?)).thenReturn(Future.failed(new RuntimeException("")))

      val card = {
        val builder = GarageCard.newBuilder()
        builder.addAllNotifications(List(n1, n2).asJava)
        builder.build()
      }

      val res = stage.processWithAsync(0, createProcessingState(card))
      val updatedNotifications = res.state.getNotificationsList.asScala

      updatedNotifications shouldBe List(updatedN1, n2)
      res.delay shouldBe ExactDelay(10.minutes)
    }
    "apply updates and not reschedule" in {
      val n1 = notificationTemplate(NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)
      val n2 = notificationTemplate(NotificationType.CREATE_CARD_FOR_PREDICTED_BUYER).markAsSent
      val n3 = notificationTemplate(NotificationType.CREATE_CARD_FOR_PREDICTED_BUYER)

      val updatedN1 = n1.markAsCancelled
      val updatedN3 = n1.markAsSent

      when(processor.process(?, ?, eq(n1))(?)).thenReturn(Future.successful(updatedN1))
      when(processor.process(?, ?, eq(n3))(?)).thenReturn(Future.successful(updatedN3))

      val card = {
        val builder = GarageCard.newBuilder()
        builder.addAllNotifications(List(n1, n2, n3).asJava)
        builder.build()
      }

      val res = stage.processWithAsync(0, createProcessingState(card))
      val updatedNotifications = res.state.getNotificationsList.asScala

      updatedNotifications shouldBe List(updatedN1, n2, updatedN3)
      res.delay.isDefault shouldBe true
    }
    "retry and reschedule" in {
      val n1 = notificationTemplate(NotificationType.PROVEN_OWNER_CHAT_NOTIFICATION)

      val updatedN1 = n1.markAsRetried

      when(processor.process(?, ?, eq(n1))(?)).thenReturn(Future.successful(updatedN1))

      val card = {
        val builder = GarageCard.newBuilder()
        builder.addAllNotifications(List(n1).asJava)
        builder.build()
      }

      val res = stage.processWithAsync(0, createProcessingState(card))
      val updatedNotifications = res.state.getNotificationsList.asScala

      updatedNotifications shouldBe List(updatedN1)
      res.delay shouldBe ExactDelay(10.minutes)
    }
  }

  private def createProcessingState(card: GarageCard): ProcessingState[GarageCard] = {
    ProcessingState(WatchingStateUpdate(card, DefaultDelay(25.hours)))
  }

  override def createProcessingStage(): NotificationsStage = {
    new NotificationsStage(processor)
  }
}
