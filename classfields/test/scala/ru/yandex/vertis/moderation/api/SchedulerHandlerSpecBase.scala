package ru.yandex.vertis.moderation.api

import akka.http.scaladsl.model.StatusCodes
import ru.yandex.vertis.moderation.api.v1.service.scheduler.SchedulerService
import ru.yandex.vertis.moderation.api.v1.service.scheduler.SchedulerService.{RunResult, TaskPatch}
import ru.yandex.vertis.moderation.api.view.SchedulerTaskInfo
import ru.yandex.vertis.moderation.model.generators.CoreGenerators.StringGen
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.util.HandlerSpecBase
import ru.yandex.vertis.moderation.util.TmsGenerators.SchedulerTaskInfoGen
import ru.yandex.vertis.scheduler.OfferResult

import scala.concurrent.Future

/**
  * @author potseluev
  */
trait SchedulerHandlerSpecBase extends HandlerSpecBase {

  import akka.http.scaladsl.model.StatusCodes.OK

  protected def schedulerService: SchedulerService

  "getTasks" should {
    import ru.yandex.vertis.moderation.api.TmsMarshalling.IterableTaskInfoUnmarshaller

    "invoke correct method" in {
      doReturn(Future.successful(SchedulerTaskInfoGen.next(2)))
        .when(schedulerService)
        .getTasks
      Get(url("/")) ~> route ~> check {
        status shouldBe OK
        there.was(one(schedulerService).getTasks)
        responseAs[Iterable[SchedulerTaskInfo]]
      }
    }
  }

  "updateTask" should {
    import ru.yandex.vertis.moderation.api.TmsMarshalling.{SchedulerTaskInfoUnmarshaller, TaskPatchMarshaller}

    "invoke correct method with author and description" in {
      doReturn(Future.successful(SchedulerTaskInfoGen.next))
        .when(schedulerService)
        .updateTask(any[String], any[TaskPatch], any[Option[String]], any[Option[String]])
      val taskId = StringGen.next
      val patch =
        TaskPatch(
          enabled = None,
          scheduleIntervalSec = Some(100)
        )
      val author = "JohnDoe"
      val description = "Whatever"
      Patch(url(s"/$taskId/?author=$author&description=$description"), patch) ~> route ~> check {
        status shouldBe OK
        there.was(one(schedulerService).updateTask(taskId, patch, Some(author), Some(description)))
        responseAs[SchedulerTaskInfo]
      }
    }

    "invoke correct method without author and description" in {
      doReturn(Future.successful(SchedulerTaskInfoGen.next))
        .when(schedulerService)
        .updateTask(any[String], any[TaskPatch], any[Option[String]], any[Option[String]])
      val taskId = StringGen.next
      val patch =
        TaskPatch(
          enabled = None,
          scheduleIntervalSec = Some(100)
        )
      Patch(url(s"/$taskId/"), patch) ~> route ~> check {
        status shouldBe OK
        there.was(one(schedulerService).updateTask(taskId, patch, None, None))
        responseAs[SchedulerTaskInfo]
      }
    }
  }

  "runTask" should {
    import ru.yandex.vertis.moderation.api.TmsMarshalling.SchedulerTaskInfoUnmarshaller

    "invoke correct method" in {
      val taskId = StringGen.next
      val runResult = RunResult(OfferResult.Accepted(taskId), SchedulerTaskInfoGen.next)
      doReturn(Future.successful(runResult))
        .when(schedulerService)
        .run(taskId)
      Post(url(s"/$taskId")) ~> route ~> check {
        status shouldBe StatusCodes.OK
        there.was(one(schedulerService).run(taskId))
        responseAs[SchedulerTaskInfo]
      }
    }
  }
}
