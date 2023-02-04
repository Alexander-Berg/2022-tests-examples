package ru.auto.salesman.tasks.schedule

import org.mockito.Mockito.{reset, times, verify}
import org.scalatest.BeforeAndAfterEach
import ru.auto.salesman.model.user.schedule.ProductSchedule
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, Epoch}
import ru.auto.salesman.service.ScheduleService
import ru.auto.salesman.service.user.schedule.ScheduleEvaluator
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.test.{ArgCaptor, DeprecatedMockitoBaseSpec}
import ru.auto.salesman.util.RequestContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.collection.JavaConverters.asScalaBufferConverter
import scala.util.{Failure, Random, Success, Try}

class EvaluateScheduledProductsAllTaskSpec
    extends DeprecatedMockitoBaseSpec
    with BeforeAndAfterEach
    with ProductScheduleModelGenerators
    with OfferModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val service = mock[ScheduleService[ProductSchedule]]
  val evaluator = mock[ScheduleEvaluator]
  val task = new EvaluateScheduledProductsAllTask(service, evaluator)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(service, evaluator)
  }

  def stubScanSchedules(schedules: Iterable[ProductSchedule]): Unit =
    stub(
      service.scanSchedules(_: Epoch, _: Boolean)(
        _: ProductSchedule => Try[Unit]
      )(_: RequestContext)
    ) { case (0L, true, callback, _) =>
      schedules.foreach(callback)
      Success(())
    }

  "EvaluateScheduledProductsAllTask" should {
    "evaluate all non-deleted schedules" in {
      forAll(list(1, 10, ProductScheduleGen)) { schedules =>
        reset(service)
        stubScanSchedules(schedules)
        when(service.evaluate(?, ?)(?)).thenReturn(Success(Iterable.empty))
        task.execute().get

        val scheduleCaptor = ArgCaptor[ProductSchedule]
        verify(service, times(schedules.size))
          .evaluate(scheduleCaptor.capture(), MockitoSupport.eq(evaluator))(?)
        scheduleCaptor.getAllValues.asScala should contain theSameElementsAs schedules
      }
    }

    "not fail if single evaluation fails" in {
      forAll(list(1, 10, ProductScheduleGen)) { schedules =>
        reset(service)
        stubScanSchedules(schedules)
        stub(
          service.evaluate(_: ProductSchedule, _: ScheduleEvaluator)(
            _: RequestContext
          )
        ) { case _ =>
          if (Random.nextDouble() < 0.5) Success(Iterable.empty)
          else Failure(new RuntimeException("artificial"))
        }
        task.execute().get

        val scheduleCaptor = ArgCaptor[ProductSchedule]
        verify(service, times(schedules.size))
          .evaluate(scheduleCaptor.capture(), MockitoSupport.eq(evaluator))(?)
        scheduleCaptor.getAllValues.asScala should contain theSameElementsAs schedules
      }
    }

    "fail if scan fails" in {
      when(service.scanSchedules(?, ?)(?)(?))
        .thenReturn(Failure(new RuntimeException("artificial")))
      task.execute().isFailure shouldBe true
    }
  }

}
