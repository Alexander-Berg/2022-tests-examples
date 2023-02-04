package vertis.spamalot.controller.tasks.instances

import org.scalacheck.magnolia._
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.spamalot.controller.tasks.SpamalotYdbTaskSpecBase
import vertis.spamalot.model.ReceiverId
import vertis.zio.BaseEnv
import vertis.zio.test.ZioSpecBase

import java.time.Instant

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class SendPushTaskIntSpec extends ZioSpecBase with SpamalotYdbTaskSpecBase {

  lazy val sendPushTask =
    SendPushTask.create(components, TestOperationalSupport.prometheusRegistry)

  lazy val addNotificationTask = AddNotificationTask.create(components, TestOperationalSupport.prometheusRegistry)

  "SendPushTask" should {

    "send pushes" in ioTest {
      val now = Instant.now()
      val receiverId = random[ReceiverId]
      val n = 10
      val operations = randomOperations(now, n, receiverId)
      for {
        _ <- addOperations(receiverId, operations)
        _ <- checkState(receiverId, expectedNotificationsToAdd = Some(n))
        _ <- addNotificationTask.run(getPartition(receiverId))
        _ <- checkState(receiverId, expectedPushes = Some(n))
        _ <- sendPushTask.run(getPartition(receiverId))
        _ <- checkState(receiverId)
        _ <- checkPushesSend(receiverId, n)
      } yield ()
    }

    // todo: stop ignoring after VSDATA-1123
    "not send pushes for read notifications" ignore ioTest {
      val now = Instant.now()
      val receiverId = random[ReceiverId]
      val n = 10
      val nRead = n / 2
      val operations = randomOperations(now, n, receiverId)
      for {
        _ <- addOperations(receiverId, operations)
        _ <- checkState(receiverId, expectedNotificationsToAdd = Some(n))
        _ <- addNotificationTask.run(getPartition(receiverId))
        _ <- components.receiverNotifications
          .markRead(
            receiverId,
            operations
              .take(n / 2)
              .map(_.getAddNotification.notification.id)
          )
          .provideSomeLayer[BaseEnv](storages.ydbLayer)
        _ <- checkState(receiverId, expectedUnread = Some(n - nRead), expectedPushes = Some(n))
        _ <- sendPushTask.run(getPartition(receiverId))
        _ <- checkState(receiverId)
        _ <- checkPushesSend(receiverId, n - nRead)
      } yield ()
    }
  }
}
