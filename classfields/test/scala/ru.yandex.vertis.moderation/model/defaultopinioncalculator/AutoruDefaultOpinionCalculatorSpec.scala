package ru.yandex.vertis.moderation.model.defaultopinioncalculator

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.model.Opinion.{Failed, Ok, Unknown}
import ru.yandex.vertis.moderation.model.autoru.{ExperimentInfo, RichCategory}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.AutoruEssentials
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, Opinion}
import ru.yandex.vertis.moderation.opinion.OpinionCalculator
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Category
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil
import ru.yandex.vertis.moderation.util.DetailedReasonUtil._
import ru.yandex.vertis.moderation.{Globals, OpinionCalculatorSpecBase}

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class AutoruDefaultOpinionCalculatorSpec extends OpinionCalculatorSpecBase {

  override protected def service: Model.Service = Model.Service.AUTORU

  override protected def calculator: OpinionCalculator = Globals.opinionCalculator(service)

  private def ok(warnReasons: Set[DetailedReason] = Set.empty,
                 isBanByInheritance: Boolean = false,
                 isFromReseller: Boolean = false
                ) = Ok(warnReasons, Some(Opinion.Autoru(isBanByInheritance, isFromReseller)))

  private def unknown(isBanByInheritance: Boolean = false,
                      isFromReseller: Boolean = false,
                      warnReasons: Set[DetailedReason] = Set.empty
                     ) = Unknown(warnReasons, Some(Opinion.Autoru(isBanByInheritance, isFromReseller)))

  private def failed(detailedReasons: Set[DetailedReason],
                     isBanByInheritance: Boolean = false,
                     isFromReseller: Boolean = false,
                     warnReasons: Set[DetailedReason] = Set.empty
                    ) = Failed(detailedReasons, warnReasons, Some(Opinion.Autoru(isBanByInheritance, isFromReseller)))

  "calculator" should {
    "return Unknown for index error signal without reasons" in {
      val domain = nextDomain()
      val signal =
        IndexErrorSignalGen.next
          .copy(domain = domain, detailedReasons = Set.empty, switchOff = None)
          .withMarker(NoMarker)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> unknown())
      actualResult shouldBe expectedResult
    }

    "return Failed for index error signal with reasons" in {
      val domain = nextDomain()
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val signal =
        IndexErrorSignalGen.next
          .copy(domain = domain, detailedReasons = Set(detailedReason), switchOff = None)
          .withMarker(NoMarker)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(detailedReasons = Set(detailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
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
      val expectedOpinion = failed(detailedReasons = Set(detailedReason))
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
      val expectedOpinion = failed(detailedReasons = Set(detailedReason))
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

    "return Failed for automatic BanSignal from indexer" in {
      val domain = nextDomain()
      val signal =
        BanSignalGen
          .suchThat(!_.detailedReason.isInstanceOf[DetailedReason.UserReseller]) // filterPayedResellers
          .next
          .copy(
            domain = domain,
            source = AutomaticSource(Application.INDEXER),
            switchOff = None
          )
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(detailedReasons = Set(signal.detailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Ok for automatic UnbanSignal from indexer" in {
      val domain = nextDomain()
      val signal =
        UnbanSignalGen.next.copy(domain = domain, source = AutomaticSource(Application.INDEXER), switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> ok())
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
      val expectedOpinion = failed(detailedReasons = Set(detailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for complaints BanSignal" in {
      val domain = nextDomain()
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val source = AutomaticSource(Application.COMPLAINTS)
      val signal =
        BanSignalGen.next.copy(domain = domain, detailedReason = detailedReason, source = source, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(detailedReasons = Set(detailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited manual BanSignal" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category))
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(detailedReasons = Set(detailedReason), isBanByInheritance = true)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Unknown for inherited manual BanSignal (category != ownerDomain)" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU))
      val detailedReason =
        DetailedReasonGen
          .suchThat(r => !isUserResellerDetailedReason(r) && !Globals.universalReasons(service).contains(r))
          .next
      val category = CategoryGen.next
      val ownerDomain = DomainUsersAutoruGen.suchThat(_.value.toString != category.toString).next
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category))
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> unknown())
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited PUNISHER BanSignal with reason USER_RESELLER (isPlacedForFree == true)" in {
      val domain = nextDomain()
      val source = AutomaticSource(Application.PUNISHER, marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReason.UserReseller(None, Seq.empty)
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val essentials =
        AutoruEssentialsGen.next.copy(category = Some(category), isPlacedForFree = Some(true), experimentInfo = None)
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedOpinion =
        failed(
          detailedReasons = Set(detailedReason),
          isBanByInheritance = true,
          isFromReseller = true
        )
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Unknown for inherited PUNISHER BanSignal with reason USER_RESELLER (isPlacedForFree == false)" in {
      val domain = nextDomain()
      val source = AutomaticSource(Application.PUNISHER, marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReason.UserReseller(None, Seq.empty)
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category), isPlacedForFree = Some(false))
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedResult =
        getExpected(
          domain ->
            unknown(isBanByInheritance = false, isFromReseller = true)
        )
      actualResult shouldBe expectedResult
    }

    "return Unknown for inherited PUNISHER BanSignal with reason USER_RESELLER (isPlacedForFree == true, isProtectedReseller == true) " in {
      val domain = nextDomain()
      val source = AutomaticSource(Application.PUNISHER, marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReason.UserReseller(None, Seq.empty)
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val essentials =
        AutoruEssentialsGen.next.copy(
          category = Some(category),
          isPlacedForFree = Some(true),
          experimentInfo = Some(ExperimentInfo(Some(true)))
        )
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedResult =
        getExpected(
          domain ->
            unknown(isBanByInheritance = false, isFromReseller = true)
        )
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited PUNISHER BanSignal with reason USER_RESELLER ant other manual (isPlacedForFree == false)" in {
      val domain = nextDomain()
      val source = AutomaticSource(Application.PUNISHER, marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReason.UserReseller(None, Seq.empty)
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val source2 = ManualSource("1", marker = Inherited(Service.USERS_AUTORU))
      val detailedReason2 = DetailedReason.BadPhoto
      val signal2 =
        BanSignalGen.next.copy(
          domain = ownerDomain,
          source = source2,
          detailedReason = detailedReason2,
          switchOff = None
        )
      val signals = SignalSet(signal, signal2)
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category), isPlacedForFree = Some(false))
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedOpinion =
        failed(
          detailedReasons = Set(detailedReason2),
          isBanByInheritance = true,
          isFromReseller = true
        )
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Unknown for inherited PUNISHER BanSignal with reason USER_RESELLER (isPlacedForFree == None)" in {
      val domain = nextDomain()
      val source = AutomaticSource(Application.PUNISHER, marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReason.UserReseller(None, Seq.empty)
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category), isPlacedForFree = None)
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedResult = Map(domain -> unknown(isFromReseller = true))
      actualResult shouldBe expectedResult
    }

    "return Ok for inherited manual BanSignal than UnbanSignal" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val banSignal =
        BanSignalGen.next.copy(
          domain = ownerDomain,
          source = source,
          detailedReason = detailedReason,
          timestamp = DateTimeUtil.fromMillis(1L),
          switchOff = None
        )
      val unbanSignal =
        UnbanSignalGen.next.copy(
          domain = ownerDomain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(2L),
          switchOff = None
        )
      val signals = SignalSet(banSignal, unbanSignal)
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category))
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedResult = getExpected(domain -> ok())
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited manual UnbanSignal than BanSignal" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val unbanSignal =
        UnbanSignalGen.next.copy(
          domain = ownerDomain,
          source = source,
          timestamp = DateTimeUtil.fromMillis(1L),
          switchOff = None
        )
      val banSignal =
        BanSignalGen.next.copy(
          domain = ownerDomain,
          source = source,
          detailedReason = detailedReason,
          timestamp = DateTimeUtil.fromMillis(2L),
          switchOff = None
        )
      val signals = SignalSet(unbanSignal, banSignal)
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category))
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedOpinion =
        failed(
          detailedReasons = Set(detailedReason),
          isBanByInheritance = true
        )
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited HoboSignal with Result.Ban" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU))
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val signal =
        HoboSignalGen.next.copy(
          domain = ownerDomain,
          source = source,
          result = HoboSignal.Result.Bad(Set(detailedReason), None),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category), isPlacedForFree = Some(true))
      val instance = nextInstance(signals).copy(essentials = essentials)

      val actualResult = calculator(instance)
      val expectedOpinion =
        failed(
          detailedReasons = Set(detailedReason),
          isBanByInheritance = true
        )
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

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

    "prefer instance signal before owner" in {
      val domain = nextDomain()
      val category = CategoryGen.next
      val ownerDomain = Domain.UsersAutoru(Model.Domain.UsersAutoru.valueOf(category.toString))
      val ownerSource = ManualSource("az-frolov", Inherited(Service.USERS_AUTORU))
      val instanceSource = ManualSource("az-frolov")
      val detailedReason = DetailedReason.DoNotExist

      val banUserSignal =
        BanSignalGen.next.copy(
          domain = ownerDomain,
          source = ownerSource,
          detailedReason = detailedReason,
          timestamp = DateTimeUtil.fromMillis(1L),
          switchOff = None
        )
      val unbanUserSignal =
        UnbanSignalGen.next.copy(
          domain = ownerDomain,
          source = ownerSource,
          timestamp = DateTimeUtil.fromMillis(2L),
          switchOff = None
        )
      val unbanSignal =
        UnbanSignalGen.next.copy(
          domain = domain,
          source = instanceSource,
          timestamp = DateTimeUtil.fromMillis(3L),
          switchOff = None
        )
      val banSignal =
        BanSignalGen.next.copy(
          domain = domain,
          source = instanceSource,
          detailedReason = detailedReason,
          timestamp = DateTimeUtil.fromMillis(4L),
          switchOff = None
        )
      val essentials = AutoruEssentialsGen.next.copy(category = Some(category))
      val instance = nextInstance(SignalSet()).copy(essentials = essentials)

      calculator(instance.copy(signals = SignalSet(banUserSignal))) shouldBe
        Map(
          domain -> failed(
            detailedReasons = Set(detailedReason),
            isBanByInheritance = true
          )
        )
      calculator(instance.copy(signals = SignalSet(banUserSignal, unbanUserSignal))) shouldBe
        Map(domain -> ok())
      calculator(instance.copy(signals = SignalSet(banUserSignal, unbanUserSignal, unbanSignal))) shouldBe
        Map(domain -> ok())
      calculator(instance.copy(signals = SignalSet(banUserSignal, unbanSignal))) shouldBe
        Map(domain -> ok())
      calculator(instance.copy(signals = SignalSet(banUserSignal, unbanUserSignal, unbanSignal, banSignal))) shouldBe
        Map(
          domain -> failed(
            detailedReasons = Set(detailedReason),
            isBanByInheritance = true
          )
        )
    }

    "return Failed for MODERATION_RULES BanSignal with any reason" in {
      val domain = nextDomain()
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val source = AutomaticSource(Application.MODERATION_RULES)
      val signal =
        BanSignalGen.next.copy(domain = domain, detailedReason = detailedReason, source = source, switchOff = None)
      val signals = SignalSet(signal)
      val instance = nextInstance(signals)

      val actualResult = calculator(instance)
      val expectedOpinion = failed(detailedReasons = Set(detailedReason))
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited MODERATION_RULES decision" in {
      val domain = nextDomain()
      val detailedReason = DetailedReason.Another
      val category = CategoryGen.next
      val source = AutomaticSource(Application.MODERATION_RULES, marker = Inherited(Service.USERS_AUTORU))
      val signal =
        BanSignalGen.next.copy(
          domain = category.toUserDomain,
          detailedReason = detailedReason,
          source = source,
          switchOff = None
        )
      val signals = SignalSet(signal)
      val instance =
        nextInstance(signals).copy(
          essentials = AutoruEssentialsGen.next.copy(category = Some(category))
        )

      val actualResult = calculator(instance).mapValues(_.withDetails(None))
      val expectedOpinion = failed(detailedReasons = Set(detailedReason)).withDetails(None)
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited from DEALERS_AUTORU manual BanSignal with any reason" in {
      val domain = nextDomain()
      val source = ManualSourceGen.next.copy(marker = Inherited(Service.DEALERS_AUTORU))
      val ownerDomain = Domain.DealersAutoru(Model.Domain.DealersAutoru.DEFAULT_DEALERS_AUTORU)
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val signal =
        BanSignalGen.next.copy(domain = ownerDomain, source = source, detailedReason = detailedReason, switchOff = None)
      val signals = SignalSet(signal)
      val essentials = AutoruEssentialsGen.next
      val instance = nextInstance(signals).copy(essentials = essentials)
      val actualResult = calculator(instance)
      val expectedOpinion =
        failed(
          detailedReasons = Set(detailedReason),
          isBanByInheritance = true
        )
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    "return Failed for inherited from DEALERS_AUTORU IndexErrorSignal with any reason (included NO_VERIFIED)" in {
      val domain = nextDomain()
      val source = AutomaticSource(Application.INDEXER, marker = Inherited(Service.DEALERS_AUTORU))
      val ownerDomain = Domain.DealersAutoru(Model.Domain.DealersAutoru.DEFAULT_DEALERS_AUTORU)
      val detailedReason = DetailedReasonGen.suchThat(!isUserResellerDetailedReason(_)).next
      val signal =
        IndexErrorSignalGen.next.copy(
          domain = ownerDomain,
          source = source,
          detailedReasons = Set(detailedReason),
          switchOff = None
        )
      val signals = SignalSet(signal)
      val essentials = AutoruEssentialsGen.next
      val instance = nextInstance(signals).copy(essentials = essentials)
      val actualResult = calculator(instance)
      val actualIsReseller =
        actualResult(domain).details.exists {
          case Opinion.Autoru(_, isFromReseller) => isFromReseller
          case other                             => fail(s"Unexpected $other")
        }
      val expectedOpinion =
        failed(
          detailedReasons = Set(detailedReason),
          isBanByInheritance = true,
          isFromReseller = actualIsReseller
        )
      val expectedResult = getExpected(domain -> expectedOpinion)
      actualResult shouldBe expectedResult
    }

    Seq(
      DetailedReason.Stopword,
      DetailedReason.PhoneInDesc(Set.empty),
      DetailedReason.RegionMismatch,
      DetailedReason.Sold,
      DetailedReason.NoAnswer,
      DetailedReason.DoNotExist,
      DetailedReason.BlockedIp,
      DetailedReason.LowPrice
    ).foreach { detailedReason =>
      s"return Failed for moderation BanSignal with reason $detailedReason" in {
        val domain = nextDomain()
        val source = AutomaticSource(Application.MODERATION)
        val signal =
          BanSignalGen.next.copy(domain = domain, detailedReason = detailedReason, source = source, switchOff = None)
        val signals = SignalSet(signal)
        val instance = nextInstance(signals)

        val actualResult = calculator(instance)
        val expectedOpinion = failed(detailedReasons = Set(detailedReason))
        val expectedResult = getExpected(domain -> expectedOpinion)
        actualResult should be(expectedResult)
      }
    }

    val defaultDetails = Some(Opinion.Autoru(isBanByInheritance = false, isFromReseller = false))

    case class DetailsTestCase(essentials: AutoruEssentials, signals: SignalSet, expected: Option[Opinion.Autoru])
    val detailsTestCases =
      Seq(
        DetailsTestCase(essentials = AutoruEssentialsGen.next, signals = SignalSet(), expected = defaultDetails),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              )
            ),
          expected = defaultDetails
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next,
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = AutomaticSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              )
            ),
          expected = defaultDetails
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next.copy(category = Some(Category.CARS)),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.CARS),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.Another,
                switchOff = None
              )
            ),
          expected = defaultDetails.map(_.copy(isBanByInheritance = true))
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next.copy(category = Some(Category.TRUCK)),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.CARS),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.Another,
                switchOff = None
              )
            ),
          expected = defaultDetails
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next.copy(category = Some(Category.CARS)),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.CARS),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.Another,
                switchOff = Some(SignalSwitchOff(ManualSourceGen.next, DateTime.now, None, None))
              )
            ),
          expected = defaultDetails
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next.copy(category = Some(Category.CARS)),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.ARTIC),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.Another,
                switchOff = None
              )
            ),
          expected = defaultDetails
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next.copy(category = Some(Category.CARS)),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.ARTIC),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReason.Another,
                switchOff = None
              )
            ),
          expected = defaultDetails
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next.copy(category = Some(Category.CARS)),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              ),
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.CARS),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.Another,
                switchOff = None
              )
            ),
          expected = defaultDetails.map(_.copy(isBanByInheritance = true))
        ),
        DetailsTestCase(
          essentials =
            AutoruEssentialsGen.next.copy(
              category = Some(Category.CARS),
              isPlacedForFree = Some(true),
              experimentInfo = None
            ),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              ),
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.CARS),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.UserReseller(None, Seq.empty),
                switchOff = None
              )
            ),
          expected = defaultDetails.map(_.copy(isBanByInheritance = true, isFromReseller = true))
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next.copy(category = Some(Category.CARS), isPlacedForFree = Some(false)),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              ),
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.CARS),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.UserReseller(None, Seq.empty),
                switchOff = None
              )
            ),
          expected = defaultDetails.map(_.copy(isFromReseller = true))
        ),
        DetailsTestCase(
          essentials =
            AutoruEssentialsGen.next.copy(
              category = Some(Category.CARS),
              isPlacedForFree = Some(true),
              experimentInfo = Some(ExperimentInfo(Some(true)))
            ),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              ),
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.CARS),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.UserReseller(None, Seq.empty),
                switchOff = None
              )
            ),
          expected = defaultDetails.map(_.copy(isFromReseller = true))
        ),
        DetailsTestCase(
          essentials = AutoruEssentialsGen.next.copy(category = Some(Category.CARS), isPlacedForFree = Some(true)),
          signals =
            SignalSet(
              BanSignalGen.next.copy(
                domain = nextDomain(),
                source = ManualSourceGen.next.copy(marker = NoMarker),
                detailedReason = DetailedReasonGen.next,
                switchOff = None
              ),
              BanSignalGen.next.copy(
                domain = Domain.UsersAutoru(Model.Domain.UsersAutoru.TRUCK),
                source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_AUTORU)),
                detailedReason = DetailedReason.UserReseller(None, Seq.empty),
                switchOff = None
              )
            ),
          expected = defaultDetails
        )
      )

    detailsTestCases.zipWithIndex.foreach { case (testCase @ DetailsTestCase(essentials, signals, _), i) =>
      s"be correct on details case $i" in {
        val domain = nextDomain()
        val instance = nextInstance(signals).copy(essentials = essentials)
        calculator(instance)(domain).details shouldBe testCase.expected
      }
    }
  }
}
