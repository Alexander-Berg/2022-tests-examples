package ru.yandex.vertis.moderation.model.defaultopinioncalculator

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.Opinion.{Failed, Ok, Unknown}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.RealtyEssentials
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, Opinion}
import ru.yandex.vertis.moderation.opinion.OpinionCalculator
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.util.DetailedReasonUtil.isUserResellerDetailedReason
import ru.yandex.vertis.moderation.{Globals, OpinionCalculatorSpecBase}

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class RealtyDefaultOpinionCalculatorSpec extends OpinionCalculatorSpecBase {

  override protected def service: Model.Service = Model.Service.REALTY

  override protected def calculator: OpinionCalculator = Globals.opinionCalculator(service)

  private def ok(warnReasons: Set[DetailedReason] = Set.empty, isBanByInheritance: Boolean = false) =
    Ok(warnReasons, Some(Opinion.Realty(isBanByInheritance)))

  private def unknown(isBanByInheritance: Boolean = false, warnReasons: Set[DetailedReason] = Set.empty) =
    Unknown(warnReasons, Some(Opinion.Realty(isBanByInheritance)))

  private def failed(detailedReasons: Set[DetailedReason],
                     isBanByInheritance: Boolean = false,
                     warnReasons: Set[DetailedReason] = Set.empty
                    ) = Failed(detailedReasons, warnReasons, Some(Opinion.Realty(isBanByInheritance)))

  "calculator" should {
    "return Failed for inherited HoboSignal with Result.Ban" in {
      val domain = nextDomain()
      val source = SourceGen.next.withMarker(Inherited(Service.USERS_REALTY))
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val signal =
        HoboSignalGen.next.copy(
          domain = Domain.UsersRealty(Model.Domain.UsersRealty.DEFAULT_USERS_REALTY),
          source = source,
          result = HoboSignal.Result.Bad(Set(detailedReason), None),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)
      val actualResult = calculator(instance)
      val expectedOpinion =
        failed(
          detailedReasons = Set(detailedReason),
          isBanByInheritance = true
        )
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Unknown for IndexErrorSignal" in {
      val detailedReasons = DetailedReasonGen.next(2).toSet
      val domain = nextDomain()
      val signal = IndexErrorSignalGen.next.copy(domain = domain, detailedReasons = detailedReasons)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> unknown())
      actualResult shouldBe expectedResult
    }

    "return Ban without reason from IndexErrorSignal" in {
      val domain = nextDomain()

      val indexErrorDetailedReason = DetailedReasonGen.next
      val indexErrorSignal =
        IndexErrorSignalGen.withoutSwitchOff.next.copy(
          domain = domain,
          detailedReasons = Set(indexErrorDetailedReason),
          source = AutomaticSource(Application.INDEXER, marker = NoMarker)
        )

      val banDetailedReason = DetailedReasonGen.suchThat(_ != indexErrorDetailedReason).next
      val banSource = ManualSourceGen.next.copy(marker = NoMarker)
      val banSignal =
        BanSignalGen.withoutSwitchOff.next.copy(
          domain = domain,
          source = banSource,
          detailedReason = banDetailedReason
        )

      val signals = SignalSet(indexErrorSignal, banSignal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(Set(banDetailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)

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
      val expectedOpinion = failed(Set(detailedReason))
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
      val expectedResult = getExpected(domain -> ok())
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
      val expectedOpinion = failed(Set(detailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for complaints BanSignal" in {
      val domain = nextDomain()
      val detailedReason = DetailedReasonGen.next
      val source = AutomaticSource(Application.COMPLAINTS)
      val signal =
        BanSignalGen.next.copy(domain = domain, detailedReason = detailedReason, source = source, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(Set(detailedReason))
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

    Seq(
      DetailedReason.AdOnPhoto,
      DetailedReason.PhoneInDesc(Set.empty),
      DetailedReason.DamagedPhoto,
      DetailedReason.Stopword,
      DetailedReason.TextOnPhotoFromCV(Seq.empty),
      DetailedReason.UserFraud,
      DetailedReason.VpnValidation
    ).foreach { detailedReason =>
      s"return Failed for moderation BanSignal with reason ${detailedReason.reason.name}" in {
        val domain = nextDomain()
        val source = AutomaticSource(Application.MODERATION)
        val signal =
          BanSignalGen.next.copy(domain = domain, detailedReason = detailedReason, source = source, switchOff = None)
        val signals = SignalSet(signal)
        val instance = nextInstance(signals)

        val actualResult = calculator(instance)
        val expectedOpinion = failed(Set(detailedReason))
        val expectedResult = getExpected(domain -> expectedOpinion)
        actualResult shouldBe expectedResult
      }
    }

    "return Failed for MODERATION_RULES BanSignal with any reason" in {
      val domain = nextDomain()
      val detailedReason = DetailedReasonGen.next
      val source = AutomaticSource(Application.MODERATION_RULES)
      val signal =
        BanSignalGen.next.copy(domain = domain, detailedReason = detailedReason, source = source, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(Set(detailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited manual BanSignal" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_REALTY))
      val detailedReason = DetailedReasonGen.next
      val signal =
        BanSignalGen.next.copy(
          domain = nextDomain(),
          source = source,
          detailedReason = detailedReason,
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)
      val actualResult = calculator(instance)
      val expectedOpinion = failed(Set(detailedReason), isBanByInheritance = true)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited automatic indexer BanSignal" in {
      val domain = nextDomain()
      val source = AutomaticSource(Application.INDEXER, marker = Inherited(Service.USERS_REALTY))
      val detailedReason = DetailedReasonGen.next
      val signal =
        BanSignalGen.next.copy(
          domain = nextDomain(),
          source = source,
          detailedReason = detailedReason,
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)
      val actualResult = calculator(instance)
      val expectedOpinion = failed(Set(detailedReason), isBanByInheritance = true)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    val DefaultDetails = Some(Opinion.Realty(isBanByInheritance = false))

    case class DetailsTestCase(essentials: RealtyEssentials, signals: SignalSet, expected: Option[Opinion.Details])

    val DetailsTestCases =
      Seq(
        DetailsTestCase(essentials = RealtyEssentialsGen.next, signals = SignalSet(), expected = DefaultDetails),
        DetailsTestCase(
          essentials = RealtyEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              )
            ),
          expected = DefaultDetails
        ),
        DetailsTestCase(
          essentials = RealtyEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = AutomaticSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              )
            ),
          expected = DefaultDetails
        ),
        DetailsTestCase(
          essentials = RealtyEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = Domain.UsersRealty.default,
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_REALTY)),
                detailedReason = DetailedReason.Another,
                switchOff = None
              )
            ),
          expected = DefaultDetails.map(_.copy(isBanByInheritance = true))
        ),
        DetailsTestCase(
          essentials = RealtyEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = Domain.UsersRealty.default,
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_REALTY)),
                detailedReason = DetailedReason.Another,
                switchOff = Some(SignalSwitchOff(ManualSourceGen.next, DateTime.now, None, None))
              )
            ),
          expected = DefaultDetails
        ),
        DetailsTestCase(
          essentials = RealtyEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = Domain.UsersRealty.default,
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReason.Another,
                switchOff = None
              )
            ),
          expected = DefaultDetails
        ),
        DetailsTestCase(
          essentials = RealtyEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              ),
              BanSignalGen.next.copy(
                domain = Domain.UsersRealty.default,
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_REALTY)),
                detailedReason = DetailedReason.Another,
                switchOff = None
              )
            ),
          expected = DefaultDetails.map(_.copy(isBanByInheritance = true))
        ),
        DetailsTestCase(
          essentials = RealtyEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              ),
              BanSignalGen.next.copy(
                domain = Domain.UsersRealty.default,
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_REALTY)),
                detailedReason = DetailedReason.UserReseller(None, Seq.empty),
                switchOff = Some(SignalSwitchOff(ManualSourceGen.next, DateTime.now, None, None))
              )
            ),
          expected = DefaultDetails
        )
      )

    DetailsTestCases.zipWithIndex.foreach { case (DetailsTestCase(essentials, signals, details), i) =>
      s"be correct on details case $i" in {
        val domain = nextDomain()
        val instance = nextInstance(signals).copy(essentials = essentials)
        calculator(instance)(domain).details shouldBe details
      }
    }

    val punisherInfectionReasons =
      Seq(
        DetailedReason.UserFraud,
        DetailedReason.UserInfo,
        DetailedReason.UserSelect,
        DetailedReason.PhotoSteal
      )
    punisherInfectionReasons.foreach { detailedReason =>
      s"return Failed for inherited automatic punisher BanSignal with reason $detailedReason" in {
        val source = AutomaticSource(Application.PUNISHER, marker = Inherited(Service.USERS_REALTY))
        val signal =
          BanSignalGen.next.copy(
            domain = Domain.Realty.default,
            source = source,
            detailedReason = detailedReason,
            switchOff = None
          )
        val instance = nextInstance(SignalSet(signal))
        val actualResult = calculator(instance)
        val expectedOpinion = failed(Set(detailedReason), isBanByInheritance = true)
        val expectedResult = getExpected(Domain.Realty.default -> expectedOpinion)
        actualResult shouldBe expectedResult
      }
    }

    val unknowReasons = DetailedReasonGen.suchThat(!punisherInfectionReasons.contains(_)).next(10).toSet
    unknowReasons.foreach { reason =>
      s"return ban for inherited automatic punisher BanSignal with any reason, reason: $reason" in {
        val source = AutomaticSource(Application.PUNISHER, marker = Inherited(Service.USERS_REALTY))
        val signal =
          BanSignalGen.next.copy(
            domain = Domain.Realty.default,
            source = source,
            detailedReason = reason,
            switchOff = None
          )
        val instance = nextInstance(SignalSet(signal))
        val actualResult = calculator(instance)
        val expectedOpinion = failed(Set(reason), isBanByInheritance = true)
        val expectedResult = getExpected(Domain.Realty.default -> expectedOpinion)
        actualResult shouldBe expectedResult
      }
    }
  }
}
