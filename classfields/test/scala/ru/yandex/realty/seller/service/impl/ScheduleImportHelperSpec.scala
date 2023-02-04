package ru.yandex.realty.seller.service.impl

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.proto.offer.CampaignType
import ru.yandex.realty.proto.offer.vos.Offer.{Vas, VasSchedule}
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.schedule.{
  FeedSchedulePatch,
  ManualSchedulePatch,
  ProductScheduleContext,
  ProductScheduleItem,
  ProductSchedulePatch,
  ProductScheduleState,
  ScheduleEnabledPatch,
  ScheduleOnceContext,
  ScheduleVisitTimePatch
}
import ru.yandex.realty.seller.service.util.ScheduleUtils
import ru.yandex.vertis.util.time.DateTimeUtil
import ScheduleUtils._
import ru.yandex.realty.proto.WeekDay

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ScheduleImportHelperSpec extends SpecBase with SellerModelGenerators {

  private val someDate = DateTimeUtil.now().withMillisOfSecond(0).withSecondOfMinute(0).minusDays(1)

  private val EveryDaySchedule = Seq(
    ProductScheduleItem(startTime = someDate, daysOfWeek = ScheduleUtils.AllDays)
  )

  private val WeekdaysSchedule = Seq(
    ProductScheduleItem(
      startTime = someDate,
      daysOfWeek = ScheduleUtils.AllDays.filter(_.getNumber <= WeekDay.FRIDAY_VALUE)
    )
  )

  private val EmptyRequested = Seq()

  private val EveryDayRequested = Seq(
    Vas
      .newBuilder()
      .setStartTimeMs(someDate.getMillis)
      .setType(CampaignType.CAMPAIGN_TYPE_RAISING)
      .setSchedule(VasSchedule.newBuilder().addAllSchedule(ScheduleUtils.AllDays.asJava))
      .build()
  )

  private val WeekdaysRequested = Seq(
    Vas
      .newBuilder()
      .setStartTimeMs(someDate.getMillis)
      .setType(CampaignType.CAMPAIGN_TYPE_RAISING)
      .setSchedule(
        VasSchedule
          .newBuilder()
          .addAllSchedule(
            ScheduleUtils.AllDays
              .filter(_.getNumber <= WeekDay.FRIDAY_VALUE)
              .asJava
          )
      )
      .build()
  )

  val EmptyState: ProductScheduleState = scheduleStateGen.next.withoutVisitTime.disabled
    .withFeed(Seq.empty)
    .withManuals(Seq.empty)

  "ScheduleImportHelper" should {
    "update feed schedule" when {
      "requested vases were presented and empty feed schedule" in {

        def checkThisCase(state: ProductScheduleState): Unit = {
          val patchOpt = ScheduleImportHelper.buildSchedulePatch(Some(state), EveryDayRequested)

          patchOpt shouldBe defined

          patchOpt.map(_.patch(state)).foreach { newState =>
            newState shouldBe state
              .withFeed(EveryDaySchedule)
              .enabled
              .withVisitTime(getNextScheduleAfterNow(EveryDaySchedule))
          }
        }

        val stateWithManual = EmptyState.enabled
          .withFeed(Seq.empty)
          .withManuals(EveryDaySchedule)

        val strangeState = EmptyState.enabled

        checkThisCase(stateWithManual)
        checkThisCase(EmptyState)
        checkThisCase(strangeState)

        val patchOpt = ScheduleImportHelper.buildSchedulePatch(None, EveryDayRequested)

        patchOpt shouldBe defined

        patchOpt.map(_.patch(EmptyState)).foreach { newState =>
          newState shouldBe EmptyState
            .withFeed(EveryDaySchedule)
            .enabled
            .withVisitTime(getNextScheduleAfterNow(EveryDaySchedule))
        }
      }

      "requested vases were presented and nonempty feed schedule" in {
        val allDaysFeed = EmptyState
          .withFeed(EveryDaySchedule)
          .withManuals(Seq.empty)
          .enabled

        val weekdaysFeed = allDaysFeed.withFeed(WeekdaysSchedule)

        val feedChangedPatch = ScheduleImportHelper.buildSchedulePatch(Some(allDaysFeed), WeekdaysRequested)

        feedChangedPatch.map(_.patch(allDaysFeed)).foreach {
          _ shouldBe allDaysFeed
            .withFeed(WeekdaysSchedule)
            .withVisitTime(getNextScheduleAfterNow(WeekdaysSchedule))
        }

        ScheduleImportHelper.buildSchedulePatch(Some(weekdaysFeed), WeekdaysRequested) shouldBe None
      }

    }

    "switch to manual" when {

      "requested weren't presented" in {

        def checkThisCase(state: ProductScheduleState): Unit = {
          val patchOpt = ScheduleImportHelper.buildSchedulePatch(Some(state), EmptyRequested)

          patchOpt.map(_.patch(state)).foreach { newState =>
            newState shouldBe state
              .withFeed(Seq.empty)
              .withoutVisitTime
              .disabled
          }
        }

        def shouldBeDisabled(state: Option[ProductScheduleState]): Unit =
          ScheduleImportHelper
            .buildSchedulePatch(state, EmptyRequested)
            .map(_.patch(state.getOrElse(EmptyState)))
            .foreach { _ shouldBe EmptyState.disabled.withoutVisitTime }

        val stateWithManualsOnly = EmptyState
          .withManuals(EveryDaySchedule)
          .withFeed(Seq.empty)
          .enabled

        val stateWithFeedOnly = EmptyState
          .withFeed(EveryDaySchedule)
          .withManuals(Seq.empty)
          .enabled

        val stateWithBoth = EmptyState
          .withFeed(EveryDaySchedule)
          .withManuals(EveryDaySchedule)
          .enabled

        checkThisCase(stateWithManualsOnly)
        checkThisCase(stateWithBoth)

        shouldBeDisabled(Some(EmptyState))
        shouldBeDisabled(Some(stateWithFeedOnly))
        shouldBeDisabled(None)
      }

    }

    "correctly ignore order in schedule equals" in {
      val items = list(10, 20, scheduleItemGen).next
      val totallyReversed = items.reverse.map { item =>
        item.copy(daysOfWeek = item.daysOfWeek.reverse)
      }

      ScheduleImportHelper.scheduleEquals(items, totallyReversed) shouldBe true

    }

    "correctly work on applied product and disabled schedule" in {

      val state = EmptyState
        .withManuals(EveryDaySchedule)
        .withFeed(Seq.empty)
        .disabled

      val res = ScheduleImportHelper.buildSchedulePatch(Some(state), EmptyRequested)
      res shouldBe None
    }
  }

  implicit class RichProductScheduleState(state: ProductScheduleState) {

    private def patchBy(patch: ProductSchedulePatch) = patch.patch(state)

    def withVisitTime(time: Option[DateTime]): ProductScheduleState =
      patchBy(ScheduleVisitTimePatch(time))

    def withoutVisitTime: ProductScheduleState = withVisitTime(None)

    def enabled: ProductScheduleState = patchBy(ScheduleEnabledPatch(true))

    def disabled: ProductScheduleState = patchBy(ScheduleEnabledPatch(false))

    def withManuals(items: Seq[ProductScheduleItem]): ProductScheduleState =
      ManualSchedulePatch(items).patch(state)

    def withFeed(items: Seq[ProductScheduleItem]): ProductScheduleState =
      FeedSchedulePatch(items).patch(state)
  }
}
