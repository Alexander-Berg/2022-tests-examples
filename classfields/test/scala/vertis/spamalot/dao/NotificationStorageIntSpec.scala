package vertis.spamalot.dao

import cats.syntax.option._
import monocle.macros.GenLens
import monocle.syntax.all._
import org.scalacheck.magnolia._
import ru.yandex.vertis.spamalot.inner.StoredNotification
import ru.yandex.vertis.ydb.Ydb
import vertis.core.utils.NoWarnFilters
import vertis.spamalot.dao.model.{Page, Pagination}
import vertis.spamalot.model.ReceiverId
import vertis.spamalot.{SpamalotOptics, SpamalotYdbTest}
import vertis.zio.test.ZioSpecBase
import zio.{UIO, ZIO}

import java.time.Instant
import scala.annotation.nowarn

/** @author kusaeva
  */
@nowarn(NoWarnFilters.Deprecation)
//todo: add more tests - listing, canceling
class NotificationStorageIntSpec extends ZioSpecBase with SpamalotYdbTest with SpamalotOptics {

  private lazy val storage = storages.notificationStorage

  "NotificationStorage" should {
    "upsert" in ydbTest {
      val notification = random[StoredNotification].copy(topic = "personal_recommendations", id = getUniqString)
      for {
        _ <- Ydb.runTx(storage.upsertOne(notification))
      } yield ()
    }

    "not insert twice" in ydbTest {
      val notification = random[StoredNotification].copy(userId = getUniqString, id = getUniqString)
      for {
        added <- Ydb.runTx(storage.insertOne(notification))
        replaced <- Ydb.runTx(storage.insertOne(notification))
        _ <- check("")(added shouldBe true)
        _ <- check("")(replaced shouldBe false)
      } yield ()
    }

    "not upsert invalid notifications" in intercept[IllegalArgumentException] {
      ydbTest {
        val notification = random[StoredNotification].copy(id = "")
        Ydb.runTx(storage.upsertOne(notification))
      }
    }

    "list all" in ydbTest {
      val receiverId = random[ReceiverId]
      val notifications =
        random[StoredNotification](3).map(n =>
          n.copy(receiverId = receiverId.proto.some, notificationObject = None, topic = "topic", id = getUniqString)
        )
      for {
        _ <- ZIO.foreach(notifications) { n =>
          Ydb.runTx(storage.upsertOne(n))
        }
        result <- Ydb.runTx(storage.list(receiverId, pagination = Pagination.default))
        _ <- check(result.items.map(_.id) should contain theSameElementsAs notifications.map(_.id))
      } yield ()
    }

    "list by topic with paging" in ydbTest {
      val topic = "topic"
      val topics = Set(topic)
      val receiverId = random[ReceiverId]
      val limit = 2
      val notifications =
        random[StoredNotification](5).map(n =>
          n.copy(receiverId = receiverId.proto.some, notificationObject = None, topic = topic, id = getUniqString)
        )
      val pagination = Pagination.default.copy(limit = limit)

      def getNextPage(page: Page) =
        if (page.next.isDefined) {
          Ydb.runTx(storage.list(receiverId, topics = topics, pagination = page.next.get))
        } else {
          UIO(Page.empty)
        }

      for {
        _ <- ZIO.foreach(notifications) { n =>
          Ydb.runTx(storage.upsertOne(n))
        }
        page1 <- Ydb.runTx(storage.list(receiverId, topics = topics, pagination = pagination))
        page2 <- getNextPage(page1)
        page3 <- getNextPage(page2)
        _ <- check(
          (page1.items ++ page2.items ++ page3.items).map(_.id) should contain
            .theSameElementsAs(notifications.map(_.id))
        )
      } yield ()
    }

    "mark read" in ydbTest {
      val receiverId = random[ReceiverId]
      val notifications =
        random[StoredNotification](3).map(n =>
          n.copy(receiverId = receiverId.proto.some, isRead = false, id = getUniqString)
        )
      for {
        _ <- ZIO.foreach(notifications) { n =>
          Ydb.runTx(storage.upsertOne(n))
        }
        _ <- Ydb.runTx(storage.markRead(receiverId, notifications.tail.map(n => n.id)))
        unread <- Ydb.runTx(storage.list(receiverId, newOnly = true, pagination = Pagination.default))
        _ <- check(unread.items.map(_.id) should contain theSameElementsAs Seq(notifications.head.id))
      } yield ()
    }

    "mark read by topic" in ydbTest {
      val receiverId = random[ReceiverId]
      val notifications =
        random[StoredNotification](5).map(n =>
          n.copy(receiverId = receiverId.proto.some, isRead = false, id = getUniqString, topic = getUniqString)
        )
      for {
        _ <- ZIO.foreach(notifications) { n =>
          Ydb.runTx(storage.upsertOne(n))
        }
        _ <- Ydb.runTx(storage.markAllRead(receiverId, topic = Some(notifications.head.topic)))
        unread <- Ydb.runTx(storage.list(receiverId, newOnly = true, pagination = Pagination.default))
        _ <- check(unread.items.map(_.id) should contain theSameElementsAs notifications.tail.map(_.id))
      } yield ()
    }

    "mark all read" in ydbTest {
      val receiverId = random[ReceiverId]
      val notifications =
        random[StoredNotification](3).map(n =>
          n.copy(receiverId = receiverId.proto.some, isRead = false, id = getUniqString)
        )
      for {
        _ <- ZIO.foreach(notifications) { n =>
          Ydb.runTx(storage.upsertOne(n))
        }
        _ <- Ydb.runTx(storage.markAllRead(receiverId, topic = None))
        unread <- Ydb.runTx(storage.list(receiverId, newOnly = true, pagination = Pagination.default))
        _ <- check(unread.items shouldBe empty)
      } yield ()
    }

    "count unread from list" in ydbTest {
      val receiverId = random[ReceiverId]
      val notifications =
        random[StoredNotification](6).map(n => n.copy(receiverId = receiverId.proto.some, id = getUniqString))
      val chosen = notifications.take(3)
      val ids = chosen.map(_.id) ++ random[String](3)
      val count = chosen.count(_.isRead == false)
      for {
        _ <- ZIO.foreach(notifications) { n =>
          Ydb.runTx(storage.upsertOne(n))
        }
        result <- Ydb.runTx(storage.countUnread(receiverId, ids))
        _ <- check(result shouldBe count)
      } yield ()
    }

    "find missing notifications" when {
      "notification has only userId set" in ydbTest {
        val userId = random[ReceiverId.User]
        @annotation.nowarn(NoWarnFilters.Deprecation)
        val notifications = randomOperations(Instant.now, 2, userId).toList
          .focus()
          .each
          .andThen(payloadNotificationOptics)
          .andThen(GenLens[StoredNotification](_.receiverId))
          .replace(None)
          .focus()
          .each
          .andThen(payloadNotificationOptics)
          .andThen(GenLens[StoredNotification](_.userId))
          .replace(userId.userId.value)
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

      "notification has receiverId set" when {
        def testForReceiverId(receiverId: ReceiverId) = {
          val notifications = randomOperations(Instant.now, 2, receiverId).toList
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
            notificationsToFindMissing = notifications
              .focus()
              .each
              .andThen(GenLens[StoredNotification](_.receiverId))
              .replace(receiverId.proto.some)
            missing <- Ydb.runTx(storages.notificationStorage.findMissing(receiverId, notificationsToFindMissing))
            _ <- check(missing should contain theSameElementsAs expectedNotifications)
          } yield ()
        }

        "receiver is user" in ydbTest {
          val receiverId = random[ReceiverId.User]
          testForReceiverId(receiverId)
        }

        "receiver is device" in ydbTest {
          val receiverId = random[ReceiverId.DeviceId]
          testForReceiverId(receiverId)
        }
      }
    }
  }
}
