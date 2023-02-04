package ru.yandex.vos2.autoru.notifications

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.letters.DealerLowPriceBanNotification
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.notifications.{FailedSent, NotifySent}
import ru.yandex.vos2.services.sender.ToEmailDelivery

/**
  * Created by sievmi on 25.03.19
  */
class BatchNotificationWorkerTest extends AnyFunSuite with MockitoSupport with InitTestDbs with BeforeAndAfterAll {

  implicit val trace = Traced.empty

  override def beforeAll(): Unit = {
    initDbs()
  }

  test("status to ready") {
    val notifyManager = mock[AutoruNotifyManager]
    when(notifyManager.send(?, ?)(?)).thenReturn(NotifySent)
    val notification =
      new DealerLowPriceBanNotification("", ToEmailDelivery("car@car.ru"))

    when(notifyManager.getUserNotification(?, ?)(?)).thenReturn(notification)

    val worker = new BatchNotificationWorker(
      components.notificationsDao,
      notifyManager,
      _ => true,
      components.featuresManager.UserBatchNotifications
    )

    val userRef = UserRef.from("ac_123")
    val notificationType = NotificationType.U_DEALER_NEW_LOW_PRICE_BAN

    addNoSentNotification(userRef, notificationType)
    assert(components.notificationsDao.getNotSent(userRef, notificationType).nonEmpty)

    worker.process()

    assert(components.notificationsDao.getNotSent(userRef, notificationType).isEmpty)
  }

  test("cancel after 3 retry") {
    val notifyManager = mock[AutoruNotifyManager]
    when(notifyManager.send(?, ?)(?)).thenReturn(FailedSent(new RuntimeException("test")))
    val notification =
      new DealerLowPriceBanNotification("", ToEmailDelivery("car@car.ru"))

    when(notifyManager.getUserNotification(?, ?)(?)).thenReturn(notification)

    val worker = new BatchNotificationWorker(
      components.notificationsDao,
      notifyManager,
      _ => true,
      components.featuresManager.UserBatchNotifications
    )

    val userRef = UserRef.from("ac_123")
    val notificationType = NotificationType.U_DEALER_NEW_LOW_PRICE_BAN

    addNoSentNotification(userRef, notificationType)

    worker.process()
    assert(components.notificationsDao.getNotSent(userRef, notificationType).get.retryCount == 1)
    worker.process()
    assert(components.notificationsDao.getNotSent(userRef, notificationType).get.retryCount == 2)
    worker.process()
    assert(components.notificationsDao.getNotSent(userRef, notificationType).get.retryCount == 3)
    worker.process()

    assert(components.notificationsDao.getNotSent(userRef, notificationType).isEmpty)
  }

  private def addNoSentNotification(userRef: UserRef, notification: NotificationType, tsReady: Long = 0): Unit = {
    components.notificationsDao.append(userRef, notification, timestampReady = tsReady) { (_, _) =>
      Array.emptyByteArray
    }
  }

}
