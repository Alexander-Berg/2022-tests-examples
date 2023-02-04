package vertis.spamalot.services

import com.google.protobuf.timestamp.Timestamp
import monocle.macros.{GenLens, GenPrism}
import monocle.syntax.all._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.magnolia._
import org.scalactic.Equality
import ru.yandex.vertis.spamalot.inner.OperationPayload
import ru.yandex.vertis.spamalot.inner.OperationPayload.Payload
import ru.yandex.vertis.ydb.Ydb
import vertis.core.utils.NoWarnFilters
import vertis.spamalot.dao.model.Pagination
import vertis.spamalot.dao.model.storage.old.{StoredChannel => OldStoredChannel}
import vertis.spamalot.model.{ReceiverId, UserId}

import java.time.Instant

/** @author kusaeva
 */
class ReceiverNotificationServiceOldSpec extends ReceiverNotificationServiceSpecBase {
  implicit private val arbitraryPositiveInt: Arbitrary[Int] = Arbitrary(Gen.posNum[Int])

  "ReceiverNotificationServiceImpl (old spec)" should {
    "return the paginated listing result".which {
      "only contains notifications from the new table" when {
        "there are notifications only in the new table for a receiver" when {
          "no topics, no ids and not only new notifications requested" in ydbTest {
            val receiverId = random[ReceiverId]
            val count = 10
            val operations = randomOperations(Instant.now(), count, receiverId)
            val pagination = Pagination("", Instant.now(), 5)

            for {
              _ <- service.addNotifications(receiverId, operations, Instant.now())
              page <- service.list(receiverId, Set.empty, newOnly = false, pagination)
              _ <- check((page.items should have).length(pagination.limit))
              _ <- check(page.next should not be empty)
            } yield ()
          }

          "filtered by topic" in ydbTest {
            val receiverId = random[ReceiverId]
            val count = 10
            val notificationsToUpdate = 5
            val topicName = "topic"
            val operations = randomOperations(Instant.now(), count, receiverId).toList
              .focus()
              .filterIndex((i: Int) => i < notificationsToUpdate)
              .andThen(topicOptics)
              .replace(topicName)
            val expectedNotificationsLength = operations
              .focus()
              .each
              .andThen(topicOptics)
              .filter(_ == topicName)
              .getAll
              .length
            val pagination = Pagination("", Instant.now(), 5)

            for {
              _ <- service.addNotifications(receiverId, operations, Instant.now())
              page <- service.list(receiverId, Set(topicName), newOnly = false, pagination)
              _ <- check((page.items should have).length(expectedNotificationsLength))
            } yield ()
          }
        }
      }
    }

    "return an option of notification by id and receiver id" when {
      "the notification is present" in ydbTest {
        implicit val tsEquality: Equality[Timestamp] = timestampEquality
        val receiverId = random[ReceiverId]
        val now = Instant.now()
        val operations = randomOperations(now, 1, receiverId)

        for {
          _ <- check((operations should have).length(1))
          operation = operations.head
          addNotification = operation.getAddNotification.notification
          _ <- service.addNotifications(receiverId, operations, now)
          optionStoredNotification <- service.get(receiverId, operation.operationId)
          _ <- check(optionStoredNotification should not be empty)
          storedNotification = optionStoredNotification.get
          _ <- check(addNotification.id should ===(storedNotification.id))
          _ <- check(addNotification.receiverId should ===(storedNotification.receiverId))
          _ <- check(addNotification.topic should ===(storedNotification.topic))
          _ <- check(addNotification.createTs should ===(storedNotification.createTs))
          _ <- check(addNotification.name should ===(storedNotification.name))
          _ <- check(addNotification.payload should ===(storedNotification.payload))
          _ <- check(addNotification.notificationObject should ===(storedNotification.notificationObject))
        } yield ()
      }

      "there is no such notification" in ydbTest {
        val receiverId = random[ReceiverId]
        val notificationId = random[String]

        for {
          storedNotification <- service.get(receiverId, notificationId)
          _ <- check(storedNotification shouldBe empty)
        } yield ()
      }
    }

    "mark read and update unread count" when {
      "unread count is in the new table only" in ydbTest {
        val receiverId = random[ReceiverId]
        val count = 5
        val now = Instant.now()
        val operations = randomOperations(now, count, receiverId)

        for {
          _ <- service.addNotifications(receiverId, operations, now)
          _ <- service.markRead(receiverId, operations.take(2).map(_.getAddNotification.notification.id))
          result <- service.getUnreadCount(receiverId)
          _ <- check(result shouldBe (count - 2))
        } yield ()
      }

      "unread count is in the old table for a user, it" should {
        "also migrate old unread count to new table" in ydbTest {
          val user = random[UserId]
          val userId = ReceiverId.User(user)
          val count = 5
          val now = Instant.now()
          val operations = randomOperations(now, count, userId)

          for {
            _ <- service.addNotifications(userId, operations, now)
            _ <- Ydb.runTx(storages.channelStorage.delete(userId))
            storedChannel = OldStoredChannel(user, count, now)
            _ <- Ydb.runTx(storages.oldChannelStorage.upsert(storedChannel)): @annotation.nowarn(
              NoWarnFilters.Deprecation
            )
            _ <- service.markRead(userId, operations.take(2).map(_.getAddNotification.notification.id))
            result <- service.getUnreadCount(userId)
            _ <- check(result shouldBe (count - 2))
            maybeNewUnreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
            _ <- check(maybeNewUnreadCount should ===(Some(count - 2)))
            maybeOldUnreadCount <- Ydb.runTx(storages.oldChannelStorage.unreadCount(user))
            _ <- check(maybeOldUnreadCount shouldBe empty)
          } yield ()
        }
      }
    }

    "skip invalid ids when marking as read" in ydbTest {
      val receiverId = random[ReceiverId]
      val count = 5
      val now = Instant.now()
      val operations = randomOperations(now, count, receiverId).toList
        .focus()
        .each
        .andThen(GenLens[OperationPayload](_.payload))
        .andThen(GenPrism[Payload, Payload.AddNotification])
        .andThen(GenLens[Payload.AddNotification](_.value.notification.id))
        .modify(_ + "_") // add "_" at the end of each message id, so that "invalid_id" string is never a message id

      for {
        _ <- service.addNotifications(receiverId, operations, now)
        _ <- service.markRead(receiverId, Seq("invalid_id"))
        result <- service.getUnreadCount(receiverId)
        _ <- check(result shouldBe count)
      } yield ()
    }

    "add only new notifications and update counters" in ydbTest {
      val receiverId = random[ReceiverId]
      val total = 5
      val initCount = 2
      val now = Instant.now()
      val operations = randomOperations(now, total, receiverId)
      val (fst, snd) = operations.splitAt(initCount)

      for {
        added <- service.addNotifications(receiverId, fst, now)
        count <- service.getUnreadCount(receiverId)
        _ <-
          check("first notifications:")(
            added should contain theSameElementsAs fst.map(_.getAddNotification.notification)
          )
        _ <- check("first counter:")(count shouldBe fst.length)
        added2 <- service.addNotifications(receiverId, operations ++ operations, now)
        count2 <- service.getUnreadCount(receiverId)
        _ <- check("second notifications:")(
          added2 should contain theSameElementsAs snd.map(_.getAddNotification.notification)
        )
        _ <- check("second counter:")(count2 shouldBe total)
      } yield ()
    }

    "update channel" when {
      "record to update is in the new table and" when {
        "receiver is user" in ydbTest {
          val userId = ReceiverId.User(random[UserId])
          val now = Instant.now()
          val addUnread = random[Int]
          for {
            _ <- Ydb.runTx(service.updateChannel(userId, addUnread, now))
            _ <- Ydb.runTx(service.updateChannel(userId, 1, now))
            count <- service.getUnreadCount(userId)
            _ <- check(count shouldBe (addUnread + 1))
          } yield ()
        }

        "receiver is a device" in ydbTest {
          val deviceId = ReceiverId.DeviceId(random[String])
          val now = Instant.now()
          val addUnread = random[Int]
          for {
            _ <- Ydb.runTx(service.updateChannel(deviceId, addUnread, now))
            _ <- Ydb.runTx(service.updateChannel(deviceId, 1, now))
            count <- service.getUnreadCount(deviceId)
            _ <- check(count shouldBe (addUnread + 1))
          } yield ()
        }
      }

      "record to update is in the old table and" when {
        "receiver is user and the old record" should {
          "be migrated to the new table" in ydbTest {
            val user = random[UserId]
            val userId = ReceiverId.User(user)
            val unreadCount = random[Int]
            val now = Instant.now()
            val oldChannel = OldStoredChannel(user, unreadCount, now)
            for {
              _ <- Ydb.runTx(storages.oldChannelStorage.upsert(oldChannel)): @annotation.nowarn(
                NoWarnFilters.Deprecation
              )
              _ <- Ydb.runTx(service.updateChannel(userId, 1, now))
              count <- service.getUnreadCount(userId)
              _ <- check(count shouldBe (unreadCount + 1))
              maybeNewUnreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(maybeNewUnreadCount should ===(Some(unreadCount + 1)))
              maybeOldUnreadCount <- Ydb.runTx(storages.oldChannelStorage.unreadCount(user))
              _ <- check(maybeOldUnreadCount shouldBe empty)
            } yield ()
          }
        }
      }
    }

    "mark notifications read and update count properly" when {

      "marking all notifications read without a topic" when {
        "the record to update is in new the table" when {
          "receiver is user" in ydbTest {
            val userId = ReceiverId.User(random[UserId])
            val count = 5
            val now = Instant.now()
            val operations = randomOperations(now, count, userId)

            for {
              _ <- service.addNotifications(userId, operations, now)
              _ <- service.markAllRead(userId, None)
              result <- service.getUnreadCount(userId)
              _ <- check(result shouldBe 0)
            } yield ()
          }

          "receiver is device" in ydbTest {
            val deviceId = ReceiverId.DeviceId(random[String])
            val count = 5
            val now = Instant.now()
            val operations = randomOperations(now, count, deviceId)

            for {
              _ <- service.addNotifications(deviceId, operations, now)
              _ <- service.markAllRead(deviceId, None)
              result <- service.getUnreadCount(deviceId)
              _ <- check(result shouldBe 0)
            } yield ()
          }
        }

        "the record to update is in the old table" when {
          "receiver is user" in ydbTest {
            val user = random[UserId]
            val userId = ReceiverId.User(user)
            val count = 5
            val now = Instant.now()
            val operations = randomOperations(now, count, userId)

            for {
              _ <- service.addNotifications(userId, operations, now)
              _ <- Ydb.runTx(storages.channelStorage.delete(userId))
              storedChannel = OldStoredChannel(user, count, now)
              _ <- Ydb.runTx(storages.oldChannelStorage.upsert(storedChannel)): @annotation.nowarn(
                NoWarnFilters.Deprecation
              )
              _ <- service.markAllRead(userId, None)
              result <- service.getUnreadCount(userId)
              _ <- check(result shouldBe 0)
              maybeOldUnread <- Ydb.runTx(storages.oldChannelStorage.unreadCount(user))
              _ <- check(maybeOldUnread shouldBe empty)
              maybeNewUnread <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(maybeNewUnread should ===(Some(0)))
            } yield ()
          }
        }
      }

      // TODO: this test must pass after VERTISTRAF-2363 is implemented
      "marking all notifications read by a particular topic" ignore ydbTest {
        val receiverId = random[ReceiverId]
        val count = 10
        val notificationsToUpdate = 5
        val topicName = "topic"
        val now = Instant.now()
        val operations = randomOperations(now, count, receiverId).toList
          .focus()
          .filterIndex((i: Int) => i < notificationsToUpdate)
          .andThen(topicOptics)
          .replace(topicName)
        val notificationsWithSetTopicName = operations
          .focus()
          .each
          .andThen(topicOptics)
          .filter(_ == topicName)
          .getAll
          .length

        for {
          _ <- service.addNotifications(receiverId, operations, now)
          _ <- service.markAllRead(receiverId, Some(topicName))
          result <- service.getUnreadCount(receiverId)
          _ <- check(result shouldBe (count - notificationsWithSetTopicName))
        } yield ()
      }
    }

    // TODO: this test must be removed in VERTISTRAF-2363
    "throw an error" when {
      "marking all notifications read in a particular topic" in ydbTest {
        val topicOptics = GenLens[OperationPayload](_.payload)
          .andThen(GenPrism[Payload, Payload.AddNotification])
          .andThen(GenLens[Payload.AddNotification](_.value.notification.topic))

        val receiverId = random[ReceiverId]
        val count = 10
        val notificationsToUpdate = 5
        val topicName = "topic"
        val now = Instant.now()
        val operations = randomOperations(now, count, receiverId).toList
          .focus()
          .filterIndex((i: Int) => i < notificationsToUpdate)
          .andThen(topicOptics)
          .replace(topicName)

        val markReadByTopic = for {
          _ <- service.addNotifications(receiverId, operations, now)
          _ <- service.markAllRead(receiverId, Some(topicName))
        } yield ()

        checkFailed(markReadByTopic)
      }
    }
  }
}
