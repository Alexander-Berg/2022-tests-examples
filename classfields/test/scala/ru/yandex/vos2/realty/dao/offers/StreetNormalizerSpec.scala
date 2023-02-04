package ru.yandex.vos2.realty.dao.offers

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.Checkers

@RunWith(classOf[JUnitRunner])
class StreetNormalizerSpec extends WordSpecLike with Matchers with Checkers {

  "Normalizer" should {

    "convert to lower case" in {
      StreetNormalizer.normalize("Открытое что-то") shouldBe "открытое что-то"
    }

    "properly handle whitespace" in {
      StreetNormalizer.normalize(" открытое  что-то   ") shouldBe "открытое что-то"
    }

    "ignore commas" in {
      StreetNormalizer.normalize("открытое,, что-то,") shouldBe "открытое что-то"
    }

    "ignore dots" in {
      StreetNormalizer.normalize("..открытое. что-то.") shouldBe "открытое что-то"
    }

    "handle ё" in {
      StreetNormalizer.normalize("зелёная") shouldBe "зеленая"
    }

    "ignore stop words" in {
      StreetNormalizer.normalize("открытое шоссе") shouldBe "открытое"
      StreetNormalizer.normalize("шоссе открытое") shouldBe "открытое"
      StreetNormalizer.normalize("ш. открытое") shouldBe "открытое"
      StreetNormalizer.normalize("открытое ш.") shouldBe "открытое"

      StreetNormalizer.normalize("открытая улица") shouldBe "открытая"
      StreetNormalizer.normalize("улица открытая") shouldBe "открытая"
      StreetNormalizer.normalize("ул. открытая") shouldBe "открытая"
      StreetNormalizer.normalize("открытая ул.") shouldBe "открытая"

      StreetNormalizer.normalize("открытый переулок") shouldBe "открытый"
      StreetNormalizer.normalize("переулок открытый") shouldBe "открытый"
      StreetNormalizer.normalize("пер. открытый") shouldBe "открытый"
      StreetNormalizer.normalize("открытый пер.") shouldBe "открытый"

      StreetNormalizer.normalize("открытый проспект") shouldBe "открытый"
      StreetNormalizer.normalize("проспект открытый") shouldBe "открытый"
      StreetNormalizer.normalize("пр. открытый") shouldBe "открытый"
      StreetNormalizer.normalize("открытый пр.") shouldBe "открытый"

      StreetNormalizer.normalize("открытый проезд") shouldBe "открытый"
      StreetNormalizer.normalize("проезд открытый") shouldBe "открытый"

      StreetNormalizer.normalize("открытый тупик") shouldBe "открытый"
      StreetNormalizer.normalize("тупик открытый") shouldBe "открытый"
      StreetNormalizer.normalize("открытый т.") shouldBe "открытый"
      StreetNormalizer.normalize("т. открытый") shouldBe "открытый"

      StreetNormalizer.normalize("открытая площадь") shouldBe "открытая"
      StreetNormalizer.normalize("площадь открытая") shouldBe "открытая"
      StreetNormalizer.normalize("пл. открытая") shouldBe "открытая"
      StreetNormalizer.normalize("открытая пл.") shouldBe "открытая"
    }
  }
}
