package ru.yandex.vertis.moderation.flink.rule

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.events.rules.RuleStatistics
import ru.yandex.vertis.moderation.model.events.rules.RuleStatistics.SignalsSummary
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.UpdateJournalRecord
import ru.yandex.vertis.moderation.model.signal.{Signal, SignalInfo, SignalInfoSet, SignalSet, SourceMarker}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Reason}
import ru.yandex.vertis.moderation.util.DateTimeUtil.OrderedDateTime
import ru.yandex.vertis.moderation.util.{DateTimeUtil, Interval}
import ru.yandex.vertis.moderation.TestUtils.asScalaFlatMap
import ru.yandex.vertis.moderation.model.rule.RuleId

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class RulesSignalsExtractorSpec extends SpecBase {

  import RulesSignalsExtractorSpec._

  val extractor: UpdateJournalRecord => Iterable[RuleStatistics] =
    new RulesSignalsExtractor(key => RuleStatistics.Key.Rule(key.ruleId))

  "extractor" should {
    "extract rules signal summary correctly" in {
      val signals =
        otherSignalsSet.next.withoutSwitchOffs ++ SignalSet(
          banSignal(
            detailedReason = DetailedReason.AdOnPhoto,
            rules = Set(1, 2)
          ).next,
          warnSignal(
            detailedReason = DetailedReason.ContactsInName,
            rules = Set(3),
            switchedOn = false
          ).next,
          hoboSignal(
            reason = Reason.DAMAGED_PHOTO,
            rules = Set(6),
            switchedOn = false
          ).next,
          warnSignal(
            detailedReason = DetailedReason.Another,
            rules = Set(4)
          ).next,
          banSignal(
            detailedReason = DetailedReason.UserReseller(None, Seq.empty),
            rules = Set(10),
            isInherited = true
          ).next
        )
      val prevSignals =
        otherSignalsSet.next.withoutSwitchOffs ++ SignalSet(
          banSignal(
            detailedReason = DetailedReason.AdOnPhoto,
            rules = Set(1)
          ).next,
          warnSignal(
            detailedReason = DetailedReason.ContactsInName,
            rules = Set(3)
          ).next,
          warnSignal(
            detailedReason = DetailedReason.AlreadyBooked,
            rules = Set(4),
            switchedOn = false
          ).next,
          hoboSignal(
            reason = Reason.DAMAGED_PHOTO,
            rules = Set(5, 6)
          ).next
        )
      val updateJournalRecord = getUpdateJournalRecord(signals, prevSignals)
      val actualResult = extractor(updateJournalRecord)
      val expectedResult =
        toRuleStatistics(
          source =
            Map(
              2 -> SignalsSummary(added = 1, deleted = 0, switchedOff = 0, switchedOn = 0),
              3 -> SignalsSummary(added = 0, deleted = 0, switchedOff = 1, switchedOn = 0),
              4 -> SignalsSummary(added = 1, deleted = 1, switchedOff = 0, switchedOn = 0),
              5 -> SignalsSummary(added = 0, deleted = 1, switchedOff = 0, switchedOn = 0),
              6 -> SignalsSummary(added = 0, deleted = 0, switchedOff = 1, switchedOn = 0)
            ),
          timestamp = updateJournalRecord.timestamp
        )

      actualResult.toList.sortBy(_.key.ruleId) shouldBe expectedResult.toList.sortBy(_.key.ruleId)
    }
  }

}

object RulesSignalsExtractorSpec {
  private val domain: Domain = DomainGen.next

  private def toRuleStatistics(source: Map[RuleId, SignalsSummary], timestamp: DateTime): Set[RuleStatistics] =
    source.map { case (ruleId, signalsSummary) =>
      RuleStatistics(
        key = RuleStatistics.Key.Rule(ruleId),
        timeInterval = Interval(timestamp, timestamp),
        signalsSummary = signalsSummary
      )
    }.toSet

  private def getUpdateJournalRecord(signals: SignalSet, prevSignals: SignalSet): UpdateJournalRecord = {
    val instance = InstanceGen.next.copy(signals = signals)
    UpdateJournalRecord.withInitialDepth(
      prev = Some(instance.copy(signals = prevSignals)),
      instance = instance,
      timestamp = DateTimeUtil.now(),
      diff = DiffGen.next
    )
  }

  private def otherSignalsSet: Gen[SignalSet] =
    for {
      n       <- Gen.chooseNum(0, 5)
      signals <- Gen.listOfN(n, otherSignal)
    } yield SignalSet(signals)

  private def otherSignal: Gen[Signal] =
    SignalGen.suchThat(!_.source.getApplication.contains(Application.MODERATION_RULES))

  private def banSignal(detailedReason: DetailedReason,
                        rules: Set[RuleId],
                        switchedOn: Boolean = true,
                        isInherited: Boolean = false
                       ): Gen[Signal] =
    BanSignalGen.map(
      _.copy(
        detailedReason = detailedReason,
        info = Some(rules.mkString(",")),
        source =
          AutomaticSourceGen.next.copy(
            application = Application.MODERATION_RULES,
            marker = markerGen(isInherited).next,
            originQualifier = None
          ),
        switchOff = if (switchedOn) None else Some(SignalSwitchOffGen.next),
        domain = domain,
        auxInfo = SignalInfoSet(SignalInfo.ModerationRules(rules))
      )
    )

  private def warnSignal(detailedReason: DetailedReason,
                         rules: Set[RuleId],
                         switchedOn: Boolean = true,
                         isInherited: Boolean = false
                        ): Gen[Signal] =
    WarnSignalGen.map(
      _.copy(
        detailedReason = detailedReason,
        info = Some(rules.mkString(",")),
        source =
          AutomaticSourceGen.next.copy(
            application = Application.MODERATION_RULES,
            marker = markerGen(isInherited).next,
            originQualifier = None
          ),
        switchOff = if (switchedOn) None else Some(SignalSwitchOffGen.next),
        domain = domain,
        auxInfo = SignalInfoSet(SignalInfo.ModerationRules(rules))
      )
    )

  private def hoboSignal(reason: Reason,
                         rules: Set[RuleId],
                         switchedOn: Boolean = true,
                         isInherited: Boolean = false
                        ): Gen[Signal] =
    HoboSignalGen.map(
      _.copy(
        info = Some(rules.mkString(",")),
        source =
          AutomaticSourceGen.next.copy(
            application = Application.MODERATION_RULES,
            marker = markerGen(isInherited).next,
            originQualifier = None
          ),
        `type` = HoboCheckType.MODERATION_RULES,
        switchOff = if (switchedOn) None else Some(SignalSwitchOffGen.next),
        domain = domain,
        auxInfo = SignalInfoSet(SignalInfo.ModerationRules(rules))
      )
    )

  private def markerGen(isInherited: Boolean): Gen[SourceMarker] =
    if (isInherited)
      InheritedSourceMarkerGen
    else
      NoSourceMarkerGen
}
