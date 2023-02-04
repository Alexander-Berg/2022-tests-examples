package ru.yandex.auto.extdata.service.canonical.methods

import cats.syntax.either._
import io.circe.parser.decode
import io.circe.{Decoder, Json}
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.auto.traffic.model._

class GetReviewUrlSpec extends FlatSpec with Matchers {

  it should "encode [[GetReviewUrl]] (with `super_gen`)" in {
    val method = GetReviewUrl(Mark.Code("CITROEN"), Some(Model.Code("C_ELYSEE")), Some(SuperGenerationId(8552559L)))
    val actual = GetReviewUrl.method.encoder(method)

    val expected = decode[Json]("""
        |{
        |    "data": [
        |        {
        |            "params": {
        |                "type": "reviews-listing-cars",
        |                "parent_category": "cars",
        |                "mark": "CITROEN",
        |                "model": "C_ELYSEE",
        |                "super_gen": 8552559
        |            }
        |        }
        |    ]
        |}
        |""".stripMargin)

    actual.asRight shouldBe expected
  }

  it should "encode [[GetReviewUrl]] (without `super_gen`)" in {
    val method = GetReviewUrl(Mark.Code("CITROEN"), Some(Model.Code("C_ELYSEE")), None)
    val actual = GetReviewUrl.method.encoder(method)

    val expected = decode[Json]("""
        |{
        |    "data": [
        |        {
        |            "params": {
        |                "type": "reviews-listing-cars",
        |                "parent_category": "cars",
        |                "mark": "CITROEN",
        |                "model": "C_ELYSEE"
        |            }
        |        }
        |    ]
        |}
        |""".stripMargin)

    actual.asRight shouldBe expected
  }

  it should "decode [[GetReviewUrl]]'s result from a valid JSON" in {
    val rawJson =
      """
      |{
      |    "urls": [
      |        {
      |            "url": "https://test.avto.ru/reviews/cars/citroen/c_elysee/8552559/"
      |        }
      |    ]
      |}
      |""".stripMargin

    implicit val decoder: Decoder[CanonicalUrl] = GetReviewUrl.method.decoder
    decode[CanonicalUrl](rawJson) shouldBe (CanonicalUrl("https://test.avto.ru/reviews/cars/citroen/c_elysee/8552559/").asRight)
  }

  it should "not decode [[GetReviewUrl]]'s result from an invalid JSON (url contains query parameter)" in {
    val rawJson =
      """
      |{
      |    "urls": [
      |        {
      |            "url": "https://test.avto.ru/reviews/cars/citroen/c_elysee/8552559/?name=value"
      |        }
      |    ]
      |}
      |""".stripMargin

    implicit val decoder: Decoder[CanonicalUrl] = GetReviewUrl.method.decoder
    decode[CanonicalUrl](rawJson).isLeft shouldBe true
  }

}
