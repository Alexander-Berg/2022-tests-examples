package ru.yandex.vertis.punisher.services

import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.scalacheck.Checkers.check
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Domain
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.convert.Reasons
import ru.yandex.vertis.punisher.services.ModerationService.SignalTypes._
import ru.yandex.vertis.punisher.services.ModerationService._
import ru.yandex.vertis.vsquality.proto.Common

@RunWith(classOf[JUnitRunner])
class ModerationServiceProtocolSpec extends BaseSpec {

  import ModelGen._
  import ModerationServiceProtocol._

  "AutoruSignal" should {
    "correctly converts" in {
      check(forAll(AutoruSignalGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "RealtySignal" should {
    "correctly converts" in {
      check(forAll(RealtySignalGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "AutoruExternalId" should {
    "correctly converts" in {
      check(forAll(AutoruExternalIdGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }

  "RealtyExternalId" should {
    "correctly converts" in {
      check(forAll(RealtyExternalIdGen) { x =>
        x == fromMessage(toMessage(x))
      })
    }
  }
}

object ModelGen {

  private val WarnGen: Gen[Warn] = Warn(Gen.chooseNum(0.1, 1.0).sample.get)

  val AutoruExternalIdGen: Gen[AutoruExternalId] =
    for {
      userId <- Gen.alphaNumStr.filter(_.nonEmpty)
    } yield AutoruExternalId(userId)

  val RealtyExternalIdGen: Gen[RealtyExternalId] =
    for {
      userId <- Gen.alphaNumStr.filter(_.nonEmpty)
    } yield RealtyExternalId(userId)

  val AutoruSignalGen: Gen[AutoruSignal] =
    for {
      signalType <- Gen.oneOf(Ban, WarnGen.sample.get)
      externalId <- AutoruExternalIdGen
      reason     <- Gen.oneOf(Model.Reason.values.toIterable)
      detailedReason = Reasons.toDetailedReason(reason)
      info           = Gen.alphaNumStr.filter(_.nonEmpty).sample
      tag            = Gen.alphaNumStr.filter(_.nonEmpty).sample
      categories <- Gen.nonEmptyListOf(Gen.oneOf(Domain.UsersAutoru.values.toIterable))
      task       <- Gen.oneOf(Common.RuleType.values().filter(_.toString.contains("AUTO")).toIterable)
    } yield AutoruSignal(signalType, externalId, detailedReason, info, tag, categories.toSet, Some(task))

  val RealtySignalGen: Gen[RealtySignal] =
    for {
      signalType <- Gen.oneOf(Ban, WarnGen.sample.get)
      externalId <- RealtyExternalIdGen
      reason     <- Gen.oneOf(Model.Reason.values.toIterable)
      detailedReason = Reasons.toDetailedReason(reason)
      info           = Gen.alphaNumStr.filter(_.nonEmpty).sample
      tag            = Gen.alphaNumStr.filter(_.nonEmpty).sample
      task <- Gen.oneOf(Common.RuleType.values().filter(_.toString.contains("REALTY")).toIterable)
    } yield RealtySignal(signalType, externalId, detailedReason, info, tag, Some(task))
}
