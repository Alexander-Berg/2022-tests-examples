package ru.auto.cabinet.service

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.cabinet.service.SearcherFilterProtocol._
import spray.json._

/** Testing on json parsing by [[SearcherFilterProtocol]]
  */
class SearcherFilterProtocolSpec extends FlatSpec with Matchers {

  val testResponse: SearcherFilterResponse =
    """
      | {
      |  "data":
      |  [
      |    {
      |     "handled-offers-count": 28,
      |     "mark-model-counts":
      |     {
      |       "SKODA":
      |       {
      |         "OCTAVIA": 13,
      |         "FABIA": 4,
      |         "YETI": 8
      |       },
      |       "JEEP":
      |       {
      |         "GRAND_CHEROKEE": 3
      |       }
      |      }
      |    }
      |  ],
      |  "errors": [{ "error": "foo" }]
      | }
    """.stripMargin.parseJson.convertTo[SearcherFilterResponse]

  "parsing of a json" should "works" in {
    (testResponse.data shouldNot be).equals(null)
    testResponse.data.length shouldBe 1
    testResponse.data.head.handledOffersCount shouldBe 28
    testResponse.data.head.markModelCounts.get("SKODA")("OCTAVIA") shouldBe 13
    testResponse.errors.isEmpty shouldBe false
  }

  "parsing of a json without mark-model-counts field" should "works" in {
    """
      | {
      |  "data":
      |  [
      |    {
      |     "handled-offers-count": 0
      |     }
      |    ],
      |  "errors": [{ "error": "foo" }]
      | }
    """.stripMargin.parseJson.convertTo[SearcherFilterResponse]
  }

  "counters" should "works" in {
    testResponse.allMarksCount() shouldBe 28
    testResponse.countByMark("SKODA") shouldBe 25
    testResponse.countByMark("JEEP") shouldBe 3
    testResponse.countByMarkAndModel("SKODA", "OCTAVIA") shouldBe 13
  }
}
