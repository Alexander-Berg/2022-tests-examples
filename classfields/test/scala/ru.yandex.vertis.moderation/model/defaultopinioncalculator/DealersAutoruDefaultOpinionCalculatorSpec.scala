package ru.yandex.vertis.moderation.model.defaultopinioncalculator

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.hobo.proto.Model.QueueId
import ru.yandex.vertis.moderation.{Globals, OpinionCalculatorSpecBase}
import ru.yandex.vertis.moderation.model.{DetailedReason, Opinion}
import ru.yandex.vertis.moderation.model.Opinion.{Failed, Ok, Unknown}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.instance.Instance
import ru.yandex.vertis.moderation.model.signal.HoboSignal.Result
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.opinion.OpinionCalculator
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.{HoboCheckType, Service}
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class DealersAutoruDefaultOpinionCalculatorSpec extends OpinionCalculatorSpecBase {
  override protected def service: Service = Service.DEALERS_AUTORU

  override protected def calculator: OpinionCalculator = Globals.opinionCalculator(service)

  private def getDealersDetails(instance: Instance): Option[Opinion.DealersAutoru] =
    instance.essentials.getEventId.map(id => Opinion.DealersAutoru(Some(id)))

  "calculator" should {

    "ignore signals with switch off" in {
      val detailedReason = DetailedReasonGen.next
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = NoMarker)
      val signal =
        BanSignal(
          domain = domain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          detailedReason = detailedReason,
          switchOff = Some(SignalSwitchOffGen.next),
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Unknown(Set.empty, getDealersDetails(instance)))
      actualResult shouldBe expectedResult
    }

    "return Unknown if no signals" in {
      val domain = nextDomain()
      val instance = nextInstance(SignalSet.Empty)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Unknown(Set.empty, getDealersDetails(instance)))
      actualResult shouldBe expectedResult
    }

    "return Failed for manual BanSignal" in {
      val detailedReason = DetailedReasonGen.next
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = NoMarker)
      val signal =
        BanSignal(
          domain = domain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          detailedReason = detailedReason,
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty, getDealersDetails(instance))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Ok for manual UnbanSignal" in {
      val domain = nextDomain()
      val signal =
        UnbanSignal(
          domain = domain,
          source = ManualSourceGen.next.copy(marker = NoMarker),
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Ok(Set.empty, getDealersDetails(instance)))
      actualResult shouldBe expectedResult
    }

    "return Failed for manual UnbanSignal than BanSignal" in {
      val detailedReason = DetailedReasonGen.next
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = NoMarker)
      val unbanSignal =
        UnbanSignal(
          domain = domain,
          source = ManualSourceGen.next.copy(marker = NoMarker),
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val banSignal =
        BanSignal(
          domain = domain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(2L),
          info = None,
          detailedReason = detailedReason,
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signals = SignalSet(unbanSignal, banSignal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty, getDealersDetails(instance))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Ok for manual BanSignal than UnbanSignal" in {
      val detailedReason = DetailedReasonGen.next
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = NoMarker)
      val banSignal =
        BanSignal(
          domain = domain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          detailedReason = detailedReason,
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val unbanSignal =
        UnbanSignal(
          domain = domain,
          source = ManualSourceGen.next.copy(marker = NoMarker),
          timestamp = DateTimeUtil.fromMillis(2L),
          info = None,
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signals = SignalSet(banSignal, unbanSignal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Ok(Set.empty, getDealersDetails(instance)))
      actualResult shouldBe expectedResult
    }

    "return Failed for IndexerErrorSignal with reason = VERIFICATION" in {
      val detailedReason = DetailedReason.NotVerified
      val source = AutomaticSource(Application.INDEXER)
      val domain = nextDomain()
      val signal =
        IndexErrorSignal(
          domain = domain,
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          source = source,
          detailedReasons = Set(detailedReason),
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty, getDealersDetails(instance))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for HoboSignal with Result.Bad(Set(DetailedReason.UserBanned))" in {
      val source = AutomaticSource(Application.HOBO)
      val detailedReason = DetailedReason.UserBanned
      val domain = nextDomain()
      val signal =
        HoboSignalGen.next.copy(
          domain = domain,
          source = source,
          `type` = HoboCheckType.PREMODERATION_DEALER,
          task = Some(HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_PREMODERATION_DEALER.toString)),
          result = Result.Bad(Set(detailedReason), None),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty, getDealersDetails(instance))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for HoboSignal with Result.Bad(Set(DetailedReason.NotVerified))" in {
      val source = AutomaticSource(Application.HOBO)
      val detailedReason = DetailedReason.NotVerified
      val domain = nextDomain()
      val signal =
        HoboSignalGen.next.copy(
          domain = domain,
          source = source,
          `type` = HoboCheckType.PREMODERATION_DEALER,
          task = Some(HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_PREMODERATION_DEALER.toString)),
          result = Result.Warn(Set(detailedReason), None),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Unknown(Set(detailedReason), getDealersDetails(instance))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Unknown(Set.empty) for HoboSignal with Result.Good" in {
      val source = AutomaticSource(Application.HOBO)
      val domain = nextDomain()
      val signal =
        HoboSignalGen.next.copy(
          domain = domain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(1L),
          `type` = HoboCheckType.PREMODERATION_DEALER,
          task = Some(HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_PREMODERATION_DEALER.toString)),
          result = Result.Good(None),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Unknown(Set.empty, getDealersDetails(instance)))
      actualResult shouldBe expectedResult
    }

    "return Failed for if good signal is after bad and bad signal is on" in {
      val source1 = AutomaticSource(Application.INDEXER)
      val source2 = AutomaticSource(Application.HOBO)
      val detailedReason = DetailedReason.NotVerified
      val domain = nextDomain()
      val signal1 =
        IndexErrorSignal(
          domain = domain,
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          source = source1,
          detailedReasons = Set(detailedReason),
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signal2 =
        HoboSignalGen.next.copy(
          domain = domain,
          source = source2,
          timestamp = DateTimeUtil.fromMillis(3L),
          `type` = HoboCheckType.PREMODERATION_DEALER,
          task = Some(HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_PREMODERATION_DEALER.toString)),
          result = Result.Good(None),
          switchOff = None
        )
      val signals = SignalSet(signal1, signal2)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty, getDealersDetails(instance))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for if good signal is after bad and bad signal is off" in {
      val source1 = AutomaticSource(Application.INDEXER)
      val source2 = AutomaticSource(Application.HOBO)
      val detailedReason = DetailedReason.NotVerified
      val domain = nextDomain()
      val signal1 =
        IndexErrorSignal(
          domain = domain,
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          source = source1,
          detailedReasons = Set(detailedReason),
          switchOff = Some(SignalSwitchOff(source1, DateTimeUtil.fromMillis(2L), None, None)),
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signal2 =
        HoboSignalGen.next.copy(
          domain = domain,
          source = source2,
          timestamp = DateTimeUtil.fromMillis(3L),
          `type` = HoboCheckType.PREMODERATION_DEALER,
          task = Some(HoboSignalTaskGen.next.copy(queue = QueueId.AUTO_RU_PREMODERATION_DEALER.toString)),
          result = Result.Good(None),
          switchOff = None
        )
      val signals = SignalSet(signal1, signal2)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Unknown(Set.empty, getDealersDetails(instance)))
      actualResult shouldBe expectedResult
    }
  }
}
