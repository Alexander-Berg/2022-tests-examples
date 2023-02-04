package vertis.spamalot.dao

import monocle.syntax.all._
import monocle.macros.GenLens
import org.scalacheck.magnolia._
import ru.yandex.vertis.spamalot.inner.StoredNotification
import ru.yandex.vertis.ydb.Ydb
import vertis.core.utils.NoWarnFilters
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.spamalot.{SpamalotOptics, SpamalotYdbTest}
import vertis.zio.test.ZioSpecBase

import java.time.Instant

class OldNotificationStorageIntSpec extends ZioSpecBase with SpamalotYdbTest with SpamalotOptics {

  "find missing notifications" when {
    "notification has only userId set" in ydbTest {
      val userId = random[UserId]
      @annotation.nowarn(NoWarnFilters.Deprecation)
      val notifications = randomOperations(Instant.now, 2, ReceiverId.User(userId)).toList
        .focus()
        .each
        .andThen(payloadNotificationOptics)
        .andThen(GenLens[StoredNotification](_.receiverId))
        .replace(None)
        .focus()
        .each
        .andThen(payloadNotificationOptics)
        .andThen(GenLens[StoredNotification](_.userId))
        .replace(userId.value)
        .focus()
        .each
        .andThen(payloadNotificationOptics)
        .getAll
      val notificationsToInsert = notifications.take(1)
      val expectedNotifications = notifications
        .drop(1)
        .focus()
        .each
        .andThen(GenLens[StoredNotification](_.id))
        .getAll

      for {
        _ <- Ydb.runTx(storages.oldNotificationStorage.upsert(notificationsToInsert))
        missing <- Ydb.runTx(storages.oldNotificationStorage.findMissing(userId, notifications))
        _ <- check(missing should contain theSameElementsAs expectedNotifications)
      } yield ()
    }

    "notification has receiverId set" when {
      "receiver is user" in ydbTest {
        val userId = random[ReceiverId.User]
        val notifications = randomOperations(Instant.now, 2, userId).toList
          .focus()
          .each
          .andThen(payloadNotificationOptics)
          .getAll
        val notificationsToInsert = notifications.take(1)
        val expectedNotifications = notifications
          .drop(1)
          .focus()
          .each
          .andThen(GenLens[StoredNotification](_.id))
          .getAll

        for {
          _ <- Ydb.runTx(storages.notificationStorage.upsert(notificationsToInsert))
          missing <- Ydb.runTx(storages.notificationStorage.findMissing(userId, notifications))
          _ <- check(missing should contain theSameElementsAs expectedNotifications)
        } yield ()
      }
    }
  }
}
