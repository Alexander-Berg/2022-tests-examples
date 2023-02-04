package ru.yandex.vertis.billing.dao

import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.Inside.inside
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.Dao.EmonBillingOperationTaskPayload
import ru.yandex.vertis.billing.dao.EmonBillingOperationTaskDao.{NewTask, TasksStat}
import ru.yandex.vertis.billing.model_core.gens.{Producer, ProtobufMessageGenerators}
import ru.yandex.vertis.billing.util.clean.CleanableDao
import ru.yandex.vertis.protobuf.ProtoInstanceProvider.defaultInstanceByType

import scala.util.Success

/**
  * @author rmuzhikov
  */
trait EmonBillingOperationTaskDaoSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach {

  protected def taskDao: EmonBillingOperationTaskDao with CleanableDao

  override def beforeEach(): Unit = {
    taskDao.clean().get
    super.beforeEach()
  }

  private val payloadGen: Gen[EmonBillingOperationTaskPayload] =
    ProtobufMessageGenerators.generate[EmonBillingOperationTaskPayload](depth = 10)
  private val taskGen: Gen[NewTask] = payloadGen.map(NewTask(_))

  "Emon billing operation task dao" should {

    "read nothing when empty" in {
      inside(taskDao.peek(10)) { case Success(tasks) =>
        tasks shouldBe Seq.empty
      }
    }

    "add tasks" in {
      inside(taskDao.add(taskGen.next(100).toSeq)) { case Success(_) =>
        ()
      }
    }

    "peek added tasks sorted by epoch" in {
      val tasks = (1000 to 100000 by 1000).map(epoch => NewTask(payloadGen.next, epoch))
      inside {
        for {
          _ <- taskDao.add(tasks)
          result <- taskDao.peek(tasks.size * 2)
        } yield result
      } { case Success(actual) =>
        actual.map(t => NewTask(t.payload, t.epoch)) shouldBe tasks
      }

    }

    "remove tasks by ids" in {
      val tasks = taskGen.next(100).toSeq
      inside {
        for {
          _ <- taskDao.add(tasks)
          tasks <- taskDao.peek(tasks.size)
          _ <- taskDao.remove(tasks.map(_.id))
          afterRemove <- taskDao.peek(tasks.size)
        } yield afterRemove
      } { case Success(tasks) =>
        tasks shouldBe Seq.empty
      }
    }

    "provide correct stat" in {
      inside {
        for {
          stat1 <- taskDao.stat
          _ <- taskDao.add(taskGen.next(41).toSeq)
          _ <- taskDao.add(Seq(taskGen.next.copy(epoch = 2048)))
          stat2 <- taskDao.stat
          tasks <- taskDao.peek(stat2.taskCount)
          _ <- taskDao.remove(tasks.map(_.id))
          stat3 <- taskDao.stat
        } yield (stat1, stat2, stat3)
      } { case Success((stat1, stat2, stat3)) =>
        stat1 shouldBe TasksStat(0, None)
        stat2 shouldBe TasksStat(42, Some(2048))
        stat3 shouldBe TasksStat(0, None)
      }
    }

  }
}
