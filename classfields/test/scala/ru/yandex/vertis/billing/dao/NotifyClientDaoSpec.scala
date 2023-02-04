package ru.yandex.vertis.billing.dao

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{Assertion, BeforeAndAfterEach}
import ru.yandex.vertis.billing.dao.NotifyClientDao.{
  ClientIdWithTid,
  LastBeforeForClientsByTid,
  LastForClient,
  UpdatedSinceBatchOrdered
}
import ru.yandex.vertis.billing.dao.NotifyClientDaoSpec.RichNotifyClient
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.exceptions.NotifyClientIgnoredException
import ru.yandex.vertis.billing.model_core.gens.{notifyClientGen, ClientIdGen, NotifyClientGenParams, Producer}
import ru.yandex.vertis.billing.model_core.{ClientId, Epoch, NotifyClient, NotifyClientRecordId}
import ru.yandex.vertis.billing.util.clean.CleanableDao

import scala.annotation.tailrec
import scala.util.Success

/**
  * Spec on [[NotifyClientDao]].
  *
  * @author ruslansd
  */
trait NotifyClientDaoSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with BeforeAndAfterEach {

  protected def dao: NotifyClientDao with CleanableDao

  override def beforeEach(): Unit = {
    super.beforeEach()
    dao.clean().get
  }

  private def nextNotificationsMap(
      clientsCount: Int,
      maxNotificationsCount: Int): Map[ClientId, Iterable[NotifyClient]] = {
    (1 to clientsCount).map { clientId =>
      val gen = for {
        notificationsCount <- Gen.choose(1, maxNotificationsCount)
        notificationsGen = notifyClientGen(NotifyClientGenParams().withClientId(clientId).withoutEpoch.withoutId)
        rawNotifications = notificationsGen.next(notificationsCount)
        // notifications should be unique by tid for client to test correctly
        notificationsGroupedByTid = rawNotifications.groupBy(_.tid)
        notificationsUniqByTid = notificationsGroupedByTid.values.map(_.head)
      } yield notificationsUniqByTid
      clientId.toLong -> gen.next
    }.toMap
  }

  private def prepareNotificationsMap(
      clientsCount: Int,
      maxNotificationsCount: Int): Map[ClientId, Iterable[NotifyClient]] = {
    val notificationsMap = nextNotificationsMap(clientsCount, maxNotificationsCount)

    val notifications = notificationsMap.values.flatten
    val notificationsSortedByTid = notifications.toSeq.sortBy(_.tid)

    notificationsSortedByTid.foreach { notification =>
      dao.append(notification) should matchPattern { case Success(_) =>
      }
    }

    notificationsMap
  }

  @tailrec
  private def checkUpdatedSinceBatchOrdered(
      sortedSource: Seq[NotifyClient],
      epoch: Epoch,
      recordId: Option[NotifyClientRecordId],
      batchSize: Int): Assertion = {
    val expected = sortedSource.take(batchSize)
    dao.get(UpdatedSinceBatchOrdered(epoch, recordId, batchSize)) match {
      case Success(actualRecords) if actualRecords.isEmpty =>
        expected.isEmpty shouldBe true
      case Success(actualRecords) =>
        actualRecords should contain theSameElementsAs expected
        val last = actualRecords.last
        val epoch = last.epoch.get
        val recordId = last.recordId
        val rest = sortedSource.drop(batchSize)
        checkUpdatedSinceBatchOrdered(rest, epoch, recordId, batchSize)
      case other =>
        fail(s"Unexpected $other")
    }
  }

  "NotifyClientDao" should {
    "get nothing when db is empty" when {
      "call get with LastForClient filter" in {
        val clientId = ClientIdGen.next
        dao.get(LastForClient(clientId)) match {
          case Success(notifications) if notifications.isEmpty =>
          case res => fail(res.toString)
        }
      }
      "call get with UpdatedSinceBatchOrdered filter" in {
        dao.get(UpdatedSinceBatchOrdered(0L, None, 1000)) match {
          case Success(notifications) if notifications.isEmpty =>
          case res => fail(res.toString)
        }
      }
      "call get with PreviousByTidForClients filter" in {
        val clientId = ClientIdGen.next
        val tid = "123"
        val clientIdWithTid = ClientIdWithTid(clientId, tid)
        dao.get(LastBeforeForClientsByTid(Seq(clientIdWithTid))) match {
          case Success(notifications) if notifications.isEmpty =>
          case res => fail(res.toString)
        }
      }
    }
    "append" when {
      "pass correct notification" in {
        val notification =
          notifyClientGen(NotifyClientGenParams().withoutEpoch).next
        dao.append(notification) should matchPattern { case Success(_) =>
        }
      }
    }
    "fail append" when {
      "pass notification with previous tid" in {
        val clientId = ClientIdGen.next

        val secondTid = "1"
        val secondNotification =
          notifyClientGen(NotifyClientGenParams().withTid(secondTid).withClientId(clientId).withoutEpoch).next
        dao.append(secondNotification) should matchPattern { case Success(_) =>
        }

        val firstTid = "0"
        val firstNotification =
          notifyClientGen(NotifyClientGenParams().withTid(firstTid).withClientId(clientId).withoutEpoch).next
        intercept[NotifyClientIgnoredException] {
          dao.append(firstNotification).get
        }
      }
    }
    "skip append" when {
      "pass notification with same tid for client" in {
        val clientId = ClientIdGen.next

        val firstTid = "0"
        val genParams = NotifyClientGenParams().withTid(firstTid).withClientId(clientId).withoutEpoch.withoutId
        val firstNotification = notifyClientGen(genParams).next
        dao.append(firstNotification) should matchPattern { case Success(_) =>
        }

        val duplicateNotification = firstNotification.copy(
          overdraftLimit = firstNotification.overdraftLimit - 1,
          overdraftSpent = firstNotification.overdraftLimit + 1
        )
        dao.append(duplicateNotification) should matchPattern { case Success(_) =>
        }

        dao.get(LastForClient(clientId)) match {
          case Success(notifications) if notifications.size == 1 =>
            val notification = notifications.head
            val notificationWithoutEpoch = notification.withoutRecordIdAndEpoch
            notificationWithoutEpoch shouldBe firstNotification
          case other =>
            fail(s"Unexpected $other")
        }
      }
    }
    "get stored notifications" when {
      "call get with LastForClient filter" in {
        val clientsCount = 10
        val maxNotificationsCount = 20
        val notificationsMap = prepareNotificationsMap(clientsCount, maxNotificationsCount)

        notificationsMap.foreach { case (clientId, clientNotifications) =>
          dao.get(LastForClient(clientId)) match {
            case Success(notifications) if notifications.size == 1 =>
              val actual = notifications.head
              val actualWithoutEpoch = actual.withoutRecordIdAndEpoch
              val expected = clientNotifications.maxBy(_.tid)
              actualWithoutEpoch shouldBe expected
            case other =>
              fail(s"Unexpected $other")
          }
        }
      }
      "call get with UpdatedSinceBatchOrdered filter" in {
        val clientsCount = 10
        val maxNotificationsCount = 20
        val notificationsMap = prepareNotificationsMap(clientsCount, maxNotificationsCount)

        dao.get(UpdatedSinceBatchOrdered(0L, None, (clientsCount + 1) * maxNotificationsCount)) match {
          case Success(actualRecords) =>
            val actualWithoutRecordIdAndEpoch = actualRecords.map(_.withoutRecordIdAndEpoch)
            val expected = notificationsMap.values.flatten
            actualWithoutRecordIdAndEpoch should contain theSameElementsAs expected
            val starEpoch = 0L
            val batchSize = 5
            checkUpdatedSinceBatchOrdered(actualRecords.toSeq, starEpoch, None, batchSize)
          case other =>
            fail(s"Unexpected $other")
        }
      }
      "call get with PreviousByTidForClients filter" in {
        val clientsCount = 10
        val maxNotificationsCount = 20
        val notificationsMap = prepareNotificationsMap(clientsCount, maxNotificationsCount)

        val data = notificationsMap.map { case (clientId, clientNotifications) =>
          val sortedClientNotifications = clientNotifications.toSeq.sortBy(_.tid)
          val expected = sortedClientNotifications.init.lastOption
          val clientIdWithTid = ClientIdWithTid(clientId, sortedClientNotifications.last.tid)
          clientIdWithTid -> expected
        }
        val clientIdsWithTid = data.keys
        val expected = data.values.flatten

        dao.get(LastBeforeForClientsByTid(clientIdsWithTid.toSeq)) match {
          case Success(actual) =>
            val actualWithoutEpoch = actual.map(_.withoutRecordIdAndEpoch)
            actualWithoutEpoch should contain theSameElementsAs expected
          case other =>
            fail(s"Unexpected $other")
        }
      }
    }

  }

}

object NotifyClientDaoSpec {

  implicit class RichNotifyClient(notification: NotifyClient) {

    def withoutRecordIdAndEpoch: NotifyClient = {
      notification.copy(recordId = None, epoch = None)
    }

  }

}
