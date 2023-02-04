package ru.auto.salesman.tasks.schedule

import org.joda.time.{DateTime, DateTimeZone}
import ru.auto.salesman.dao.ScheduleInstanceDao
import ru.auto.salesman.dao.ScheduleInstanceDao.{InstanceLimit, InstanceOrder}
import ru.auto.salesman.dao.user.ProductScheduleDao
import ru.auto.salesman.model.user.product.ProductProvider.AutoruBundles.Vip
import ru.auto.salesman.model.user.schedule.ProductSchedule
import ru.auto.salesman.model.user.schedule.ScheduleParameters.OnceAtTime
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ScheduleInstance}
import ru.auto.salesman.service.ScheduleInstanceService
import ru.auto.salesman.service.impl.user.ProductScheduleServiceImpl
import ru.auto.salesman.service.user.schedule.SimpleEvaluator
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.test.model.gens.{OfferModelGenerators, ScheduleInstanceGenerators}
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

import scala.concurrent.duration._

class ProductScheduleServiceWithNotAllowedToRescheduleSpec
    extends BaseSpec
    with ProductScheduleModelGenerators
    with ScheduleInstanceGenerators
    with OfferModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
  implicit private val rc: RequestContext = AutomatedContext("test")

  private val scheduleDao = mock[ProductScheduleDao]
  private val instanceDao = mock[ScheduleInstanceDao]
  private val service = new ProductScheduleServiceImpl(scheduleDao, instanceDao)
  private val evaluator = new SimpleEvaluator(12.hours)

  private val baseCreateTime = DateTime.now.withTimeAtStartOfDay()
  private val baseUpdateTime = baseCreateTime.plusSeconds(1)

  "ProductScheduleServiceImpl" should {
    "not reschedule task if allowMultipleReschedule is false and task was run today" in {
      for (shift: Int <- 0 to 23) {
        val schedule = randomProductSchedule(baseUpdateTime, shift)
        val instances =
          todayExecutedScheduleInstancesList(schedule, baseCreateTime, shift)

        inSequence {
          mockInstanceDaoGet.expects(*, *, *).returningT(instances)

          val today = baseCreateTime
            .withZone(schedule.scheduleParameters.timezone)
            .toLocalDate
          (instanceDao.insert _)
            .expects {
              argAssert { sources: Seq[ScheduleInstanceService.Source] =>
                val hasTodayInstancesInserted =
                  sources.exists(
                    today == _.fireTime
                      .withZone(schedule.scheduleParameters.timezone)
                      .toLocalDate
                  )
                hasTodayInstancesInserted shouldBe false
              }
            }
            .returningT(Seq())
            // if (current time - 12 hours) > (latest instance fire time) then insert() is called once, else - never
            .noMoreThanOnce()

          mockInstanceDaoGet.expects(*, *, *).returningT(List())
        }

        service.evaluate(schedule, evaluator).success
      }
    }

    "reschedule task if allowMultipleReschedule is false and task wasn't run today" in {
      for (shift: Int <- 0 to 23) {
        val schedule = randomProductSchedule(baseUpdateTime, shift)
        val instances = todayNotExecutedScheduleInstancesList

        inSequence {
          mockInstanceDaoGet.expects(*, *, *).returningT(instances)

          (instanceDao.insert _)
            .expects(*)
            .returningT(Seq())
            .noMoreThanOnce()

          mockInstanceDaoGet.expects(*, *, *).returningT(List())
        }

        service.evaluate(schedule, evaluator).success
      }
    }

    "not reschedule task if current schedule doesn't have active instances, but parent schedule has" in {
      for (shift: Int <- 0 to 23) {
        val parentSchedule = randomProductSchedule(baseUpdateTime, shift)
        val parentScheduleInstances = todayExecutedScheduleInstancesList(
          parentSchedule,
          baseCreateTime,
          shift
        )
        val schedule =
          randomProductSchedule(baseUpdateTime, shift, Some(parentSchedule.id))

        inSequence {
          // First call to instanceDao.get() is on schedule, which doesn't have any instances...
          mockInstanceDaoGet.expects(*, *, *).returningT(Seq())

          (scheduleDao.getById _)
            .expects(parentSchedule.id)
            .returningT(Some(parentSchedule))

          // ... second call to instanceDao.get() is on parentSchedule and is's has some instances
          mockInstanceDaoGet
            .expects(*, *, *)
            .returningT(parentScheduleInstances)

          val today = baseCreateTime
            .withZone(schedule.scheduleParameters.timezone)
            .toLocalDate
          (instanceDao.insert _)
            .expects {
              argAssert { sources: Seq[ScheduleInstanceService.Source] =>
                val hasTodayInstancesInserted =
                  sources.exists(
                    today == _.fireTime
                      .withZone(schedule.scheduleParameters.timezone)
                      .toLocalDate
                  )
                hasTodayInstancesInserted shouldBe false
              }
            }
            .returningT(Seq())
            .noMoreThanOnce()

          mockInstanceDaoGet.expects(*, *, *).returningT(List())
        }

        service.evaluate(schedule, evaluator).success
      }
    }

    "should throw exception" in {
      val schedule = randomProductSchedule(baseUpdateTime, 0)

      inSequence {
        mockInstanceDaoGet
          .expects(*, *, *)
          .throwingT(
            new RuntimeException(
              "Database is not available or smth else got wrong"
            )
          )
      }

      service.evaluate(schedule, evaluator).failed
    }
  }

  private def mockInstanceDaoGet =
    toMockFunction3(
      instanceDao.get(_: ScheduleInstanceDao.InstanceFilter)(
        _: InstanceOrder,
        _: InstanceLimit
      )
    )

  private def randomProductSchedule(
      updateTime: DateTime,
      shift: Int,
      prevScheduleId: Option[Long] = None
  ) = {
    val params = OnceAtTime(
      Set(1, 2, 3, 4, 5, 6, 7),
      updateTime.plusHours(shift).toLocalTime,
      DateTimeZone.forID("Europe/Moscow")
    )

    val schedule = {
      val randomSchedule = productScheduleGen(
        isDeletedGen = false,
        parametersGen = params,
        expireDateGen = Some(DateTime.now.plusDays(60)),
        allowMultipleRescheduleGen = false
      ).next

      randomSchedule.copy(
        updatedAt = updateTime.plusHours(shift),
        product = Vip,
        epoch = updateTime.plusHours(shift),
        prevScheduleId = prevScheduleId
      )
    }

    schedule
  }

  // One schedule instance have already run today
  private def todayExecutedScheduleInstancesList(
      schedule: ProductSchedule,
      createTime: DateTime,
      shift: Int
  ) =
    List(
      ScheduleInstance(
        posNum[Long].next,
        schedule.id,
        schedule.updatedAt.withZone(schedule.scheduleParameters.timezone),
        createTime.plusHours(shift),
        schedule.updatedAt.withZone(schedule.scheduleParameters.timezone),
        ScheduleInstance.Statuses.Done,
        createTime.plusHours(shift)
      )
    )

  private def todayNotExecutedScheduleInstancesList = List()
}
