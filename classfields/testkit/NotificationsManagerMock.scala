package auto.c2b.lotus.logic.testkit

import auto.c2b.lotus.logic.NotificationsManager
import auto.c2b.lotus.logic.NotificationsManager.SubscriptionUrls
import auto.c2b.lotus.model.Notification
import auto.c2b.lotus.model.errors.LotusError
import zio.{Has, IO, Task, UIO, ULayer, ZIO, ZLayer}

import java.util.UUID

class NotificationsManagerMock extends NotificationsManager.Service {

  override def subscribe(userId: String): IO[LotusError, SubscriptionUrls] =
    IO.succeed(SubscriptionUrls(UUID.randomUUID.toString, UUID.randomUUID().toString))

  override def start(): UIO[Unit] = UIO.unit

  override def sendNotification(notification: Notification): UIO[Boolean] = ZIO.succeed(true)
}

object NotificationsManagerMock {

  val live: ULayer[Has[NotificationsManager.Service]] =
    ZLayer.succeed(new NotificationsManagerMock(): NotificationsManager.Service)
}
