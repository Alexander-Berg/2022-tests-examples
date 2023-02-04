package ru.yandex.vertis.moderation.model.defaultopinioncalculator

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.{Globals, OpinionCalculatorSpecBase}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.model.Opinion.{Failed, Ok, Unknown}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.opinion.{DefaultOpinionCalculator, OpinionCalculator}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.model.defaultopinioncalculator.UsersAutoruDefaultOpinionCalculatorSpec.{
  DetailedReasonGenNotForReplicate,
  DetailedReasonsForReplicate
}
import ru.yandex.vertis.moderation.util.CollectionUtil._

/**
  * Specs for [[DefaultOpinionCalculator]] related to USERS_AUTORU
  *
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class UsersAutoruDefaultOpinionCalculatorSpec extends OpinionCalculatorSpecBase {

  override protected def service: Model.Service = Model.Service.USERS_AUTORU

  override protected def calculator: OpinionCalculator = Globals.opinionCalculator(service)

  "calculator" should {

    "return Failed for manual BanSignal" in {
      val detailedReason = DetailedReasonGenNotForReplicate.next
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
      val expectedOpinion = Failed(Set(detailedReason), Set.empty)
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
      val expectedResult = getExpected(domain -> Ok(Set.empty))
      actualResult shouldBe expectedResult
    }

    "return Failed for manual UnbanSignal than BanSignal" in {
      val detailedReason = DetailedReasonGenNotForReplicate.next
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
      val expectedOpinion = Failed(Set(detailedReason), Set.empty)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Ok for manual BanSignal than UnbanSignal" in {
      val detailedReason = DetailedReasonGenNotForReplicate.next
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
      val expectedResult = getExpected(domain -> Ok(Set.empty))
      actualResult shouldBe expectedResult
    }

    "return Failed for PUNISHER BanSignal with reason USER_RESELLER" in {
      val detailedReason = DetailedReason.UserReseller(None, Seq.empty)
      val domain = nextDomain()
      val source = AutomaticSource(Application.PUNISHER, marker = NoMarker)
      val signal =
        BanSignalGen.next.copy(domain = domain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for MODERATION_RULES decision with reason ANOTHER" in {
      val detailedReason = DetailedReason.Another
      val domain = nextDomain()
      val source = AutomaticSource(Application.MODERATION_RULES, marker = NoMarker)
      val signal =
        BanSignalGen.next.copy(domain = domain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Unknown after HoboSignal with good result" in {
      val source = SourceGen.suchThat(_.marker == NoMarker).next
      val domain = nextDomain()
      val signal =
        HoboSignalGen.next.copy(
          domain = domain,
          result = HoboSignal.Result.Good(None),
          source = source,
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Unknown(Set.empty))
      actualResult shouldBe expectedResult
    }

    "return Failed for HoboSignal with bad result" in {
      val source = SourceGen.suchThat(_.marker == NoMarker).next
      val domain = nextDomain()
      val detailedReason = DetailedReasonGenNotForReplicate.next
      val signal =
        HoboSignalGen.next.copy(
          domain = domain,
          result = HoboSignal.Result.Bad(Set(detailedReason), None),
          source = source,
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

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
      val expectedResult = getExpected(domain -> Unknown(Set.empty))
      actualResult shouldBe expectedResult
    }

    "return Unknown if no signals" in {
      val domain = nextDomain()
      val instance = nextInstance(SignalSet.Empty)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Unknown(Set.empty))
      actualResult shouldBe expectedResult
    }

    s"replicate $DetailedReasonsForReplicate reasons" in {
      DetailedReasonsForReplicate.foreach { detailedReason =>
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
        val expectedResult =
          getExpected(Domain.UsersAutoru.values.map {
            case d if d == domain => d -> Failed(Set(detailedReason), Set.empty)
            case d                => d -> Failed(Set(detailedReason), Set.empty)
          }.toSeq: _*)
        actualResult shouldBe expectedResult
      }
    }

    "return Failed for MODERATION BanSignal with reason USER_RESELLER" in {
      val detailedReason = DetailedReason.UserReseller(None, Seq.empty)
      val domain = nextDomain()
      val source = AutomaticSource(Application.MODERATION, marker = NoMarker)
      val signal =
        BanSignalGen.next.copy(domain = domain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }
  }
}

object UsersAutoruDefaultOpinionCalculatorSpec {

  val DetailedReasonsForReplicate: Set[DetailedReason] = Globals.universalReasons(Service.USERS_AUTORU)
  val DetailedReasonGenNotForReplicate: Gen[DetailedReason] =
    DetailedReasonGen.suchThat(DetailedReasonsForReplicate.notContains)
}
