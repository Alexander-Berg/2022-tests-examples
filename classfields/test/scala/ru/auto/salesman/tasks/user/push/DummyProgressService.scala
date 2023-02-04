package ru.auto.salesman.tasks.user.push

import ru.auto.salesman.tasks.user.push.ProgressService.Progress
import ru.auto.salesman.tasks.user.push.ProgressService.Progress.{Finished, InProgress}
import zio.{UIO, ZIO}

class DummyProgressService[ProgressTrackingId, LastHandled]
    extends ProgressService[ProgressTrackingId, LastHandled] {

  def getProgress(trackingId: ProgressTrackingId): UIO[Progress[LastHandled]] =
    ZIO.succeed(progress.getOrElse(trackingId, Progress.empty))

  def markLastHandled(trackingId: ProgressTrackingId)(
      lastHandled: LastHandled
  ): UIO[Unit] =
    update(trackingId, InProgress(Some(lastHandled)))

  def markFinished(trackingId: ProgressTrackingId): UIO[Unit] =
    update(trackingId, Finished)

  @volatile private var progress =
    Map.empty[ProgressTrackingId, Progress[LastHandled]]

  private def update(
      trackingId: ProgressTrackingId,
      newProgress: Progress[LastHandled]
  ) =
    ZIO.succeed(progress += trackingId -> newProgress)
}
