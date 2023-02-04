package ru.yandex.vertis.moderation.mosru

import cats.Endo
import org.scalacheck.Gen
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.DetailedReason
import ru.yandex.vertis.moderation.model.DetailedReason.UserReseller
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.signal.{NoMarker, Signal, SignalSet}
import ru.yandex.vertis.moderation.mosru.MosRuDecider.{Source, Verdict}
import ru.yandex.vertis.moderation.mosru.MosRuDeciderSpec.TestCase
import ru.yandex.vertis.moderation.proto.Model.DetailedReason.Details.UserReseller.ResellerType
import ru.yandex.vertis.moderation.proto.Model.Reason
import ru.yandex.vertis.moderation.proto.Model.Signal.SignalType

/**
  * @author potseluev
  */
class MosRuDeciderSpec extends SpecBase {

  implicit private val featureRegistry: FeatureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)
  private val decider: MosRuDecider = new MosRuDeciderImpl

  private def nextSignal(detailedReason: DetailedReason,
                         signalType: SignalType,
                         isActive: Boolean = true,
                         isInherited: Boolean = false
                        ): Signal = {
    val baseGen: Gen[Signal] =
      signalType match {
        case SignalType.BAN =>
          BanSignalGen.map(_.copy(detailedReason = detailedReason))
        case SignalType.WARN =>
          WarnSignalGen.map(_.copy(detailedReason = detailedReason))
        case _ => ???
      }
    val mappers: Seq[Endo[Signal]] =
      Seq(
        _.withSwitchOff(if (isActive) None else Some(SignalSwitchOffGen.next)),
        _.withMarker(if (isInherited) InheritedSourceMarkerGen.next else NoMarker)
      )
    baseGen.map(mappers.reduce(_ andThen _)).next
  }

  private val mosRuValidation = DetailedReason.fromReason(Reason.MOS_RU_VALIDATION)
  private val notMosRuValidation = DetailedReasonGen.filter(_.reason != Reason.MOS_RU_VALIDATION).next
  private val userResellerAnonymous =
    DetailedReason.UserReseller(shouldStayActive = None, resellerTypes = Seq(ResellerType.ANONYMOUS))
  private val userResellerNotAnonymous =
    DetailedReason.UserReseller(shouldStayActive = None, resellerTypes = Seq(ResellerType.FAST_RESALE))
  private val anyOtherDetailedReason =
    DetailedReasonGen.filter(dr => dr.reason != Reason.MOS_RU_VALIDATION && dr.reason != Reason.USER_RESELLER).next

  private val s1 = nextSignal(mosRuValidation, SignalType.BAN)
  private val s2 = nextSignal(mosRuValidation, SignalType.BAN, isInherited = true)
  private val s3 = nextSignal(mosRuValidation, SignalType.WARN, isActive = false)
  private val s4 = nextSignal(notMosRuValidation, SignalType.BAN)
  private val s5 = nextSignal(userResellerAnonymous, SignalType.BAN)
  private val s6 = nextSignal(userResellerNotAnonymous, SignalType.BAN)
  private val s7 = nextSignal(anyOtherDetailedReason, SignalType.BAN)

  private val signalSet1 = SignalSet(s1, s2, s3, s4, s5, s7)
  private val signalSet2 = SignalSet(s1, s2, s3, s5, s6, s7)

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "return active not inherited MOS_RU_VALIDATION signal keys to switch off if mosHasSnils = true",
        mosHasSnils = Some(true),
        mosTrusted = Some(false),
        gosUslugiTrusted = None,
        signals = signalSet1,
        expectedVerdict = Verdict(toSwitchOff = Set(s1.key)),
        isSwitchOff4UserResellerEnabled = false
      ),
      TestCase(
        description = "return Verdict.Empty if mosHasSnils = false, mosTrusted = None",
        mosHasSnils = Some(false),
        mosTrusted = None,
        gosUslugiTrusted = None,
        signals = SignalSet(s1),
        expectedVerdict = Verdict.Empty,
        isSwitchOff4UserResellerEnabled = false
      ),
      TestCase(
        description =
          "return active not inherited MOS_RU_VALIDATION and USER_RESELLER signal keys to switch off if mosHasSnils = true",
        mosHasSnils = Some(true),
        mosTrusted = Some(false),
        gosUslugiTrusted = None,
        signals = signalSet2,
        expectedVerdict = Verdict(toSwitchOff = Set(s1.key, s5.key)),
        isSwitchOff4UserResellerEnabled = true
      ),
      TestCase(
        description =
          "return active not inherited MOS_RU_VALIDATION signal keys to switch off if mosTrusted = true and MosRuDecider feature enabled",
        mosHasSnils = Some(false),
        mosTrusted = Some(true),
        gosUslugiTrusted = None,
        signals = signalSet2,
        expectedVerdict = Verdict(toSwitchOff = Set(s1.key)),
        isSwitchOff4UserResellerEnabled = true
      ),
      TestCase(
        description =
          "return active not inherited MOS_RU_VALIDATION and USER_RESELLER signal keys to switch off if gosUslugiTrusted = true",
        mosHasSnils = Some(false),
        mosTrusted = Some(false),
        gosUslugiTrusted = Some(true),
        signals = signalSet2,
        expectedVerdict = Verdict(toSwitchOff = Set(s1.key, s5.key)),
        isSwitchOff4UserResellerEnabled = true
      ),
      TestCase(
        description = "return active not inherited MOS_RU_VALIDATION if gosUslugiTrusted = true and feature disabled",
        mosHasSnils = Some(false),
        mosTrusted = Some(true),
        gosUslugiTrusted = Some(true),
        signals = signalSet1,
        expectedVerdict = Verdict(toSwitchOff = Set(s1.key)),
        isSwitchOff4UserResellerEnabled = false
      )
    )

  "MosRuDecider" should {
    testCases.foreach {
      case TestCase(
             description,
             mosHasSnils,
             mosTrusted,
             gosUslugiTrusted,
             signals,
             expectedVerdict,
             isSwitchOff4UserResellerEnabled
           ) =>
        description in {
          featureRegistry
            .updateFeature(
              MosRuDeciderImpl.SwitchOffResellerByMosruEnabled,
              isSwitchOff4UserResellerEnabled
            )
            .futureValue
          val instance =
            InstanceGen.next.copy(
              signals = signals,
              essentials =
                UserAutoruEssentialsGen.next.copy(
                  mosHasSnils = mosHasSnils,
                  mosTrusted = mosTrusted,
                  gosUslugiTrusted = gosUslugiTrusted
                )
            )
          val source = Source(instance)
          decider(source) shouldBe expectedVerdict
        }
    }
  }

}

object MosRuDeciderSpec {

  case class TestCase(description: String,
                      mosHasSnils: Option[Boolean],
                      mosTrusted: Option[Boolean],
                      gosUslugiTrusted: Option[Boolean],
                      signals: SignalSet,
                      expectedVerdict: Verdict,
                      isSwitchOff4UserResellerEnabled: Boolean
                     )

}
