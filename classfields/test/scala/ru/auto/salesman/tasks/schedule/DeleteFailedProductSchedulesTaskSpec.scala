package ru.auto.salesman.tasks.schedule

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.{never, reset, times, verify}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import ru.auto.salesman.model.ScheduleInstance.{Status, Statuses}
import ru.auto.salesman.model.user.schedule.ProductSchedule
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  Epoch,
  ScheduleInstance
}
import ru.auto.salesman.service.ScheduleService
import ru.auto.salesman.tasks.schedule.DeleteFailedProductSchedulesTask.InstancesLimit
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.test.model.gens.ScheduleInstanceGenerators
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.util.{DateTimeInterval, RequestContext}

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.util.{Success, Try}

class DeleteFailedProductSchedulesTaskSpec
    extends DeprecatedMockitoBaseSpec
    with BeforeAndAfterEach
    with ProductScheduleModelGenerators
    with ScheduleInstanceGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val service = mock[ScheduleService[ProductSchedule]]
  val task = new DeleteFailedProductSchedulesTask(service)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(service)
  }

  private def stubServiceScan(schedules: Iterable[ProductSchedule]): Unit =
    stub(
      service.scanSchedules(_: Epoch, _: Boolean)(
        _: ProductSchedule => Try[Unit]
      )(_: RequestContext)
    ) { case (0L, true, callback, _) =>
      schedules.foreach(callback)
      Success(())
    }

  def instancesGen(
      schedule: ProductSchedule,
      statusGen: Gen[Status],
      n: Int = 10
  ): List[ScheduleInstance] =
    ScheduleInstanceGen
      .next(n)
      .map(
        _.copy(
          scheduleId = schedule.id,
          scheduleUpdateTime = schedule.updatedAt,
          status = statusGen.next
        )
      )
      .toList

  "DeleteFailedProductSchedulesTask" should {
    "scan all schedules" in {
      forAll(list(1, 10, ProductScheduleGen)) { schedules =>
        reset(service)
        stubServiceScan(schedules)
        when(service.getLastInstances(?, ?, ?)(?))
          .thenReturn(Success(Iterable.empty))
        when(service.deleteSchedule(?)(?)).thenReturn(Success(()))

        task.execute().get

        val scheduleCaptor: ArgumentCaptor[Option[ProductSchedule]] =
          ArgumentCaptor.forClass(classOf[Option[ProductSchedule]])
        val optIntCaptor: ArgumentCaptor[Option[Int]] =
          ArgumentCaptor.forClass(classOf[Option[Int]])
        val optIntervalCaptor: ArgumentCaptor[Option[DateTimeInterval]] =
          ArgumentCaptor.forClass(classOf[Option[DateTimeInterval]])
        verify(service, times(schedules.size)).getLastInstances(
          scheduleCaptor.capture(),
          optIntCaptor.capture(),
          optIntervalCaptor.capture()
        )(?)
        val scheduleValues = scheduleCaptor.getAllValues.asScala
        scheduleValues.forall(_.isDefined) shouldBe true
        scheduleValues.flatten should contain theSameElementsAs schedules
        val limitValues = optIntCaptor.getAllValues.asScala
        limitValues.forall(_.isDefined) shouldBe true
        limitValues.flatten.toSet should (have size 1 and contain(
          InstancesLimit
        ))

        val intervalValues = optIntervalCaptor.getAllValues.asScala
        intervalValues.forall(_.isDefined) shouldBe true
        intervalValues.flatten.toSet should have size 1
      }
    }

    "filter outdated failed instances" in {
      val schedule = ProductScheduleGen.next
      val instances = ScheduleInstanceGen
        .filter(_.scheduleUpdateTime != schedule.updatedAt)
        .next(10)
        .map(
          _.copy(scheduleId = schedule.id, status = Statuses.Failed)
        )
      stubServiceScan(Iterable(schedule))
      when(service.getLastInstances(?, ?, ?)(?)).thenReturn(Success(instances))
      task.execute().get
      verify(service, never()).deleteSchedule(?)(?)
    }

    "not delete if last completed is not failed" in {
      val schedule = ProductScheduleGen.next
      val nonCompleted = instancesGen(schedule, NonCompletedStatus)
      val done = instancesGen(schedule, DoneStatus, 1)
      val failed = instancesGen(schedule, FailedStatus)
      stubServiceScan(Iterable(schedule))
      when(service.getLastInstances(?, ?, ?)(?))
        .thenReturn(Success(nonCompleted ++ done ++ failed))

      task.execute().get
      verify(service, never()).deleteSchedule(?)(?)
    }

    "not delete if failed sequence is less that threshold" in {
      val schedule = ProductScheduleGen.next
      val failed = instancesGen(schedule, FailedStatus, 2)
      stubServiceScan(Iterable(schedule))
      when(service.getLastInstances(?, ?, ?)(?)).thenReturn(Success(failed))

      task.execute().get
      verify(service, never()).deleteSchedule(?)(?)
    }
  }
}
