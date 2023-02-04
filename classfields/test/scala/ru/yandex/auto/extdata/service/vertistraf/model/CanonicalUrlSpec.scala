package ru.yandex.auto.traffic.model

import io.circe.parser.decode
import org.scalatest.{FlatSpec, Matchers}

class CanonicalUrlSpec extends FlatSpec with Matchers {

  it should "decode [[CanonicalUrl]] from a valid JSON string" in {
    val rawJson =
      """
      |[
      |  "https://test.avto.ru/reviews/cars/citroen/c_elysee/8552559/"
      |]
      |""".stripMargin

    decode[List[CanonicalUrl]](rawJson).isRight shouldBe true
  }

  it should "not decode [[CanonicalUrl]] from an empty JSON string" in {
    val rawJson =
      """
      |[
      |  ""
      |]
      |""".stripMargin

    decode[List[CanonicalUrl]](rawJson).isLeft shouldBe true
  }

  it should "not decode [[CanonicalUrl]] from a JSON string that contains query parameter" in {
    val rawJson =
      """
      |[
      |  "https://test.avto.ru/reviews/cars/citroen/c_elysee/?name=value"
      |]
      |""".stripMargin

    decode[List[CanonicalUrl]](rawJson).isLeft shouldBe true
  }

  it should "returns correct segments of the URL" in {
    val urlOpt = CanonicalUrl.make("/cars/audi/a3/2018-year/used/transmission-automatic/").toOption

    urlOpt should not be empty

    val urlSegments = urlOpt.get.segments

    urlSegments shouldBe List("cars", "audi", "a3", "2018-year", "used", "transmission-automatic")
  }
}
