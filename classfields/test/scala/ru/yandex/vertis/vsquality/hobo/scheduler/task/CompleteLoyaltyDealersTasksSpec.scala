package ru.yandex.vertis.vsquality.hobo.scheduler.task

import ru.yandex.vertis.vsquality.hobo.model._
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators.taskSourceGen
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.hobo.proto.Model
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.hobo.proto.Model.Task.State
import ru.yandex.vertis.vsquality.hobo.service._
import ru.yandex.vertis.vsquality.hobo.service.impl.mysql.MySqlTaskService
import ru.yandex.vertis.vsquality.hobo.service.impl.stub.{StubQueueSettingsService, StubUserService}
import ru.yandex.vertis.vsquality.hobo.util.{MySqlSpecBase, SpecBase}

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CompleteLoyaltyDealersTasksSpec extends SpecBase with MySqlSpecBase {
  val validComments = Set("automatically_approved", "white_list")

  "CompleteLoyaltyDealersTasks" should {
    validComments.foreach(shouldComplete)
    "do nothing with wrong comment" in {
      val result =
        for {
          task <- createTaskWithComment("foobar")
          _    <- checkTask(task.key, stateShouldBe = State.NEW)
        } yield ()
      result.futureValue
    }
  }

  private val queue = QueueId.AUTO_RU_LOYALTY_DEALERS

  private val taskService: TaskService = {
    val settingsService: QueueSettingsService = StubQueueSettingsService()
    val taskFactory = new TaskFactory(settingsService)
    val onCompleteActionsRegistry = OnCompleteActionsRegistry.Stub
    val onCompleteActionsFactory = new OnCompleteActionsFactory(settingsService, onCompleteActionsRegistry)
    new MySqlTaskService(
      database,
      maxBatchSize = 1000,
      needSaveHistory = true,
      taskFactory = taskFactory,
      onCompleteActionsFactory = onCompleteActionsFactory,
      readUserSupport = new StubUserService
    )
  }
  private val completeTask: CompleteLoyaltyDealersTasks = new CompleteLoyaltyDealersTasks(taskService, 10, UserId("1"))
  implicit private val rc: RequestContext = completeTask.rc

  private def shouldComplete(comment: String): Unit =
    s"complete successfully with $comment" in {
      val result =
        for {
          task <- createTaskWithComment(comment)
          _    <- checkTask(task.key, stateShouldBe = State.COMPLETED)
        } yield ()
      result.futureValue
    }

  private def createTaskWithComment(comment: String): Future[Task] = {
    val source =
      taskSourceGen[PayloadSource.External]()
        .map(_.copy(comment = Some(comment)))
        .next
    taskService.create(queue, source)
  }

  @nowarn("cat=w-flag-value-discard")
  private def checkTask(key: TaskKey, stateShouldBe: State): Future[Unit] =
    completeTask.action
      .flatMap(_ => taskService.get(queue, key))
      .map { task =>
        task.state shouldBe stateShouldBe
        if (stateShouldBe == State.COMPLETED) resolutionIsValid(task.resolution) shouldBe true
      }

  private def resolutionIsValid(resolution: Option[Resolution]): Boolean =
    resolution match {
      case Some(AutoruLoyaltyDealersResolution(value, _, comment)) =>
        value == Model.AutoruLoyaltyDealersResolution.Value.UNBAN &&
        validComments.contains(comment)
      case _ => false
    }
}
