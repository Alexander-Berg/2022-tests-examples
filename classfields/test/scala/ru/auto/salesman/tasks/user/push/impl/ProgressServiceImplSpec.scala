package ru.auto.salesman.tasks.user.push.impl

import ru.auto.salesman.dao.impl.jdbc.user.push.JdbcProgressDao
import ru.auto.salesman.dao.slick.invariant.StaticQuery
import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.tasks.Partition
import ru.auto.salesman.tasks.user.push.BroadcastPushingTask.ProgressTrackingId
import ru.auto.salesman.tasks.user.push.ProgressService.Progress.InProgress
import ru.auto.salesman.tasks.user.push.model.BroadcastPushingScheduleId
import ru.auto.salesman.tasks.user.push.{ProgressService, ProgressServiceSpec}
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

class ProgressServiceImplSpec
    extends ProgressServiceSpec[ProgressTrackingId, AutoruUser]
    with SalesmanUserJdbcSpecTemplate {

  "ProgressServiceImpl" should {

    "parse correctly existing progress record" in {
      dao.database.withSession { implicit session =>
        StaticQuery
          .updateNA("""
            INSERT INTO `progress`
            (`progress_tracking_id`, `last_handled`, `finished`)
            VALUES ('broadcast-pushing-schedule-1-partition-0-of-1', 'user:3', 0)
            """)
          .execute
      }
      progressService.getProgress(trackingId).success.value shouldBe InProgress(
        Some(lastHandled)
      )
    }
  }

  private val dao =
    new JdbcProgressDao[ProgressTrackingId, AutoruUser](database)

  override protected val progressService: ProgressService[ProgressTrackingId, AutoruUser] =
    new ProgressServiceImpl(dao)

  override protected val trackingId: ProgressTrackingId =
    ProgressTrackingId(BroadcastPushingScheduleId(1), Partition.all(1).head)

  override protected val anotherTrackingId: ProgressTrackingId =
    ProgressTrackingId(BroadcastPushingScheduleId(2), Partition.all(1).head)

  override protected val lastHandled: AutoruUser = AutoruUser(3)
  override protected val laterLastHandled: AutoruUser = AutoruUser(4)
}
