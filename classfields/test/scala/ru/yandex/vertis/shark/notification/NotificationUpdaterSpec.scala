package ru.yandex.vertis.shark.notification

import cats.implicits._
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.shark.model.{Tag, _}
import ru.yandex.vertis.shark.model.generators.CreditApplicationGen
import ru.yandex.vertis.shark.proto.model.Notification.State
import ru.yandex.vertis.shark.util.RichModel._
import ru.yandex.vertis.zio_baker.util.EmptyString
import zio.clock.Clock
import zio.test._
import zio.test.Assertion._
import zio.test.environment._
import zio.ZIO

import java.time.Instant

object NotificationUpdaterSpec extends DefaultRunnableSpec with CreditApplicationGen {

  private lazy val creditApplicationNotificationUpdaterLayer = Clock.any ++ NotificationUpdater.creditApplicationLive

  private val notifications: Seq[Notification] = Seq(
    notificationSource(idempotencyKey = "0"),
    notificationSource(idempotencyKey = "1"),
    notificationSource(idempotencyKey = "2"),
    notificationSource(idempotencyKey = "3")
  ).map(Notification.fromSource(_, Instant.now))

  private val creditApplication: CreditApplication =
    sampleAutoruCreditApplication().updateNotifications(
      newNotifications = notifications,
      updatedTimestamp = Instant.now
    )

  private def notificationSource(idempotencyKey: String): NotificationSource =
    ChatNotificationSource(
      content = ChatNotification.PayloadContent(EmptyString),
      idempotencyKey = idempotencyKey.some
    )

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("NotificationUpdaterImpl")(
      testM("add") {
        val source = notificationSource(idempotencyKey = "2")
        val res = ZIO.service[NotificationUpdater.Service[CreditApplication]].flatMap { notificationUpdater =>
          notificationUpdater.add(creditApplication, source, Instant.now)
        }
        assertM(res.map(_.notifications))(equalTo(notifications))
          .provideLayer(creditApplicationNotificationUpdaterLayer)
      },
      testM("listNotSent") {
        val sentNotifications = creditApplication.notifications.take(2).map(_.updateState(State.SENT, Instant.now))
        val obj = creditApplication.updateNotifications(notifications.drop(2) ++ sentNotifications, Instant.now)
        val res = ZIO.service[NotificationUpdater.Service[CreditApplication]].flatMap(_.listNotSent(obj))
        assertM(res)(equalTo(notifications.drop(2))).provideLayer(creditApplicationNotificationUpdaterLayer)
      },
      testM("updateState") {
        val ts = Instant.now
        val notificationId = "1".taggedWith[Tag.NotificationId]
        val notificationsWithId = notifications.map { notification =>
          val id = notification.idempotencyKey.getOrElse(EmptyString).taggedWith[Tag.NotificationId]
          notification match {
            case chat: ChatNotification => chat.copy(id = id)
            case push: PushNotification => push.copy(id = id)
          }
        }
        val obj = creditApplication.updateNotifications(notificationsWithId, Instant.now)
        val res = ZIO.service[NotificationUpdater.Service[CreditApplication]].flatMap { notificationUpdater =>
          notificationUpdater.updateState(obj, notificationId, State.CANCELED, ts)
        }
        val expected = notificationsWithId.map { notification =>
          if (notification.id == notificationId) notification.updateState(State.CANCELED, ts)
          else notification
        }
        assertM(res.map(_.notifications))(equalTo(expected)).provideLayer(creditApplicationNotificationUpdaterLayer)
      }
    )
  }
}
