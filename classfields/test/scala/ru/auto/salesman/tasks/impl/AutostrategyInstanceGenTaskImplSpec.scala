package ru.auto.salesman.tasks.impl

import org.joda.time.Days
import org.scalatest.{BeforeAndAfter, Inspectors}
import ru.auto.salesman.dao.ScheduleInstanceDao.InstanceFilter.ForScheduleIds
import ru.auto.salesman.dao.impl.jdbc.test.{
  JdbcAutostrategiesDaoForTests,
  JdbcScheduleInstanceDaoForTests
}
import ru.auto.salesman.environment.now
import ru.auto.salesman.model.ScheduleInstance.Statuses._
import ru.auto.salesman.service.{AutostrategyInstanceGenImpl, EpochService}
import ru.auto.salesman.tasks.impl.AutostrategyInstanceGenTaskImpl.Marker
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.template.SalesmanJdbcSpecTemplate
import ru.yandex.vertis.generators.ProducerProvider.asProducer

class AutostrategyInstanceGenTaskImplSpec
    extends BaseSpec
    with SalesmanJdbcSpecTemplate
    with BeforeAndAfter {

  private val autostrategiesDao = new JdbcAutostrategiesDaoForTests(database)

  private val instanceDao = new JdbcScheduleInstanceDaoForTests(
    database,
    "autostrategy_instance",
    "autostrategy_id"
  )

  private val epochService = mock[EpochService]

  private val instanceGen = new AutostrategyInstanceGenImpl

  private val task = new AutostrategyInstanceGenTaskImpl(
    autostrategiesDao,
    epochService,
    instanceDao,
    instanceGen
  )

  before {
    autostrategiesDao.clean()
    instanceDao.clean()
  }

  "Autostrategy instance gen task" should {

    "generate instances" in {
      val autostrategies = AutostrategyListGen.next.take(3)
      autostrategiesDao.put(autostrategies).success
      val stored = autostrategiesDao.all().success.value
      (epochService.getOptional _).expects(Marker).returningT(None)
      val expectedEpoch = stored.maxBy(_.epoch).epoch
      (epochService.set _).expects(Marker, expectedEpoch).returningT(())
      task.execute(regenerateAll = false).success
      Inspectors.forEvery(stored) { autostrategy =>
        val instances =
          instanceDao.get(ForScheduleIds(autostrategy.id))().success.value
        Inspectors.forEvery(instances)(_.status shouldBe Pending)
        import autostrategy.props.{fromDate, maxApplicationsPerDay, toDate}
        val applyIntervalLength = Days.daysBetween(fromDate, toDate).getDays
        val days =
          List.iterate(fromDate, applyIntervalLength + 1)(_.plusDays(1))
        val allDaysInstances = instances.groupBy(_.fireTime.toLocalDate)
        Inspectors.forEvery(days) { day =>
          val dayInstances = allDaysInstances.getOrElse(day, Nil)
          if (day == now().toLocalDate)
            // instances for today may be not generated
            for (count <- maxApplicationsPerDay)
              dayInstances should ((have size count).or(have) size 0)
          else
            maxApplicationsPerDay match {
              case Some(count) =>
                dayInstances should have size count
              case None =>
                dayInstances.size shouldBe >(0)
            }
        }
      }
    }

    "inactivate instances" in {
      // it's unclear why these tests fail after testcontainers integration
      pending
      val autostrategies = AutostrategyListGen.next.take(3)
      autostrategiesDao.put(autostrategies).success
      val stored = autostrategiesDao.all().success.value
      (epochService.getOptional _).expects(Marker).returningT(None).twice()
      val expectedEpoch = stored.maxBy(_.epoch).epoch
      (epochService.set _).expects(Marker, expectedEpoch).returningT(())
      task.execute(regenerateAll = false).success
      autostrategiesDao.delete(autostrategies.map(_.id)).success
      val deleted = autostrategiesDao.all().success.value
      val expectedDeletedEpoch = deleted.maxBy(_.epoch).epoch
      (epochService.set _).expects(Marker, expectedDeletedEpoch).returningT(())
      task.execute(regenerateAll = false).success
      Inspectors.forEvery(stored) { autostrategy =>
        val instances =
          instanceDao.get(ForScheduleIds(autostrategy.id))().success.value
        Inspectors.forEvery(instances)(_.status shouldBe Cancelled)
        import autostrategy.props.{fromDate, maxApplicationsPerDay, toDate}
        val applyIntervalLength = Days.daysBetween(fromDate, toDate).getDays
        val days =
          List.iterate(fromDate, applyIntervalLength + 1)(_.plusDays(1))
        val allDaysInstances = instances.groupBy(_.fireTime.toLocalDate)
        Inspectors.forEvery(days) { day =>
          val dayInstances = allDaysInstances.getOrElse(day, Nil)
          if (day == now().toLocalDate)
            // instances for today may be not generated
            for (count <- maxApplicationsPerDay)
              dayInstances should ((have size count).or(have) size 0)
          else
            maxApplicationsPerDay match {
              case Some(count) =>
                dayInstances should have size count
              case None =>
                dayInstances.size shouldBe >(0)
            }
        }
      }
    }

    "regenerate instances" in {
      pending
      val autostrategies = AutostrategyListGen.next.take(3)
      autostrategiesDao.put(autostrategies).success
      val stored = autostrategiesDao.all().success.value
      (epochService.getOptional _).expects(Marker).returningT(None)
      val expectedEpoch = stored.maxBy(_.epoch).epoch
      (epochService.set _).expects(Marker, expectedEpoch).returningT(()).twice()
      task.execute(regenerateAll = false).success
      task.execute(regenerateAll = true).success
      Inspectors.forEvery(stored) { autostrategy =>
        val instances =
          instanceDao.get(ForScheduleIds(autostrategy.id))().success.value
        import autostrategy.props.{fromDate, maxApplicationsPerDay, toDate}
        val applyIntervalLength = Days.daysBetween(fromDate, toDate).getDays
        val days =
          List.iterate(fromDate, applyIntervalLength + 1)(_.plusDays(1))
        val allDaysInstances = instances.groupBy(_.fireTime.toLocalDate)
        Inspectors.forEvery(days) { day =>
          val dayInstances =
            allDaysInstances.getOrElse(day, Nil).groupBy(_.status)
          val pendingInstances = dayInstances.getOrElse(Pending, Nil)
          val cancelledInstances = dayInstances.getOrElse(Cancelled, Nil)
          if (day == now().toLocalDate)
            // instances for today may be not generated
            for (count <- maxApplicationsPerDay) {
              pendingInstances should ((have size count).or(have) size 0)
              cancelledInstances should ((have size count).or(have) size 0)
            }
          else
            maxApplicationsPerDay match {
              case Some(count) =>
                pendingInstances should have size count
                cancelledInstances should have size count
              case None =>
                pendingInstances.size shouldBe >(0)
                cancelledInstances.size shouldBe >(0)
            }
        }
      }
    }

    "ignore old autostrategies" in {
      val autostrategies = AutostrategyListGen.next.take(3)
      autostrategiesDao.put(autostrategies).success
      val stored = autostrategiesDao.all().success.value
      (epochService.getOptional _)
        .expects(Marker)
        .returningT(Some(stored.maxBy(_.epoch).epoch + 1))
      task.execute(regenerateAll = false).success
      instanceDao.get()().success.value shouldBe empty
    }
  }
}
