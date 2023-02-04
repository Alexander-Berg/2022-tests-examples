package ru.auto.salesman.service.user

import org.mockito.Mockito
import org.scalacheck.Gen
import ru.auto.salesman.model.ScheduleInstance.Statuses
import ru.auto.salesman.model.user.schedule.ProductSchedule
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ScheduleInstance}
import ru.auto.salesman.service.ScheduleInstanceService.Action
import ru.auto.salesman.service.ScheduleService
import ru.auto.salesman.service.user.schedule.ScheduleEvaluator
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.model.gens.ScheduleInstanceGenerators
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering

import scala.collection.mutable
import scala.util.{Success, Try}

trait ProductScheduleServiceSpec
    extends DeprecatedMockitoBaseSpec
    with ProductScheduleModelGenerators
    with ScheduleInstanceGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 10)

  implicit private val rc: RequestContext = AutomatedContext(
    "ProductScheduleServiceSpec"
  )

  def newService(
      schedules: Iterable[ProductSchedule] = Iterable.empty,
      instances: Iterable[ScheduleInstance] = Iterable.empty
  ): ScheduleService[ProductSchedule]

  "ProductScheduleService" should {
    "get should fire in schedule instances" in {
      forAll(
        listUnique[ScheduleInstance, Long](1, 10, ScheduleInstanceGen)(_.id),
        dateTimeIntervalGen
      ) { (instances, interval) =>
        val service = newService(instances = instances)
        val expectedResult = instances.filter { instance =>
          instance.status == Statuses.Pending &&
          interval.contains(instance.fireTime)
        }
        val result = service.getShouldFireIn(interval).get
        result should contain theSameElementsAs expectedResult
      }
    }

    "fail to acquire" when {
      val pendingInstance = ScheduleInstanceGen.next
        .copy(status = Statuses.Pending)
      "instance has wrong status" in {
        val service = newService()
        val withWrongStatusGen: Gen[ScheduleInstance] =
          Gen
            .oneOf(Statuses.values.filterNot(_ == Statuses.Pending).toSeq)
            .map(v => pendingInstance.copy(status = v))
        forAll(withWrongStatusGen) { instance =>
          service.acquire(instance).get shouldBe empty
        }
      }
      "instance's schedule is not found or is in illegal state" in {
        val scheduleForInstance = ProductScheduleGen.next
          .copy(
            id = pendingInstance.scheduleId,
            updatedAt = pendingInstance.scheduleUpdateTime,
            isDeleted = false
          )

        val withOtherIdGen: Gen[ProductSchedule] =
          Gen
            .posNum[Long]
            .suchThat(_ != scheduleForInstance.id)
            .map(id => scheduleForInstance.copy(id = id))
        val deleted: Gen[ProductSchedule] =
          Gen.const(scheduleForInstance.copy(isDeleted = true))
        val withOtherUpdateTime: Gen[ProductSchedule] = dateTimeInPast()
          .suchThat(_ != scheduleForInstance.updatedAt)
          .map(updatedAt => scheduleForInstance.copy(updatedAt = updatedAt))

        forAll(Gen.oneOf(withOtherIdGen, withOtherUpdateTime, deleted)) { schedule =>
          val service = newService(
            schedules = Iterable(schedule),
            instances = Iterable(pendingInstance)
          )
          service.acquire(pendingInstance).get shouldBe empty
          service
            .getScheduleInstance(pendingInstance.id)
            .get
            .status shouldBe Statuses.Pending
        }
      }
      "instance was not updated" in {
        val scheduleForInstance = ProductScheduleGen.next
          .copy(
            id = pendingInstance.scheduleId,
            updatedAt = pendingInstance.scheduleUpdateTime,
            isDeleted = false
          )
        val service = newService(schedules = Iterable(scheduleForInstance))
        service.acquire(pendingInstance).get shouldBe empty
      }
    }

    "acquire pending schedule instance" in {
      val gen: Gen[(ProductSchedule, ScheduleInstance)] =
        ProductWithScheduleGen.map { case (schedule, instance) =>
          (schedule, instance.copy(status = Statuses.Pending))
        }
      forAll(gen) { case (schedule, instance) =>
        val service = newService(
          schedules = Iterable(schedule),
          instances = Iterable(instance)
        )
        service.acquire(instance).get should contain(schedule)
        service
          .getScheduleInstance(instance.id)
          .get
          .status shouldBe Statuses.Acquired
      }
    }

    "complete acquired schedule instance" in {
      forAll(ScheduleInstanceGen, bool) { (instance, success) =>
        val service = newService(instances = Iterable(instance))
        val result = service
          .process(instance, if (success) Action.Done else Action.Fail)
          .get
        val updatedInstance = service.getScheduleInstance(instance.id).get
        if (instance.status == Statuses.Acquired) {
          result shouldBe true
          val expectedStatus = if (success) Statuses.Done else Statuses.Failed
          updatedInstance.status shouldBe expectedStatus
        } else {
          result shouldBe false
          updatedInstance shouldBe instance
        }
      }
    }

    "reset acquired schedule instance" in {
      forAll(ScheduleInstanceGen) { instance =>
        val service = newService(instances = Iterable(instance))
        val result = service.process(instance, Action.Reset).get
        val updatedInstance = service.getScheduleInstance(instance.id).get
        if (instance.status == Statuses.Acquired) {
          result shouldBe true
          updatedInstance.status shouldBe Statuses.Pending
        } else {
          result shouldBe false
          updatedInstance shouldBe instance
        }
      }
    }

    "skip pending schedule instances" in {
      forAll(
        listUnique[ScheduleInstance, Long](0, 10, ScheduleInstanceGen)(_.id)
      ) { instances =>
        val service = newService(instances = instances)
        service.process(instances, Action.Skip).get

        instances.foreach { instance =>
          val updated = service.getScheduleInstance(instance.id).get
          if (instance.status == Statuses.Pending)
            updated.status shouldBe Statuses.Skipped
          else
            updated shouldBe instance
        }
      }
    }

    "evaluate deleted schedule" in {
      forAll(
        ProductScheduleGen.suchThat(_.isDeleted),
        listUnique[ScheduleInstance, Long](5, 25, ScheduleInstanceGen)(_.id)
      ) { (schedule, instancesSrc) =>
        val instances = instancesSrc.map(_.copy(scheduleId = schedule.id))

        val service = newService(Iterable(schedule), instances)
        service.evaluate(schedule, mock[ScheduleEvaluator]).get shouldBe empty
        val (pending, other) = instances.partition(_.status == Statuses.Pending)
        pending.foreach { instance =>
          service
            .getScheduleInstance(instance.id)
            .get
            .status shouldBe Statuses.Cancelled
        }
        other.foreach { instance =>
          service.getScheduleInstance(instance.id).get shouldBe instance
        }
      }
    }

    "evaluate non-deleted schedule" in {
      forAll(
        ProductScheduleGen,
        listUnique[ScheduleInstance, Long](5, 25, ScheduleInstanceGen)(_.id)
      ) { (scheduleSrc, instancesSrc) =>
        val now = DateTimeUtil.now()
        val updateTimes @ (latest :: _) =
          now.minusDays(1) :: now.minusDays(2) :: Nil

        val schedule = scheduleSrc.copy(updatedAt = latest, isDeleted = false)
        val instances = instancesSrc.map(
          _.copy(
            scheduleId = schedule.id,
            scheduleUpdateTime = Gen.oneOf(updateTimes).next
          )
        )

        val nextFireTime = now.plusYears(1)
        val evaluator = {
          val m = mock[ScheduleEvaluator]
          when(m.evaluate(?, ?)).thenReturn(Success(Seq(nextFireTime)))
          m
        }

        val service = newService(Iterable(schedule), instances)

        val afterEvaluation = service.evaluate(schedule, evaluator).get
        val actual =
          instances.filter(_.scheduleUpdateTime == schedule.updatedAt)
        actual.foreach { instance =>
          service.getScheduleInstance(instance.id).get shouldBe instance
        }
        val actualPending = actual.filter(_.status == Statuses.Pending)
        val active = actual.filter(instance =>
          ScheduleInstance.ActiveStatuses.contains(instance.status)
        )
        val lastActive = Try(active.maxBy(_.fireTime)).toOption
        Mockito
          .verify(evaluator)
          .evaluate(schedule, ScheduleEvaluator.Context(lastActive))
        afterEvaluation should (have size (actualPending.size + 1) and contain allElementsOf actualPending)
        afterEvaluation.map(_.fireTime) should contain(nextFireTime)
      }
    }

    "scan schedules" in {
      forAll(list(5, 20, ProductScheduleGen), bool, Gen.chooseNum(1, 5)) {
        (schedules, skipDeleted, sinceShift) =>
          val service = newService(schedules)
          val since = DateTimeUtil.now().minusDays(sinceShift).getMillis
          val acc = mutable.ArrayBuffer.empty[ProductSchedule]
          val expected = schedules.filter { schedule =>
            !(skipDeleted && schedule.isDeleted) && schedule.epoch.getMillis > since
          }

          service.scanSchedules(since, skipDeleted)(s => Success(acc += s)).get
          acc should contain theSameElementsAs expected
      }
    }
  }
}
