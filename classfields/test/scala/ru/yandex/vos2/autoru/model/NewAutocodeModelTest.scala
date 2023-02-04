package ru.yandex.vos2.autoru.model

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers._
import play.api.libs.json.Json
import ru.auto.api.DiffLogModel.BodyNumberDiff.BodyNumberType
import NewAutocodeModelTest._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NewAutocodeModelTest extends AnyFunSuite {
  test("parse autocode response") {
    val json = Json.parse(testResponse)
    val model = NewAutocodeModel.modelReads.reads(json).get

    model.data.identifiers shouldBe defined
    model.data.identifiers.get.vin.isDefined shouldBe true
    model.data.identifiers.get.body.isDefined shouldBe true
    model.data.identifiers.get.chassis.isDefined shouldBe true

    model.data.identifiers.get.getIdentifierWithType shouldBe Some(BodyNumberType.VIN -> "JHMFD16707S210107")
  }

  test("prefer values in correct order ") {
    val none = Identifiers(None, None, None, Some("X000X00"))
    none.getIdentifierWithType shouldBe None
    val vin = Identifiers(Some("vin"), Some("s"), None, Some("X000X00"))
    vin.getIdentifierWithType shouldBe Some(BodyNumberType.VIN -> "vin")
    val body = Identifiers(None, Some("body"), Some("ch"), Some("X000X00"))
    body.getIdentifierWithType shouldBe Some(BodyNumberType.BODY -> "body")
    val chassis = Identifiers(None, None, Some("chassis"), Some("X000X00"))
    chassis.getIdentifierWithType shouldBe Some(BodyNumberType.CHASSIS -> "chassis")
  }
}

object NewAutocodeModelTest {
  val testResponse = """{
    |  "state": "ok",
    |  "size": 1,
    |  "stamp": "2018-02-07T15:05:37.913Z",
    |  "data": [
    |    {
    |      "domain_uid": "autoru",
    |      "report_type_uid": "autoru_1_sources@autoru",
    |      "vehicle_id": "Т082ВВ777",
    |      "query": {
    |        "type": "GRZ",
    |        "body": "Т082ВВ777"
    |      },
    |      "progress_ok": 2,
    |      "progress_wait": 0,
    |      "progress_error": 0,
    |      "state": {
    |        "sources": [
    |          {
    |            "_id": "base",
    |            "state": "OK",
    |            "data": {
    |              "from_cache": false,
    |              "real_status": "OK"
    |            }
    |          },
    |          {
    |            "_id": "sub.base",
    |            "state": "OK",
    |            "data": {
    |              "from_cache": false,
    |              "real_status": "OK"
    |            }
    |          }
    |        ]
    |      },
    |      "content": {
    |        "identifiers": {
    |          "vehicle": {
    |            "vin": "JHMFD16707S210107",
    |            "body": "LWEW178453",
    |            "chassis": "LWEW178453",
    |            "reg_num": "Т082ВВ777"
    |          }
    |        },
    |        "tech_data": {
    |          "_comment": "Характеристики ТС",
    |          "brand": {
    |            "_comment": "Марка",
    |            "name": {
    |              "original": "ХОНДА ЦИВИК"
    |            }
    |          },
    |          "model": {
    |            "_comment": "Модель"
    |          },
    |          "type": {
    |            "_comment": "Тип (Вид) ТС",
    |            "name": "Седан"
    |          },
    |          "body": {
    |            "_comment": "Кузов",
    |            "color": {
    |              "name": "Синий Темный",
    |              "type": "Иные Цвета"
    |            }
    |          },
    |          "chassis": {
    |            "_comment": "Шасси"
    |          },
    |          "engine": {
    |            "_comment": "Двигатель",
    |            "fuel": {
    |              "type": "Бензиновый"
    |            },
    |            "volume": 1799,
    |            "power": {
    |              "hp": 138.7,
    |              "kw": 102
    |            }
    |          },
    |          "weight": {
    |            "_comment": "Масса",
    |            "netto": 1292,
    |            "max": 1700
    |          },
    |          "transmission": {
    |            "_comment": "Трансмиссия"
    |          },
    |          "drive": {
    |            "_comment": "Привод"
    |          },
    |          "wheel": {
    |            "_comment": "Рулевое колесо",
    |            "position": "LEFT"
    |          },
    |          "year": 2007
    |        },
    |        "additional_info": {
    |          "vehicle": {
    |            "category": {
    |              "code": "B"
    |            }
    |          }
    |        }
    |      },
    |      "uid": "autoru_1_sources_eyJ0eXBlIjoiR1JaIiwiYm9keSI6ItCiMDgy0JLQkjc3NyJ9@autoru",
    |      "name": "NONAME",
    |      "comment": "",
    |      "tags": "",
    |      "created_at": "2018-02-07T15:05:09.560Z",
    |      "created_by": "system",
    |      "updated_at": "2018-02-07T15:05:12.378Z",
    |      "updated_by": "system",
    |      "active_from": "1900-01-01T00:00:00.000Z",
    |      "active_to": "3000-01-01T00:00:00.000Z"
    |    }
    |  ]
    |}""".stripMargin
}
