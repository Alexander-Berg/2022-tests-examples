package ru.yandex.auto.vin.decoder.partners.autocode.model

import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.data.Model

class AutocodeReportResponseSpec extends AnyFlatSpec with MockitoSugar with Matchers with BeforeAndAfter {

  import AutocodeReportResponseSpec._

  "AutocodeReportResponse" should "parse json" in {
    val parsed = Json.parse(Model.commonModel).validate[AutocodeReportResponse]
    parsed.isSuccess shouldBe true
    val model = parsed.get
    model.data.stWait shouldBe 2
    model.data.stOk shouldBe 10
    model.data.stError shouldBe 3
    model.data.stError shouldBe 3
    model.state shouldBe "ok"
    model.data.query.`type` shouldBe AutocodeQueryType.GRZ
    model.data.query.body shouldBe "А738ВН777"
    model.data.content.get.techData.get.powerHp shouldBe 184
    model.data.isRejected() shouldBe true
  }

  it should "parse small json" in {
    val parsed = Json.parse(AutocodeReportResponseSpec.auto_1_sources).validate[AutocodeReportResponse]

    parsed.isSuccess shouldBe true

    val model = parsed.get
    model.data.stOk shouldBe 2

    // TODO write some test for missing fields, and test all this fields for presence

  }

  it should "parse autocode-empty json" in {
    val parsed = Json.parse(AutocodeReportResponseSpec.newStatuses).validate[AutocodeReportResponse]

    parsed.isSuccess shouldBe true

    val model = parsed.get
    val err = model.data.sources
      .filter(state => state._id == SourceId.Base || state._id == SourceId.SubBase)
      .forall(_.state == SourceState.OK) &&
      model.data.content.flatMap(_.techData.map(_.markModel)).getOrElse("").trim.isEmpty

    err shouldBe true
  }

  it should "parse main update json" in {
    val parsed = Json.parse(AutocodeReportResponseSpec.mainUpdate).validate[AutocodeReportResponse]

    parsed.isSuccess shouldBe true

    val model = parsed.get
    model.isEmpty shouldBe false
  }

  it should "parse taxi data" in {
    val parsed = Json.parse(Model.commonModel).validate[AutocodeReportResponse]
    parsed.isSuccess shouldBe true
    val model = parsed.get
    val content = model.data.content.get
    content.taxi.size shouldBe 1
    val taxi = content.taxi.head
    taxi.start.get shouldBe "2018-06-29 00:00:00"
    taxi.end.get shouldBe "2023-06-28 00:00:00"
    taxi.cancel.get shouldBe "2022-06-28 00:00:00"
    taxi.licenseNumber.get shouldBe "0226178"
    taxi.licenseStatus.get shouldBe "ACTIVE"
    taxi.company.get shouldBe "Ракурс"
    taxi.ogrn.get shouldBe "1187746362726"
    taxi.tin.get shouldBe "7725487881"
    taxi.numberPlateIsYellow.get shouldBe true
    taxi.mark.get shouldBe "HYUNDAI"
    taxi.model.get shouldBe "SOLARIS"
    taxi.color.get shouldBe "Белый (бело-желто-серый)"
    taxi.regNum.get shouldBe "О992ЕР799"
    taxi.year.get shouldBe 2018
    taxi.regionCode.get shouldBe "50"
    taxi.cancelReason.get shouldBe "Лицензия отозвана из-за технических нарушений"
    taxi.permitNumber.get shouldBe "МО 0217326"
    taxi.cityName.get shouldBe "Московская область"
  }

  it should "parse taxi with missed data" in {
    val parsed = Json.parse(AutocodeReportResponseSpec.taxiWithMissedField).validate[AutocodeReportResponse]
    parsed.isSuccess shouldBe true
    val taxiList = parsed.get.data.content.get.taxi
    taxiList.size shouldBe 1
  }

  it should "parse erroneous main" in {
    val json = fromFile("autoru_main_report_XTA219050D0207245@autoru.json")
    json.isRecoverableError shouldBe true
  }

  it should "parse taxi with canceled source" in {
    val json = fromFile("autoru_taxi_report_X9FGXXEEDGAG11345@autoru.json")
    json.isRecoverableError shouldBe false
  }

  it should "parse diagnostic cards" in {
    val json = fromFile("autoru_diagnostic_cards_report@autoru.json")
    json.isRecoverableError shouldBe false
  }
}

object AutocodeReportResponseSpec {

  def fromFile(filename: String): AutocodeReportResponse = {
    val source = getClass.getResourceAsStream(s"/$filename")
    Json.parse(source).as[AutocodeReportResponse]
  }

  val auto_1_sources =
    """{
      |  "state": "ok",
      |  "size": 1,
      |  "stamp": "2018-06-20T14:52:05.103Z",
      |  "data": [
      |    {
      |      "domain_uid": "autoru",
      |      "report_type_uid": "autoru_1_sources@autoru",
      |      "vehicle_id": "С649НР43",
      |      "query": {
      |        "type": "GRZ",
      |        "body": "С649НР43"
      |      },
      |      "progress_ok": 2,
      |      "progress_wait": 0,
      |      "progress_error": 0,
      |      "state": {
      |        "sources": [
      |          {
      |            "_id": "base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "sub.base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          }
      |        ]
      |      },
      |      "content": {
      |        "identifiers": {
      |          "vehicle": {
      |            "vin": "XTH311000Y0969214",
      |            "reg_num": "А897ОН43"
      |          }
      |        },
      |        "tech_data": {
      |          "_comment": "Характеристики ТС",
      |          "brand": {
      |            "_comment": "Марка",
      |            "name": {
      |              "original": "ГАЗ 3110"
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
      |            "number": "311000Y0378924",
      |            "color": {
      |              "name": "Белый",
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
      |            "volume": 2300,
      |            "power": {
      |              "hp": 144.1,
      |              "kw": 106
      |            }
      |          },
      |          "weight": {
      |            "_comment": "Масса",
      |            "netto": 1400,
      |            "max": 1790
      |          },
      |          "transmission": {
      |            "_comment": "Трансмиссия"
      |          },
      |          "drive": {
      |            "_comment": "Привод",
      |            "type": "Переднеприводной"
      |          },
      |          "wheel": {
      |            "_comment": "Рулевое колесо",
      |            "position": "LEFT"
      |          },
      |          "year": 2000
      |        },
      |        "additional_info": {
      |          "vehicle": {
      |            "category": {
      |              "code": "B"
      |            }
      |          }
      |        }
      |      },
      |      "uid": "autoru_1_sources_eyJ0eXBlIjoiR1JaIiwiYm9keSI6ItChNjQ50J3QoDQzIn0=@autoru",
      |      "name": "NONAME",
      |      "comment": "",
      |      "tags": "",
      |      "created_at": "2018-06-07T14:32:25.011Z",
      |      "created_by": "system",
      |      "updated_at": "2018-06-07T14:32:27.907Z",
      |      "updated_by": "system",
      |      "active_from": "1900-01-01T00:00:00.000Z",
      |      "active_to": "3000-01-01T00:00:00.000Z"
      |    }
      |  ]
      |}""".stripMargin

  val newStatuses =
    """{
      |  "state": "ok",
      |  "size": 1,
      |  "stamp": "2018-06-20T14:52:05.103Z",
      |  "data": [
      |    {
      |      "domain_uid": "test4",
      |      "report_type_uid": "test_for_nic@test4",
      |      "vehicle_id": "XX8ZZZ61ZJG003029",
      |      "query": {
      |        "type": "VIN",
      |        "body": "XX8ZZZ61ZJG003029"
      |      },
      |      "progress_ok": 6,
      |      "progress_wait": 1,
      |      "progress_error": 1,
      |      "state": {
      |        "sources": [
      |          {
      |            "_id": "base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "base.moscow",
      |            "state": "ERROR",
      |            "extended_state": "ERROR"
      |          },
      |          {
      |            "_id": "customs.base",
      |            "state": "PROGRESS",
      |            "extended_state": "PROGRESS"
      |          },
      |          {
      |            "_id": "gibdd.dtp",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.history",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.restrict",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.wanted",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "sub.base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          }
      |        ]
      |      },
      |      "content": {
      |        "identifiers": {
      |          "_comment": "Идентификаторы",
      |          "vehicle": {
      |            "vin": "XX8ZZZ61ZJG003029"
      |          }
      |        },
      |        "tech_data": {
      |          "_comment": "Характеристики ТС",
      |          "manufacturer": {},
      |          "brand": {
      |            "_comment": "Марка",
      |            "name": {}
      |          },
      |          "model": {
      |            "_comment": "Модель",
      |            "name": {}
      |          },
      |          "type": {
      |            "_comment": "Тип (Вид) ТС"
      |          },
      |          "body": {
      |            "_comment": "Кузов",
      |            "color": {}
      |          },
      |          "chassis": {
      |            "_comment": "Шасси"
      |          },
      |          "engine": {
      |            "_comment": "Двигатель",
      |            "fuel": {},
      |            "model": {},
      |            "power": {}
      |          },
      |          "weight": {
      |            "_comment": "Масса"
      |          },
      |          "transmission": {
      |            "_comment": "Трансмиссия"
      |          },
      |          "drive": {
      |            "_comment": "Привод"
      |          },
      |          "wheel": {
      |            "_comment": "Рулевое колесо"
      |          }
      |        },
      |        "additional_info": {
      |          "_comment": "Дополнительная информация",
      |          "vehicle": {
      |            "category": {},
      |            "owner": {
      |              "geo": {}
      |            },
      |            "passport": {
      |              "date": {},
      |              "org": {}
      |            },
      |            "sts": {
      |              "date": {}
      |            },
      |            "notes": []
      |          },
      |          "identifiers": {
      |            "vin": {}
      |          }
      |        },
      |        "registration_actions": {
      |          "_comment": "Регистрационные действия",
      |          "items": [],
      |          "count": 0
      |        },
      |        "repairs": {
      |          "_comment": "Ремонтные работы",
      |          "history": {
      |            "_comment": "История ремонтных работ по страховке",
      |            "items": [],
      |            "count": 0
      |          }
      |        },
      |        "carfax": {
      |          "_comment": "Данные от CarFax",
      |          "check": {
      |            "_comment": "Данные быстрой проверки на наличие данных у CarFax"
      |          }
      |        },
      |        "calculate": {
      |          "_comment": "Калькуляторы",
      |          "tax": {
      |            "moscow": {
      |              "yearly": {}
      |            },
      |            "regions": {
      |              "yearly": {}
      |            }
      |          },
      |          "osago": []
      |        },
      |        "car_price": {
      |          "_comment": "Приблизительные стоимости аналогичных автомобилей",
      |          "items": [],
      |          "count": 0
      |        },
      |        "fines": {
      |          "_comment": "Штрафы",
      |          "items": [],
      |          "count": 0
      |        },
      |        "leasings": {
      |          "_comment": "Лизинг",
      |          "items": [],
      |          "count": 0
      |        },
      |        "customs": {
      |          "history": {
      |            "_comment": "История по таможне",
      |            "items": [],
      |            "count": 0
      |          }
      |        },
      |        "taxi": {
      |          "history": {
      |            "_comment": "Такси",
      |            "items": [],
      |            "count": 0
      |          },
      |          "used_in_taxi": false
      |        },
      |        "pledges": {
      |          "_comment": "Обременения на ТС",
      |          "items": [],
      |          "_delete": true,
      |          "count": 0
      |        },
      |        "insurance": {
      |          "osago": {
      |            "_comment": "ОСАГО",
      |            "items": [],
      |            "count": 0
      |          }
      |        },
      |        "accidents": {
      |          "history": {
      |            "_comment": "История ДТП",
      |            "items": [],
      |            "count": 0
      |          },
      |          "insurance": {
      |            "_comment": "История повреждений из страховых компаний",
      |            "items": [],
      |            "count": 0
      |          }
      |        },
      |        "restrictions": {
      |          "registration_actions": {
      |            "_comment": "Ограничения на рег. действия",
      |            "items": [],
      |            "count": 0,
      |            "has_restrictions": false
      |          }
      |        },
      |        "stealings": {
      |          "_comment": "Проверка на угон",
      |          "count": 0,
      |          "is_wanted": false,
      |          "items": []
      |        },
      |        "ownership": {
      |          "history": {
      |            "_comment": "История владения",
      |            "items": [],
      |            "count": 0
      |          }
      |        },
      |        "mileages": {
      |          "_comment": "Пробеги",
      |          "items": [],
      |          "count": 0
      |        },
      |        "diagnostic_cards": {
      |          "_comment": "Диагностические карты",
      |          "items": [],
      |          "count": 0
      |        },
      |        "utilizations": {
      |          "_comment": "Утилизация",
      |          "items": [],
      |          "was_utilized": false,
      |          "count": 0
      |        },
      |        "images": {
      |          "_comment": "Изображения, связанные с ТС",
      |          "photos": {
      |            "_comment": "Фотографии ТС",
      |            "items": [],
      |            "count": 0
      |          }
      |        },
      |        "driver_licenses": {
      |          "_comment": "Данные о водительских удостоверениях",
      |          "history": {
      |            "_comment": "История выданных водительских удостоверений",
      |            "items": [],
      |            "count": 0
      |          }
      |        },
      |        "persons": {
      |          "_comment": "Данные о физ. лицах, связанных с данным ТС",
      |          "owners": {
      |            "_comment": "Наиболее вероятные владельцы ТС",
      |            "items": [],
      |            "count": 0
      |          }
      |        },
      |        "_metadata": {
      |          "_comment": "Мета-данные, не связанные с отчётом",
      |          "report": {
      |            "version": "1.0.0.0"
      |          },
      |          "application": {
      |            "version": "1.11.0-201808231034",
      |            "uid": "avtoraport-processing@processing-node-01-staging-avto-dev-ru",
      |            "kill all humans?": "hmm, why not?"
      |          }
      |        }
      |      },
      |      "uid": "test_for_nic_XX8ZZZ61ZJG003029@test4",
      |      "name": "NONAME",
      |      "comment": "",
      |      "tags": "",
      |      "created_at": "2018-08-23T11:42:37.576Z",
      |      "created_by": "system",
      |      "updated_at": "2018-08-23T11:46:02.241Z",
      |      "updated_by": "system",
      |      "active_from": "1900-01-01T00:00:00.000Z",
      |      "active_to": "3000-01-01T00:00:00.000Z"
      |    }
      |  ]
      |}""".stripMargin

  val mainUpdate =
    """
      |{
      |  "data": [
      |    {
      |      "uid": "autoru_main_update_report_SJNFANF15U6164381@autoru",
      |      "name": "NONAME",
      |      "tags": "",
      |      "query": {
      |        "body": "SJNFANF15U6164381",
      |        "type": "VIN"
      |      },
      |      "state": {
      |        "sources": [
      |          {
      |            "_id": "gibdd.restrict",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.wanted",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "pledge",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.history",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "gibdd.dtp",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          }
      |        ]
      |      },
      |      "comment": "",
      |      "content": {
      |        "pledges": {
      |          "count": 0,
      |          "items": []
      |        },
      |        "accidents": {
      |          "history": {
      |            "count": 0,
      |            "items": []
      |          },
      |          "has_accidents": false
      |        },
      |        "ownership": {
      |          "history": {
      |            "count": 1,
      |            "items": [
      |              {
      |                "_id": 1380763672,
      |                "date": {
      |                  "start": "2012-03-31 00:00:00"
      |                },
      |                "owner": {
      |                  "type": "PERSON"
      |                },
      |                "last_operation": {
      |                  "code": "11",
      |                  "description": "Первичная регистрация"
      |                }
      |              }
      |            ]
      |          }
      |        },
      |        "stealings": {
      |          "count": 0,
      |          "items": [],
      |          "is_wanted": false
      |        },
      |        "restrictions": {
      |          "registration_actions": {
      |            "count": 1,
      |            "items": [
      |              {
      |                "_id": 2099733312,
      |                "date": {
      |                  "added": "2019-07-21 00:00:00",
      |                  "start": "2019-07-19 00:00:00"
      |                },
      |                "vehicle": {
      |                  "year": 2011
      |                },
      |                "restrict": {
      |                  "type": "Запрет на регистрационные действия",
      |                  "number": "1282165236/5059",
      |                  "reason": "Документ: 1282165236/5059 от 19.07.2019, лобачева юлия владимировна, спи: 46591018318924, ип: 788534/19/50059-ип от 06.03.2019"
      |                },
      |                "initiator": {
      |                  "name": "Судебный Пристав",
      |                  "region": {
      |                    "name": "Московская Область"
      |                  }
      |                }
      |              }
      |            ],
      |            "has_restrictions": true
      |          }
      |        }
      |      },
      |      "active_to": "3000-01-01T00:00:00.000Z",
      |      "created_at": "2019-08-28T15:20:32.133Z",
      |      "created_by": "system",
      |      "domain_uid": "autoru",
      |      "updated_at": "2019-09-02T12:09:01.745Z",
      |      "updated_by": "system",
      |      "vehicle_id": "SJNFANF15U6164381",
      |      "active_from": "1900-01-01T00:00:00.000Z",
      |      "progress_ok": 5,
      |      "progress_wait": 0,
      |      "progress_error": 0,
      |      "report_type_uid": "autoru_main_update_report@autoru"
      |    }
      |  ],
      |  "size": 1,
      |  "stamp": "2019-09-02T13:41:37.597Z",
      |  "state": "ok"
      |}
      |""".stripMargin

  val taxiWithMissedField =
    """
      |{
      |  "data": [
      |    {
      |      "uid": "autoru_taxi_report_XW8AD4NH9EK106090@autoru",
      |      "name": "NONAME",
      |      "tags": "",
      |      "query": {
      |        "body": "XW8AD4NH9EK106090",
      |        "type": "VIN"
      |      },
      |      "state": {
      |        "sources": [
      |          {
      |            "_id": "base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "av.taxi",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          },
      |          {
      |            "_id": "sub.base",
      |            "state": "OK",
      |            "extended_state": "OK"
      |          }
      |        ]
      |      },
      |      "comment": "",
      |      "content": {
      |        "taxi": {
      |          "history": {
      |            "count": 1,
      |            "items": [
      |              {
      |                "_id": 1853712838,
      |                "tin": "5031098242",
      |                "city": {
      |                  "name": "Московская область"
      |                },
      |                "date": {
      |                  "end": "2021-08-29 00:00:00",
      |                  "start": "2016-08-30 00:00:00"
      |                },
      |                "ogrn": "1115031007508",
      |                "permit": {
      |                  "number": "МО 0119860"
      |                },
      |                "region": {
      |                  "code": "50"
      |                },
      |                "company": {
      |                  "name": "Консультант"
      |                },
      |                "license": {
      |                  "number": "0117402",
      |                  "status": "ANNULLED"
      |                },
      |                "vehicle": {
      |                  "year": 2014,
      |                  "brand": {
      |                    "name": "SKODA"
      |                  },
      |                  "color": "Белый",
      |                  "model": {
      |                    "name": "RAPID"
      |                  },
      |                  "reg_num": "Х547КХ777"
      |                },
      |                "number_plate": {
      |                  "is_yellow": false
      |                }
      |              }
      |            ]
      |          },
      |          "used_in_taxi": true
      |        },
      |        "identifiers": {
      |          "vehicle": {
      |            "vin": "XW8AD4NH9EK106090",
      |            "reg_num": "Х547КХ777"
      |          }
      |        }
      |      },
      |      "active_to": "3000-01-01T00:00:00.000Z",
      |      "created_at": "2019-09-12T15:17:28.376Z",
      |      "created_by": "system",
      |      "domain_uid": "autoru",
      |      "updated_at": "2019-09-12T15:17:29.962Z",
      |      "updated_by": "system",
      |      "vehicle_id": "XW8AD4NH9EK106090",
      |      "active_from": "1900-01-01T00:00:00.000Z",
      |      "progress_ok": 3,
      |      "progress_wait": 0,
      |      "progress_error": 0,
      |      "report_type_uid": "autoru_taxi_report@autoru"
      |    }
      |  ],
      |  "size": 1,
      |  "stamp": "2019-09-12T16:03:09.307Z",
      |  "state": "ok"
      |}
      |""".stripMargin
}
