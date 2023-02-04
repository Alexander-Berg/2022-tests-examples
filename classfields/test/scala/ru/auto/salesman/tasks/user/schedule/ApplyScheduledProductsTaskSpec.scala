package ru.auto.salesman.tasks.user.schedule

import org.joda.time.DateTime
import org.scalacheck.Gen
import ru.auto.salesman.dao.user.GoodsDao
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.model.user.product.BindedOfferProduct
import ru.auto.salesman.model.user.product.ProductSource.AutoApply
import ru.auto.salesman.model.user.product.Products.OfferProduct
import ru.auto.salesman.model.user.schedule.ProductSchedule
import ru.auto.salesman.model.user.{ProductPrice, ProductType}
import ru.auto.salesman.model.{
  AutoruUser,
  DeprecatedDomain,
  DeprecatedDomains,
  Epoch,
  Funds,
  ScheduleInstance,
  TransactionId
}
import ru.auto.salesman.service.ProductApplyService.Request
import ru.auto.salesman.service.ProductApplyService.Response.NotAvailableLinkedCardsAndFunds
import ru.auto.salesman.service.ScheduleInstanceService.Action
import ru.auto.salesman.service.impl.user.PriceForScheduledProductCalculator
import ru.auto.salesman.service.user.{GoodsService, UserFeatureService}
import ru.auto.salesman.service.{
  EpochService,
  ProductApplyService,
  ScheduleInstanceService,
  ScheduleService
}
import ru.auto.salesman.tasks.Markers
import ru.auto.salesman.tasks.schedule.ApplyScheduledInstancesTask
import ru.auto.salesman.tasks.user.schedule.ApplyScheduledProductsTaskSpec.asRequest
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.ScheduleInstanceGenerators
import ru.auto.salesman.test.model.gens.user.ProductScheduleModelGenerators
import ru.auto.salesman.util.{AutomatedContext, DateTimeInterval, RequestContext}
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Success

class ApplyScheduledProductsTaskSpec
    extends BaseSpec
    with ProductScheduleModelGenerators
    with ScheduleInstanceGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 10)

  val epochService: EpochService = mock[EpochService]

  val scheduledProductPriceCalculator: PriceForScheduledProductCalculator =
    mock[PriceForScheduledProductCalculator]
  val productScheduleService = mock[ScheduleService[ProductSchedule]]
  val productApplyService: ProductApplyService = mock[ProductApplyService]
  val goodsService = mock[GoodsService]
  val featureService = mock[UserFeatureService]

  implicit val rc: RequestContext = AutomatedContext("unit-test")

  def newTask(
      forwardWindow: FiniteDuration = 10.minutes,
      backWindowLimit: FiniteDuration = 1.hour
  ): ApplyScheduledProductsTask =
    new ApplyScheduledProductsTask(
      productScheduleService,
      scheduledProductPriceCalculator,
      productApplyService,
      goodsService,
      featureService,
      epochService,
      ApplyScheduledInstancesTask.Settings(
        forwardWindow,
        backWindowLimit
      )
    ) {
      override def rcForExecution(startTime: DateTime): RequestContext = rc
    }

  private def whenProductScheduleServiceProcessBatch =
    toMockFunction3(
      productScheduleService
        .process(
          _: Iterable[ScheduleInstance],
          _: ScheduleInstanceService.Action
        )(_: RequestContext)
    )

  private def whenProductScheduleServiceProcessOne =
    toMockFunction3(
      productScheduleService
        .process(_: ScheduleInstance, _: ScheduleInstanceService.Action)(
          _: RequestContext
        )
    )

  private def whenProductScheduleServiceAcquire =
    toMockFunction2(
      productScheduleService.acquire(_: ScheduleInstance)(_: RequestContext)
    )

  private def whenProductApplyServiceApplyProduct =
    toMockFunction1(
      productApplyService.applyProduct(_: Request)
    )

  "ApplyScheduledProductsTask" should {
    "build correct interval" in {
      val now = DateTimeUtil.now()
      val epochGen: Gen[Epoch] =
        Gen.chooseNum(now.getMillis - 20.minutes.toMillis, now.getMillis)
      val windowGen: Gen[FiniteDuration] = Gen.chooseNum(1, 5).map(_.minutes)
      val lagGen: Gen[FiniteDuration] = Gen.chooseNum(5, 15).map(_.minutes)
      forAll(epochGen, windowGen, lagGen) { (epoch, window, lag) =>
        (epochService.getOptional _)
          .expects(Markers.ApplyProductScheduleInstancesEpoch)
          .returningT(Some(epoch))
        val expectedFrom = math.max(epoch, now.getMillis - lag.toMillis)
        val expectedTo = now.getMillis + window.toMillis
        val interval = newTask(window, lag).buildInterval(now).get
        interval.from.getMillis shouldBe expectedFrom
        interval.to.getMillis shouldBe expectedTo
      }
    }
    "find nearest instance and skip older" in {
      val now = DateTimeUtil.now()
      val updateTimes @ (latest :: _) =
        now.minusDays(1) :: now.minusDays(2) :: Nil
      val instanceGen: Gen[ScheduleInstance] = for {
        instance <- ScheduleInstanceGen
        updateTime <- Gen.oneOf(updateTimes)
        fireTime <- Gen.chooseNum(-5, 5).map(now.plusMinutes)
      } yield instance.copy(scheduleUpdateTime = updateTime, fireTime = fireTime)

      forAll(Gen.listOfN(10, instanceGen)) { instances =>
        whenever(instances.exists(_.scheduleUpdateTime == latest)) {
          val withLatestUpdateTime =
            instances.filter(_.scheduleUpdateTime == latest).sortBy(_.fireTime)
          val firstRightIdx =
            withLatestUpdateTime.indexWhere(_.fireTime.isAfter(now))
          val expectedIdx =
            if (firstRightIdx == 0)
              0
            else if (firstRightIdx == -1)
              withLatestUpdateTime.length - 1
            else {
              val leftDiff = now.getMillis - withLatestUpdateTime(
                firstRightIdx - 1
              ).fireTime.getMillis
              val rightDiff = withLatestUpdateTime(
                firstRightIdx
              ).fireTime.getMillis - now.getMillis
              if (leftDiff <= rightDiff) firstRightIdx - 1 else firstRightIdx
            }
          val expected = withLatestUpdateTime(expectedIdx)
          val expectedSkipped = withLatestUpdateTime.take(expectedIdx)

          if (expectedSkipped.nonEmpty)
            whenProductScheduleServiceProcessBatch
              .expects(expectedSkipped, Action.Skip, rc)
              .returningT(0)

          val result = newTask().findNearest(instances, now)

          result should contain(expected)
        }
      }
    }

    "do nothing if can't acquire instance" in {
      forAll(ScheduleInstanceGen) { instance =>
        whenProductScheduleServiceAcquire
          .expects(instance, rc)
          .returningT(None)
        newTask().applyInstance(instance) shouldBe Success(())
      }
    }

    "reset instance if product apply service failed" in {
      val ex = new RuntimeException
      forAll(ProductScheduleGen, ScheduleInstanceGen, ProductPriceGen) {
        (schedule, instance, productPrice) =>
          inSequence {
            whenProductScheduleServiceAcquire
              .expects(instance, rc)
              .returningT(Some(schedule))
            (scheduledProductPriceCalculator
              .calculate(
                _: OfferProduct,
                _: Option[Funds],
                _: AutoruUser,
                _: AutoruOfferId
              )(_: RequestContext))
              .expects(*, *, *, *, *)
              .returningT(productPrice)
            (featureService.useTrustForScheduledPayments _)
              .expects()
              .returning(false)
            whenProductApplyServiceApplyProduct
              .expects(asRequest(schedule, instance, productPrice))
              .throwingZ(ex)
            whenProductScheduleServiceProcessOne
              .expects(instance, Action.Reset, rc)
              .returningT(true)
          }
          newTask().applyInstance(instance).failure.exception shouldBe ex
      }
    }

    "fail instance if product apply service respond with NotApplied" in {
      forAll(ProductScheduleGen, ScheduleInstanceGen, ProductPriceGen) {
        (schedule, instance, productPrice) =>
          inSequence {
            whenProductScheduleServiceAcquire
              .expects(instance, rc)
              .returningT(Some(schedule))
            (scheduledProductPriceCalculator
              .calculate(
                _: OfferProduct,
                _: Option[Funds],
                _: AutoruUser,
                _: AutoruOfferId
              )(_: RequestContext))
              .expects(*, *, *, *, *)
              .returningT(productPrice)
            (featureService.useTrustForScheduledPayments _)
              .expects()
              .returning(false)
            whenProductApplyServiceApplyProduct
              .expects(asRequest(schedule, instance, productPrice))
              .returningZ(NotAvailableLinkedCardsAndFunds)
            whenProductScheduleServiceProcessOne
              .expects(instance, Action.Fail, rc)
              .returningT(true)
          }
          newTask().applyInstance(instance) shouldBe Success(())
      }
    }

    "execute" in {
      val schedules = ProductScheduleGen.next(5).toSeq
      val schedulesMap = schedules.map(s => s.id -> s).toMap
      val instances = Gen
        .listOfN(
          50,
          for {
            instance <- ScheduleInstanceGen
            schedule <- Gen.oneOf(schedules)
          } yield
            instance.copy(
              scheduleId = schedule.id,
              scheduleUpdateTime = schedule.updatedAt
            )
        )
        .next
      val expectedToApply = instances
        .groupBy(_.scheduleId)
        .values
        .map(_.minBy(_.fireTime)(DateTimeOrdering))
      val productPrice = ProductPriceGen.next

      (epochService.getOptional _).expects(*).returningT(None)
      inSequence {
        (productScheduleService
          .getShouldFireIn(_: DateTimeInterval)(_: RequestContext))
          .expects(*, rc)
          .returningT(instances)
        expectedToApply.foreach { instance =>
          val schedule = schedulesMap(instance.scheduleId)
          whenProductScheduleServiceAcquire
            .expects(instance, *)
            .returningT(Some(schedule))
          (scheduledProductPriceCalculator
            .calculate(
              _: OfferProduct,
              _: Option[Funds],
              _: AutoruUser,
              _: AutoruOfferId
            )(_: RequestContext))
            .expects(*, *, *, *, *)
            .returningT(productPrice)
          (featureService.useTrustForScheduledPayments _)
            .expects()
            .returning(false)
          whenProductApplyServiceApplyProduct
            .expects(asRequest(schedule, instance, productPrice))
            .returningZ(ProductApplyService.Response.Applied)
          whenProductScheduleServiceProcessOne
            .expects(instance, Action.Done, rc)
            .returningT(true)
        }
        (epochService.set _)
          .expects(Markers.ApplyProductScheduleInstancesEpoch, *)
          .returningT(())
      }
      newTask().execute() shouldBe Success(())
    }

    "pass parentTransactionId from last transaction when feature enabled" in {
      val schedules = ProductScheduleGen
        .suchThat(_.product.productType == ProductType.Bundle)
        .next(5)
        .toSeq
      val schedulesMap = schedules.map(s => s.id -> s).toMap
      val instances = Gen
        .listOfN(
          50,
          for {
            instance <- ScheduleInstanceGen
            schedule <- Gen.oneOf(schedules)
          } yield
            instance.copy(
              scheduleId = schedule.id,
              scheduleUpdateTime = schedule.updatedAt
            )
        )
        .next
      val expectedToApply = instances
        .groupBy(_.scheduleId)
        .values
        .map(_.minBy(_.fireTime)(DateTimeOrdering))
      val productPrice = ProductPriceGen.next

      (epochService.getOptional _).expects(*).returningT(None)
      inSequence {
        (productScheduleService
          .getShouldFireIn(_: DateTimeInterval)(_: RequestContext))
          .expects(*, rc)
          .returningT(instances)
        expectedToApply.foreach { instance =>
          val schedule = schedulesMap(instance.scheduleId)
          whenProductScheduleServiceAcquire
            .expects(instance, *)
            .returningT(Some(schedule))
          (scheduledProductPriceCalculator
            .calculate(
              _: OfferProduct,
              _: Option[Funds],
              _: AutoruUser,
              _: AutoruOfferId
            )(_: RequestContext))
            .expects(*, *, *, *, *)
            .returningT(productPrice)
          (featureService.useTrustForScheduledPayments _)
            .expects()
            .returning(true)

          val goods = goodsGen().next(2).toSeq

          (goodsService
            .get(_: GoodsDao.Filter.ForProductUserOffer))
            .expects(
              GoodsDao.Filter.ForProductUserOffer(
                schedule.product,
                schedule.user.asPrivate.toString,
                schedule.offerId
              )
            )
            .returningZ(goods)
          whenProductApplyServiceApplyProduct
            .expects(
              asRequest(
                schedule,
                instance,
                productPrice,
                Some(goods.head.transactionId)
              )
            )
            .returningZ(ProductApplyService.Response.Applied)
          whenProductScheduleServiceProcessOne
            .expects(instance, Action.Done, rc)
            .returningT(true)
        }
        (epochService.set _)
          .expects(Markers.ApplyProductScheduleInstancesEpoch, *)
          .returningT(())
      }
      newTask().execute() shouldBe Success(())
    }
  }
}

object ApplyScheduledProductsTaskSpec {

  def asRequest(
      schedule: ProductSchedule,
      instance: ScheduleInstance,
      productPrice: ProductPrice,
      parentTransactionId: Option[TransactionId] = None
  ): Request =
    Request(
      BindedOfferProduct(
        schedule.user.asPrivate,
        schedule.offerId,
        schedule.product
      ),
      productPrice,
      AutoApply(instance.id),
      parentTransactionId
    )

}
