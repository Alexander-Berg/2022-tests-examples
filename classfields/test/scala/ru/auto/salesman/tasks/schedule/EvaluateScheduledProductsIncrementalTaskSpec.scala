package ru.auto.salesman.tasks.schedule

import org.mockito.Mockito.{reset, times, verify}
import org.mockito.ArgumentCaptor
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import ru.auto.salesman.model.user.schedule.ProductSchedule
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Epoch}
import ru.auto.salesman.service.user.schedule.ScheduleEvaluator
import ru.auto.salesman.service.{EpochService, ScheduleService}
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.test.DeprecatedMockitoBaseSpec
import ru.auto.salesman.util.RequestContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.util.{Failure, Random, Success, Try}

class EvaluateScheduledProductsIncrementalTaskSpec
    extends DeprecatedMockitoBaseSpec
    with BeforeAndAfterEach
    with ProductScheduleModelGenerators
    with OfferModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(service, evaluator, epochService)
  }

  val service = mock[ScheduleService[ProductSchedule]]
  val evaluator = mock[ScheduleEvaluator]
  val epochService = mock[EpochService]

  val task = new EvaluateScheduledProductsIncrementalTask(
    service,
    epochService,
    evaluator
  )

  def stubScanSchedules(
      epoch: Epoch,
      schedules: Iterable[ProductSchedule]
  ): Unit =
    stub(
      service.scanSchedules(_: Epoch, _: Boolean)(
        _: ProductSchedule => Try[Unit]
      )(_: RequestContext)
    ) { case (`epoch`, false, callback, _) =>
      schedules.foreach(callback)
      Success(())
    }

  "EvaluateScheduledProductsIncrementalTask" should {
    "evaluate all schedules changed since" in {
      forAll(Gen.posNum[Long], list(1, 10, ProductScheduleGen)) { (epoch, schedules) =>
        reset(service, epochService)
        when(
          epochService.getOptional(
            Markers.EvaluateChangedProductSchedulesEpoch
          )
        )
          .thenReturn(Success(Some(epoch)))
        when(epochService.set(?, ?)).thenReturn(Success(()))
        stubScanSchedules(epoch, schedules)
        when(service.evaluate(?, ?)(?)).thenReturn(Success(Iterable.empty))
        task.execute().get

        val scheduleCaptor: ArgumentCaptor[ProductSchedule] =
          ArgumentCaptor.forClass(classOf[ProductSchedule])
        verify(service, times(schedules.size))
          .evaluate(scheduleCaptor.capture(), MockitoSupport.eq(evaluator))(?)
        scheduleCaptor.getAllValues.asScala should contain theSameElementsAs schedules
        verify(epochService).set(?, ?)
      }
    }

    "not fail if single evaluation fails" in {
      forAll(Gen.posNum[Long], list(1, 10, ProductScheduleGen)) { (epoch, schedules) =>
        reset(service, epochService)
        when(
          epochService.getOptional(
            Markers.EvaluateChangedProductSchedulesEpoch
          )
        )
          .thenReturn(Success(Some(epoch)))
        when(epochService.set(?, ?)).thenReturn(Success(()))
        stubScanSchedules(epoch, schedules)
        stub(
          service.evaluate(_: ProductSchedule, _: ScheduleEvaluator)(
            _: RequestContext
          )
        ) { case _ =>
          if (Random.nextDouble() < 0.5) Success(Iterable.empty)
          else Failure(new RuntimeException("artificial"))
        }
        task.execute().get

        val scheduleCaptor: ArgumentCaptor[ProductSchedule] =
          ArgumentCaptor.forClass(classOf[ProductSchedule])
        verify(service, times(schedules.size))
          .evaluate(scheduleCaptor.capture(), MockitoSupport.eq(evaluator))(?)
        scheduleCaptor.getAllValues.asScala should contain theSameElementsAs schedules
        verify(epochService).set(?, ?)
      }
    }

    "fail if scan fails" in {
      when(
        epochService.getOptional(Markers.EvaluateChangedProductSchedulesEpoch)
      )
        .thenReturn(Success(Some(0L)))
      when(service.scanSchedules(?, ?)(?)(?))
        .thenReturn(Failure(new RuntimeException("artificial")))
      task.execute().isFailure shouldBe true
    }

    "use zero if no epoch provided" in {
      when(
        epochService.getOptional(Markers.EvaluateChangedProductSchedulesEpoch)
      )
        .thenReturn(Success(None))
      when(epochService.set(?, ?)).thenReturn(Success(()))
      when(service.scanSchedules(?, ?)(?)(?)).thenReturn(Success(()))
      when(service.evaluate(?, ?)(?)).thenReturn(Success(Iterable.empty))
      task.execute().get
      val epochCaptor: ArgumentCaptor[Epoch] =
        ArgumentCaptor.forClass(classOf[Epoch])

      verify(service).scanSchedules(epochCaptor.capture(), ?)(?)(?)
      epochCaptor.getValue shouldBe 0L
      verify(epochService).set(?, ?)
    }

    "fail if can't set epoch" in {
      when(
        epochService.getOptional(Markers.EvaluateChangedProductSchedulesEpoch)
      )
        .thenReturn(Success(Some(0L)))
      when(epochService.set(?, ?))
        .thenReturn(Failure(new RuntimeException("artificial")))
      when(service.scanSchedules(?, ?)(?)(?)).thenReturn(Success(()))
      when(service.evaluate(?, ?)(?)).thenReturn(Success(Iterable.empty))
      task.execute().isFailure shouldBe true
      verify(epochService).set(?, ?)
    }
  }
}
