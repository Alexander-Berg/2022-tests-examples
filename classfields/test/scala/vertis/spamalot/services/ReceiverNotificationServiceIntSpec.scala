package vertis.spamalot.services

import monocle.macros.GenLens
import monocle.syntax.all._
import org.scalacheck.magnolia._
import org.scalacheck.{Arbitrary, Gen}
import org.scalactic.Equality
import ru.yandex.vertis.spamalot.inner.{OperationPayload, StoredNotification}
import ru.yandex.vertis.spamalot.model.ReceiverThrottleConfiguration
import ru.yandex.vertis.ydb.Ydb
import vertis.spamalot.dao.model.Pagination
import vertis.spamalot.dao.model.storage.StoredChannel
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.ydb.YEnv
import zio.RIO

import java.time.Instant
import scala.reflect.runtime.universe.WeakTypeTag

/** @author tymur-lysenko
  */
class ReceiverNotificationServiceIntSpec extends ReceiverNotificationServiceSpecBase {
  import vertis.spamalot.dao.OperationsQueueStorage.OperationPayloadCodec

  "ReceiverNotificationServiceImpl" should implement {
    "addNotifications method".which {
      "adds notifications" when {
        def testForReceiverId(receiverId: ReceiverId) = {
          implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
          val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
          val now = Instant.now()
          val operations = randomOperations(now, operationsCount, receiverId)

          for {
            _ <- service.addNotifications(receiverId, operations, now)
            notificationsPage <- Ydb.runTx(
              storages.notificationStorage.list(receiverId, pagination = Pagination("", now, 100))
            )
            operationsNotifications = operations.map(_.getAddNotification.notification)
            _ <- check(notificationsPage.items should contain theSameElementsAs operationsNotifications)
          } yield ()
        }

        "receiver is user" in ydbTest {
          val userId = random[ReceiverId.User]
          testForReceiverId(userId)
        }

        "receiver is device" in ydbTest {
          val deviceId = random[ReceiverId.DeviceId]
          testForReceiverId(deviceId)
        }
      }

      "adds push operations to queue for receiver" when {
        def testsForReceiverId[T: WeakTypeTag: Arbitrary](implicit ev: T <:< ReceiverId): Unit = {
          def getPushNotificationsFromQueueByReceiverId(receiverId: ReceiverId): RIO[YEnv, List[StoredNotification]] = {
            val partition = getPartition(receiverId)
            val now = Instant.now()

            for {
              sendPushQueueElements <- Ydb.runTx {
                storages.operationStorage.peekElements[OperationPayload](now, partition, "SendPush", 100)
              }
            } yield sendPushQueueElements.toList
              .focus()
              .each
              .andThen(queueElementSendPushNotificationOptics)
              .filter(
                _.receiverId
                  .flatMap(r =>
                    receiverId match {
                      case ReceiverId.User(UserId(userId)) => r.id.userId.map(_ == userId)
                      case ReceiverId.DeviceId(deviceId) => r.id.deviceId.map(_ == deviceId)
                    }
                  )
                  .getOrElse(false)
              )
              .getAll
          }

          "notifications' topics are not banned" in ydbTest {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val receiverId = random[T]
            val operationsCount = 5
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId)

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              notificationsPage <- Ydb.runTx(
                storages.notificationStorage.list(receiverId, pagination = Pagination("", now, 100))
              )
              expectedNotifications = operations
                .map(_.getAddNotification.notification)
              _ <- check(notificationsPage.items should contain theSameElementsAs expectedNotifications)
              actualPushNotifications <- getPushNotificationsFromQueueByReceiverId(receiverId)
              _ <- check(actualPushNotifications should contain theSameElementsAs expectedNotifications)
            } yield ()
          }

          "there are notifications for banned topic" in ydbTest {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val receiverId = random[ReceiverId]
            val operationsCount = 5
            val forbiddenTopic = "FORBIDDEN_TOPIC"
            val notificationsWithForbiddenTopicsCount = 2
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId).toList
              .focus()
              .each
              .andThen(topicOptics)
              .modify(_ + "_")
            val operationsWithForbiddenTopics = operations
              .focus()
              .filterIndex((i: Int) => i < notificationsWithForbiddenTopicsCount)
              .andThen(topicOptics)
              .replace(forbiddenTopic)

            for {
              _ <- receiverSettingsService.updateConfig(
                receiverId,
                ReceiverThrottleConfiguration(bannedTopics = Seq(forbiddenTopic))
              )
              _ <- service.addNotifications(receiverId, operationsWithForbiddenTopics, now)
              notificationsPage <- Ydb.runTx(
                storages.notificationStorage.list(receiverId, pagination = Pagination("", now, 100))
              )
              expectedAddedNotifications = operationsWithForbiddenTopics
                .map(_.getAddNotification.notification)
              _ <- check(notificationsPage.items should contain theSameElementsAs expectedAddedNotifications)
              actualPushNotifications <- getPushNotificationsFromQueueByReceiverId(receiverId)
              expectedPushNotifications = operationsWithForbiddenTopics
                .drop(notificationsWithForbiddenTopicsCount)
                .map(_.getAddNotification.notification)
              _ <- check(actualPushNotifications should contain theSameElementsAs expectedPushNotifications)
            } yield ()
          }
        }

        "receiver is user" when {
          behave.like(testsForReceiverId[ReceiverId.User])
        }

        "receiver is device" when {
          behave.like(testsForReceiverId[ReceiverId.DeviceId])
        }
      }

      "updates number of unread notifications" when {
        def updateUnreadCounterTests[T: WeakTypeTag: Arbitrary](implicit ev: T <:< ReceiverId): Unit = {
          "there are no unread notifications" in ydbTest {
            val receiverId = random[T]
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId)

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              unreadCount <- service.getUnreadCount(receiverId)
              _ <- check(unreadCount should ===(operationsCount))
            } yield ()
          }

          "there are unread notifications" in ydbTest {
            val receiverId = random[T]
            val operationsCount = 5
            val now = Instant.now()
            val allOperations = randomOperations(now, operationsCount, receiverId)
            val firstOperations = allOperations.take(2)
            val secondOperations = allOperations.drop(2)

            for {
              _ <- service.addNotifications(receiverId, firstOperations, now)
              _ <- service.addNotifications(receiverId, secondOperations, now)
              totalUnreadCount <- service.getUnreadCount(receiverId)
              _ <- check(totalUnreadCount should ===(operationsCount))
            } yield ()
          }

          "there are read notifications" in ydbTest {
            val receiverId = random[T]
            val operationsCount = 5
            val now = Instant.now()
            val allOperations = randomOperations(now, operationsCount, receiverId)
            val firstOperations = allOperations.take(2)
            val secondOperations = allOperations.drop(2)

            for {
              _ <- service.addNotifications(receiverId, firstOperations, now)
              _ <- service.markAllRead(receiverId, None)
              _ <- service.addNotifications(receiverId, secondOperations, now)
              totalUnreadCount <- service.getUnreadCount(receiverId)
              _ <- check(totalUnreadCount should ===(3))
            } yield ()
          }
        }

        "receiver is user" when {
          behave.like(updateUnreadCounterTests[ReceiverId.User])
        }

        "receiver is device" when {
          behave.like(updateUnreadCounterTests[ReceiverId.DeviceId])
        }
      }

      "deduplicates passed notifications by notification id" when {
        def testForReceiverId(receiverId: ReceiverId) = {
          implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
          val operationsCount = 5
          val duplicatedOperationsCount = 3
          val duplicatedId = "duplicated_id"
          val now = Instant.now()
          val operations = randomOperations(now, operationsCount, receiverId).toList
            .focus()
            .each
            .andThen(notificationIdOptics)
            .modify(_ + "_")
          val operationsWithDuplicatedNotifications = operations
            .focus()
            .filterIndex((i: Int) => i < duplicatedOperationsCount)
            .andThen(notificationIdOptics)
            .replace(duplicatedId)
          val uniqueOperations = operationsWithDuplicatedNotifications.distinctBy(_.getAddNotification.notification.id)

          for {
            _ <- service.addNotifications(receiverId, operationsWithDuplicatedNotifications, now)
            notificationsPage <- Ydb.runTx(
              storages.notificationStorage.list(receiverId, pagination = Pagination("", now, 100))
            )
            operationsNotifications = uniqueOperations.map(_.getAddNotification.notification)
            _ <- check(notificationsPage.items should contain theSameElementsAs operationsNotifications)
          } yield ()
        }

        "receiver is user" in ydbTest {
          val userId = random[ReceiverId.User]
          testForReceiverId(userId)
        }

        "receiver is device" in ydbTest {
          val deviceId = random[ReceiverId.DeviceId]
          testForReceiverId(deviceId)
        }
      }

      "does not insert notifications that are already present for receiver (compared by notification id)" when {
        def testForReceiverId(receiverId: ReceiverId) = {
          implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
          val operationsCount = 4
          val duplicatedId = "duplicated_id"
          val now = Instant.now()
          val operations = randomOperations(now, operationsCount, receiverId).toList
            .focus()
            .each
            .andThen(notificationIdOptics)
            .modify(_ + "_")
          val firstOperations = operations
            .take(2)
            .focus()
            .filterIndex((i: Int) => i == 1)
            .andThen(notificationIdOptics)
            .replace(duplicatedId)
          val secondOperations = operations
            .drop(2)
            .focus()
            .filterIndex((i: Int) => i == 1)
            .andThen(notificationIdOptics)
            .replace(duplicatedId)
          val expectedOperations = firstOperations ++ secondOperations.take(1)

          for {
            _ <- service.addNotifications(receiverId, firstOperations, now)
            _ <- service.addNotifications(receiverId, secondOperations, now)
            notificationsPage <- Ydb.runTx(
              storages.notificationStorage.list(receiverId, pagination = Pagination("", now, 100))
            )
            expectedNotifications = expectedOperations.map(_.getAddNotification.notification)
            _ <- check(notificationsPage.items should contain theSameElementsAs expectedNotifications)
          } yield ()
        }

        "receiver is user" in ydbTest {
          val userId = random[ReceiverId.User]
          testForReceiverId(userId)
        }

        "receiver is device" in ydbTest {
          val deviceId = random[ReceiverId.User]
          testForReceiverId(deviceId)
        }
      }
    }

    "updateChannel method".which {
      "adds the specified positive number to the current unread count" when {
        "there is no unread counter for receiver" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            val now = Instant.now()

            for {
              _ <- Ydb.runTx(service.updateChannel(receiverId, 1, now))
              unreadCount <- service.getUnreadCount(receiverId)
              _ <- check(unreadCount should ===(1))
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }

        "receiver has unread counter" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            val initialUnreadCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val now = Instant.now()
            val storedChannel = StoredChannel(receiverId.proto, initialUnreadCount, now)

            for {
              _ <- Ydb.runTx(storages.channelStorage.upsert(storedChannel))
              _ <- Ydb.runTx(service.updateChannel(receiverId, 1, now))
              unreadCount <- service.getUnreadCount(receiverId)
              _ <- check(unreadCount should ===(initialUnreadCount + 1))
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }
    }

    "markRead method".which {
      "updates unread notifications counter" when {
        def testForReceiverId(receiverId: ReceiverId, additionalNotificationsIdsToRead: List[String]) = {
          val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(5, 10)))
          val now = Instant.now()
          val operations = randomOperations(now, operationsCount, receiverId).toList
            .focus()
            .each
            .andThen(notificationIdOptics)
            .modify(_ + "_")
          val readNotificationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
          val readNotificationsIds = operations
            .take(readNotificationsCount)
            .focus()
            .each
            .andThen(notificationIdOptics)
            .getAll ++ additionalNotificationsIdsToRead

          for {
            _ <- service.addNotifications(receiverId, operations, now)
            _ <- service.markRead(receiverId, readNotificationsIds)
            unreadCount <- service.getUnreadCount(receiverId)
            _ <- check(unreadCount should ===(operationsCount - readNotificationsCount))
          } yield ()
        }

        "the passed ids all belong to the receiver" when {
          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId, List.empty)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId, List.empty)
          }
        }

        "the some of the ids belong to the receiver and others do not" when {
          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId, List(nonExistentNotificationId))
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId, List(nonExistentNotificationId))
          }

        }
      }

      "marks notifications with the passed ids as read for receiver" when {
        def testForReceiverId(receiverId: ReceiverId, additionalNotificationsIdsToRead: List[String]) = {
          implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
          val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(5, 10)))
          val now = Instant.now()
          val operations = randomOperations(now, operationsCount, receiverId).toList
            .focus()
            .each
            .andThen(notificationIdOptics)
            .modify(_ + "_")
          val readNotificationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
          val readNotificationsIds = operations
            .take(readNotificationsCount)
            .focus()
            .each
            .andThen(notificationIdOptics)
            .getAll ++ additionalNotificationsIdsToRead

          for {
            _ <- service.addNotifications(receiverId, operations, now)
            _ <- service.markRead(receiverId, readNotificationsIds)
            notificationsPage <- service.list(
              receiverId,
              topics = Set.empty,
              newOnly = false,
              pagination = Pagination("", now, 100)
            )
            expectedReadNotifications = operations
              .take(readNotificationsCount)
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .andThen(GenLens[StoredNotification](_.isRead))
              .replace(true)
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .getAll
            expectedUnreadNotifications = operations
              .drop(readNotificationsCount)
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .getAll
            _ <- check(
              notificationsPage.items should contain theSameElementsAs (expectedReadNotifications ++ expectedUnreadNotifications)
            )
          } yield ()
        }

        "the passed ids all belong to the receiver" when {
          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId, List.empty)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId, List.empty)
          }
        }

        "some of the ids belong to the receiver and others do not" when {
          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId, List(nonExistentNotificationId))
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId, List(nonExistentNotificationId))
          }
        }
      }
    }

    "markAllRead method".which {
      // TODO: remove topic constraint in VERTISTRAF-2363
      "does not throw an error when no topic is passed and".which {
        "sets unread notifications counter to zero" when {
          "receiver has notifications" when {
            def testForReceiverId(receiverId: ReceiverId) = {
              val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, receiverId)

              for {
                _ <- service.addNotifications(receiverId, operations, now)
                _ <- service.markAllRead(receiverId, topic = None)
                unreadCount <- service.getUnreadCount(receiverId)
                _ <- check(unreadCount should ===(0))
              } yield ()
            }

            "receiver is user" in ydbTest {
              val userId = random[ReceiverId.User]
              testForReceiverId(userId)
            }

            "receiver is device" in ydbTest {
              val deviceId = random[ReceiverId.DeviceId]
              testForReceiverId(deviceId)
            }
          }
        }

        "marks all receiver's notifications read" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId)

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              _ <- service.markAllRead(receiverId, topic = None)
              notificationsPage <- service.list(
                receiverId,
                topics = Set.empty,
                newOnly = false,
                pagination = Pagination("", now, 100)
              )
              expectedNotifications = operations.toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .andThen(GenLens[StoredNotification](_.isRead))
                .replace(true)
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              _ <- check(notificationsPage.items should contain theSameElementsAs expectedNotifications)
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }

        "does nothing" when {
          "there are no notifications for receiver" when {
            def testForReceiverId(receiverId: ReceiverId) = {
              for {
                result <- service.markAllRead(receiverId, topic = None)
                _ <- check(result should ===(()))
              } yield ()
            }

            "receiver is user" in ydbTest {
              val userId = random[ReceiverId.User]
              testForReceiverId(userId)
            }

            "receiver is device" in ydbTest {
              val deviceId = random[ReceiverId.DeviceId]
              testForReceiverId(deviceId)
            }
          }
        }
      }

      // TODO: remove this test in VERTISTRAF-2363
      "throws an error" when {
        "a topic is passed" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId)

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              result <- service.markAllRead(receiverId, topic = Some("personal_recommendations")).either
              _ <- check(result.left.value shouldBe an[IllegalArgumentException])
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }
    }

    "get method".which {
      "returns receiver's notification by id" when {
        "there is notification with the id for the receiver" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val operationsCount = 1
            val notificationId = "notification_id"
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId).toList
              .focus()
              .each
              .andThen(notificationIdOptics)
              .replace(notificationId)
            val expectedNotification = operations
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .headOption

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              actualNotification <- service.get(receiverId, notificationId)
              _ <- check(actualNotification.value should ===(expectedNotification.value))
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }

      "return None" when {
        "there is no notifications with the id for the receiver" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            val operationsCount = 1
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId).toList
              .focus()
              .each
              .andThen(notificationIdOptics)
              .replace("notification_id")

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              notification <- service.get(receiverId, nonExistentNotificationId)
              _ <- check(notification shouldBe empty)
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }
    }

    "getUnreadCount method".which {
      "returns the number of unread messages by receiver" when {
        "there is a record in a table" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId)

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              unreadCount <- service.getUnreadCount(receiverId)
              _ <- check(unreadCount should ===(operationsCount))
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }

      "returns zero" when {
        def zeroUnreadCountTests[T: WeakTypeTag: Arbitrary](implicit ev: T <:< ReceiverId): Unit = {

          "there is no record in the table" in ydbTest {
            val receiverId = random[T]

            for {
              totalUnreadCount <- service.getUnreadCount(receiverId)
              _ <- check(totalUnreadCount should ===(0))
            } yield ()
          }
        }

        "receiver is user" when {
          behave.like(zeroUnreadCountTests[ReceiverId.User])
        }

        "receiver is device" when {
          behave.like(zeroUnreadCountTests[ReceiverId.DeviceId])
        }
      }
    }

    "list method".which {
      "returns a full page of receiver's notifications" when {
        "receiver's notification fit one page" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val operationsCount = 5
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId)

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              page <- service.list(
                receiverId,
                topics = Set.empty,
                newOnly = false,
                pagination = Pagination("", now, operationsCount)
              )
              expectedNotifications = operations.toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              _ <- check(page.items should contain theSameElementsAs expectedNotifications)
              _ <- check(page.next shouldBe defined)
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }

      "returns page with free space" when {
        "there are less notifications left than requested by pagination" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val operationsCount = 5
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId)

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              page <- service.list(
                receiverId,
                topics = Set.empty,
                newOnly = false,
                pagination = Pagination("", now, operationsCount + 1)
              )
              expectedNotifications = operations.toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              _ <- check(page.items should contain theSameElementsAs expectedNotifications)
              _ <- check(page.next shouldBe empty)
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }

      "returns an empty page" when {
        "there are no notifications for receiver" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            for {
              page <- service.list(
                receiverId,
                topics = Set.empty,
                newOnly = false,
                pagination = Pagination("", Instant.now(), 10)
              )
              _ <- check(page.items shouldBe empty)
              _ <- check(page.next shouldBe empty)
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }

      "filter by topics" when {
        "topics are passed" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val operationsCount = 5
            val topic = "topic"
            val notificationsInCustomTopic = 3
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId).toList
              .focus()
              .each
              .andThen(topicOptics)
              .modify(_ + "_")
              .focus()
              .filterIndex((i: Int) => i < notificationsInCustomTopic)
              .andThen(topicOptics)
              .replace(topic)

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              page <- service.list(
                receiverId,
                topics = Set(topic),
                newOnly = false,
                pagination = Pagination("", now, operationsCount)
              )
              expectedNotifications = operations
                .take(notificationsInCustomTopic)
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              _ <- check(page.items should contain theSameElementsAs expectedNotifications)
              _ <- check(page.next shouldBe empty)
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }

      "filter only new notifications" when {
        "newOnly is set to true" when {
          def testForReceiverId(receiverId: ReceiverId) = {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val operationsCount = 5
            val readNotificationsCount = 3
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverId).toList
            val readNotificationIds = operations
              .take(readNotificationsCount)
              .focus()
              .each
              .andThen(notificationIdOptics)
              .getAll

            for {
              _ <- service.addNotifications(receiverId, operations, now)
              _ <- service.markRead(receiverId, readNotificationIds)
              page <- service.list(
                receiverId,
                topics = Set.empty,
                newOnly = true,
                pagination = Pagination("", now, operationsCount)
              )
              expectedNotifications = operations
                .drop(readNotificationsCount)
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              _ <- check(page.items should contain theSameElementsAs expectedNotifications)
              _ <- check(page.next shouldBe empty)
            } yield ()
          }

          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            testForReceiverId(userId)
          }

          "receiver is device" in ydbTest {
            val deviceId = random[ReceiverId.DeviceId]
            testForReceiverId(deviceId)
          }
        }
      }
    }
  }
}
