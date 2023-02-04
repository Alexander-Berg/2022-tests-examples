package ru.yandex.vertis.moderation.model.defaultopinioncalculator

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.{Globals, OpinionCalculatorSpecBase}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, Opinion}
import ru.yandex.vertis.moderation.model.Opinion.{Failed, Ok, Unknown, UsersRealty}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.opinion.{DefaultOpinionCalculator, OpinionCalculator}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.util.DetailedReasonUtil.isUserResellerDetailedReason

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class UsersRealtyDefaultOpinionCalculatorSpec extends OpinionCalculatorSpecBase {

  override protected def service: Model.Service = Model.Service.USERS_REALTY

  override protected def calculator: OpinionCalculator = Globals.opinionCalculator(service)

  private def ok(warnReasons: Set[DetailedReason] = Set.empty, isReseller: Boolean = false) =
    Ok(warnReasons, Some(Opinion.UsersRealty(isReseller)))

  private def unknown(isReseller: Boolean = false, warnReasons: Set[DetailedReason] = Set.empty) =
    Unknown(warnReasons, Some(Opinion.UsersRealty(isReseller)))

  private def failed(detailedReasons: Set[DetailedReason],
                     isReseller: Boolean = false,
                     warnReasons: Set[DetailedReason] = Set.empty
                    ) = Failed(detailedReasons, warnReasons, Some(Opinion.UsersRealty(isReseller)))

  "calculator" should {

    "ignore signals with switch off" in {
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
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
      val expectedResult = getExpected(domain -> unknown())
      actualResult shouldBe expectedResult
    }

    "return Unknown if no signals" in {
      val domain = nextDomain()
      val instance = nextInstance(SignalSet.Empty)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> unknown())
      actualResult shouldBe expectedResult
    }

    "return Failed for manual BanSignal" in {
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
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
      val expectedOpinion = failed(Set(detailedReason))
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
      val expectedResult = getExpected(domain -> ok())
      actualResult shouldBe expectedResult
    }

    "return Failed for manual UnbanSignal than BanSignal" in {
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = NoMarker)
      val unbanSignal =
        UnbanSignal(
          domain = domain,
          source = source,
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
      val expectedOpinion = failed(Set(detailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Ok for manual BanSignal than UnbanSignal" in {
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
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
      val expectedResult = getExpected(domain -> ok())
      actualResult shouldBe expectedResult
    }

    "return Failed for automatic INDEXER BanSignal" in {
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val domain = nextDomain()
      val source = AutomaticSource(Application.INDEXER, None, NoMarker)
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
      val expectedOpinion = failed(Set(detailedReason))
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
      val expectedResult = getExpected(domain -> unknown())
      actualResult shouldBe expectedResult
    }

    "return Failed for HoboSignal with bad result" in {
      val source = SourceGen.suchThat(_.marker == NoMarker).next
      val domain = nextDomain()
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
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
      val expectedOpinion = failed(Set(detailedReason))
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
      val expectedOpinion = Failed(Set(detailedReason), Set.empty, Some(UsersRealty(false)))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed with correct details for RESELLER" in {
      val source = SourceGen.suchThat(_.marker == NoMarker).next
      val domain = nextDomain()
      val signal =
        BanSignalGen.next.copy(
          domain = nextDomain(),
          source = ManualSourceGen.next.copy(marker = NoMarker),
          detailedReason = DetailedReason.UserReseller(None, Seq.empty),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(Set(DetailedReason.UserReseller(None, Seq.empty)), isReseller = true)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    val punisherInfectionDetailedReasons =
      Seq(
        DetailedReason.UserFraud,
        DetailedReason.UserInfo,
        DetailedReason.UserSelect,
        DetailedReason.PhotoSteal
      )
    punisherInfectionDetailedReasons.foreach { detailedReason =>
      s"return Failed for automatic punisher BanSignal with reason $detailedReason" in {
        val source = AutomaticSource(Application.PUNISHER, marker = NoMarker)
        val signal =
          BanSignalGen.next.copy(
            domain = Domain.UsersRealty.default,
            source = source,
            detailedReason = detailedReason,
            switchOff = None
          )
        val instance = nextInstance(SignalSet(signal))
        val actualResult = calculator(instance)
        val expectedOpinion = failed(Set(detailedReason))
        val expectedResult = getExpected(Domain.UsersRealty.default -> expectedOpinion)
        actualResult shouldBe expectedResult
      }
    }

    val unknownDetailedReasons =
      DetailedReasonGen.suchThat(!punisherInfectionDetailedReasons.contains(_)).next(10).toSet
    unknownDetailedReasons.foreach { detailedReason =>
      s"return ban for automatic punisher BanSignal with any reason, reason: $detailedReason" in {
        val source = AutomaticSource(Application.PUNISHER, marker = NoMarker)
        val signal =
          BanSignalGen.next.copy(
            domain = Domain.UsersRealty.default,
            source = source,
            detailedReason = detailedReason,
            switchOff = None
          )
        val reseller = isUserResellerDetailedReason(detailedReason)
        val instance = nextInstance(SignalSet(signal))
        val actualResult = calculator(instance)
        val expectedOpinion = failed(Set(detailedReason), isReseller = reseller)
        val expectedResult = getExpected(Domain.UsersRealty.default -> expectedOpinion)
        actualResult shouldBe expectedResult
      }
    }

    case class DetailsTestCase(description: String, signal: Signal, expectedIsReseller: Boolean)

    val DetailsCases: Seq[DetailsTestCase] =
      Seq(
        DetailsTestCase(
          "be reseller with active WarnSignal(PUNISHER, USER_RESELLER)",
          WarnSignalGen.next.copy(
            domain = nextDomain(),
            source = AutomaticSource(Application.PUNISHER, marker = NoMarker),
            detailedReason = DetailedReason.UserReseller(None, Seq.empty),
            switchOff = None
          ),
          expectedIsReseller = true
        ),
        DetailsTestCase(
          "be reseller with active HoboSignal.Warn(USER_RESELLER)",
          HoboSignalGen.next
            .copy(
              domain = nextDomain(),
              result = HoboSignal.Result.Warn(Set(DetailedReason.UserReseller(None, Seq.empty)), None),
              switchOff = None
            )
            .withMarker(NoMarker),
          expectedIsReseller = true
        ),
        DetailsTestCase(
          "be reseller with active HoboSignal.Warn(USER_RESELLER, ...)",
          HoboSignalGen.next
            .copy(
              domain = nextDomain(),
              result =
                HoboSignal.Result.Warn(Set(DetailedReason.UserReseller(None, Seq.empty), DetailedReason.Another), None),
              switchOff = None
            )
            .withMarker(NoMarker),
          expectedIsReseller = true
        ),
        DetailsTestCase(
          "be non-reseller with HoboSignal.Warn(!USER_RESELLER)",
          HoboSignalGen.next
            .copy(
              domain = nextDomain(),
              result = HoboSignal.Result.Warn(Set(DetailedReason.Another), None),
              switchOff = None
            )
            .withMarker(NoMarker),
          expectedIsReseller = false
        ),
        DetailsTestCase(
          "be reseller with manual WarnSignal(USER_RESELLER)",
          WarnSignalGen.next.copy(
            domain = nextDomain(),
            source = ManualSource("1", marker = NoMarker),
            detailedReason = DetailedReason.UserReseller(None, Seq.empty),
            switchOff = None
          ),
          expectedIsReseller = true
        ),
        DetailsTestCase(
          "be non-reseller with disabled WarnSignal(PUNISHER, USER_RESELLER)",
          WarnSignalGen.next.copy(
            domain = nextDomain(),
            source = AutomaticSource(Application.PUNISHER, marker = NoMarker),
            detailedReason = DetailedReason.UserReseller(None, Seq.empty),
            switchOff = Some(SignalSwitchOff(ManualSource("1"), DateTimeUtil.now(), None, None))
          ),
          expectedIsReseller = false
        )
      )
    DetailsCases.foreach { case DetailsTestCase(description, signal, expectedIsReseller) =>
      s"return Unknown and $description" in {
        val domain = nextDomain()
        val signals = SignalSet(signal)
        val instance = nextInstance(signals)

        val actualResult = calculator(instance)
        val expectedWarns = Set(signal).filter(_.switchOff.isEmpty).flatMap(_.getDetailedReasons)
        val expectedOpinion = unknown(isReseller = expectedIsReseller, warnReasons = expectedWarns)
        val expectedResult = getExpected(domain -> expectedOpinion)
        actualResult shouldBe expectedResult
      }
    }
  }
}
