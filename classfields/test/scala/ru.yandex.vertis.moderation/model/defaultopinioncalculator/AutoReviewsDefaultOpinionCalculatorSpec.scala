package ru.yandex.vertis.moderation.model.defaultopinioncalculator

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.{Globals, OpinionCalculatorSpecBase}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain}
import ru.yandex.vertis.moderation.model.Opinion.{Failed, Ok, Unknown}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.opinion.OpinionCalculator
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.proto.Model.Service.AUTO_REVIEWS
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.util.DetailedReasonUtil.isUserResellerDetailedReason

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class AutoReviewsDefaultOpinionCalculatorSpec extends OpinionCalculatorSpecBase {
  override protected def service: Service = AUTO_REVIEWS

  override protected def calculator: OpinionCalculator = Globals.opinionCalculator(service)

  "calculator" should {

    "return Failed for manual BanSignal" in {
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
      val warnSignalReason = DetailedReasonGen.next
      val warnSignal =
        WarnSignal(
          domain = domain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          detailedReason = warnSignalReason,
          weight = 1,
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signals = SignalSet(banSignal, warnSignal)
      val instance = nextInstance(signals)
      val actualResult = calculator(instance)
      val expectedOpinion =
        Failed(
          detailedReasons = Set(detailedReason),
          warnDetailedReasons = Set(warnSignalReason)
        )
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
      val expectedOpinion = Failed(Set(detailedReason), Set.empty)
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
      val expectedResult = getExpected(domain -> Ok(Set.empty))
      actualResult shouldBe expectedResult
    }

    "return Ok after HoboSignal with good result" in {
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
      val expectedResult = getExpected(domain -> Ok(Set.empty))
      actualResult shouldBe expectedResult
    }

    "return Failed for HoboSignal with bad result" in {
      val source = SourceGen.suchThat(_.marker == NoMarker).next
      val domain = nextDomain()
      val detailedReason = DetailedReasonGen.next
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

    "return Unknown if no signals" in {
      val domain = nextDomain()
      val instance = nextInstance(SignalSet.Empty)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Unknown(Set.empty))
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited manual ban in REVIEWS domain" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU))
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.REVIEWS)
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Failed(Set(detailedReason), Set.empty)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Unknown for inherited manual ban in not REVIEWS domain" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU))
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.CARS)
      val detailedReason =
        DetailedReasonGen
          .suchThat(reason => !Globals.universalReasons(Service.AUTO_REVIEWS).contains(reason))
          .next
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> Unknown(Set.empty))
      actualResult shouldBe expectedResult
    }

    "return Unknown with extracted warn reasons" in {
      val source = SourceGen.suchThat(_.marker == NoMarker).next
      val domain = nextDomain()
      val detailedReason = DetailedReasonGen.next
      val signal =
        HoboSignalGen.next.copy(
          domain = domain,
          result = HoboSignal.Result.Warn(Set(detailedReason), None),
          source = source,
          switchOff = None
        )
      val warnSignalReason = DetailedReasonGen.next
      val warnSignal =
        WarnSignal(
          domain = domain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(1L),
          info = None,
          detailedReason = warnSignalReason,
          weight = 1,
          switchOff = None,
          ttl = None,
          outerComment = None,
          auxInfo = SignalInfoSet.Empty
        )
      val signals = SignalSet(signal, warnSignal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = Unknown(Set(detailedReason, warnSignalReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    Seq(DetailedReason.Stopword, DetailedReason.PhoneInDesc(Set.empty)).foreach { detailedReason =>
      s"return Failed for moderation BanSignal with reason $detailedReason" in {
        val domain = nextDomain()
        val source = AutomaticSource(Application.MODERATION)
        val signal =
          BanSignalGen.next.copy(domain = domain, detailedReason = detailedReason, source = source, switchOff = None)
        val signals = SignalSet(signal)
        val instance = nextInstance(signals)

        val actualResult = calculator(instance)
        val expectedOpinion = Failed(Set(detailedReason), Set.empty)
        val expectedResult = getExpected(domain -> expectedOpinion)
        actualResult should be(expectedResult)
      }
    }
  }
}
