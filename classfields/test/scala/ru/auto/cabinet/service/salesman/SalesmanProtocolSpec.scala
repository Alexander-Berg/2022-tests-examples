package ru.auto.cabinet.service.salesman

import java.time.{Instant, OffsetDateTime}

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.cabinet.environment.TimeZone
import spray.json._

class SalesmanProtocolSpec
    extends FlatSpec
    with Matchers
    with SalesmanProtocol {

  "Salesman protocol" should "parse good" in {
    val s =
      """
        |{
        |  "epoch": 1506932465789,
        |  "offerHash": "7ca16b",
        |  "to": "2017-10-03T11:18:02.000+03:00",
        |  "from": "2017-09-27T11:18:02.000+03:00",
        |  "category": "cars",
        |  "offer": 1053404466,
        |  "product": "all_sale_premium"
        |}
      """.stripMargin
    val good = goodReader.read(s.parseJson)
    good.offerId shouldBe "1053404466-7ca16b"
    good.epoch shouldBe OffsetDateTime.ofInstant(
      Instant.ofEpochMilli(1506932465789L),
      TimeZone)
  }

  it should "throw error for non-salesman json" in {
    a[SalesmanException] should be thrownBy goodReader.read("{}".parseJson)
  }
}
