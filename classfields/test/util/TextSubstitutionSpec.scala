package ru.yandex.vertis.general.wizard.api.util

import ru.yandex.vertis.general.wizard.core.utils.TextSubstitution
import zio.test.{DefaultRunnableSpec, ZSpec}
import zio.ZIO
import zio.test.Assertion._
import zio.test._

object TextSubstitutionSpec extends DefaultRunnableSpec {

  private val ArgsLessTemplate: String =
    "Большая база объявлений от частников и компаний — размещайте, продавайте и покупайте товары на Яндекс.Объявлениях."

  private def substituteSpec(name: String, template: String, args: Map[String, String], expected: String) = {
    testM(name) {
      ZIO.succeed(TextSubstitution.substitute(template, args)).map(assert(_)(equalTo(expected)))
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TextSubstitution")(
      substituteSpec(
        "return string itself without params",
        ArgsLessTemplate,
        Map.empty,
        ArgsLessTemplate
      ),
      substituteSpec(
        "correctly substitute param",
        "Яндекс.Объявления — свежие объявления ${REGION_PREPOSITION}",
        Map("REGION_PREPOSITION" -> "в Санкт-Петербурге"),
        "Яндекс.Объявления — свежие объявления в Санкт-Петербурге"
      ),
      substituteSpec(
        "drop unexisting arg",
        "Яндекс.Объявления — свежие объявления ${REGION_PREPOSITION}",
        Map.empty,
        "Яндекс.Объявления — свежие объявления"
      ),
      substituteSpec(
        name = "work in special case #1",
        "${CATEGORY} ${PROIZVODITEL-MOBILNOGO-TELEFONA_454GHB} — свежие объявления ${REGION_PREPOSITION}",
        Map(
          "CATEGORY" -> "Мобильные телефоны",
          "PROIZVODITEL-MOBILNOGO-TELEFONA_454GHB" -> "Samsung",
          "REGION_PREPOSITION" -> "в Санкт-Петербурге"
        ),
        "Мобильные телефоны Samsung — свежие объявления в Санкт-Петербурге"
      ),
      substituteSpec(
        name = "work in special case #2",
        "${CATEGORY} ${PROIZVODITEL-MOBILNOGO-TELEFONA_454GHB} — свежие объявления ${REGION_PREPOSITION}",
        Map(
          "CATEGORY" -> "Мобильные телефоны",
          "REGION_PREPOSITION" -> "в Санкт-Петербурге"
        ),
        "Мобильные телефоны — свежие объявления в Санкт-Петербурге"
      ),
      substituteSpec(
        "drop whitespaces between args if not exist",
        "text ${arg1} ${arg2} ${arg3}",
        Map("arg1" -> "1", "arg3" -> "3"),
        "text 1 3"
      ),
      substituteSpec(
        "return empty if args not exist",
        "${1} ${mama} ${papa-3} ${dada_4}",
        Map.empty,
        ""
      ),
      substituteSpec(
        "correctly work with spaces less",
        "${CATEGORY},${PROIZVODITEL-MOBILNOGO-TELEFONA_454GHB}—свежие объявления",
        Map("CATEGORY" -> "Мобильные телефоны"),
        "Мобильные телефоны,—свежие объявления"
      )
    )
}
