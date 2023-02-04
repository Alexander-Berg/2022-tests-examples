package ru.yandex.vertis.moderation.util

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.Opinion.{Autoru, Realty}
import ru.yandex.vertis.moderation.model.{DetailedReason, Domain, Opinion, Opinions}
import ru.yandex.vertis.moderation.model.signal.{HoboSignal, ManualSource, SignalInfoSet, SignalSet, UnbanSignal}
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer

import scala.concurrent.duration._

/**
  * @author semkagtn
  */
@RunWith(classOf[JUnitRunner])
class TskvUtilSpec extends SpecBase {

  case class Point(x: Int, y: Int)
  case class Wierd(`wierd-name`: String)
  case object Obj

  sealed abstract class Line {

    def width: Int
  }
  case object ThinLine extends Line {
    override def width: Int = 1
  }
  case class MediumLine(color: Int) extends Line {
    override def width: Int = 2
  }
  class BoldLine(color: Int) extends Line {
    override def width: Int = 3
  }

  "toTskvRow" should {

    "correctly convert Some(v)" in {
      val some = Some(5)
      val actualResult = TskvUtil.toMap("x", some)
      val expectedResult = Map("x" -> "5")
      actualResult shouldBe expectedResult
    }

    "correctly convert None" in {
      val actualResult = TskvUtil.toMap("x", None)
      val expectedResult = Map.empty
      actualResult shouldBe expectedResult
    }

    "correctly convert null" in {
      val actualResult = TskvUtil.toMap("x", null)
      val expectedResult = Map.empty
      actualResult shouldBe expectedResult
    }

    "correctly convert non-empty array" in {
      val array = Array(1, 2)
      val actualResult = TskvUtil.toMap("a", array)
      val expectedResult = Map("a@0" -> "1", "a@1" -> "2")
      actualResult shouldBe expectedResult
    }

    "correctly convert empty array" in {
      val actualResult = TskvUtil.toMap("a", Array.empty)
      val expectedResult = Map.empty
      actualResult shouldBe expectedResult
    }

    "correctly convert non-empty seq" in {
      val seq = Seq(1, 2)
      val actualResult = TskvUtil.toMap("a", seq)
      val expectedResult = Map("a@0" -> "1", "a@1" -> "2")
      actualResult shouldBe expectedResult
    }

    "correctly convert empty seq" in {
      val actualResult = TskvUtil.toMap("a", Seq.empty)
      val expectedResult = Map.empty
      actualResult shouldBe expectedResult
    }

    "correctly convert non-empty map" in {
      val map = Map("k1" -> 42, "k2" -> 24)
      val actualResult = TskvUtil.toMap("m", map)
      val expectedResult = Map("m.k1" -> "42", "m.k2" -> "24")
      actualResult shouldBe expectedResult
    }

    "correctly convert empty map" in {
      val actualResult = TskvUtil.toMap("m", Map.empty)
      val expectedResult = Map.empty
      actualResult shouldBe expectedResult
    }

    "correctly convert signal set" in {
      val signalSet =
        SignalSet(
          UnbanSignal(
            domain = Domain.default(Service.REALTY),
            source = ManualSource("user"),
            timestamp = DateTimeUtil.now(),
            info = None,
            switchOff = None,
            ttl = None,
            outerComment = None,
            auxInfo = SignalInfoSet.Empty
          )
        )
      val actualMap = TskvUtil.toMap("s", signalSet)
      val expectedKeys =
        Set(
          "s.manual_unban",
          "s.manual_unban.timestamp",
          "s.manual_unban.domain",
          "s.manual_unban.domain.value",
          "s.manual_unban.source",
          "s.manual_unban.source.marker",
          "s.manual_unban.source.userId"
        )
      actualMap.keySet shouldBe expectedKeys
      val expectedPairs =
        Set(
          "s.manual_unban" -> "UnbanSignal",
          "s.manual_unban.domain" -> "Realty",
          "s.manual_unban.source" -> "ManualSource"
        )
      actualMap should contain allElementsOf expectedPairs
    }

    "correctly convert case class" in {
      val point = Point(x = 1, y = 2)
      val actualResult = TskvUtil.toMap("p", point)
      val expectedResult = Map("p.x" -> "1", "p.y" -> "2")
      actualResult shouldBe expectedResult
    }

    "correctly convert case class with wierd name" in {
      val wierd = Wierd("v")
      val actualResult = TskvUtil.toMap("w", wierd)
      val expectedResult = Map("w.wierd-name" -> "v")
      actualResult shouldBe expectedResult
    }

    "correctly convert case object" in {
      val actualResult = TskvUtil.toMap("o", Obj)
      val expectedResult = Map.empty
      actualResult shouldBe expectedResult
    }

    "correctly convert FiniteDuration" in {
      val duration = 1.minute
      val actualResult = TskvUtil.toMap("d", duration)
      val expectedResult = Map("d" -> "60000")
      actualResult shouldBe expectedResult
    }

    "correctly convert ExternalId" in {
      val externalId = ExternalIdGen.next
      val actualResult = TskvUtil.toMap("e", externalId)
      val expectedResult = Map("e" -> externalId.id)
      actualResult shouldBe expectedResult
    }

    "correctly convert other value" in {
      val actualResult = TskvUtil.toMap("int", 1)
      val expectedResult = Map("int" -> "1")
      actualResult shouldBe expectedResult
    }

    "correctly convert undefined hobo signal" in {
      val hoboSignal = HoboSignalGen.next
      val actualPairs = TskvUtil.toMap("s", hoboSignal.copy(result = HoboSignal.Result.Undefined))
      val expectedPair = "s.result" -> "Undefined"
      actualPairs should contain(expectedPair)
    }
    "correctly convert bad hobo signal" in {
      val hoboSignal = HoboSignalGen.next
      val detailedReason = DetailedReasonGen.next
      val actualPairs = TskvUtil.toMap("s", hoboSignal.copy(result = HoboSignal.Result.Bad(Set(detailedReason), None)))
      val expectedPairs =
        Set(
          "s.result.reasons@0" -> detailedReason.reason.toString,
          "s.result" -> "Bad"
        )
      actualPairs should contain allElementsOf expectedPairs
    }
    "correctly convert warn hobo signal" in {
      val hoboSignal = HoboSignalGen.next
      val detailedReason = DetailedReasonGen.next
      val actualPairs = TskvUtil.toMap("s", hoboSignal.copy(result = HoboSignal.Result.Warn(Set(detailedReason), None)))
      val expectedPairs =
        Set(
          "s.result.reasons@0" -> detailedReason.reason.toString,
          "s.result" -> "Warn"
        )
      actualPairs should contain allElementsOf expectedPairs
    }

    "correctly convert instance with undefined hobo signal" in {
      val instance = InstanceGen.next
      val hoboSignal = HoboSignalGen.next.copy(result = HoboSignal.Result.Undefined)
      val newInstance = instance.copy(signals = SignalSet(hoboSignal))
      val actualPairs = TskvUtil.toMap("new", newInstance)
      val expectedPairs =
        Set(
          s"new.signals.${hoboSignal.key}.result" -> "Undefined"
        )
      actualPairs should contain allElementsOf expectedPairs
    }

    case class OpinionsConvertCase(opinions: Opinions, expectedKvPart: Set[(String, String)])

    val OpinionsConvertCases =
      Seq(
        OpinionsConvertCase(
          opinions = Opinions(Domain.default(Service.AUTORU) -> Opinion.Ok(Set.empty)),
          expectedKvPart = Set("opinions.Autoru(DEFAULT_AUTORU)" -> "Ok")
        ),
        OpinionsConvertCase(
          opinions =
            Opinions(
              Domain.default(Service.AUTORU) ->
                Opinion.Ok(Set.empty, Some(Autoru(isBanByInheritance = true, isFromReseller = false)))
            ),
          expectedKvPart =
            Set(
              "opinions.Autoru(DEFAULT_AUTORU)" -> "Ok",
              "opinions.Autoru(DEFAULT_AUTORU).details" -> "Autoru",
              "opinions.Autoru(DEFAULT_AUTORU).details.isBanByInheritance" -> "true",
              "opinions.Autoru(DEFAULT_AUTORU).details.isFromReseller" -> "false"
            )
        ),
        OpinionsConvertCase(
          opinions = Opinions(Domain.default(Service.AUTORU) -> Opinion.Unknown(Set.empty)),
          expectedKvPart = Set("opinions.Autoru(DEFAULT_AUTORU)" -> "Unknown")
        ),
        OpinionsConvertCase(
          opinions =
            Opinions(
              Domain.default(Service.AUTORU) ->
                Opinion.Unknown(Set.empty, Some(Realty(isBanByInheritance = true)))
            ),
          expectedKvPart =
            Set(
              "opinions.Autoru(DEFAULT_AUTORU)" -> "Unknown",
              "opinions.Autoru(DEFAULT_AUTORU).details" -> "Realty",
              "opinions.Autoru(DEFAULT_AUTORU).details.isBanByInheritance" -> "true"
            )
        ),
        OpinionsConvertCase(
          opinions =
            Opinions(
              Domain.UsersAutoru.withName("CARS") -> Opinion.Unknown(Set.empty),
              Domain.UsersAutoru.withName("TRUCK") -> Opinion.Ok(Set.empty)
            ),
          expectedKvPart =
            Set(
              "opinions.UsersAutoru(CARS)" -> "Unknown",
              "opinions.UsersAutoru(TRUCK)" -> "Ok"
            )
        ),
        OpinionsConvertCase(
          opinions =
            Opinions(
              Domain.default(Service.REALTY) ->
                Opinion.Failed(Set(DetailedReason.BadPhoto), Set(DetailedReason.UserBan))
            ),
          expectedKvPart =
            Set(
              "opinions.Realty(DEFAULT_REALTY)" -> "Failed",
              "opinions.Realty(DEFAULT_REALTY).reasons@0" -> "BAD_PHOTO"
            )
        ),
        OpinionsConvertCase(
          opinions =
            Opinions(
              Domain.default(Service.AUTORU) ->
                Opinion.Failed(Set(DetailedReason.BadPhoto), Set.empty, Some(Realty(isBanByInheritance = true)))
            ),
          expectedKvPart =
            Set(
              "opinions.Autoru(DEFAULT_AUTORU)" -> "Failed",
              "opinions.Autoru(DEFAULT_AUTORU).reasons@0" -> "BAD_PHOTO",
              "opinions.Autoru(DEFAULT_AUTORU).details" -> "Realty",
              "opinions.Autoru(DEFAULT_AUTORU).details.isBanByInheritance" -> "true"
            )
        )
      )
    OpinionsConvertCases.zipWithIndex.foreach { case (OpinionsConvertCase(opinions, expectedPairs), i) =>
      s"correctly convert opinions on case $i" in {
        val actualPairs = TskvUtil.toMap("opinions", opinions)
        actualPairs should contain allElementsOf expectedPairs
      }
    }

    val SealedAbstractClassBasedConvert: Seq[(Line, Set[(String, String)])] =
      Seq(
        (ThinLine, Set("l" -> "ThinLine")),
        (MediumLine(255), Set("l" -> "MediumLine", "l.color" -> "255"))
      )

    SealedAbstractClassBasedConvert.foreach { case (line, expectedPairs) =>
      s"correctly convert $line" in {
        val actualPairs = TskvUtil.toMap("l", line)
        actualPairs should contain allElementsOf expectedPairs
      }
    }

    "correctly convert class" in {
      val line = new BoldLine(120)
      val actualPairs = TskvUtil.toMap("l", line)
      actualPairs.size should be(1)
      actualPairs("l").startsWith("ru.yandex.vertis.moderation.util.TskvUtilSpec$BoldLine") should be(true)
    }
  }
}
