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
import vertis.spamalot.dao.model.storage.old.{StoredChannel => OldStoredChannel}
import vertis.spamalot.model.ReceiverId

import java.time.Instant
import scala.reflect.runtime.universe.WeakTypeTag

class ReceiverNotificationServiceBackCompSpec extends ReceiverNotificationServiceSpecBase {
  import vertis.spamalot.dao.OperationsQueueStorage.OperationPayloadCodec

  "ReceiverNotificationServiceImpl" should implement {
    "addNotifications method".which {
      "adds new notifications to the new table only" when {
        "receiver is user" in ydbTest {
          implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
          val receiverUserId = random[ReceiverId.User]
          val operationsCount = 3
          val now = Instant.now()
          val operations = randomOperations(now, operationsCount, receiverUserId)

          for {
            _ <- service.addNotifications(receiverUserId, operations, now)
            operationsNotifications = operations.map(_.getAddNotification.notification)
            notificationsPageNew <- Ydb.runTx(
              storages.notificationStorage.list(receiverUserId, pagination = Pagination("", now, 100))
            )
            _ <- check(notificationsPageNew.items should contain theSameElementsAs operationsNotifications)
            notificationsPageOld <- Ydb.runTx(
              storages.oldNotificationStorage.list(receiverUserId.userId, pagination = Pagination("", now, 100))
            )
            _ <- check(notificationsPageOld.items shouldBe empty)
          } yield ()
        }
      }

      "updates number of unread notifications in new table" when {
        "receiver is user" when {
          "the number of unread notifications is in the old table" in ydbTest {
            val receiverUserId = random[ReceiverId.User]
            val initialUnreadCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 10)))
            val operationsCount = 3
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverUserId)

            for {
              _ <- Ydb.runTx(
                storages.oldChannelStorage.upsert(
                  OldStoredChannel(receiverUserId.userId, initialUnreadCount, now)
                )
              )
              _ <- service.addNotifications(receiverUserId, operations, now)
              unreadCount <- service.getUnreadCount(receiverUserId)
              _ <- check(unreadCount should ===(initialUnreadCount + operationsCount))
            } yield ()
          }

          "the number of unread notifications is in the new table" in ydbTest {
            val receiverUserId = random[ReceiverId.User]
            val initialUnreadCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 10)))
            val operationsCount = 3
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, receiverUserId)

            for {
              _ <- Ydb.runTx(
                storages.channelStorage.upsert(
                  StoredChannel(receiverUserId.proto, initialUnreadCount, now)
                )
              )
              _ <- service.addNotifications(receiverUserId, operations, now)
              unreadCount <- service.getUnreadCount(receiverUserId)
              _ <- check(unreadCount should ===(initialUnreadCount + operationsCount))
            } yield ()
          }
        }
      }

      "migrates unread counter from the old table" when {
        "receiver is user" in ydbTest {
          val receiverUserId = random[ReceiverId.User]
          val initialUnreadCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 10)))
          val operationsCount = 3
          val now = Instant.now()
          val operations = randomOperations(now, operationsCount, receiverUserId)

          for {
            _ <- Ydb.runTx(
              storages.oldChannelStorage.upsert(
                OldStoredChannel(receiverUserId.userId, initialUnreadCount, now)
              )
            )
            _ <- service.addNotifications(receiverUserId, operations, now)
            unreadCountNew <- Ydb.runTx(storages.channelStorage.unreadCount(receiverUserId))
            _ <- check(unreadCountNew should ===(Some(initialUnreadCount + operationsCount)))
            unreadCountOld <- Ydb.runTx(storages.oldChannelStorage.unreadCount(receiverUserId.userId))
            _ <- check(unreadCountOld shouldBe empty)
          } yield ()
        }
      }

      "does not insert notifications".which {
        "are already present for receiver (compared by id)" when {
          "receiver is user" when afterWord("user had notifications in") {
            "the old table only" in ydbTest {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val receiverUserId = random[ReceiverId.User]
              val operationsCount = 4
              val duplicatedId = "duplicated_id"
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, receiverUserId).toList
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

              val oldNotifications = firstOperations.focus().each.andThen(payloadNotificationOptics).getAll
              for {
                _ <- Ydb.runTx(storages.oldNotificationStorage.upsert(oldNotifications))
                _ <- service.addNotifications(receiverUserId, secondOperations, now)
                actualNotifications <- service.list(
                  receiverUserId,
                  Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, 100)
                )
                expectedNotifications = expectedOperations.focus().each.andThen(payloadNotificationOptics).getAll
                _ <- check(actualNotifications.items should contain theSameElementsAs expectedNotifications)
              } yield ()
            }

            "the new table only" in ydbTest {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val receiverUserId = random[ReceiverId.User]
              val operationsCount = 4
              val duplicatedId = "duplicated_id"
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, receiverUserId).toList
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

              val newNotifications = firstOperations.focus().each.andThen(payloadNotificationOptics).getAll
              for {
                _ <- Ydb.runTx(storages.notificationStorage.upsert(newNotifications))
                _ <- service.addNotifications(receiverUserId, secondOperations, now)
                actualNotifications <- service.list(
                  receiverUserId,
                  Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, 100)
                )
                expectedNotifications = expectedOperations.focus().each.andThen(payloadNotificationOptics).getAll
                _ <- check(actualNotifications.items should contain theSameElementsAs expectedNotifications)
              } yield ()
            }

            "both tables" in ydbTest {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val receiverUserId = random[ReceiverId.User]
              val operationsCount = 7
              val duplicatedId1 = "duplicated_id_1"
              val duplicatedId2 = "duplicated_id_2"
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, receiverUserId).toList
                .focus()
                .each
                .andThen(notificationIdOptics)
                .modify(_ + "_")
              val firstOperations = operations
                .take(2)
                .focus()
                .filterIndex((i: Int) => i == 1)
                .andThen(notificationIdOptics)
                .replace(duplicatedId1)
              val secondOperations = operations
                .slice(2, 4)
                .focus()
                .filterIndex((i: Int) => i == 1)
                .andThen(notificationIdOptics)
                .replace(duplicatedId2)
              val thirdOperations = operations
                .drop(4)
                .focus()
                .filterIndex((i: Int) => i == 0)
                .andThen(notificationIdOptics)
                .replace(duplicatedId1)
                .focus()
                .filterIndex((i: Int) => i == 1)
                .andThen(notificationIdOptics)
                .replace(duplicatedId2)
              val expectedOperations = firstOperations ++ secondOperations ++ thirdOperations.lastOption

              val oldNotifications = firstOperations.focus().each.andThen(payloadNotificationOptics).getAll
              val newNotifications = secondOperations.focus().each.andThen(payloadNotificationOptics).getAll
              for {
                _ <- Ydb.runTx(storages.oldNotificationStorage.upsert(oldNotifications))
                _ <- Ydb.runTx(storages.notificationStorage.upsert(newNotifications))
                _ <- service.addNotifications(receiverUserId, thirdOperations, now)
                actualNotifications <- service.list(
                  receiverUserId,
                  Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, 100)
                )
                expectedNotifications = expectedOperations.focus().each.andThen(payloadNotificationOptics).getAll
                _ <- check(actualNotifications.items should contain theSameElementsAs expectedNotifications)
              } yield ()
            }
          }
        }

        "have their topic banned by receiver" when {
          "receiver is user" when afterWord("banned topics are in") {
            def bannedTopicTest(useNewTable: Boolean) = {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val partition = getPartition(userId)
              val operationsCount = 5
              val forbiddenTopic = "FORBIDDEN_TOPIC"
              val notificationsWithForbiddenTopicsCount = 2
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, userId).toList
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
                _ <- Ydb.runTx(
                  if (useNewTable)
                    storages.receiverConfigStorage.update(userId, ReceiverThrottleConfiguration(Seq(forbiddenTopic)))
                  else
                    storages.userConfigStorage.update(userId.userId, ReceiverThrottleConfiguration(Seq(forbiddenTopic)))
                )
                _ <- service.addNotifications(userId, operationsWithForbiddenTopics, now)
                notificationsPage <- Ydb.runTx(
                  storages.notificationStorage.list(userId, pagination = Pagination("", now, 100))
                )
                expectedAddedNotifications = operationsWithForbiddenTopics
                  .map(_.getAddNotification.notification)
                _ <- check(notificationsPage.items should contain theSameElementsAs expectedAddedNotifications)
                sendPushQueueElements <- Ydb.runTx {
                  storages.operationStorage.peekElements[OperationPayload](now, partition, "SendPush", 100)
                }
                expectedPushNotifications = operationsWithForbiddenTopics
                  .drop(notificationsWithForbiddenTopicsCount)
                  .map(_.getAddNotification.notification)
                actualPushNotifications = sendPushQueueElements.toList
                  .focus()
                  .each
                  .andThen(queueElementSendPushNotificationOptics)
                  .filter(_.receiverId.flatMap(_.id.userId.map(_ == userId.userId.value)).getOrElse(false))
                  .getAll
                _ <- check(actualPushNotifications should contain theSameElementsAs expectedPushNotifications)
              } yield ()
            }

            "the old table only" in ydbTest {
              bannedTopicTest(false)
            }

            "the new table only" in ydbTest {
              bannedTopicTest(true)
            }
          }
        }
      }
    }

    "updateChannel method".which {
      "migrates unread counter from the old table" when {
        "receiver is user" in ydbTest {
          val userId = random[ReceiverId.User]
          val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
          val now = Instant.now()
          val oldStoredChannel = OldStoredChannel(userId.userId, operationsCount, now)

          for {
            _ <- Ydb.runTx(storages.oldChannelStorage.upsert(oldStoredChannel))
            _ <- Ydb.runTx(service.updateChannel(userId, 1, now))
            oldUnreadCount <- Ydb.runTx(storages.oldChannelStorage.unreadCount(userId.userId))
            _ <- check(oldUnreadCount shouldBe empty)
            newUnreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
            _ <- check(newUnreadCount shouldBe Some(operationsCount + 1))
          } yield ()
        }
      }
    }

    "markRead method".which {
      "updates unread notifications counter" when {
        "receiver is user" when {
          "the number of unread notifications is in the old table" in ydbTest {
            val userId = random[ReceiverId.User]
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(5, 10)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, userId).toList
            val readNotificationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val readNotificationsIds = operations
              .take(readNotificationsCount)
              .focus()
              .each
              .andThen(notificationIdOptics)
              .getAll

            for {
              _ <- service.addNotifications(userId, operations, now)
              _ <- Ydb.runTx {
                for {
                  _ <- storages.channelStorage.delete(userId)
                  oldStoredChannel = OldStoredChannel(userId.userId, operationsCount, now)
                  _ <- storages.oldChannelStorage.upsert(oldStoredChannel)
                } yield ()
              }
              _ <- service.markRead(userId, readNotificationsIds)
              unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(unreadCount.value should ===(operationsCount - readNotificationsCount))
            } yield ()
          }

          "the number of unread notifications is in the new table" in ydbTest {
            val userId = random[ReceiverId.User]
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(5, 10)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, userId).toList
            val readNotificationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val readNotificationsIds = operations
              .take(readNotificationsCount)
              .focus()
              .each
              .andThen(notificationIdOptics)
              .getAll

            for {
              _ <- service.addNotifications(userId, operations, now)
              unreadCountFromTable <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(unreadCountFromTable.value should ===(operationsCount))
              _ <- service.markRead(userId, readNotificationsIds)
              unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(unreadCount.value should ===(operationsCount - readNotificationsCount))
            } yield ()
          }

          "the user had notifications only in the old table" in ydbTest {
            val userId = random[ReceiverId.User]
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(5, 10)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, userId).toList
            val notificationsToInsert = operations
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .getAll
            val readNotificationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val readNotificationsIds = operations
              .take(readNotificationsCount)
              .focus()
              .each
              .andThen(notificationIdOptics)
              .getAll

            for {
              _ <- Ydb.runTx {
                for {
                  _ <- storages.oldNotificationStorage.upsert(notificationsToInsert)
                  storedChannel = StoredChannel(userId.proto, operationsCount, now)
                  _ <- storages.channelStorage.upsert(storedChannel)
                } yield ()
              }
              _ <- service.markRead(userId, readNotificationsIds)
              unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(unreadCount.value should ===(operationsCount - readNotificationsCount))
            } yield ()
          }

          "the user had notifications only in the new table" in ydbTest {
            val userId = random[ReceiverId.User]
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(5, 10)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, userId).toList
            val notificationsToInsert = operations
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .getAll
            val readNotificationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val readNotificationsIds = operations
              .take(readNotificationsCount)
              .focus()
              .each
              .andThen(notificationIdOptics)
              .getAll

            for {
              _ <- Ydb.runTx {
                for {
                  _ <- storages.notificationStorage.upsert(notificationsToInsert)
                  storedChannel = StoredChannel(userId.proto, operationsCount, now)
                  _ <- storages.channelStorage.upsert(storedChannel)
                } yield ()
              }
              _ <- service.markRead(userId, readNotificationsIds)
              unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(unreadCount.value should ===(operationsCount - readNotificationsCount))
            } yield ()
          }

          "the user had notifications in both tables" in ydbTest {
            val userId = random[ReceiverId.User]
            val operationsCount = 6
            val now = Instant.now()
            val notifications = randomOperations(now, operationsCount, userId).toList
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .getAll
            val oldNotifications = notifications.take(3)
            val newNotifications = notifications.drop(3)
            val readNotifications = oldNotifications.take(2) ++ newNotifications.take(2)
            val readNotificationsIds = readNotifications
              .focus()
              .each
              .andThen(GenLens[StoredNotification](_.id))
              .getAll

            for {
              _ <- Ydb.runTx {
                for {
                  _ <- storages.oldNotificationStorage.upsert(oldNotifications)
                  _ <- storages.notificationStorage.upsert(newNotifications)
                  storedChannel = StoredChannel(userId.proto, operationsCount, now)
                  _ <- storages.channelStorage.upsert(storedChannel)
                } yield ()
              }
              _ <- service.markRead(userId, readNotificationsIds)
              unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(unreadCount.value should ===(2))
            } yield ()
          }
        }
      }

      "migrates unread counter from the old table" when {
        "receiver is user" in ydbTest {
          val userId = random[ReceiverId.User]
          val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
          val now = Instant.now()
          val operations = randomOperations(now, operationsCount, userId).toList

          for {
            _ <- service.addNotifications(userId, operations, now)
            _ <- Ydb.runTx {
              for {
                _ <- storages.channelStorage.delete(userId)
                oldStoredChannel = OldStoredChannel(userId.userId, operationsCount, now)
                _ <- storages.oldChannelStorage.upsert(oldStoredChannel)
              } yield ()
            }
            _ <- service.markRead(userId, Seq(nonExistentNotificationId))
            oldUnreadCount <- Ydb.runTx(storages.oldChannelStorage.unreadCount(userId.userId))
            _ <- check(oldUnreadCount shouldBe empty)
            newUnreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
            _ <- check(newUnreadCount shouldBe defined)
          } yield ()
        }
      }

      "marks notifications with the passed ids as read for receiver" when {
        "receiver is user" when afterWord("the user had notifications") {
          def testWithNewOldTable(useNewTable: Boolean) = {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val userId = random[ReceiverId.User]
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(5, 10)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, userId).toList
            val notificationsToInsert = operations
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .getAll
            val readNotificationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val readNotificationsIds = operations
              .take(readNotificationsCount)
              .focus()
              .each
              .andThen(notificationIdOptics)
              .getAll

            for {
              _ <- Ydb.runTx {
                for {
                  _ <-
                    if (useNewTable)
                      storages.notificationStorage.upsert(notificationsToInsert)
                    else
                      storages.oldNotificationStorage.upsert(notificationsToInsert)
                  storedChannel = StoredChannel(userId.proto, operationsCount, now)
                  _ <- storages.channelStorage.upsert(storedChannel)
                } yield ()
              }
              _ <- service.markRead(userId, readNotificationsIds)
              notificationsPage <- service.list(
                userId,
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
              expectedNotifications = expectedReadNotifications ++ expectedUnreadNotifications
              _ <- check(notificationsPage.items should contain theSameElementsAs expectedNotifications)
            } yield ()
          }

          "in the old table" in ydbTest {
            testWithNewOldTable(false)
          }

          "in the new table" in ydbTest {
            testWithNewOldTable(true)
          }

          "in both tables" in ydbTest {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val userId = random[ReceiverId.User]
            val operationsCount = 6
            val now = Instant.now()
            val notifications = randomOperations(now, operationsCount, userId).toList
              .focus()
              .each
              .andThen(payloadNotificationOptics)
              .getAll
            val oldReadNotifications = notifications.take(3)
            val newReadNotifications = notifications.drop(3)
            val readNotifications = oldReadNotifications.take(2) ++ newReadNotifications.take(2)
            val readNotificationsIds = readNotifications
              .focus()
              .each
              .andThen(GenLens[StoredNotification](_.id))
              .getAll
            val unreadNotifications = oldReadNotifications.drop(2) ++ newReadNotifications.drop(2)
            val expectedNotifications = readNotifications
              .focus()
              .each
              .andThen(GenLens[StoredNotification](_.isRead))
              .replace(true) ++ unreadNotifications

            for {
              _ <- Ydb.runTx {
                for {
                  _ <- storages.oldNotificationStorage.upsert(oldReadNotifications)
                  _ <- storages.notificationStorage.upsert(newReadNotifications)
                  storedChannel = StoredChannel(userId.proto, operationsCount, now)
                  _ <- storages.channelStorage.upsert(storedChannel)
                } yield ()
              }
              _ <- service.markRead(userId, readNotificationsIds)
              notificationsPage <- service.list(
                userId,
                topics = Set.empty,
                newOnly = false,
                pagination = Pagination("", now, 100)
              )
              _ <- check(notificationsPage.items should contain theSameElementsAs expectedNotifications)
            } yield ()
          }
        }
      }
    }

    "markAllRead method".which {
      // TODO: remove topic constraint in VERTISTRAF-2363
      "does not throw an error when no topic is passed and".which {
        "sets unread  notifications counter to zero" when {
          "receiver is user" when {
            "the number of unread notifications is in the old table" in ydbTest {
              val userId = random[ReceiverId.User]
              val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 10)))
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, userId).toList

              for {
                _ <- service.addNotifications(userId, operations, now)
                _ <- Ydb.runTx {
                  for {
                    _ <- storages.channelStorage.delete(userId)
                    oldStoredChannel = OldStoredChannel(userId.userId, operationsCount, now)
                    _ <- storages.oldChannelStorage.upsert(oldStoredChannel)
                  } yield ()
                }
                _ <- service.markAllRead(userId, topic = None)
                unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
                _ <- check(unreadCount.value should ===(0))
              } yield ()
            }

            "the number of unread notifications is in the new table" in ydbTest {
              val userId = random[ReceiverId.User]
              val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, userId).toList

              for {
                _ <- service.addNotifications(userId, operations, now)
                unreadCountFromTable <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
                _ <- check(unreadCountFromTable.value should ===(operationsCount))
                _ <- service.markAllRead(userId, topic = None)
                unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
                _ <- check(unreadCount.value should ===(0))
              } yield ()
            }

            "the user had notifications only in the old table" in ydbTest {
              val userId = random[ReceiverId.User]
              val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
              val now = Instant.now()
              val notificationsToInsert = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll

              for {
                _ <- Ydb.runTx {
                  for {
                    _ <- storages.oldNotificationStorage.upsert(notificationsToInsert)
                    storedChannel = StoredChannel(userId.proto, operationsCount, now)
                    _ <- storages.channelStorage.upsert(storedChannel)
                  } yield ()
                }
                _ <- service.markAllRead(userId, topic = None)
                unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
                _ <- check(unreadCount.value should ===(0))
              } yield ()
            }

            "the user had notifications only in the new table" in ydbTest {
              val userId = random[ReceiverId.User]
              val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
              val now = Instant.now()
              val notificationsToInsert = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll

              for {
                _ <- Ydb.runTx {
                  for {
                    _ <- storages.notificationStorage.upsert(notificationsToInsert)
                    storedChannel = StoredChannel(userId.proto, operationsCount, now)
                    _ <- storages.channelStorage.upsert(storedChannel)
                  } yield ()
                }
                _ <- service.markAllRead(userId, topic = None)
                unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
                _ <- check(unreadCount.value should ===(0))
              } yield ()
            }

            "the user had notifications in both tables" in ydbTest {
              val userId = random[ReceiverId.User]
              val operationsCount = 4
              val now = Instant.now()
              val notifications = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              val oldNotifications = notifications.take(2)
              val newNotifications = notifications.drop(2)

              for {
                _ <- Ydb.runTx {
                  for {
                    _ <- storages.oldNotificationStorage.upsert(oldNotifications)
                    _ <- storages.notificationStorage.upsert(newNotifications)
                    storedChannel = StoredChannel(userId.proto, operationsCount, now)
                    _ <- storages.channelStorage.upsert(storedChannel)
                  } yield ()
                }
                _ <- service.markAllRead(userId, topic = None)
                unreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
                _ <- check(unreadCount.value should ===(0))
              } yield ()
            }
          }
        }

        "migrates unread counter from the old table" when {
          "receiver is user" in ydbTest {
            val userId = random[ReceiverId.User]
            val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, userId).toList

            for {
              _ <- service.addNotifications(userId, operations, now)
              _ <- Ydb.runTx {
                for {
                  _ <- storages.channelStorage.delete(userId)
                  oldStoredChannel = OldStoredChannel(userId.userId, operationsCount, now)
                  _ <- storages.oldChannelStorage.upsert(oldStoredChannel)
                } yield ()
              }
              _ <- service.markAllRead(userId, topic = None)
              oldUnreadCount <- Ydb.runTx(storages.oldChannelStorage.unreadCount(userId.userId))
              _ <- check(oldUnreadCount shouldBe empty)
              newUnreadCount <- Ydb.runTx(storages.channelStorage.unreadCount(userId))
              _ <- check(newUnreadCount shouldBe defined)
            } yield ()
          }
        }

        "marks all receiver's notifications read" when {
          "receiver is user" when afterWord("the user had notifications") {
            def testWithNewOldTable(useNewTable: Boolean) = {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.chooseNum(1, 5)))
              val now = Instant.now()
              val notificationsToInsert = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll

              for {
                _ <- Ydb.runTx {
                  for {
                    _ <-
                      if (useNewTable)
                        storages.notificationStorage.upsert(notificationsToInsert)
                      else
                        storages.oldNotificationStorage.upsert(notificationsToInsert)
                    storedChannel = StoredChannel(userId.proto, operationsCount, now)
                    _ <- storages.channelStorage.upsert(storedChannel)
                  } yield ()
                }
                _ <- service.markAllRead(userId, topic = None)
                notificationsPage <- service.list(
                  userId,
                  topics = Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, 100)
                )
                expectedNotifications = notificationsToInsert
                  .focus()
                  .each
                  .andThen(GenLens[StoredNotification](_.isRead))
                  .replace(true)
                _ <- check(notificationsPage.items should contain theSameElementsAs expectedNotifications)
              } yield ()
            }

            "in the old table" in ydbTest {
              testWithNewOldTable(false)
            }

            "in the new table" in ydbTest {
              testWithNewOldTable(true)
            }

            "in both tables" in ydbTest {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 6
              val now = Instant.now()
              val notifications = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              val oldReadNotifications = notifications.take(3)
              val newReadNotifications = notifications.drop(3)

              for {
                _ <- Ydb.runTx {
                  for {
                    _ <- storages.oldNotificationStorage.upsert(oldReadNotifications)
                    _ <- storages.notificationStorage.upsert(newReadNotifications)
                    storedChannel = StoredChannel(userId.proto, operationsCount, now)
                    _ <- storages.channelStorage.upsert(storedChannel)
                  } yield ()
                }
                _ <- service.markAllRead(userId, topic = None)
                notificationsPage <- service.list(
                  userId,
                  topics = Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, 100)
                )
                expectedNotifications = notifications
                  .focus()
                  .each
                  .andThen(GenLens[StoredNotification](_.isRead))
                  .replace(true)
                _ <- check(notificationsPage.items should contain theSameElementsAs expectedNotifications)
              } yield ()
            }
          }
        }
      }
    }

    "get method".which {
      "returns receiver's notification by id" when {
        "receiver is user" when {
          def testWithNewOldTable(useNewTable: Boolean) = {
            implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
            val userId = random[ReceiverId.User]
            val operationsCount = 1
            val notificationId = "notification_id"
            val now = Instant.now()
            val operations = randomOperations(now, operationsCount, userId).toList
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
              _ <- Ydb.runTx {
                if (useNewTable)
                  storages.notificationStorage.upsert(expectedNotification.toSeq)
                else
                  storages.oldNotificationStorage.upsert(expectedNotification.toSeq)
              }
              actualNotification <- service.get(userId, notificationId)
              _ <- check(actualNotification.value should ===(expectedNotification.value))
            } yield ()
          }

          "the notification is in the old table" in ydbTest {
            testWithNewOldTable(false)
          }

          "the notification is in the new table" in ydbTest {
            testWithNewOldTable(true)
          }
        }
      }
    }

    "getUnreadCount method".which {
      "returns the number of unread messages by receiver" when {
        "receiver is user" when afterWord("unread counter is in the") {
          "old table" in ydbTest {
            val receiverUserId = random[ReceiverId.User]
            val expectedUnreadCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.posNum[Int]))
            val now = Instant.now()
            val storedChannel = OldStoredChannel(receiverUserId.userId, expectedUnreadCount, now)

            for {
              _ <- Ydb.runTx(storages.oldChannelStorage.upsert(storedChannel))
              actualUnreadCount <- service.getUnreadCount(receiverUserId)
              _ <- check(expectedUnreadCount should ===(actualUnreadCount))
            } yield ()
          }

          "new table" in ydbTest {
            val receiverUserId = random[ReceiverId.User]
            val expectedUnreadCount = random[Int](implicitly[WeakTypeTag[Int]], Arbitrary(Gen.posNum[Int]))
            val now = Instant.now()
            val storedChannel = StoredChannel(receiverUserId.proto, expectedUnreadCount, now)

            for {
              _ <- Ydb.runTx(storages.channelStorage.upsert(storedChannel))
              actualUnreadCount <- service.getUnreadCount(receiverUserId)
              _ <- check(expectedUnreadCount should ===(actualUnreadCount))
            } yield ()
          }
        }
      }
    }

    "list method".which {
      "returns a full page of receiver's notifications" when {
        "receiver's notification fit one page" when {
          "receiver is user" when afterWord("notifications are") {
            def test(useNewTable: Boolean) = {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 5
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, userId)
              val notificationsToInsert = operations.toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll

              for {
                _ <- Ydb.runTx {
                  if (useNewTable)
                    storages.notificationStorage.upsert(notificationsToInsert)
                  else
                    storages.oldNotificationStorage.upsert(notificationsToInsert)
                }
                page <- service.list(
                  userId,
                  topics = Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, operationsCount)
                )
                _ <- check(page.items should contain theSameElementsAs notificationsToInsert)
                _ <- check(page.next shouldBe defined)
              } yield ()
            }

            "only in the new table" in ydbTest {
              test(useNewTable = false)
            }

            "only in the old table" in ydbTest {
              test(useNewTable = true)
            }

            "in both tables" in ydbTest {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 6
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, userId)
              val oldNotifications = operations.toList
                .take(3)
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              val newNotifications = operations.toList
                .drop(3)
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll

              for {
                _ <- Ydb.runTx(storages.notificationStorage.upsert(oldNotifications))
                _ <- Ydb.runTx(storages.oldNotificationStorage.upsert(newNotifications))
                page <- service.list(
                  userId,
                  topics = Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, operationsCount)
                )
                expectedNotifications = oldNotifications ++ newNotifications
                _ <- check(page.items should contain theSameElementsAs expectedNotifications)
                _ <- check(page.next shouldBe defined)
              } yield ()
            }
          }
        }
      }

      "returns page with free space" when {
        "there are less notifications left than requested by pagination" when {
          "receiver is user" when afterWord("notifications are") {
            def test(useNewTable: Boolean) = {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 5
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, userId)
              val notificationsToInsert = operations.toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll

              for {
                _ <- Ydb.runTx {
                  if (useNewTable)
                    storages.notificationStorage.upsert(notificationsToInsert)
                  else
                    storages.oldNotificationStorage.upsert(notificationsToInsert)
                }
                page <- service.list(
                  userId,
                  topics = Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, operationsCount + 1)
                )
                _ <- check(page.items should contain theSameElementsAs notificationsToInsert)
                _ <- check(page.next shouldBe empty)
              } yield ()
            }

            "only in the new table" in ydbTest {
              test(useNewTable = false)
            }

            "only in the old table" in ydbTest {
              test(useNewTable = true)
            }

            "in both tables" in ydbTest {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 6
              val now = Instant.now()
              val notifications = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              val oldNotifications = notifications.take(3)
              val newNotifications = notifications.drop(3)

              for {
                _ <- Ydb.runTx(storages.notificationStorage.upsert(oldNotifications))
                _ <- Ydb.runTx(storages.oldNotificationStorage.upsert(newNotifications))
                page <- service.list(
                  userId,
                  topics = Set.empty,
                  newOnly = false,
                  pagination = Pagination("", now, operationsCount + 1)
                )
                expectedNotifications = oldNotifications ++ newNotifications
                _ <- check(page.items should contain theSameElementsAs expectedNotifications)
                _ <- check(page.next shouldBe empty)
              } yield ()
            }
          }
        }
      }

      "filter by topics" when {
        "topics are passed" when {
          "receiver is user" when afterWord("notifications are") {
            def test(useNewTable: Boolean) = {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 5
              val topic = "topic"
              val notificationsInCustomTopic = 3
              val now = Instant.now()
              val operations = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(topicOptics)
                .modify(_ + "_")
                .focus()
                .filterIndex((i: Int) => i < notificationsInCustomTopic)
                .andThen(topicOptics)
                .replace(topic)
              val notificationsToInsert = operations
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll

              for {
                _ <- Ydb.runTx {
                  if (useNewTable)
                    storages.notificationStorage.upsert(notificationsToInsert)
                  else
                    storages.oldNotificationStorage.upsert(notificationsToInsert)
                }
                page <- service.list(
                  userId,
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

            "only in the new table" in ydbTest {
              test(useNewTable = false)
            }

            "only in the old table" in ydbTest {
              test(useNewTable = true)
            }

            "in both tables" in ydbTest {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 6
              val topic = "topic"
              val now = Instant.now()
              val notifications = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(topicOptics)
                .modify(_ + "_")
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              val oldNotifications = notifications
                .take(3)
                .focus()
                .filterIndex((i: Int) => i < 2)
                .andThen(GenLens[StoredNotification](_.topic))
                .replace(topic)
              val newNotifications = notifications
                .drop(3)
                .focus()
                .filterIndex((i: Int) => i < 2)
                .andThen(GenLens[StoredNotification](_.topic))
                .replace(topic)

              for {
                _ <- Ydb.runTx(storages.notificationStorage.upsert(oldNotifications))
                _ <- Ydb.runTx(storages.oldNotificationStorage.upsert(newNotifications))
                page <- service.list(
                  userId,
                  topics = Set(topic),
                  newOnly = false,
                  pagination = Pagination("", now, operationsCount + 1)
                )
                expectedNotifications = oldNotifications.take(2) ++ newNotifications.take(2)
                _ <- check(page.items should contain theSameElementsAs expectedNotifications)
                _ <- check(page.next shouldBe empty)
              } yield ()
            }
          }
        }
      }

      "filter only new notifications" when {
        "newOnly is set to true" when {
          "receiver is user" when afterWord("notifications are") {
            def test(useNewTable: Boolean) = {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 5
              val readNotificationsCount = 3
              val now = Instant.now()
              val notificationsToInsert = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              val readNotificationIds = notificationsToInsert
                .take(readNotificationsCount)
                .focus()
                .each
                .andThen(GenLens[StoredNotification](_.id))
                .getAll

              for {
                _ <- Ydb.runTx {
                  for {
                    _ <-
                      if (useNewTable)
                        storages.notificationStorage.upsert(notificationsToInsert)
                      else
                        storages.oldNotificationStorage.upsert(notificationsToInsert)
                    storedChannel = StoredChannel(userId.proto, operationsCount, now)
                    _ <- storages.channelStorage.upsert(storedChannel)
                  } yield ()
                }
                _ <- service.markRead(userId, readNotificationIds)
                page <- service.list(
                  userId,
                  topics = Set.empty,
                  newOnly = true,
                  pagination = Pagination("", now, operationsCount)
                )
                expectedNotifications = notificationsToInsert.drop(readNotificationsCount)
                _ <- check(page.items should contain theSameElementsAs expectedNotifications)
                _ <- check(page.next shouldBe empty)
              } yield ()
            }

            "only in the new table" in ydbTest {
              test(useNewTable = false)
            }

            "only in the old table" in ydbTest {
              test(useNewTable = true)
            }

            "in both tables" in ydbTest {
              implicit val storedNotificationEquality: Equality[StoredNotification] = insertedStoredNotificationEquality
              val userId = random[ReceiverId.User]
              val operationsCount = 6
              val now = Instant.now()
              val notifications = randomOperations(now, operationsCount, userId).toList
                .focus()
                .each
                .andThen(payloadNotificationOptics)
                .getAll
              val oldNotifications = notifications.take(3)
              val newNotifications = notifications.drop(3)
              val oldReadNotificationIds = oldNotifications
                .take(2)
                .focus()
                .each
                .andThen(GenLens[StoredNotification](_.id))
                .getAll
              val newReadNotificationIds = newNotifications
                .take(2)
                .focus()
                .each
                .andThen(GenLens[StoredNotification](_.id))
                .getAll

              for {
                _ <- Ydb.runTx {
                  for {
                    _ <- storages.notificationStorage.upsert(oldNotifications)
                    _ <- storages.oldNotificationStorage.upsert(newNotifications)
                    storedChannel = StoredChannel(userId.proto, operationsCount, now)
                    _ <- storages.channelStorage.upsert(storedChannel)
                  } yield ()
                }
                _ <- service.markRead(userId, oldReadNotificationIds)
                _ <- service.markRead(userId, newReadNotificationIds)
                page <- service.list(
                  userId,
                  topics = Set.empty,
                  newOnly = true,
                  pagination = Pagination("", now, operationsCount + 1)
                )
                expectedNotifications = oldNotifications.drop(2) ++ newNotifications.drop(2)
                _ <- check(page.items should contain theSameElementsAs expectedNotifications)
                _ <- check(page.next shouldBe empty)
              } yield ()
            }
          }
        }
      }
    }
  }
}
