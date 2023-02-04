package vertis.spamalot.controller.tasks.instances

import java.time.Instant
import ru.yandex.vertis.ops.test.TestOperationalSupport
import vertis.spamalot.controller.tasks.SpamalotYdbTaskSpecBase
import vertis.spamalot.model.{ReceiverId, UserId}
import vertis.zio.test.ZioSpecBase
import org.scalacheck.magnolia._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class AddNotificationTaskIntSpec extends ZioSpecBase with SpamalotYdbTaskSpecBase {

  lazy val task = AddNotificationTask.create(components, TestOperationalSupport.prometheusRegistry)

  "AddNotificationTask" should {

    "add notifications" in ioTest {
      val now = Instant.now()
      val receiverId = random[ReceiverId]
      val n = 10
      val operations = randomOperations(now, n, receiverId)
      for {
        _ <- addOperations(receiverId, operations)
        _ <- task.run(getPartition(receiverId))
        _ <- checkFullState(receiverId, 0, n, n, n)
      } yield ()
    }

    "skip duplicates" in {
      val now = Instant.now()
      val receiverId = random[ReceiverId]
      val operations = randomOperations(now, 1, receiverId)
      for {
        _ <- addOperations(receiverId, operations)
        _ <- task.run(getPartition(receiverId))
        _ <- checkFullState(receiverId, 0, 1, 1, 1)
        _ <- addOperations(receiverId, operations)
        _ <- checkFullState(receiverId, 1, 1, 1, 1)
        _ <- task.run(getPartition(receiverId))
        _ <- checkFullState(receiverId, 0, 1, 1, 1)
      } yield ()
    }
  }

}
