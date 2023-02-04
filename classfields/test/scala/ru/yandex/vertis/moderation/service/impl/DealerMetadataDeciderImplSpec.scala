package ru.yandex.vertis.moderation.service.impl

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.meta.Metadata.Dealer
import ru.yandex.vertis.moderation.model.meta.{DealerResolution, MetadataSet}
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, SignalInfo, SignalInfoSet, SignalSet}
import ru.yandex.vertis.moderation.proto.Model.HoboCheckType
import ru.yandex.vertis.moderation.service.DealerMetadataDecider
import ru.yandex.vertis.moderation.service.DealerMetadataDecider.HoboTaskDecision

@RunWith(classOf[JUnitRunner])
class DealerMetadataDeciderImplSpec extends SpecBase {

  private case class TestCase(description: String,
                              instance: Instance,
                              currentLoyaltyInfo: Dealer.LoyaltyInfo,
                              forPeriodLoyaltyInfo: Option[Dealer.LoyaltyInfo],
                              check: DealerMetadataDecider.Decision => Boolean
                             )

  private val tomorrow = DateTime.now().plusDays(1)
  private val periodId = "period1".toOption

  private val testCases: Seq[TestCase] =
    Seq(
      {
        val overridingResolution = DealerResolution.Loyal(3)
        val dealerMetadata = DealerMetadataGen.next
        val updatableDealerMetadata =
          UpdatableDealerGen.next.copy(resolution = overridingResolution, overrideUntil = tomorrow.toOption)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        TestCase(
          description = "Metadata current resolution with override",
          instance = InstanceGen.next.copy(metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.NotLoyal),
          forPeriodLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next.toOption,
          check =
            (decision: DealerMetadataDecider.Decision) => decision.metadata.current.resolution == overridingResolution
        )
      }, {
        val forPeriod = DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(6))
        val dealerMetadata = DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        TestCase(
          description = "Metadata forPeriod previous resolution without override",
          instance = InstanceGen.next.copy(metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = None,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.metadata.forPeriod.exists(_.resolution == DealerResolution.Loyal(6))
        )
      }, {
        val forPeriod = DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(6))
        val dealerMetadata = DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        TestCase(
          description = "Metadata forPeriod new resolution without override",
          instance = InstanceGen.next.copy(metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo =
            DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(12)).toOption,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.metadata.forPeriod.exists(_.resolution == DealerResolution.Loyal(12))
        )
      }, {
        val overridingResolution = DealerResolution.Loyal(3)
        val forPeriod = DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(6))
        val dealerMetadata = DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption)
        val updatableDealerMetadata =
          UpdatableDealerGen.next.copy(resolution = overridingResolution, overrideUntil = tomorrow.toOption)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        TestCase(
          description = "Metadata forPeriod with resolution override",
          instance = InstanceGen.next.copy(metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo =
            DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(12)).toOption,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.metadata.forPeriod.exists(_.resolution == DealerResolution.Loyal(3))
        )
      }, {
        val forPeriodApproved =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(6), periodId = periodId)
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(12), periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriodApproved.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        TestCase(
          description = "Metadata forPeriodApproved Loyal without override",
          instance = InstanceGen.next.copy(metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = None,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.metadata.forPeriodApproved.exists(_.resolution == DealerResolution.Loyal(12))
        )
      }, {
        val forPeriodApproved =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.NotLoyal, periodId = periodId)
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(12), periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriodApproved.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        TestCase(
          description = "Metadata forPeriodApproved NotLoyal without override",
          instance = InstanceGen.next.copy(metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = None,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.metadata.forPeriodApproved.exists(_.resolution == DealerResolution.NotLoyal)
        )
      }, {
        val overridingResolution = DealerResolution.Loyal(3)
        val forPeriodApproved =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(6), periodId = periodId)
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(12), periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriodApproved.toOption)
        val updatableDealerMetadata =
          UpdatableDealerGen.next.copy(resolution = overridingResolution, overrideUntil = tomorrow.toOption)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        TestCase(
          description = "Metadata forPeriodApproved Loyal with override",
          instance = InstanceGen.next.copy(metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = None,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.metadata.forPeriodApproved.exists(_.resolution == DealerResolution.Loyal(3))
        )
      }, {
        val forPeriod =
          DealerMetadataLoyaltyInfoGen
            .filter(_.automaticallyApproved.contains(true))
            .next
            .copy(resolution = DealerResolution.Loyal(12), periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriod.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val signals = SignalSet.Empty
        TestCase(
          description = "Hobo create on Loyal with automatically_approved",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              checkHoboCreateDecisions(decision.hobo, Some(DealerMetadataDeciderImpl.AutomaticallyApproved))
        )
      }, {
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.NotLoyal, periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriod.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val signals = SignalSet.Empty
        TestCase(
          description = "Hobo not create on NotLoyal",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check = (decision: DealerMetadataDecider.Decision) => decision.hobo.isEmpty
        )
      }, {
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(3), periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriod.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val hoboSignal =
          HoboSignalGen.next.copy(
            labels = periodId.toSet,
            `type` = HoboCheckType.LOYALTY_DEALERS,
            result = HoboSignal.Result.Undefined,
            source = SourceWithoutMarkerGen.next,
            task = HoboSignalTaskGen.next.toOption,
            auxInfo = SignalInfoSet(SignalInfo.HoboTaskInfo(autoruLoyaltyDealerPeriodId = periodId.head)),
            switchOff = None
          )
        val signals = SignalSet(hoboSignal)
        TestCase(
          description = "Hobo not create on Loyal if already exist uncompleted task",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check = (decision: DealerMetadataDecider.Decision) => decision.hobo.isEmpty
        )
      }, {
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(3), periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriod.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val hoboSignal =
          HoboSignalGen.next.copy(
            labels = periodId.toSet,
            `type` = HoboCheckType.LOYALTY_DEALERS,
            result = HoboSignal.Result.Good(None),
            source = SourceWithoutMarkerGen.next,
            task = HoboSignalTaskGen.next.toOption,
            auxInfo = SignalInfoSet(SignalInfo.HoboTaskInfo(autoruLoyaltyDealerPeriodId = periodId.head)),
            switchOff = None
          )
        val signals = SignalSet(hoboSignal)
        TestCase(
          description = "Hobo not create on Loyal if already exist completed task",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check = (decision: DealerMetadataDecider.Decision) => decision.hobo.isEmpty
        )
      }, {
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.NotLoyal, periodId = periodId)
        val dealerMetadata = DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = None)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val hoboSignal =
          HoboSignalGen.next.copy(
            labels = periodId.toSet,
            `type` = HoboCheckType.LOYALTY_DEALERS,
            result = HoboSignal.Result.Undefined,
            source = SourceWithoutMarkerGen.next,
            task = HoboSignalTaskGen.next.toOption,
            auxInfo = SignalInfoSet(SignalInfo.HoboTaskInfo(autoruLoyaltyDealerPeriodId = periodId.head)),
            switchOff = None
          )
        val signals = SignalSet(hoboSignal)
        TestCase(
          description = "Hobo cancel on NotLoyal",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.hobo.length == 1 &&
                decision.hobo.exists(_.isInstanceOf[DealerMetadataDecider.HoboTaskDecision.Cancel])
        )
      }, {
        val anotherPeriodId = "anotherPeriodId"
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(3), periodId = periodId)
        val dealerMetadata = DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = None)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val hoboSignal =
          HoboSignalGen.next.copy(
            labels = Set(anotherPeriodId),
            `type` = HoboCheckType.LOYALTY_DEALERS,
            result = HoboSignal.Result.Undefined,
            source = SourceWithoutMarkerGen.next,
            task = HoboSignalTaskGen.next.toOption,
            auxInfo = SignalInfoSet(SignalInfo.HoboTaskInfo(autoruLoyaltyDealerPeriodId = anotherPeriodId)),
            switchOff = None
          )
        val signals = SignalSet(hoboSignal)
        TestCase(
          description = "Hobo cancel previous and on Loyal create new",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.hobo.length == 2 &&
                decision.hobo.exists(_.isInstanceOf[DealerMetadataDecider.HoboTaskDecision.Cancel]) &&
                decision.hobo.exists(_.isInstanceOf[DealerMetadataDecider.HoboTaskDecision.Create])
        )
      }, {
        val anotherPeriodId = "anotherPeriodId"
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.NotLoyal, periodId = periodId)
        val dealerMetadata = DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = None)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val hoboSignal =
          HoboSignalGen.next.copy(
            labels = Set(anotherPeriodId),
            `type` = HoboCheckType.LOYALTY_DEALERS,
            result = HoboSignal.Result.Undefined,
            source = SourceWithoutMarkerGen.next,
            task = HoboSignalTaskGen.next.toOption,
            auxInfo = SignalInfoSet(SignalInfo.HoboTaskInfo(autoruLoyaltyDealerPeriodId = anotherPeriodId)),
            switchOff = None
          )
        val signals = SignalSet(hoboSignal)
        TestCase(
          description = "Hobo cancel previous and on NotLoyal not create new",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              decision.hobo.length == 1 &&
                decision.hobo.exists(_.isInstanceOf[DealerMetadataDecider.HoboTaskDecision.Cancel])
        )
      }, {
        val forPeriod =
          DealerMetadataLoyaltyInfoGen.next.copy(resolution = DealerResolution.Loyal(12), periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriod.toOption)
        val updatableDealerMetadata =
          UpdatableDealerGen.next.copy(
            overrideUntil = Some(tomorrow),
            resolution = DealerResolution.Loyal(12)
          )
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val signals = SignalSet.Empty
        TestCase(
          description = "Hobo create on Loyal with white_list",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check =
            (decision: DealerMetadataDecider.Decision) =>
              checkHoboCreateDecisions(decision.hobo, Some(DealerMetadataDeciderImpl.WhiteList))
        )
      }, {
        val forPeriod =
          DealerMetadataLoyaltyInfoGen
            .filter(_.automaticallyApproved.isEmpty)
            .next
            .copy(resolution = DealerResolution.Loyal(12), periodId = periodId)
        val dealerMetadata =
          DealerMetadataGen.next.copy(forPeriod = forPeriod.toOption, forPeriodApproved = forPeriod.toOption)
        val updatableDealerMetadata = UpdatableDealerGen.next.copy(overrideUntil = None)
        val metadataSet = MetadataSet(dealerMetadata, updatableDealerMetadata)
        val signals = SignalSet.Empty
        TestCase(
          description = "Hobo create on Loyal with empty comment",
          instance = InstanceGen.next.copy(signals = signals, metadata = metadataSet),
          currentLoyaltyInfo = DealerMetadataLoyaltyInfoGen.next,
          forPeriodLoyaltyInfo = forPeriod.toOption,
          check = (decision: DealerMetadataDecider.Decision) => checkHoboCreateDecisions(decision.hobo, None)
        )
      }
    )

  "DealerMetadataDeciderImpl" should {
    testCases.foreach { case TestCase(description, instance, currentLoyaltyInfo, forPeriodLoyaltyInfo, check) =>
      description in {
        val actualResult = DealerMetadataDeciderImpl.decide(instance, currentLoyaltyInfo, forPeriodLoyaltyInfo)
        check(actualResult) shouldBe true
      }
    }
  }

  private def checkHoboCreateDecisions(decisions: Seq[HoboTaskDecision], commentShouldBe: Option[String]): Boolean =
    decisions match {
      case Seq(HoboTaskDecision.Create(source)) => source.comment == commentShouldBe
      case _                                    => false
    }
}
