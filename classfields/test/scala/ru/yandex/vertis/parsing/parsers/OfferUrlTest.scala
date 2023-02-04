package ru.yandex.vertis.parsing.parsers

import org.apache.http.client.methods.HttpGet
import org.scalatest.{FunSuite, Matchers}

class OfferUrlTest extends FunSuite with Matchers {
  test("null query") {
    val rawUrl =
      "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb|aifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818/"
    val offerUrl = OfferUrl(rawUrl)
    offerUrl.cleanUrl shouldBe "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb%7Caifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818"
    offerUrl.fullUrl shouldBe "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb%7Caifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818"
  }

  test("trim slashes") {
    val rawUrl =
      "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb|aifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818/?arg=test&arg=test&arg2=tt"
    val offerUrl = OfferUrl(rawUrl)
    offerUrl.cleanUrl shouldBe "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb%7Caifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818"
    offerUrl.fullUrl shouldBe "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb%7Caifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818?arg%3Dtest%26arg%3Dtest%26arg2%3Dtt"
  }

  test("url encoding") {
    val rawUrl =
      "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb|aifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818?arg=test&arg=test&arg2=tt"
    val offerUrl = OfferUrl(rawUrl)
    offerUrl.cleanUrl shouldBe "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb%7Caifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818"
    offerUrl.fullUrl shouldBe "https://youla.ru/moskva/smartfony-planshety/smartfony/iphone-11-pro-max-512-gb%7Caifon-11-pro-maks-512-gb-61543fae06e81e7ae06e2818?arg%3Dtest%26arg%3Dtest%26arg2%3Dtt"
    noException shouldBe thrownBy(new HttpGet(offerUrl.fullUrl))
  }
}
