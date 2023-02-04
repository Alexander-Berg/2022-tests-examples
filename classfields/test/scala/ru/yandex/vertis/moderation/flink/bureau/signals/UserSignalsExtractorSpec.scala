package ru.yandex.vertis.moderation.flink.bureau.signals

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.TestUtils.asScalaFlatMap
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{Diff, UpdateJournalRecord}
import ru.yandex.vertis.moderation.model.meta.{SignalsSummary, TimedCounter}
import ru.yandex.vertis.moderation.model.signal.HoboSignal.Result
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Signal.SignalType
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Reason, Service}
import ru.yandex.vertis.moderation.util.DateTimeUtil.OrderedDateTime
import ru.yandex.vertis.moderation.util.{DateTimeUtil, Interval}

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class UserSignalsExtractorSpec extends SpecBase {

  import UserSignalsExtractorSpec._

  private val service = Service.AUTORU
  private val ownerService = Service.USERS_AUTORU
  private val domain = Domain.default(service)

  def extractor(signalFilter: Signal => Boolean): UpdateJournalRecord => Iterable[UserSignalsStatistics] =
    new UserSignalsExtractor(ownerService, signalFilter, signalKey = _.signalType.toString)

  private val testCases =
    Seq(
      TestCase(
        description = "correctly extract signals summary",
        signalFilter = _.signalType == SignalType.BAN,
        signals =
          SignalSet(
            banSignal(DetailedReason.AdOnPhoto),
            banSignal(DetailedReason.AdOnPhoto, Application.COMPLAINTS),
            banSignal(DetailedReason.WrongPartsRules)
          ),
        prevSignals =
          SignalSet(
            banSignal(DetailedReason.AdOnPhoto, switchedOn = false),
            banSignal(DetailedReason.WrongPartsRules),
            banSignal(DetailedReason.ContactsInName)
          ),
        expectedResult =
          timestamp =>
            Map(
              SignalsSummary.AutoruQualifier(SignalType.BAN, Reason.AD_ON_PHOTO) -> TimedCounter(
                1,
                Some(Interval(timestamp, timestamp))
              ),
              SignalsSummary.AutoruQualifier(SignalType.BAN, Reason.CONTACTS_IN_NAME) -> TimedCounter(-1, None)
            )
      ),
      TestCase(
        description = "ignore signals with unrelated types",
        signalFilter = _.signalType == SignalType.WARN,
        signals =
          SignalSet(
            banSignal(DetailedReason.AdOnPhoto),
            banSignal(DetailedReason.AdOnPhoto, Application.COMPLAINTS),
            banSignal(DetailedReason.WrongPartsRules)
          ),
        prevSignals =
          SignalSet(
            banSignal(DetailedReason.AdOnPhoto, switchedOn = false),
            banSignal(DetailedReason.WrongPartsRules),
            banSignal(DetailedReason.ContactsInName)
          ),
        expectedResult = _ => Map.empty
      ),
      TestCase(
        description = "correctly extract hobo",
        signalFilter = {
          case HoboSignal(_, _, _, _, _, _, Result.Bad(_, _), _, _, _, _, _, _, _, _, _, _, _) => true
          case _                                                                               => false
        },
        signals =
          SignalSet(
            hoboSignal(Result.Bad(Set(DetailedReason.AdOnPhoto, DetailedReason.AgencyAddressDesc), None)),
            hoboSignal(Result.Warn(Set(DetailedReason.AlreadyBooked), None), checkType = HoboCheckType.CHAT_CHECK)
          ),
        prevSignals =
          SignalSet(
            hoboSignal(Result.Bad(Set(DetailedReason.AgencyAddressDesc), None))
          ),
        expectedResult =
          timestamp =>
            Map(
              SignalsSummary.AutoruQualifier(SignalType.HOBO, Reason.AD_ON_PHOTO) -> TimedCounter(
                1,
                Some(Interval(timestamp, timestamp))
              )
            )
      ),
      TestCase(
        description = "ignore inherited if has such filter",
        signalFilter = !_.isInherited,
        signals =
          SignalSet(
            banSignal(inherited = true)
          ),
        prevSignals = SignalSet.Empty,
        expectedResult = _ => Map.empty
      ),
      TestCase(
        description = "ignore prev switched off signals if they are not among new signals",
        signalFilter = _.signalType == SignalType.BAN,
        signals = SignalSet(),
        prevSignals =
          SignalSet(
            banSignal(DetailedReason.AdOnPhoto, switchedOn = false)
          ),
        expectedResult = _ => Map.empty
      )
    )

  "extractor" should {
    testCases.foreach { case TestCase(description, signals, prevSignals, signalTypes, expectedResultFunc, timestamp) =>
      description in {
        val updateJournalRecord = getUpdateJournalRecord(signals, prevSignals, service, timestamp)
        val expectedResult = expectedResultFunc(timestamp)
        val actualResult: Iterable[UserSignalsStatistics] = extractor(signalTypes)(updateJournalRecord)
        if (expectedResult.isEmpty) {
          actualResult should be(empty)
        } else {
          actualResult.size shouldBe 1
          val userStatistics = actualResult.head
          userStatistics.user shouldBe updateJournalRecord.instance.externalId.user
          userStatistics.statistics.signals shouldBe expectedResult
        }
      }
    }
  }

  private def getUpdateJournalRecord(signals: SignalSet,
                                     prevSignals: SignalSet,
                                     service: Service,
                                     timestamp: DateTime
                                    ): UpdateJournalRecord = {
    val externalId = ExternalIdGen.next.copy(user = AutoruUserGen.next)
    val essentials = essentialsGen(service).next
    val instance = instanceGen(externalId).next.copy(signals = signals, essentials = essentials)
    UpdateJournalRecord.withInitialDepth(
      prev = Some(instance.copy(signals = prevSignals)),
      instance = instance,
      timestamp = timestamp,
      diff =
        if (signals == prevSignals)
          Diff.empty(service)
        else
          Diff.signals(service)
    )
  }

  private def banSignal(detailedReason: DetailedReason = DetailedReasonGen.next,
                        application: Application = Application.MODERATION,
                        switchedOn: Boolean = true,
                        inherited: Boolean = false
                       ): Signal =
    BanSignalGen
      .map(
        _.copy(
          detailedReason = detailedReason,
          switchOff = if (switchedOn) None else Some(SignalSwitchOffGen.next),
          domain = domain,
          source =
            AutomaticSourceGen.next.copy(
              application = application,
              marker = if (inherited) InheritedSourceMarkerGen.next else NoMarker
            )
        )
      )
      .next

  private def warnSignal(detailedReason: DetailedReason,
                         application: Application = Application.MODERATION,
                         switchedOn: Boolean = true
                        ): Signal =
    WarnSignalGen
      .map(
        _.copy(
          detailedReason = detailedReason,
          source =
            AutomaticSourceGen.next.copy(
              application = application,
              marker = NoMarker
            ),
          switchOff = if (switchedOn) None else Some(SignalSwitchOffGen.next),
          domain = domain
        )
      )
      .next

  private def hoboSignal(taskResult: Result,
                         application: Application = Application.MODERATION,
                         switchedOn: Boolean = true,
                         checkType: HoboCheckType = HoboCheckType.CALL_CENTER
                        ): HoboSignal =
    HoboSignalGen
      .map(
        _.copy(
          source =
            AutomaticSourceGen.next.copy(
              application = application,
              marker = NoMarker
            ),
          switchOff = if (switchedOn) None else Some(SignalSwitchOffGen.next),
          domain = domain,
          result = taskResult,
          `type` = checkType
        )
      )
      .next

}

object UserSignalsExtractorSpec {

  case class TestCase(description: String,
                      signals: SignalSet,
                      prevSignals: SignalSet,
                      signalFilter: Signal => Boolean,
                      expectedResult: DateTime => Map[SignalsSummary.Qualifier, TimedCounter],
                      timestamp: DateTime = DateTimeUtil.now()
                     )

}
