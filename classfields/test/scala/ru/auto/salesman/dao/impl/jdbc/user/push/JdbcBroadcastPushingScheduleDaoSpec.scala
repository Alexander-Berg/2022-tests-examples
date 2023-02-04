package ru.auto.salesman.dao.impl.jdbc.user.push

import ru.auto.salesman.dao.impl.jdbc.user.push.TestJdbcBroadcastPushingScheduleDao.newBroadcastPushingScheduleGen
import ru.auto.salesman.tasks.user.push.model.BroadcastPushingScheduleId
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.auto.salesman.test.{BaseSpec, IntegrationPropertyCheckConfig}
import ru.yandex.vertis.generators.DateTimeGenerators.{dateTimeInFuture, dateTimeInPast}

import scala.concurrent.duration._

class JdbcBroadcastPushingScheduleDaoSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate
    with IntegrationPropertyCheckConfig {

  "JdbcBroadcastPushingScheduleDao.getActive" should {

    "return nothing if there are no schedules at all" in {
      assertNoActiveSchedule()
    }

    "return nothing if only schedule in the past exists" in {
      forAll(
        newBroadcastPushingScheduleGen(
          startGen = dateTimeInPast,
          deadlineGen = dateTimeInPast,
          finishedGen = false
        )
      ) { scheduleInPast =>
        dao.insert(scheduleInPast)
        assertNoActiveSchedule()
      }
    }

    "return nothing if only schedule in the future exists" in {
      forAll(
        newBroadcastPushingScheduleGen(
          // add 20 minutes to avoid test lags at all
          startGen = dateTimeInFuture(minDistance = 20.minutes),
          deadlineGen = dateTimeInFuture(minDistance = 20.minutes),
          finishedGen = false
        )
      ) { scheduleInFuture =>
        dao.insert(scheduleInFuture)
        assertNoActiveSchedule()
      }
    }

    "return nothing if only finished schedule exists" in {
      forAll(
        newBroadcastPushingScheduleGen(
          startGen = dateTimeInPast,
          deadlineGen = dateTimeInFuture(),
          finishedGen = true
        )
      ) { finishedSchedule =>
        dao.insert(finishedSchedule)
        assertNoActiveSchedule()
      }
    }

    "return active schedule if it exists" in {
      forAll(
        newBroadcastPushingScheduleGen(
          startGen = dateTimeInPast,
          // add 20 minutes to avoid test lags at all
          deadlineGen = dateTimeInFuture(minDistance = 20.minutes),
          finishedGen = false
        )
      ) { activeSchedule =>
        dao.insert(activeSchedule)
        val result = dao.getActive.success.value.value
        (
          result.title,
          result.body,
          result.name,
          result.start
        ) shouldBe (activeSchedule.title, activeSchedule.body, activeSchedule.name, activeSchedule.start)
        result.sourceId.asLong shouldBe activeSchedule.sourceId
      }
    }
  }

  "JdbcBroadcastPushingScheduleDao.markFinished" should {

    "deactivate finished schedule" in {
      forAll(
        newBroadcastPushingScheduleGen(
          startGen = dateTimeInPast,
          // add 20 minutes to avoid test lags at all
          deadlineGen = dateTimeInFuture(minDistance = 20.minutes),
          finishedGen = false
        )
      ) { activeSchedule =>
        dao.insert(activeSchedule)
        val effect = for {
          inserted <- dao.getActive
          _ <- dao.markFinished(inserted.value.id)
        } yield dao.getActive.success.value shouldBe empty
        effect.success
      }
    }

    "not touch schedule if another id is given" in {
      forAll(
        newBroadcastPushingScheduleGen(
          startGen = dateTimeInPast,
          // add 20 minutes to avoid test lags at all
          deadlineGen = dateTimeInFuture(minDistance = 20.minutes),
          finishedGen = false
        )
      ) { activeSchedule =>
        dao.insert(activeSchedule)
        val effect = for {
          inserted <- dao.getActive
          _ <- dao.markFinished(
            BroadcastPushingScheduleId(inserted.value.id.asLong + 1)
          )
          same <- dao.getActive
        } yield same shouldBe inserted
        effect.success
      }
    }
  }

  private val dao =
    new TestJdbcBroadcastPushingScheduleDao(database)

  private def assertNoActiveSchedule(): Unit =
    dao.getActive.success.value shouldBe empty
}
