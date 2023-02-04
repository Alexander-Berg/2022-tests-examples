package ru.auto.salesman.tasks.user.push

import cats.implicits._
import ru.auto.salesman.tasks.user.push.BroadcastPushingTask.ProgressTrackingId
import ru.auto.salesman.tasks.user.push.MarkBroadcastPushingFinishedTaskSpec.TestEnv
import ru.auto.salesman.tasks.user.push.model._
import MarkBroadcastPushingFinishedTaskSpec._
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.push.PushSourceType.PeriodicalDiscount
import ru.auto.salesman.model.push.{PushBody, PushName, PushSourceId, PushTitle}
import ru.auto.salesman.tasks.Partition
import ru.auto.salesman.test.BaseSpec
import zio.interop.catz.core._

class MarkBroadcastPushingFinishedTaskSpec extends BaseSpec {

  "MarkBroadcastPushingFinishedTask" should {

    "do nothing if no active broadcast pushing" in
      new TestEnv(activeSchedule = None) {
        private val effect = for {
          _ <- task.task
          active <- broadcastPushingScheduleService.getActive
        } yield active shouldBe empty
        effect.success
      }

    "not touch broadcast pushing with unfinished partitions" in
      new TestEnv(Some(activeSchedule)) {
        private val notFinished :: finished =
          Partition
            .all(BroadcastPushingTask.Parallelism)
            .map(ProgressTrackingId(activeSchedule.id, _))
        private val effect = for {
          _ <- progressService.markLastHandled(notFinished)(TestLastHandled)
          _ <- finished.traverse_(progressService.markFinished)
          _ <- task.task
          active <- broadcastPushingScheduleService.getActive
        } yield active.value shouldBe activeSchedule
        effect.success
      }

    "mark broadcast pushing with all finished partitions as finished" in
      new TestEnv(Some(activeSchedule)) {
        private val trackingIds =
          Partition
            .all(BroadcastPushingTask.Parallelism)
            .map(ProgressTrackingId(activeSchedule.id, _))
        private val effect = for {
          _ <- trackingIds.traverse_(progressService.markFinished)
          _ <- task.task
          active <- broadcastPushingScheduleService.getActive
        } yield active shouldBe empty
        effect.success
      }
  }
}

object MarkBroadcastPushingFinishedTaskSpec {

  object TestLastHandled

  class TestEnv(activeSchedule: Option[BroadcastPushingSchedule]) {

    protected val broadcastPushingScheduleService: BroadcastPushingScheduleService =
      new DummyBroadcastPushingScheduleService(activeSchedule)

    protected val progressService: ProgressService[ProgressTrackingId, TestLastHandled.type] =
      new DummyProgressService

    protected val task: MarkBroadcastPushingFinishedTask =
      new MarkBroadcastPushingFinishedTask(
        broadcastPushingScheduleService,
        progressService
      )
  }

  private val activeSchedule =
    BroadcastPushingSchedule(
      BroadcastPushingScheduleId(1),
      PushTitle("Скидка 70%"),
      PushBody("Только сегодня скидка 70% на Турбо-продажу!"),
      PushName("VAS_080919_turbo"),
      now(),
      PeriodicalDiscount,
      PushSourceId(100)
    )
}
