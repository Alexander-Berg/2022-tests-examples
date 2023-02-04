package ru.auto.salesman.tasks

import org.joda.time.{DateTime, DateTimeZone, LocalTime}
import ru.auto.salesman.model._
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Boost
import ru.auto.salesman.model.user.schedule.{
  IsVisible,
  ProductSchedule,
  ScheduleParameters
}
import ru.auto.salesman.service.DealerProductApplyService.NotAppliedReason.{
  PermanentError,
  TemporaryError
}
import ru.auto.salesman.service.DealerProductApplyService.ProductApplyResult.{
  Applied,
  NotApplied
}
import ru.auto.salesman.service.GoodsDecider.DeactivateReason.InactiveClient
import ru.auto.salesman.service.ScheduleInstanceService.Action.{Done, Fail, Reset}
import ru.auto.salesman.service.{
  DealerProductApplyService,
  EpochService,
  ScheduleInstanceService,
  ScheduleService
}
import ru.auto.salesman.tasks.schedule.ApplyScheduledInstancesTask
import ru.auto.salesman.test.model.gens.ScheduleInstanceGenerators._
import ru.auto.salesman.test.model.gens._
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.auto.salesman.util.{AutomatedContext, RequestContext}

import scala.concurrent.duration._

class ApplyScheduledDealerProductsTaskSpec extends BaseSpec {

  private val scheduleService = mock[ScheduleService[ProductSchedule]]
  private val applyService = mock[DealerProductApplyService]
  private val epochService = mock[EpochService]

  // we don't care about settings in this test
  private val settings =
    ApplyScheduledInstancesTask.Settings(1.second, 1.second)

  private val task =
    new ApplyScheduledDealerProductsTask(
      scheduleService,
      applyService,
      epochService,
      settings
    )

  private def mockAcquireInstance =
    toMockFunction2(
      scheduleService.acquire(_: ScheduleInstance)(_: RequestContext)
    )

  private def mockApplyProduct =
    toMockFunction5(
      applyService
        .applyProduct(
          _: AutoruOfferId,
          _: ProductId,
          _: AutoruDealer,
          _: Option[Funds]
        )(_: RequestContext)
    )

  private def mockProcessInstance =
    toMockFunction3(
      scheduleService.process(
        _: ScheduleInstance,
        _: ScheduleInstanceService.Action
      )(_: RequestContext)
    )

  implicit private val rc: RequestContext = AutomatedContext("test")

  private def testSchedule(
      scheduleId: Long,
      offerId: AutoruOfferId,
      customPrice: Option[Funds] = None
  ) =
    ProductSchedule(
      scheduleId,
      offerId,
      AutoruDealer(1),
      Boost,
      DateTime.now(),
      ScheduleParameters
        .OnceAtTime(Set(1), LocalTime.now(), DateTimeZone.UTC),
      isDeleted = false,
      DateTime.now(),
      IsVisible(true),
      expireDate = None,
      customPrice,
      allowMultipleReschedule = true,
      prevScheduleId = None
    )

  private val testProduct = ProductId.Fresh

  "ApplyScheduledDealerProductsTaskSpec" should {

    "apply proper instance" in {
      forAll(ScheduleInstanceGen, autoruOfferIdGen(), optFundsGen) {
        (instance, offerId, customPrice) =>
          val schedule = testSchedule(instance.scheduleId, offerId)
            .copy(customPrice = customPrice)
          mockAcquireInstance.expects(instance, rc).returningT(Some(schedule))
          mockApplyProduct
            .expects(offerId, testProduct, AutoruDealer(1), customPrice, rc)
            .returningT(Applied)
          mockProcessInstance.expects(instance, Done, rc).returningT(true)
          task.applyInstance(instance).success.value shouldBe (())
      }
    }

    "fail on permanent apply error" in {
      forAll(ScheduleInstanceGen, autoruOfferIdGen()) { (instance, offerId) =>
        val schedule = testSchedule(instance.scheduleId, offerId)
        mockAcquireInstance.expects(instance, rc).returningT(Some(schedule))
        mockApplyProduct
          .expects(offerId, testProduct, *, *, rc)
          .returningT(NotApplied(PermanentError(InactiveClient)))
        mockProcessInstance.expects(instance, Fail, rc).returningT(true)
        task.applyInstance(instance).success.value shouldBe (())
      }
    }

    "fail on user schedule" in {
      forAll(ScheduleInstanceGen, autoruOfferIdGen()) { (instance, offerId) =>
        val schedule =
          testSchedule(instance.scheduleId, offerId).copy(user = AutoruUser(1))
        mockAcquireInstance.expects(instance, rc).returningT(Some(schedule))
        mockProcessInstance.expects(instance, Fail, rc).returningT(true)
        task.applyInstance(instance).success.value shouldBe (())
      }
    }

    "reset on temporary apply error" in {
      forAll(ScheduleInstanceGen, autoruOfferIdGen()) { (instance, offerId) =>
        val schedule = testSchedule(instance.scheduleId, offerId)
        mockAcquireInstance.expects(instance, rc).returningT(Some(schedule))
        mockApplyProduct
          .expects(offerId, testProduct, *, *, rc)
          .returningT(NotApplied(TemporaryError("")))
        mockProcessInstance.expects(instance, Reset, rc).returningT(true)
        task.applyInstance(instance).success.value shouldBe (())
      }
    }

    "reset on unexpected error" in {
      forAll(ScheduleInstanceGen, autoruOfferIdGen()) { (instance, offerId) =>
        val schedule = testSchedule(instance.scheduleId, offerId)
        mockAcquireInstance.expects(instance, rc).returningT(Some(schedule))
        mockApplyProduct
          .expects(offerId, testProduct, *, *, rc)
          .throwingT(new TestException)
        mockProcessInstance.expects(instance, Reset, rc).returningT(true)
        task.applyInstance(instance).success.value shouldBe (())
      }
    }
  }
}
