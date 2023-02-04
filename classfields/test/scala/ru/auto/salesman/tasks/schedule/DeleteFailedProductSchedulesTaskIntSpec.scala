package ru.auto.salesman.tasks.schedule

import org.joda.time.DateTime
import ru.auto.salesman.dao.ScheduleInstanceDao.InstanceFilter.ForStatuses
import ru.auto.salesman.dao.ScheduleInstanceDao.Patch.StatusPatch
import ru.auto.salesman.dao.impl.jdbc.user._
import ru.auto.salesman.dao.slick.invariant.StaticQuery
import ru.auto.salesman.model.ScheduleInstance.Statuses.{Done, Pending}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.ScheduleInstanceService
import ru.auto.salesman.service.impl.user.ProductScheduleServiceImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

import scala.util.Try

class DeleteFailedProductSchedulesTaskIntSpec
    extends BaseSpec
    with SalesmanUserJdbcSpecTemplate
    with ProductScheduleModelGenerators {
  private val scheduleDao = new JdbcProductScheduleDao(database)

  private val instanceDao = new JdbcProductScheduleInstanceDao(database)

  private val task = new DeleteFailedProductSchedulesTask(
    new ProductScheduleServiceImpl(scheduleDao, instanceDao)
  )

  implicit private val rc: RequestContext = AutomatedContext("test")

  "DeleteFailedProductSchedulesTask" should {

    "delete schedule if all schedule instances are failed in last 70 days" in {
      val now = DateTime.parse("2020-08-01T14:35:00+03:00")
      val lastInstanceFailure = DateTime.parse("2020-07-31T19:00:00+03:00")
      val scheduleSource = scheduleSourceGen().next
      for {
        _ <- scheduleDao.insertIfAbsent(scheduleSource)
        schedule <- scheduleDao.get().map(_.head)
        // Расписание может быть настроено не на каждый день,
        // а только на конкретный день недели (или дни).
        instanceSources = (66 to 0 by -7).map { daysBack =>
          val thisInstanceFailure = lastInstanceFailure.minusDays(daysBack)
          ScheduleInstanceService.Source(
            scheduleId = schedule.id,
            fireTime = thisInstanceFailure,
            scheduleUpdateTime = schedule.updatedAt
          )
        }
        _ <- instanceDao.insert(instanceSources)
        _ <- failAllInstances()
        _ <- task.doExecute(now)
        updatedSchedule <- scheduleDao.get().map(_.head)
      } yield updatedSchedule.isDeleted shouldBe true
    }.success

    "delete schedule if all schedule instances are failed in last 70 days, and there is a pending instance" in {
      val now = DateTime.parse("2020-08-01T14:35:00+03:00")
      val lastInstanceFailure = DateTime.parse("2020-07-31T19:00:00+03:00")
      val scheduleSource = scheduleSourceGen().next
      for {
        _ <- scheduleDao.insertIfAbsent(scheduleSource)
        schedule <- scheduleDao.get().map(_.head)
        // Расписание может быть настроено не на каждый день,
        // а только на конкретный день недели (или дни).
        instanceSources = (66 to 0 by -7).map { daysBack =>
          val thisInstanceFailure = lastInstanceFailure.minusDays(daysBack)
          ScheduleInstanceService.Source(
            scheduleId = schedule.id,
            fireTime = thisInstanceFailure,
            scheduleUpdateTime = schedule.updatedAt
          )
        }
        _ <- instanceDao.insert(instanceSources)
        _ <- failAllInstances()
        pending = ScheduleInstanceService.Source(
          scheduleId = schedule.id,
          fireTime = now,
          scheduleUpdateTime = schedule.updatedAt
        )
        _ <- instanceDao.insert(List(pending))
        _ <- task.doExecute(now)
        updatedSchedule <- scheduleDao.get().map(_.head)
      } yield updatedSchedule.isDeleted shouldBe true
    }.success

    "delete schedule if all schedule instances are failed in last 70 days, and there is an acquired instance" in {
      val now = DateTime.parse("2020-08-01T14:35:00+03:00")
      val lastInstanceFailure = DateTime.parse("2020-07-31T19:00:00+03:00")
      val scheduleSource = scheduleSourceGen().next
      for {
        _ <- scheduleDao.insertIfAbsent(scheduleSource)
        schedule <- scheduleDao.get().map(_.head)
        // Расписание может быть настроено не на каждый день,
        // а только на конкретный день недели (или дни).
        instanceSources = (66 to 0 by -7).map { daysBack =>
          val thisInstanceFailure = lastInstanceFailure.minusDays(daysBack)
          ScheduleInstanceService.Source(
            scheduleId = schedule.id,
            fireTime = thisInstanceFailure,
            scheduleUpdateTime = schedule.updatedAt
          )
        }
        _ <- instanceDao.insert(instanceSources)
        _ <- failAllInstances()
        pending = ScheduleInstanceService.Source(
          scheduleId = schedule.id,
          fireTime = lastInstanceFailure.minusDays(40),
          scheduleUpdateTime = schedule.updatedAt
        )
        _ <- instanceDao.insert(List(pending))
        _ <- acquireAllPendingInstances()
        _ <- task.doExecute(now)
        updatedSchedule <- scheduleDao.get().map(_.head)
      } yield updatedSchedule.isDeleted shouldBe true
    }.success

    "not delete schedule if there are many failed instances, but at least one succeeded instance in last 60 days" in {
      val now = DateTime.parse("2020-08-01T14:35:00+03:00")
      val lastInstanceFailure = DateTime.parse("2020-07-24T19:00:00+03:00")
      val scheduleSource = scheduleSourceGen().next
      for {
        _ <- scheduleDao.insertIfAbsent(scheduleSource)
        schedule <- scheduleDao.get().map(_.head)
        // Расписание может быть настроено не на каждый день,
        // а только на конкретный день недели (или дни).
        failedInstancesSources = (52 to 0 by -7).map { daysBack =>
          val thisInstanceFailure = lastInstanceFailure.minusDays(daysBack)
          ScheduleInstanceService.Source(
            scheduleId = schedule.id,
            fireTime = thisInstanceFailure,
            scheduleUpdateTime = schedule.updatedAt
          )
        }
        _ <- instanceDao.insert(failedInstancesSources)
        _ <- failAllInstances()
        succeededInstanceSource = ScheduleInstanceService.Source(
          scheduleId = schedule.id,
          fireTime = lastInstanceFailure.plusDays(7),
          scheduleUpdateTime = schedule.updatedAt
        )
        _ <- instanceDao.insert(List(succeededInstanceSource))
        _ <- instanceDao.update(StatusPatch(Done), ForStatuses(Pending))
        _ <- task.doExecute(now)
        updatedSchedule <- scheduleDao.get().map(_.head)
      } yield updatedSchedule.isDeleted shouldBe false
    }.success

    "not delete schedule if there are no failed instances in last 60 days" in {
      val now = DateTime.parse("2020-08-01T14:35:00+03:00")
      val scheduleSource = scheduleSourceGen().next
      for {
        _ <- scheduleDao.insertIfAbsent(scheduleSource)
        _ <- task.doExecute(now)
        schedule <- scheduleDao.get().map(_.head)
      } yield schedule.isDeleted shouldBe false
    }.success
  }

  private def failAllInstances(): Try[Unit] = Try {
    database.withTransaction { implicit session =>
      // Обновляем явно fire_time, иначе MySQL в тесте неявно сбрасывает
      // fire_time в now. Как это пофиксить через конфиг, непонятно.
      // В MDB такого поведения нет.
      // https://stackoverflow.com/a/409305/5053865:
      // Timestamps in MySQL are generally used to track changes to records,
      // and are often updated every time the record is changed.
      StaticQuery
        .updateNA(
          "UPDATE product_schedule_instance " +
          "SET status = 'Failed', fire_time = fire_time"
        )
        .execute
    }
  }

  private def acquireAllPendingInstances(): Try[Unit] = Try {
    database.withTransaction { implicit session =>
      // см. failAllInstances(), почему UPDATE руками
      StaticQuery
        .updateNA(
          "UPDATE product_schedule_instance " +
          "SET status = 'Acquired', fire_time = fire_time " +
          "WHERE status = 'Pending'"
        )
        .execute
    }
  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
