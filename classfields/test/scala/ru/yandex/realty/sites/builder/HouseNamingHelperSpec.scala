package ru.yandex.realty.sites.builder

import org.apache.commons.text.WordUtils
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.sites.builder.HouseNamingHelper.readableName
import ru.yandex.vertis.generators.BasicGenerators

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class HouseNamingHelperSpec extends SpecBase with PropertyChecks {

  import HouseNamingHelperSpec._

  "HouseNamingHelper.readableName()" when {
    "handling specified values" should {
      "terminate timely and produce the expected outcomes" in {
        forAll(UnabridgedGen) { unabridgedCases =>
          readableName(buildInputFromParts(unabridgedCases)) shouldBe buildExpectedFromParts(unabridgedCases)
        }

        forAll(BuildingIdCasesGen) { buildingIdCase =>
          readableName(buildInputFromParts(List(buildingIdCase))) shouldBe buildExpectedFromParts(List(buildingIdCase))
        }

        forAll(PlotIdCasesGen) { plotIdCase =>
          readableName(buildInputFromParts(List(plotIdCase))) shouldBe buildExpectedFromParts(List(plotIdCase))
        }

        forAll(BuildingNameGen) { buildingNameCase =>
          readableName(buildInputFromParts(List(buildingNameCase))) shouldBe buildExpectedFromParts(
            List(buildingNameCase)
          )
        }

        forAll(PlotNameGen) { plotNameCase =>
          readableName(buildInputFromParts(List(plotNameCase))) shouldBe buildExpectedFromParts(List(plotNameCase))
        }

        forAll(PlotPartGen) { plotPartCase =>
          readableName(buildInputFromParts(List(plotPartCase))) shouldBe buildExpectedFromParts(List(plotPartCase))
        }

        forAll(PlaceGen) { placeCase =>
          readableName(buildInputFromParts(List(placeCase))) shouldBe buildExpectedFromParts(List(placeCase))
        }

        forAll(PrecedenceGen) { precedenceCase =>
          readableName(buildInputFromParts(List(precedenceCase))) shouldBe buildExpectedFromParts(List(precedenceCase))
        }

        forAll(DemesneGen) { demesneCase =>
          readableName(buildInputFromParts(List(demesneCase))) shouldBe buildExpectedFromParts(List(demesneCase))
        }

        forAll(ComboGen) { comboCases =>
          readableName(buildInputFromParts(comboCases)) shouldBe buildExpectedFromParts(comboCases)
        }
      }
    }
  }

}

object HouseNamingHelperSpec extends BasicGenerators {
  private def buildInputFromParts(parts: List[ReadableByOriginal]): String = {
    String.join(" ", parts.map(_.original).asJava)
  }

  private def buildExpectedFromParts(parts: List[ReadableByOriginal]): Option[String] = {
    Some(WordUtils.capitalize(String.join(" ", parts.map(_.readable.trim()).asJava)))
  }

  case class ReadableByOriginal(readable: String, original: String)

  private val BuildingIdCasesGen: Gen[ReadableByOriginal] = Gen.oneOf(
    Gen.const(ReadableByOriginal("корпус 1", "к. 1")),
    Gen.const(ReadableByOriginal("корпус 4, 5", "к. 4, 5")),
    Gen.const(ReadableByOriginal("корпус Ж", "к.Ж"))
  )

  private val PlotIdCasesGen: Gen[ReadableByOriginal] = Gen.oneOf(
    Gen.const(ReadableByOriginal("дом 4", "д.4")),
    Gen.const(ReadableByOriginal("дом 10, корпус 1", "д. 10, к.1")),
    Gen.const(ReadableByOriginal("дом 7.1, ЮГ", "д. 7.1, «ЮГ»"))
  )

  private val BuildingNameGen: Gen[ReadableByOriginal] = Gen.oneOf(
    Gen.const(ReadableByOriginal("амстердам", "к. «Амстердам»")),
    Gen.const(ReadableByOriginal("liberty", "к. Liberty"))
  )

  private val PlotNameGen: Gen[ReadableByOriginal] = Gen.oneOf(
    Gen.const(ReadableByOriginal("aqua", "д. Aqua")),
    Gen.const(ReadableByOriginal("liberty", "д.Liberty"))
  )

  private val PlotPartGen: Gen[ReadableByOriginal] =
    Gen.const(ReadableByOriginal("третьяков", "секция «Третьяков»"))

  private val PlaceGen: Gen[ReadableByOriginal] =
    Gen.const(ReadableByOriginal("позиция 21", "поз. 21"))

  private val PrecedenceGen: Gen[ReadableByOriginal] = Gen.oneOf(
    Gen.const(ReadableByOriginal("очередь III", "оч.III")),
    Gen.const(ReadableByOriginal("очередь IV, корпус 3", "Оч. IV, к. 3"))
  )

  private val DemesneGen: Gen[ReadableByOriginal] =
    Gen.const(ReadableByOriginal("владение 31", "вл. 31"))

  private val UnabridgedGen: Gen[List[ReadableByOriginal]] =
    Gen.listOf(readableString).map(list => list.map(item => ReadableByOriginal(item, item)))

  private val ComboGen: Gen[List[ReadableByOriginal]] = Gen.listOf(
    Gen.oneOf(
      DemesneGen,
      PrecedenceGen,
      PlaceGen,
      PlotPartGen,
      PlotNameGen,
      BuildingNameGen,
      PlotIdCasesGen,
      PlotIdCasesGen,
      BuildingIdCasesGen
    )
  )

}
