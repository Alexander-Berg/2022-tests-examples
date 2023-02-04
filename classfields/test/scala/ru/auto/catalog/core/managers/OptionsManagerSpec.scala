package ru.auto.catalog.core.managers

import ru.auto.catalog.BaseSpec
import ru.auto.catalog.core.model.raw.cars.{CarsCatalogWrapper, Equipment}
import ru.auto.catalog.model.api.ApiModel.DescriptionParseRequest
import ru.yandex.vertis.baker.util.api.{Request, RequestImpl}

class OptionsManagerSpec extends BaseSpec {
  private val carsCatalog = mock[CarsCatalogWrapper]
  private val manager = new OptionsManager(carsCatalog)

  implicit private val req: Request = new RequestImpl

  "Options manager" should {

    "parse default options" in {
      val params = DescriptionParseRequest
        .newBuilder()
        .setDescription("first second third something else F S TTW ddd two words")
        .build()

      when(carsCatalog.equipment)
        .thenReturn(
          Map(
            "first" -> Equipment("first", "first", Map.empty, List.empty, List.empty, Map("first" -> "(first)".r)),
            "second" -> Equipment("second", "second", Map.empty, List.empty, List.empty, Map("second" -> "(second)".r)),
            "two words" -> Equipment(
              "two words",
              "two words",
              Map.empty,
              List.empty,
              List.empty,
              Map("two words" -> "(two words)".r)
            )
          )
        )

      val res = manager.parseDescriptionToOptions(params)

      assertResult(3)(res.getOptionsCount)
      assertResult(1)(res.getOptions(0).getMeta.getFoundAtCount)
      assertResult(0)(res.getOptions(0).getMeta.getFoundAt(0).getStart)
      assertResult(5)(res.getOptions(0).getMeta.getFoundAt(0).getEnd)
      assertResult(1)(res.getOptions(0).getMeta.getAliasesCount)
      assertResult("first")(res.getOptions(0).getMeta.getAliases(0))
    }

    "parse options with aliases" in {
      val params = DescriptionParseRequest
        .newBuilder()
        .setDescription("feurst second third two words forst something else F S TTW ddd")
        .build()

      when(carsCatalog.equipment)
        .thenReturn(
          Map(
            "first" -> Equipment(
              "first",
              "first",
              Map.empty,
              List.empty,
              List.empty,
              Map("feurst" -> "(feurst)".r, "forst" -> "(forst)".r)
            ),
            "second" -> Equipment(
              "second",
              "second",
              Map.empty,
              List.empty,
              List.empty,
              Map("second" -> "(second)".r, "scnd" -> "(scnd)".r)
            ),
            "two words" -> Equipment(
              "two words",
              "two words",
              Map.empty,
              List.empty,
              List.empty,
              Map("two words" -> "(two words)".r)
            )
          )
        )

      val res = manager.parseDescriptionToOptions(params)

      assertResult(3)(res.getOptionsCount)
      assertResult(2)(res.getOptions(0).getMeta.getFoundAtCount)
      assertResult(30)(res.getOptions(0).getMeta.getFoundAt(1).getStart)
      assertResult(35)(res.getOptions(0).getMeta.getFoundAt(1).getEnd)
      assertResult(2)(res.getOptions(0).getMeta.getAliasesCount)
      assertResult("forst")(res.getOptions(0).getMeta.getAliases(1))
    }

    "dont parse option inside a word" in {
      val params = DescriptionParseRequest
        .newBuilder()
        .setDescription("some газа word")
        .build()

      when(carsCatalog.equipment)
        .thenReturn(
          Map(
            "long" -> Equipment(
              "long",
              "long",
              Map.empty,
              List.empty,
              List.empty,
              Map("газ" -> OptionsManager.aliasToRegex("газ"))
            )
          )
        )

      val res = manager.parseDescriptionToOptions(params)

      assertResult(0)(res.getOptionsCount)
    }

    checkNds("НДС", true)
    checkNds("а ндс а", true)
    checkNds("андса", false)
    checkNds("без ндс", false)
    checkNds("без НДС", false)
    checkNds("нет ндс", false)
    checkNds("нет НДС", false)
    checkNds("НДС нет", false)
    checkNds("ндс нет", false)
    checkNds("НДС не", false)
    checkNds("ндс не", false)
  }

  private def checkNds(input: String, expected: Boolean) =
    s"""return show_with_nds=$expected for "$input"""" in {
      val params = DescriptionParseRequest.newBuilder().setDescription(input).build()

      when(carsCatalog.equipment).thenReturn(Map.empty[String, Equipment])

      val result = manager.parseDescriptionToOptions(params)

      assertResult(expected)(result.hasShowWithNds)
      if (expected) {
        assertResult(true)(result.getShowWithNds.getValue())
      }
    }
}
