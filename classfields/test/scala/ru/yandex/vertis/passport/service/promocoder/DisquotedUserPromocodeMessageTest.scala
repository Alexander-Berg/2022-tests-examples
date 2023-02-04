package ru.yandex.vertis.passport.service.promocoder

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.test.SpecBase

class DisquotedUserPromocodeMessageTest extends WordSpec with SpecBase {

  "DisquotedUserPromocodeMessage" should {
    "offersPluralGenitive" in {
      Seq(
        1 -> "первого объявления",
        2 -> "двух объявлений",
        3 -> "трёх объявлений",
        4 -> "четырёх объявлений",
        5 -> "5 объявлений",
        10 -> "10 объявлений",
        11 -> "11 объявлений",
        21 -> "21 объявления"
      ).foreach {
        case (n, expected) =>
          DisquotedUserPromocodeMessage.offersPluralGenitive(n) shouldBe expected
      }
    }

    "daysPluralNominative" in {
      Seq(
        1 -> "день",
        2 -> "дня",
        3 -> "дня",
        4 -> "дня",
        5 -> "дней",
        10 -> "дней",
        11 -> "дней",
        21 -> "день",
        22 -> "дня",
        23 -> "дня"
      ).foreach {
        case (n, expected) =>
          DisquotedUserPromocodeMessage.daysPluralNominative(n) shouldBe expected
      }
    }
  }
}
