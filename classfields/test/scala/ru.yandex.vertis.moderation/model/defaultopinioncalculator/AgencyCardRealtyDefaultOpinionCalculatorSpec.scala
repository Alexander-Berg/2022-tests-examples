package ru.yandex.vertis.moderation.model.defaultopinioncalculator

import cats.Endo
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.{Globals, OpinionCalculatorSpecBase}
import ru.yandex.vertis.moderation.model.Opinion.Details.ReasonKey
import ru.yandex.vertis.moderation.model.Opinion.{AgencyCardRealty, Details, Failed, Ok, Unknown}
import ru.yandex.vertis.moderation.model.defaultopinioncalculator.AgencyCardRealtyDefaultOpinionCalculatorSpec.{
  DetailsTestCase,
  OpinionTestCase
}
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.signal._
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, EssentialsVersion, Opinion}
import ru.yandex.vertis.moderation.opinion.{DefaultOpinionCalculator, OpinionCalculator}
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.AutomaticSource.Application
import ru.yandex.vertis.moderation.proto.Model.Signal.SignalType
import ru.yandex.vertis.moderation.proto.Model.{Reason, Service}
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class AgencyCardRealtyDefaultOpinionCalculatorSpec extends OpinionCalculatorSpecBase {

  override protected def service: Service = Service.AGENCY_CARD_REALTY

  override protected def calculator: OpinionCalculator = Globals.opinionCalculator(service)

  private val detailsTestCases: Seq[DetailsTestCase] =
    Seq(
      {
        val v1 = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(0L)))
        val v2 = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(1L)))
        val reason = Reason.AD_ON_PHOTO
        val reasonKey = ReasonKey(reason, isBan = true)
        val s1Gen =
          signalGen(
            signalType = SignalType.BAN,
            reason = reason,
            essentialsVersion = Some(v1),
            isInherited = false
          )
        val s2Gen =
          signalGen(
            signalType = SignalType.BAN,
            reason = reason,
            essentialsVersion = Some(v2),
            isInherited = true
          )
        DetailsTestCase(
          description = "use last version if there are two versions for the same reasonKey",
          signalSet =
            SignalSet(
              Seq(s1Gen, s2Gen).map(_.next)
            ),
          expectedDetails =
            Some(
              AgencyCardRealty(
                Map(
                  reasonKey -> Details.ReasonInfo(v2)
                ),
                None
              )
            )
        )
      }, {
        val reason = Reason.AD_ON_PHOTO
        val v1 = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(0L)))
        val k1 = ReasonKey(reason, isBan = false)
        val v2 = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(1L)))
        val k2 = k1.copy(isBan = true)
        val s1Gen =
          signalGen(
            essentialsVersion = Some(v1),
            isInherited = false,
            reason = reason,
            signalType = SignalType.WARN
          )
        val s2Gen =
          signalGen(
            essentialsVersion = Some(v2),
            isInherited = false,
            reason = reason,
            signalType = SignalType.BAN
          )
        DetailsTestCase(
          description = "collect version for warn reasons as well",
          signalSet =
            SignalSet(
              Seq(s1Gen, s2Gen).map(_.next)
            ),
          expectedDetails =
            Some(
              AgencyCardRealty(
                Map(
                  k1 -> Details.ReasonInfo(v1),
                  k2 -> Details.ReasonInfo(v2)
                ),
                None
              )
            )
        )
      }
    )

  private val calculatorTestCases: Seq[OpinionTestCase] =
    Seq(
      {
        val reason = Reason.STOLEN_NAME
        val detailedReason = DetailedReason.StolenName
        val k = ReasonKey(reason, isBan = true)
        val v = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(1L)))
        val signal =
          signalGen(
            domain = Domain.UsersRealty(Model.Domain.UsersRealty.DEFAULT_USERS_REALTY),
            signalType = SignalType.HOBO,
            reason = reason,
            essentialsVersion = Some(v),
            isInherited = true
          ).next
        val expectedOpinion =
          Failed(
            detailedReasons = Set(detailedReason),
            warnDetailedReasons = Set.empty,
            details = Some(AgencyCardRealty(Map(k -> Details.ReasonInfo(v)), None))
          )
        OpinionTestCase(
          description = "Failed for inherited HoboSignal with Result.Ban",
          signalSet = SignalSet(Seq(signal)),
          expectedOpinion = expectedOpinion
        )
      }, {
        val reason = Reason.STOLEN_NAME
        val detailedReason = DetailedReason.StolenName
        val k = ReasonKey(reason, isBan = true)
        val v = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(1L)))
        val signal =
          signalGen(
            signalType = SignalType.HOBO,
            reason = reason,
            essentialsVersion = Some(v),
            isInherited = false
          ).next
        val expectedOpinion =
          Failed(
            detailedReasons = Set(detailedReason),
            warnDetailedReasons = Set.empty,
            details = Some(AgencyCardRealty(Map(k -> Details.ReasonInfo(v)), None))
          )
        OpinionTestCase(
          description = "Failed for not inherited HoboSignal with Result.Ban",
          signalSet = SignalSet(Seq(signal)),
          expectedOpinion = expectedOpinion
        )
      }, {
        val reason = Reason.STOLEN_NAME
        val detailedReason = DetailedReason.StolenName
        val k = ReasonKey(reason, isBan = true)
        val v = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(1L)))
        val signal =
          signalGen(
            signalType = SignalType.BAN,
            reason = reason,
            source = ManualSourceGen.next.copy(marker = NoMarker),
            essentialsVersion = Some(v),
            isInherited = false
          ).next
        val expectedOpinion =
          Failed(
            detailedReasons = Set(detailedReason),
            warnDetailedReasons = Set.empty,
            details = Some(AgencyCardRealty(Map(k -> Details.ReasonInfo(v)), None))
          )
        OpinionTestCase(
          description = "Failed for manual BanSignal",
          signalSet = SignalSet(Seq(signal)),
          expectedOpinion = expectedOpinion
        )
      }, {
        val reason = Reason.STOLEN_NAME
        val detailedReason = DetailedReason.StolenName
        val k = ReasonKey(reason, isBan = true)
        val v = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(1L)))
        val source = ManualSourceGen.next.copy(marker = Inherited(Service.USERS_REALTY))
        val signal =
          signalGen(
            signalType = SignalType.BAN,
            reason = reason,
            source = source,
            essentialsVersion = Some(v),
            isInherited = true
          ).next
        val expectedOpinion =
          Failed(
            detailedReasons = Set(detailedReason),
            warnDetailedReasons = Set.empty,
            details = Some(AgencyCardRealty(Map(k -> Details.ReasonInfo(v)), None))
          )
        OpinionTestCase(
          description = "Failed for inherited manual BanSignal",
          signalSet = SignalSet(Seq(signal)),
          expectedOpinion = expectedOpinion
        )
      }, {
        val signal =
          UnbanSignal(
            domain = nextDomain(),
            source = ManualSourceGen.next.copy(marker = NoMarker),
            timestamp = DateTimeUtil.fromMillis(1L),
            info = None,
            switchOff = None,
            ttl = None,
            outerComment = None,
            auxInfo = SignalInfoSet.Empty
          )
        val expectedOpinion =
          Ok(
            warnDetailedReasons = Set.empty,
            details = Some(AgencyCardRealty(Map.empty, None))
          )
        OpinionTestCase(
          description = "return Ok for manual UnbanSignal",
          signalSet = SignalSet(signal),
          expectedOpinion = expectedOpinion
        )
      }, {
        val signal =
          signalGen(
            signalType = SignalType.INDEXER,
            reason = Reason.STOLEN_NAME,
            essentialsVersion = None,
            isInherited = false
          ).next
        val expectedOpinion =
          Unknown(
            warnDetailedReasons = Set(),
            details = Some(AgencyCardRealty(Map.empty, None))
          )
        OpinionTestCase(
          description = "UnknownNoReason for IndexErrorSignal",
          signalSet = SignalSet(Seq(signal)),
          expectedOpinion = expectedOpinion
        )
      }, {
        val reason = Reason.STOLEN_NAME
        val detailedReason = DetailedReason.StolenName
        val k = ReasonKey(reason, isBan = true)
        val v = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(1L)))
        val source = AutomaticSource(Application.INDEXER, marker = Inherited(Service.USERS_REALTY))
        val signal =
          signalGen(
            signalType = SignalType.BAN,
            reason = reason,
            source = source,
            essentialsVersion = Some(v),
            isInherited = true
          ).next
        val expectedOpinion =
          Failed(
            detailedReasons = Set(detailedReason),
            warnDetailedReasons = Set.empty,
            details = Some(AgencyCardRealty(Map(k -> Details.ReasonInfo(v)), None))
          )
        OpinionTestCase(
          description = "Failed for inherited automatic indexer BanSignal",
          signalSet = SignalSet(Seq(signal)),
          expectedOpinion = expectedOpinion
        )
      }, {
        val reason = Reason.STOLEN_NAME
        val signal1 =
          signalGen(
            signalType = SignalType.INDEXER,
            reason = reason,
            essentialsVersion = None,
            isInherited = false
          ).next
        val detailedReason = DetailedReason.StolenName
        val k = ReasonKey(reason, isBan = true)
        val v = EssentialsVersion(Some("hash"), Some(DateTimeUtil.fromMillis(1L)))
        val signal2 =
          signalGen(
            signalType = SignalType.BAN,
            reason = reason,
            source = ManualSourceGen.next.copy(marker = NoMarker),
            essentialsVersion = Some(v),
            isInherited = false
          ).next
        val expectedOpinion =
          Failed(
            detailedReasons = Set(detailedReason),
            warnDetailedReasons = Set.empty,
            details = Some(AgencyCardRealty(Map(k -> Details.ReasonInfo(v)), None))
          )
        OpinionTestCase(
          description = "Failed in case of manual BanSignal and IndexErrorSignal",
          signalSet = SignalSet(Seq(signal1, signal2)),
          expectedOpinion = expectedOpinion
        )
      }, {
        val expectedOpinion =
          Unknown(
            warnDetailedReasons = Set(),
            details = Some(AgencyCardRealty(Map.empty, None))
          )
        OpinionTestCase(
          description = "UnknownNoReason if no signals at all",
          signalSet = SignalSet.Empty,
          expectedOpinion = expectedOpinion
        )
      }
    )

  private def signalGen(signalType: SignalType,
                        reason: Reason,
                        essentialsVersion: Option[EssentialsVersion],
                        isInherited: Boolean,
                        domain: Domain = nextDomain(),
                        source: Source = SourceGen.suchThat(_.marker == NoMarker).next,
                        isHoboResultOk: Boolean = false,
                        finishTime: Option[DateTime] = None
                       ): Gen[Signal] = {
    val detailedReason = DetailedReason.fromReason(reason)
    val baseGen: Gen[Signal] =
      signalType match {
        case SignalType.HOBO =>
          if (isHoboResultOk) {
            HoboSignalGen.map(
              _.copy(
                source = source,
                result = HoboSignal.Result.Good(None),
                finishTime = finishTime
              )
            )
          } else {
            HoboSignalGen.map(
              _.copy(
                source = source,
                result = HoboSignal.Result.Bad(Set(detailedReason), None)
              )
            )
          }

        case SignalType.INDEXER =>
          IndexErrorSignalGen.map(_.copy(source = source, detailedReasons = Set(detailedReason)))

        case SignalType.BAN =>
          BanSignalGen.map(_.copy(source = source, detailedReason = detailedReason))

        case SignalType.WARN =>
          WarnSignalGen.map(_.copy(source = source, detailedReason = detailedReason))

        case _ => ???
      }
    val mappers: Seq[Endo[Signal]] =
      Seq(
        _.withSwitchOff(None),
        _.withAuxInfo(SignalInfoSet(essentialsVersion.map(SignalInfo.Version))),
        _.withMarker(if (isInherited) Inherited(Service.USERS_REALTY) else NoMarker),
        _.withDomain(domain)
      )
    baseGen.map(mappers.reduce(_ andThen _))
  }

  "OpinionCalculator" should {
    detailsTestCases.foreach { case DetailsTestCase(description, signalSet, expectedDetails) =>
      s"calculate details correctly: $description" in {
        val instance = nextInstance(signalSet)
        val actualDetails = calculator.single(instance).details
        actualDetails shouldBe expectedDetails
      }
    }

    "calculate details and correctly show essentials version for Opinion.Ok if card was updated after hobotask finished" in {
      val reason = Reason.STOLEN_NAME
      val ev1 = EssentialsVersion(Some("hash1"), Some(DateTimeUtil.fromMillis(3L)))
      val ev2 = EssentialsVersion(Some("hash2"), Some(DateTimeUtil.fromMillis(5L)))
      val hoboSignal1 =
        signalGen(
          signalType = SignalType.HOBO,
          reason = reason,
          essentialsVersion = Some(ev1),
          isInherited = false,
          isHoboResultOk = true,
          finishTime = Some(DateTimeUtil.fromMillis(4L))
        ).next
      val signalSet = SignalSet(Seq(hoboSignal1))
      val expectedDetails = Some(AgencyCardRealty(Map.empty, Some(ev1)))
      val instance = nextInstance(signalSet)
      val updatedEssentials = AgencyCardRealtyEssentialsGen.map(_.copy(essentialsVersion = Some(ev2))).next
      val updatedInstance = instance.copy(essentials = updatedEssentials)
      val actualDetails = calculator.single(updatedInstance).details
      actualDetails shouldBe expectedDetails
    }

    calculatorTestCases.foreach { case OpinionTestCase(description, signalSet, expectedOpinion) =>
      s"calculate opinion correctly: $description" in {
        val instance = nextInstance(signalSet)
        val actualOpinion = calculator.single(instance)
        actualOpinion shouldBe expectedOpinion
      }
    }
  }
}

object AgencyCardRealtyDefaultOpinionCalculatorSpec {

  case class DetailsTestCase(description: String,
                             signalSet: SignalSet,
                             expectedDetails: Option[Opinion.AgencyCardRealty]
                            )

  case class OpinionTestCase(description: String, signalSet: SignalSet, expectedOpinion: Opinion)

}
