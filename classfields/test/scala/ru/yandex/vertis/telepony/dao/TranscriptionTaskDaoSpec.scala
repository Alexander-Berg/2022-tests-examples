package ru.yandex.vertis.telepony.dao

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model.TranscriptionTask
import ru.yandex.vertis.telepony.model.TranscriptionTaskStatusValues._
import ru.yandex.vertis.telepony.service.TranscriptionTaskService.Filter
import ru.yandex.vertis.telepony.util.Range
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

/**
  * @author neron
  */
trait TranscriptionTaskDaoSpec extends SpecBase with DatabaseSpec with ScalaCheckPropertyChecks {

  def dao: TranscriptionTaskDao

  override protected def beforeEach(): Unit = {
    dao.clear().futureValue
    super.beforeEach()
  }

  "TranscriptionTaskDao" should {

    "create new" in {
      val recordId = ShortStr.next
      val expectedTask = TranscriptionTask.newTask(recordId)
      dao.create(expectedTask).futureValue
      val actualTasks = dao.list(Filter.Empty, Range.Full).futureValue
      actualTasks should have size 1
      actualTasks.head should ===(expectedTask)
    }

    "create duplicate without fails" in {
      val recordId = ShortStr.next
      val expectedTask = TranscriptionTask.newTask(recordId)
      dao.create(expectedTask).futureValue
      dao.create(expectedTask).futureValue
      val actualTasks = dao.list(Filter.Empty, Range.Full).futureValue
      actualTasks should have size 1
      actualTasks.head should ===(expectedTask)
    }

    "update" in {
      forAll(TranscriptionTaskUpdateRequestGen) { request =>
        dao.clear().futureValue
        val recordId = ShortStr.next
        val task = TranscriptionTask.newTask(recordId)
        dao.create(task).futureValue
        dao.update(recordId, request).futureValue
        val actualTasks = dao.list(Filter.Empty, Range.Full).futureValue
        actualTasks should have size 1
        val expectedTask = task.copy(
          status = request.status,
          updateTime = request.updateTime
        )
        actualTasks.head should ===(expectedTask)
      }
    }

    "list by status" in {
      val recordId1 = ShortStr.next
      val recordId2 = ShortStr.next
      val newTask = TranscriptionTask.newTask(recordId1)
      val failedTask = TranscriptionTask.newTask(recordId2).copy(status = Failed)
      dao.create(newTask).futureValue
      dao.create(failedTask).futureValue
      val newTasks = dao.list(Filter.ByStatus(New), Range.Full).futureValue
      newTasks should have size 1
      newTasks.head should ===(newTask)
    }

    "list all" in {
      val recordId1 = ShortStr.next
      val recordId2 = ShortStr.next
      val newTask = TranscriptionTask.newTask(recordId1)
      val failedTask = TranscriptionTask.newTask(recordId2).copy(status = Failed)
      dao.create(newTask).futureValue
      dao.create(failedTask).futureValue
      val newTasks = dao.list(Filter.Empty, Range.Full).futureValue
      newTasks should have size 2
    }

  }

}
