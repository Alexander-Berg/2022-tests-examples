package ru.auto.salesman.tasks.user.push

import ru.auto.salesman.tasks.user.push.model._
import zio.UIO
import zio.UIO.when

class DummyBroadcastPushingScheduleService(
    active: Option[BroadcastPushingSchedule]
) extends BroadcastPushingScheduleService {

  def getActive: UIO[Option[BroadcastPushingSchedule]] =
    UIO(currentActive)

  def markFinished(id: BroadcastPushingScheduleId): UIO[Unit] =
    when(currentActive.exists(_.id == id)) {
      UIO(currentActive = None)
    }

  private var currentActive = active
}
