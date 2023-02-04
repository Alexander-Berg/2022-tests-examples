package ru.yandex.vertis.moderation.hobo.decider

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.DetailedReason.NotVerified
import ru.yandex.vertis.moderation.model.ModerationRequest.InitialDepth
import ru.yandex.vertis.moderation.model.{Opinion, Opinions}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.Diff
import ru.yandex.vertis.moderation.model.meta.OffersSummary.AutoruQualifier
import ru.yandex.vertis.moderation.model.meta.MetadataSet
import ru.yandex.vertis.moderation.model.signal.HoboSignal.Task
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, Inherited, SignalSet, SignalSwitchOff}
import ru.yandex.vertis.moderation.proto.Autoru
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Diff.DealersAutoru.Value._
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service, Visibility}
import ru.yandex.vertis.moderation.util.DateTimeUtil

@RunWith(classOf[JUnitRunner])
class DealersAutoruPremoderationDeciderSpec extends SpecBase {

  implicit private val featureRegistry: FeatureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
  private val decider: HoboDecider = new DealersAutoruPremoderationDecider

  private case class TestCase(description: String,
                              decider: HoboDecider,
                              source: HoboDecider.Source,
                              isLogicEnabled: Boolean,
                              check: Option[HoboDecider.Verdict] => Boolean,
                              expectedResult: Boolean
                             )

  val appropriateDiff = DealersAutoruDiffGen.next.copy(values = Set(SIGNALS))
  val notAppropriateDiff = DealersAutoruDiffGen.next.copy(values = Set(DEALER_METADATA))

  val inheritedSignalSource =
    AutomaticSourceGen.next.copy(
      application = Application.INDEXER,
      marker = Inherited(Service.DEALERS_AUTORU)
    )
  val notInheritedSignalSource =
    AutomaticSourceGen.next.copy(
      application = Application.INDEXER
    )
  val automaticHoboSignalSource =
    AutomaticSourceGen.next.copy(
      application = Application.MODERATION,
      marker = NoSourceMarkerGen.next
    )
  val inheritedSignal =
    IndexErrorSignalGen.next.copy(
      domain = DomainDealersAutoruGen.next,
      source = inheritedSignalSource,
      switchOff = None
    )
  val notInheritedSignal =
    IndexErrorSignalGen.next.copy(
      domain = DomainDealersAutoruGen.next,
      source = notInheritedSignalSource,
      switchOff = None
    )
  val inheritedSignalWithSwitchOff =
    inheritedSignal.copy(
      switchOff =
        Some(
          SignalSwitchOff(
            source = inheritedSignalSource,
            timestamp = DateTimeUtil.now(),
            None,
            None
          )
        )
    )
  val notInheritedSignalWithSwitchOff =
    notInheritedSignal.copy(
      switchOff =
        Some(
          SignalSwitchOff(
            source = notInheritedSignalSource,
            timestamp = DateTimeUtil.now(),
            None,
            None
          )
        )
    )
  val notDealersSignal =
    inheritedSignal.copy(
      domain = DomainAutoruGen.next
    )
  val hoboSignalPremod =
    HoboSignalGen.next.copy(
      domain = DomainDealersAutoruGen.next,
      source = automaticHoboSignalSource,
      `type` = HoboCheckType.PREMODERATION_DEALER,
      task = Some(Task(QueueId.AUTO_RU_PREMODERATION_DEALER.toString, "qwendfqiwjfniejvnejv")),
      switchOff = None,
      result = HoboSignal.Result.Undefined
    )

  val blockedOffersVisibilitySummary =
    Map(AutoruQualifier(Visibility.BLOCKED, Autoru.AutoruEssentials.Category.BUS) -> 1)
  val offersStatisticsMetaWithBlockedOffer =
    OffersStatisticsMetadataGen.suchThat { offerStatistics =>
      offerStatistics.statistics.offersVisibilitySummary == blockedOffersVisibilitySummary
    }

  val visibleOffersVisibilitySummary =
    Map(AutoruQualifier(Visibility.VISIBLE, Autoru.AutoruEssentials.Category.SPECIAL) -> 1)
  val offersStatisticsMetaWithVisibleOffer =
    OffersStatisticsMetadataGen.suchThat { offerStatistics =>
      offerStatistics.statistics.offersVisibilitySummary == visibleOffersVisibilitySummary
    }

  val appropriateMeta = MetadataSet.Empty + offersStatisticsMetaWithBlockedOffer.next
  val notAppropriateMeta = MetadataSet.Empty + offersStatisticsMetaWithVisibleOffer.next

  val signalSet1 = SignalSet(Iterable(inheritedSignal))
  val signalSet2 = SignalSet(Iterable(inheritedSignalWithSwitchOff))
  val signalSet3 = SignalSet(Iterable(notDealersSignal))
  val signalSet4 = SignalSet(Iterable(notInheritedSignal))
  val signalSet5 = SignalSet(Iterable(notInheritedSignalWithSwitchOff))
  val signalSet6 = SignalSet(Iterable(hoboSignalPremod, inheritedSignal))
  val signalSet7 = SignalSet(Iterable(notInheritedSignal, hoboSignalPremod))

  val opinionUnknown = Opinion.Unknown(warnDetailedReasons = Set())
  val failedOpinion = Opinion.Failed(detailedReasons = Set(NotVerified), warnDetailedReasons = Set())
  val okOpinion = Opinion.Ok(Set.empty)

  val hoboDeciderSource1 = newSource(signalSet1, appropriateDiff, failedOpinion, appropriateMeta)
  val hoboDeciderSource2 = newSource(signalSet2, appropriateDiff, failedOpinion, appropriateMeta)
  val hoboDeciderSource3 = newSource(signalSet1, appropriateDiff, opinionUnknown, appropriateMeta)
  val hoboDeciderSource4 = newSource(signalSet2, appropriateDiff, okOpinion, appropriateMeta)
  val hoboDeciderSource5 = newSource(signalSet4, appropriateDiff, failedOpinion, appropriateMeta)
  val hoboDeciderSource6 = newSource(signalSet5, appropriateDiff, failedOpinion, appropriateMeta)
  val hoboDeciderSource7 = newSource(signalSet4, appropriateDiff, opinionUnknown, appropriateMeta)
  val hoboDeciderSource8 = newSource(signalSet5, appropriateDiff, okOpinion, appropriateMeta)
  val hoboDeciderSource9 = newSource(signalSet3, notAppropriateDiff, failedOpinion, appropriateMeta)
  val hoboDeciderSource10 = newSource(signalSet1, appropriateDiff, failedOpinion, notAppropriateMeta)
  val hoboDeciderSource11 = newSource(signalSet4, appropriateDiff, failedOpinion, notAppropriateMeta)
  val hoboDeciderSource12 = newSource(signalSet6, appropriateDiff, failedOpinion, appropriateMeta)
  val hoboDeciderSource13 = newSource(signalSet7, appropriateDiff, failedOpinion, appropriateMeta)

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "create hobo task if appropriate diff and signal is inherited",
        decider = decider,
        source = hoboDeciderSource1,
        isLogicEnabled = true,
        check = isCreate,
        true
      ),
      TestCase(
        description = "not create hobo task if appropriate diff and signal is inherited but not appropriate meta",
        decider = decider,
        source = hoboDeciderSource10,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description = "not create hobo task if there is hobo signal already",
        decider = decider,
        source = hoboDeciderSource12,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description = "create hobo task if appropriate diff and signal is not inherited",
        decider = decider,
        source = hoboDeciderSource5,
        isLogicEnabled = true,
        check = isCreate,
        true
      ),
      TestCase(
        description = "not create hobo task if appropriate diff and signal is not inherited but there is hobo signal",
        decider = decider,
        source = hoboDeciderSource13,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description = "not create hobo task if appropriate diff and signal is not inherited but not appropriate meta",
        decider = decider,
        source = hoboDeciderSource11,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description = "not create hobo task cause diff is not appropriate",
        decider = decider,
        source = hoboDeciderSource9,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description = "not create hobo cause logic is not enabled",
        decider = decider,
        source = hoboDeciderSource1,
        isLogicEnabled = false,
        check = isCreate,
        false
      ),
      TestCase(
        description = "not create hobo task cause inherited signal is switched off",
        decider = decider,
        source = hoboDeciderSource2,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description = "not create hobo task cause not inherited signal is switched off",
        decider = decider,
        source = hoboDeciderSource6,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description = "not create hobo task cause source already have got unknown opinion for inherited signal",
        decider = decider,
        source = hoboDeciderSource3,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description = "not create hobo task cause source already have got unknown opinion for not inherited signal",
        decider = decider,
        source = hoboDeciderSource7,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description =
          "not create hobo task cause source already have got ok opinion and inherited signal is switched off",
        decider = decider,
        source = hoboDeciderSource4,
        isLogicEnabled = true,
        check = isCreate,
        false
      ),
      TestCase(
        description =
          "not create hobo task cause source already have got ok opinion and not inherited signal is switched off",
        decider = decider,
        source = hoboDeciderSource8,
        isLogicEnabled = true,
        check = isCreate,
        false
      )
    )

  private def newSource(signals: SignalSet, diff: Diff, opinion: Opinion, metadata: MetadataSet): HoboDecider.Source = {
    val instance =
      InstanceGen.next.copy(
        signals = signals,
        essentials = DealersAutoruEssentialsGen.next,
        context = ContextGen.next.copy(visibility = Visibility.VISIBLE),
        metadata = metadata
      )
    val opinions = Opinions(map = Map(DomainDealersAutoruGen.next -> opinion))
    HoboDecider.Source(instance, None, diff, opinions, DateTimeUtil.now(), InitialDepth)
  }

  private def isCreate(verdict: Option[HoboDecider.Verdict]): Boolean =
    verdict match {
      case Some(v: HoboDecider.NeedCreate) =>
        v.request.hoboSignalSource.`type` == DealersAutoruPremoderationDecider.CheckType
      case _ => false
    }

  "DealersAutoruPremoderationDecider" should {
    testCases.foreach { case TestCase(description, decider, source, isLogicEnabled, check, expectedResult) =>
      description in {
        featureRegistry
          .updateFeature(
            DealersAutoruPremoderationDecider.DealersAutoruPremoderationNewLogic,
            isLogicEnabled
          )
          .futureValue
        check(decider.decide(source).futureValue) shouldBe expectedResult
      }
    }
  }
}
