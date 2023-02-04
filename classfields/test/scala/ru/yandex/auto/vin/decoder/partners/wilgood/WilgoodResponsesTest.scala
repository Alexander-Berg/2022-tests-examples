package ru.yandex.auto.vin.decoder.partners.wilgood

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.partners.wilgood.WilgoodResponses._

class WilgoodResponsesTest extends AnyFunSuite {

  private val rawResponse =
    """
      |{
      |  "STATUS": "OK",
      |  "CODE": "4",
      |  "MESSAGE": {
      |    "vin": "0000NZT2400033851",
      |    "service_book": {
      |      "mark": "TOYOTA",
      |      "name": "TOYOTA Premio 1.5 бензин (109 л.с.) Серый № К515АР154 VIN 0000NZT2400033851 2002 г.в.",
      |      "year": "2002",
      |      "model": "Premio",
      |      "orders": [
      |        {
      |          "mileage": "288000",
      |          "products": [
      |            {
      |              "name": "",
      |              "brand": ""
      |            }
      |          ],
      |          "services": [
      |            {
      |              "name": "Колодки тормозные задние - замена"
      |            },
      |            {
      |              "name": "Втулки стабилизатора переднего - замена"
      |            }
      |          ],
      |          "sto_city": "Новосибирск",
      |          "sto_name": "АС \"Новосибирск-2\"",
      |          "order_date": "2017-03-09",
      |          "description": "слесарка",
      |          "recommendations": ""
      |        }
      |      ]
      |    }
      |  }
      |}
    """.stripMargin

  test("deserialization") {
    assert(Json.parse(rawResponse).validate[InfoResponse].isSuccess)
  }
}
