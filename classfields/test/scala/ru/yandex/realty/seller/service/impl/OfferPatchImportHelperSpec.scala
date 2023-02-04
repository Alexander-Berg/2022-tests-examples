package ru.yandex.realty.seller.service.impl

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.features.SimpleFeatures
import ru.yandex.realty.model.offer.{PaymentType, Offer => RealtyOffer}
import ru.yandex.realty.model.user.PassportUser
import ru.yandex.realty.proto.WeekDay
import ru.yandex.realty.proto.offer.CampaignType
import ru.yandex.realty.proto.offer.vos.Offer
import ru.yandex.realty.proto.offer.vos.Offer.{Vas, VasSchedule}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product._
import ru.yandex.realty.seller.model.schedule.{
  FeedSchedulePatch,
  ProductScheduleContext,
  ProductScheduleItem,
  ProductSchedulePatch,
  ScheduleEnabledPatch,
  ScheduleMultiPatch,
  ScheduleOnceContext,
  ScheduleVisitTimePatch
}
import ru.yandex.realty.seller.service.util.ScheduleUtils
import ru.yandex.vertis.util.time.DateTimeUtil
import ru.yandex.vertis.util.time.DateTimeUtil.DateTimeOrdering
import ru.yandex.realty.util.Mappings._
import ru.yandex.realty.seller.service.util.ScheduleUtils._

import scala.math.Ordering.Implicits._
import scala.concurrent.duration.DurationInt
import scala.collection.JavaConverters._

/**
  * @author Vsevolod Levin
  */
@RunWith(classOf[JUnitRunner])
class OfferPatchImportHelperSpec extends SpecBase with SellerModelGenerators {

  private val DateFormatter =
    DateTimeFormat
      .forPattern("dd.MM.yy HH:mm")
      .withZone(DateTimeUtil.DefaultTimeZone)

  implicit class RichString(s: String) {
    def toDate: DateTime = DateTime.parse(s, DateFormatter)
  }

  private val features = new SimpleFeatures()

  private val helper = new OfferPatchImportHelperImpl(features)

  private val AllDays = WeekDay.values().filter(d => d != WeekDay.UNRECOGNIZED && d != WeekDay.WEEKDAY_UNKNOWN)

  private val raising = Offer.Vas.newBuilder().setType(CampaignType.CAMPAIGN_TYPE_RAISING).build()
  private val premium = Offer.Vas.newBuilder().setType(CampaignType.CAMPAIGN_TYPE_PREMIUM).build()
  private val promotion = Offer.Vas.newBuilder().setType(CampaignType.CAMPAIGN_TYPE_PROMOTION).build()

  private val target = OfferTarget("12345")
  private val owner = PassportUser(98765)
  private val startTime = DateTimeUtil
    .now()
    .minusDays(7)
    .withHourOfDay(15)
    .withMinuteOfHour(30)
    .withSecondOfMinute(0)
    .withMillisOfSecond(0)
  private val raisingWithStartTime = Offer.Vas
    .newBuilder()
    .setType(CampaignType.CAMPAIGN_TYPE_RAISING)
    .setStartTimeMs(startTime.getMillis)
    .build()

  private val feedPurchasedProductGen: Gen[PurchasedProduct] =
    purchasedProductGen.map(p => p.copy(source = FeedSource))

  override def beforeAll(): Unit = {
    features.UseNewRaisingScheduling.setNewState(false)
  }

  private def checkCorrectScheduleFromVases(vases: Seq[Vas], expectedFeedSchedule: Seq[ProductScheduleItem]): Unit = {
    val resultPatch: Seq[ProductSchedulePatch] = Seq(
      ScheduleEnabledPatch(true),
      FeedSchedulePatch(expectedFeedSchedule),
      ScheduleVisitTimePatch(ScheduleUtils.getNextScheduleAfterNow(expectedFeedSchedule))
    )

    val res = helper.buildBatch(owner, target, Seq.empty, active = true, vases, None, new RealtyOffer)
    res.schedulesUpdate.flatMap(_.schedulePatch.asInstanceOf[ScheduleMultiPatch].patches) should contain theSameElementsAs resultPatch
  }

  private def buildRaisingVas(date: String, schedule: Seq[WeekDay] = Seq.empty): Vas =
    Vas
      .newBuilder()
      .setType(CampaignType.CAMPAIGN_TYPE_RAISING)
      .setStartTimeMs(date.toDate.getMillis)
      .applyTransformIf(
        schedule.nonEmpty,
        _.setSchedule(
          VasSchedule
            .newBuilder()
            .addAllSchedule(schedule.asJava)
        )
      )
      .build()

  private def checkNewLogicForDates(dates: Seq[String], expectedFeedSchedule: Seq[ProductScheduleItem]): Unit = {
    features.MigrateRaisingToSchedules.setNewState(true)
    checkCorrectScheduleFromVases(dates.map(buildRaisingVas(_)), expectedFeedSchedule)
  }

  "DefaultIdxRequestImportHelper" should {
    "add simple vases" in {
      val vases = Seq(raising, premium, promotion)
      val beforeOp = DateTimeUtil.now()

      val operation = helper.buildBatch(owner, target, Seq.empty, active = true, vases, None, new RealtyOffer)

      val afterOp = DateTimeUtil.now()

      operation.toSkip shouldBe empty
      operation.toUpdate shouldBe empty

      val creates = operation.toCreate.map(_.product)
      creates should have size 3
      creates.map(_.product) should contain theSameElementsAs
        Seq(ProductTypes.Raising, ProductTypes.Premium, ProductTypes.Promotion)

      creates.foreach { p =>
        p.purchaseId shouldBe None
        p.owner shouldBe owner
        p.target shouldBe target
        p.source shouldBe FeedSource
        p.createTime should be >= beforeOp
        p.createTime should be <= afterOp
        p.startTime.get should be >= beforeOp
        p.startTime.get should be <= afterOp
        p.endTime.get shouldBe p.startTime.get.plusDays(1)
        p.status shouldBe PurchasedProductStatuses.Pending
        p.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.NoOp
        p.context shouldBe ProductContext(1.day, Some(PaymentType.JURIDICAL_PERSON))
        p.priceContext shouldBe None
        p.expirationPolicy shouldBe Prolong(1.day.toMillis)
        p.visitTime shouldBe p.startTime
      }
    }

    "add raising with start time" in {
      val vases = Seq(raisingWithStartTime)
      val beforeOp = DateTimeUtil.now()

      val operation = helper.buildBatch(owner, target, Seq.empty, active = true, vases, None, new RealtyOffer)

      val afterOp = DateTimeUtil.now()

      operation.toSkip shouldBe empty
      operation.toUpdate shouldBe empty

      val creates = operation.toCreate
      creates should have size 1

      val p = creates.head.product
      p.purchaseId shouldBe None
      p.owner shouldBe owner
      p.target shouldBe target
      p.product shouldBe ProductTypes.Raising
      p.source shouldBe FeedSource
      p.createTime should be >= beforeOp
      p.createTime should be <= afterOp
      p.startTime.get shouldBe beforeOp
        .withHourOfDay(15)
        .withMinuteOfHour(30)
        .withSecondOfMinute(0)
        .withMillisOfSecond(0)
      p.endTime.get shouldBe p.startTime.get.plusDays(1)
      p.status shouldBe PurchasedProductStatuses.Pending
      p.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.NoOp
      p.context shouldBe ProductContext(1.day, Some(PaymentType.JURIDICAL_PERSON))
      p.priceContext shouldBe None
      p.expirationPolicy shouldBe Prolong(1.day.toMillis)
      p.visitTime shouldBe p.startTime
    }

    "ignore start time for non-raising" in {
      val vases = Seq(
        premium.toBuilder.setStartTimeMs(startTime.getMillis).build(),
        promotion.toBuilder.setStartTimeMs(startTime.getMillis).build()
      )
      val beforeOp = DateTimeUtil.now()

      val operation = helper.buildBatch(owner, target, Seq.empty, active = true, vases, None, new RealtyOffer)

      val afterOp = DateTimeUtil.now()

      operation.toSkip shouldBe empty
      operation.toUpdate shouldBe empty

      val creates = operation.toCreate.map(_.product)
      creates should have size 2

      creates.foreach { p =>
        p.purchaseId shouldBe None
        p.owner shouldBe owner
        p.target shouldBe target
        p.source shouldBe FeedSource
        p.createTime should be >= beforeOp
        p.createTime should be <= afterOp
        p.startTime.get should be >= beforeOp
        p.startTime.get should be <= afterOp
        p.endTime.get shouldBe p.startTime.get.plusDays(1)
        p.status shouldBe PurchasedProductStatuses.Pending
        p.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.NoOp
        p.context shouldBe ProductContext(1.day, Some(PaymentType.JURIDICAL_PERSON))
        p.priceContext shouldBe None
        p.expirationPolicy shouldBe Prolong(1.day.toMillis)
        p.visitTime shouldBe p.startTime
      }
    }

    "turn off removed vases" in {
      val product = feedPurchasedProductGen.next
        .copy(target = target, owner = owner)

      val operation = helper.buildBatch(owner, target, Seq(product), active = true, Seq.empty, None, new RealtyOffer)

      operation.toCreate shouldBe empty
      operation.toSkip shouldBe empty

      operation.toUpdate should have size 1

      val updateOperation = operation.toUpdate.head
      updateOperation.oldState shouldBe product
      val update = updateOperation.update
      update.id shouldBe product.id
      val updatedProduct = update.patch.applyTo(product)
      updatedProduct.expirationPolicy shouldBe Stop
      updatedProduct shouldBe product.copy(expirationPolicy = Stop)
    }

    "change start time for raising" in {
      val product = feedPurchasedProductGen.next
        .copy(
          product = ProductTypes.Raising,
          target = target,
          owner = owner,
          source = FeedSource,
          context = ProductContext(1.day, Some(PaymentType.JURIDICAL_PERSON))
        )

      val vases = Seq(raisingWithStartTime)

      val beforeOp = DateTimeUtil.now()

      val operation = helper.buildBatch(owner, target, Seq(product), active = true, vases, None, new RealtyOffer)

      val afterOp = DateTimeUtil.now()

      operation.toCreate should have size 1
      val toCreate = operation.toCreate.head.product
      toCreate.purchaseId shouldBe product.purchaseId
      toCreate.owner shouldBe owner
      toCreate.target shouldBe target
      toCreate.product shouldBe ProductTypes.Raising
      toCreate.source shouldBe FeedSource
      toCreate.createTime should be >= beforeOp
      toCreate.createTime should be <= afterOp
      toCreate.startTime.get shouldBe beforeOp
        .plusDays(1)
        .withHourOfDay(15)
        .withMinuteOfHour(30)
        .withSecondOfMinute(0)
        .withMillisOfSecond(0)
      toCreate.endTime.get shouldBe toCreate.startTime.get.plusDays(1)
      toCreate.status shouldBe PurchasedProductStatuses.Pending
      toCreate.deliveryStatus shouldBe PurchaseProductDeliveryStatuses.NoOp
      toCreate.context shouldBe ProductContext(1.day, Some(PaymentType.JURIDICAL_PERSON))
      toCreate.priceContext shouldBe None
      toCreate.expirationPolicy shouldBe Prolong(1.day.toMillis)
      toCreate.visitTime.get should be <= afterOp

      operation.toSkip shouldBe empty

      operation.toUpdate should have size 1
      val updateOperation = operation.toUpdate.head
      updateOperation.oldState shouldBe product
      val update = updateOperation.update
      update.id shouldBe product.id
      val updatedProduct = update.patch.applyTo(product)
      updatedProduct.expirationPolicy shouldBe Stop
      updatedProduct shouldBe product.copy(expirationPolicy = Stop)
    }

    "skip if non-rising products already exist" in {
      val promotionProduct = feedPurchasedProductGen.next
        .copy(product = ProductTypes.Promotion, target = target, owner = owner)
      val premiumProduct = feedPurchasedProductGen.next
        .copy(product = ProductTypes.Premium, target = target, owner = owner)

      val products = Seq(promotionProduct, premiumProduct)
      val vases = Seq(promotion, premium)

      val operation = helper.buildBatch(owner, target, products, active = true, vases, None, new RealtyOffer)

      operation.toCreate shouldBe empty
      operation.toUpdate shouldBe empty

      operation.toSkip should have size 2
      operation.toSkip should contain theSameElementsAs products
    }

    "skip if rising start time not changed" in {
      val product = feedPurchasedProductGen.next
        .copy(product = ProductTypes.Raising, startTime = Some(startTime.plusDays(5)), target = target, owner = owner)

      val vases = Seq(raisingWithStartTime)

      val operation = helper.buildBatch(owner, target, Seq(product), active = true, vases, None, new RealtyOffer)

      operation.toCreate shouldBe empty
      operation.toUpdate shouldBe empty

      operation.toSkip should have size 1
      operation.toSkip.head shouldBe product
    }

    "not turn off manual vases" in {
      val product = purchasedProductGen.next
        .copy(target = target, owner = owner, source = ManualSource)

      val operation = helper.buildBatch(owner, target, Seq(product), active = true, Seq.empty, None, new RealtyOffer)

      operation.toCreate shouldBe empty
      operation.toSkip should have size 1
      operation.toSkip.head shouldBe product
      operation.toUpdate shouldBe empty
    }

    "create correct schedule raising vases in scheduling with empty schedules in base" in {
      features.UseNewRaisingScheduling.setNewState(true)

      def scheduleItem(date: String) =
        ProductScheduleItem(
          date.toDate,
          AllDays
        )

      checkNewLogicForDates(
        Seq("01.01.20 13:00"),
        Seq(scheduleItem("01.01.20 13:00"))
      )

      checkNewLogicForDates(
        Seq("01.01.20 13:00", "08.01.20 13:00"),
        Seq(scheduleItem("01.01.20 13:00"))
      )

      checkNewLogicForDates(
        Seq("01.01.20 13:00", "02.01.20 13:00", "08.01.20 13:00"),
        Seq(scheduleItem("01.01.20 13:00"))
      )

      checkNewLogicForDates(
        Seq("01.01.20 13:00", "02.01.20 13:30", "08.01.20 13:00"),
        Seq(
          scheduleItem("01.01.20 13:00"),
          scheduleItem("02.01.20 13:30")
        )
      )

      checkNewLogicForDates(
        Seq("01.01.20 13:00", "08.01.20 13:00", "09.01.20 13:30"),
        Seq(
          scheduleItem("01.01.20 13:00"),
          scheduleItem("09.01.20 13:30")
        )
      )

      // zero time check
      val result = helper.buildBatch(owner, target, Seq.empty, active = true, Seq(raising), None, new RealtyOffer)

      val startTime = result.schedulesUpdate
        .applySideEffect(_ should have size 1)
        .head
        .schedulePatch
        .asInstanceOf[ScheduleMultiPatch]
        .patches
        .collect {
          case x: FeedSchedulePatch => x
        }
        .applySideEffect(_ should have size 1)
        .head
        .items
        .applySideEffect(_ should have size 1)
        .head
        .applySideEffect(_.feedWithZeroTime shouldBe true)
        .startTime

      startTime >= "01.01.70 00:00".toDate shouldBe true
      startTime < "02.01.70 00:00".toDate shouldBe true

      val hours = Seq(9, 12, 16)

      hours.contains(startTime.getHourOfDay) shouldBe true
    }

    "create correct raising from feed with schedule set" in {
      features.MigrateRaisingToSchedules.setNewState(true)

      val vasesWithNoSchedule = Seq(
        buildRaisingVas("01.01.20 10:00"), // no schedule set,
        buildRaisingVas("02.01.20 10:00", Seq(WeekDay.MONDAY))
      )

      val expectedWithNoSchedule = Seq(
        ProductScheduleItem(
          startTime = "01.01.20 10:00".toDate,
          daysOfWeek = AllDays
        )
      )
      checkCorrectScheduleFromVases(vasesWithNoSchedule, expectedWithNoSchedule)

      val vasesForMerge = Seq(
        buildRaisingVas(
          "01.01.20 10:00",
          Seq(
            WeekDay.MONDAY,
            WeekDay.TUESDAY,
            WeekDay.WEDNESDAY,
            WeekDay.THURSDAY,
            WeekDay.FRIDAY
          )
        ),
        buildRaisingVas(
          "02.01.20 10:00",
          Seq(
            WeekDay.SATURDAY,
            WeekDay.SUNDAY
          )
        )
      )

      val expectedForMerge = Seq(
        ProductScheduleItem(
          startTime = "01.01.20 10:00".toDate,
          daysOfWeek = AllDays
        )
      )
      checkCorrectScheduleFromVases(vasesForMerge, expectedForMerge)

      val vasesNotMerge =
        Seq(
          buildRaisingVas(
            "01.01.20 10:00",
            Seq(WeekDay.MONDAY)
          ),
          buildRaisingVas(
            "02.01.20 11:00",
            Seq(WeekDay.TUESDAY)
          )
        )

      val expectedNotMerge =
        Seq(
          ProductScheduleItem(
            "01.01.20 10:00".toDate,
            Seq(WeekDay.MONDAY)
          ),
          ProductScheduleItem(
            "02.01.20 11:00".toDate,
            Seq(WeekDay.TUESDAY)
          )
        )
      checkCorrectScheduleFromVases(vasesNotMerge, expectedNotMerge)

    }

    "don't create another new raising with no time" when {
      "no time schedule exist" in {

        val inBaseState = scheduleStateGen.next.copy(
          scheduleContext = ProductScheduleContext(
            ScheduleOnceContext(
              Seq(
                buildFeedWithNoTimeSchedule
              )
            )
          )
        )

        val operation =
          helper.buildBatch(owner, target, Seq.empty, active = true, Seq(raising), Some(inBaseState), new RealtyOffer)

        operation.schedulesUpdate should have size 0
      }
    }
  }

}
