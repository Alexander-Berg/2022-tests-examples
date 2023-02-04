package ru.auto.salesman.tasks.user.push

import ru.auto.salesman.tasks.user.push.ProgressService.Progress
import ru.auto.salesman.tasks.user.push.ProgressService.Progress.{Finished, InProgress}
import ru.auto.salesman.test.BaseSpec

trait ProgressServiceSpec[ProgressTrackingId, LastHandled] extends BaseSpec {

  protected val progressService: ProgressService[
    ProgressTrackingId,
    LastHandled
  ]

  import progressService._

  "ProgressService" should {

    "return empty progress if there is no such progress yet" in {
      getProgress(trackingId).success.value shouldBe Progress.empty
    }

    "return progress only with lastHandled after markLastHandled" in {
      val effect =
        markLastHandled(trackingId)(lastHandled) *> getProgress(trackingId)
      effect.success.value shouldBe InProgress(Some(lastHandled))
    }

    "return progress with later lastHandled after two markLastHandled invocations" in {
      val effect =
        markLastHandled(trackingId)(lastHandled) *>
          markLastHandled(trackingId)(laterLastHandled) *>
          getProgress(trackingId)
      effect.success.value shouldBe InProgress(Some(laterLastHandled))
    }

    "return finished after markFinished" in {
      val effect =
        markFinished(trackingId) *> getProgress(trackingId)
      effect.success.value shouldBe Finished
    }

    "return finished after markLastHandled and markFinished" in {
      val effect =
        markLastHandled(trackingId)(lastHandled) *>
          markFinished(trackingId) *>
          getProgress(trackingId)
      effect.success.value shouldBe Finished
    }

    "not return progress of trackingId for anotherTrackingId" in {
      val effect =
        markLastHandled(trackingId)(lastHandled) *>
          getProgress(anotherTrackingId)
      effect.success.value shouldBe Progress.empty
    }
  }

  protected val trackingId: ProgressTrackingId
  protected val anotherTrackingId: ProgressTrackingId
  protected val lastHandled: LastHandled
  protected val laterLastHandled: LastHandled
}
