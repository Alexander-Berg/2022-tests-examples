package ru.yandex.auto.vin.decoder.raw.allelevators

import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.JsResultException
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.raw.allevacuators.AllEvacuatorsRawModel

class AllEvacuatorsRawModelTest extends AnyFunSuite {

  private val TestVin = VinCode.apply("XYFGA695JD3012372")

  test("Evacuator record exists") {
    val code = 200
    val raw = ResponseWithoutClientCard

    val result = AllEvacuatorsRawModel.apply(raw, code, TestVin)
    val record = result.records.head

    assert(record.car.name.contains("Chevrolet Cruze"))
    assert(record.toCity.contains("Москва"))
    assert(record.clientCard.isEmpty)
  }

  test("Empty record correctly parsed") {
    val code = 200
    val raw = EmptyResponse

    val model = AllEvacuatorsRawModel.apply(raw, code, TestVin)
    assert(model.records.isEmpty)
  }

  test("Invalid response propagates exception") {
    val code = 200
    val raw = InvalidResponse

    intercept[JsResultException](
      AllEvacuatorsRawModel.apply(raw, code, TestVin)
    )
  }

  private val ResponseWithoutClientCard: String =
    """{
      |    "72037": {
      |        "car": {
      |            "name": "Chevrolet Cruze",
      |            "number": "T089XY190",
      |            "vin": "XYFGA695JD3012372"
      |        },
      |        "executeDate": "2020-07-31 10:00:00",
      |        "orderTypeId": 1,
      |        "orderType": "Эвакуатор легковая",
      |        "fromAddress": "Одинцовский городской округ, Московская область, Россия, село Усово",
      |        "toAddress": "Москва, Россия, Коптевская улица, 69Ас1",
      |        "fromRegion": "Московская область",
      |        "toRegion": "Московская область",
      |        "fromCity": "Москва",
      |        "toCity": "Москва"
      |    }
      |}""".stripMargin

  private val EmptyResponse = "[]"

  private val InvalidResponse = """{
                          |    "meta": {
                          |        "error_code": 2,
                          |        "error_message": "meta/uid tag not found"
                          |    },
                          |    "data": []
                          |}""".stripMargin
}
