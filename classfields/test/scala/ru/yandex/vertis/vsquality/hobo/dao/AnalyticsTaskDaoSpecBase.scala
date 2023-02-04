package ru.yandex.vertis.vsquality.hobo.dao

import ru.yandex.vertis.vsquality.hobo.model.TrueFalseResolution
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators._
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.vsquality.hobo.util.SpecBase

/**
  * @author semkagtn
  */
trait AnalyticsTaskDaoSpecBase extends SpecBase {

  def analyticsTaskDao: AnalyticsTaskDao

  before {
    analyticsTaskDao.clear().futureValue
  }

  "upsert" should {

    "correctly insert one task" in {
      val task = TaskGen.next
      val result = analyticsTaskDao.upsert(Iterable(task)).futureValue
      result should smartEqual(())
    }

    "accept supplementary characters in string fields" in {
      val task = TaskGen.next.copy(resolution = Some(TrueFalseResolution(true, "📲")))
      val result = analyticsTaskDao.upsert(Iterable(task)).futureValue
      result should smartEqual(())
    }

    "correctly insert two tasks" in {
      val tasks = TaskGen.next(2).toList
      val result = analyticsTaskDao.upsert(tasks).futureValue
      result should smartEqual(())
    }

    "correctly update task" in {
      val oldTask = TaskGen.next
      val newTask = TaskGen.next.copy(key = oldTask.key)
      analyticsTaskDao.upsert(Iterable(oldTask)).futureValue

      val result = analyticsTaskDao.upsert(Iterable(newTask)).futureValue
      result should smartEqual(())
    }
  }

  "remove before" should {

    "correctly remove old tasks" in {
      val createdBefore = DateTimeGen.next
      val tasks = TaskGen.next(2).map(_.copy(createTime = createdBefore.plus(1)))
      val taskToDelete = TaskGen.next(2).toList.map(_.copy(createTime = createdBefore.minus(1)))
      analyticsTaskDao.upsert(taskToDelete ++ tasks).futureValue
      analyticsTaskDao.removeBefore(createdBefore).futureValue shouldBe tasks.size
    }
  }
}
