package ru.yandex.vertis.vsquality.hobo.dao.ydb

import org.scalatest.OptionValues.convertOptionToValuable
import ru.yandex.vertis.generators.NetGenerators.asProducer
import ru.yandex.vertis.vsquality.hobo.dao.TaskViewDao
import ru.yandex.vertis.vsquality.hobo.model.TaskViewKey
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators.NewTaskViewGen
import ru.yandex.vertis.vsquality.hobo.util.SpecBase

import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext

trait TaskViewDaoSpecBase extends SpecBase {
  def taskViewDao: TaskViewDao
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "dao" should {
    "upsert" in {
      val taskView = NewTaskViewGen.map { taskView =>
        // because of different java.time.Instant resolution in CI (microseconds -> nanoseconds)
        val expireAt = taskView.expireAt.truncatedTo(ChronoUnit.SECONDS)
        val updateAt = taskView.updateAt.truncatedTo(ChronoUnit.SECONDS)
        taskView.copy(expireAt = expireAt, updateAt = updateAt)
      }.next
      val taskViewFromDb = for {
        _      <- taskViewDao.upsert(taskView)
        fromDb <- taskViewDao.get(TaskViewKey(taskView.queue, taskView.key))
      } yield fromDb
      taskViewFromDb.futureValue.value shouldBe taskView
    }
  }
}
