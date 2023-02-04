package ru.yandex.vertis.shark

import baker.common.client.dadata.model.Organization
import baker.common.client.dadata.model.Responses.SuccessResponse
import io.circe.Decoder
import io.circe.parser._
import ru.yandex.vertis.zio_baker.scalapb_utils.Validation.ValidationException
import ru.yandex.vertis.shark.model.{Api, AutoruCreditApplication}

import scala.util.{Failure, Success, Try}

trait TmsStaticSamples {

  private val jsonDadataOrganization: String =
    """
      |{
      |  "suggestions": [
      |    {
      |      "value": "ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"",
      |      "unrestricted_value": "ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"",
      |      "data": {
      |        "kpp": "770501001",
      |        "capital": {
      |          "type": "УСТАВНЫЙ КАПИТАЛ",
      |          "value": 10000
      |        },
      |        "management": {
      |          "name": "Штань Данила Александрович",
      |          "post": "ГЕНЕРАЛЬНЫЙ ДИРЕКТОР",
      |          "disqualified": null
      |        },
      |        "founders": [
      |          {
      |            "ogrn": "5157746192742",
      |            "inn": "7704340327",
      |            "name": "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.ВЕРТИКАЛИ\"",
      |            "hid": "d41472b48a34a8387fd1fc8134e99634d5275bfacc2a3d2c2841b17bbd688211",
      |            "type": "LEGAL",
      |            "share": {
      |              "value": 100,
      |              "type": "PERCENT"
      |            }
      |          }
      |        ],
      |        "managers": [
      |          {
      |            "inn": "667115230784",
      |            "fio": {
      |              "surname": "Штань",
      |              "name": "Данила",
      |              "patronymic": "Александрович",
      |              "gender": "MALE",
      |              "source": "ШТАНЬ ДАНИЛА АЛЕКСАНДРОВИЧ",
      |              "qc": null
      |            },
      |            "post": "ГЕНЕРАЛЬНЫЙ ДИРЕКТОР",
      |            "hid": "ed8f82d8b0caafc4da3dfd2dce188289aad9d907bd8c4e2afe536ca49c960605",
      |            "type": "EMPLOYEE"
      |          }
      |        ],
      |        "predecessors": null,
      |        "successors": null,
      |        "branch_type": "MAIN",
      |        "branch_count": 0,
      |        "source": null,
      |        "qc": null,
      |        "hid": "1e648f9fbec150621a28c9973b13f23f97b000a401ee8e6fd484e2381271adc0",
      |        "type": "LEGAL",
      |        "state": {
      |          "status": "ACTIVE",
      |          "code": null,
      |          "actuality_date": 1609459200000,
      |          "registration_date": 1469664000000,
      |          "liquidation_date": null
      |        },
      |        "opf": {
      |          "type": "2014",
      |          "code": "12300",
      |          "full": "Общество с ограниченной ответственностью",
      |          "short": "ООО"
      |        },
      |        "name": {
      |          "full_with_opf": "ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"",
      |          "short_with_opf": "ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"",
      |          "latin": null,
      |          "full": "ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ",
      |          "short": "ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ"
      |        },
      |        "inn": "7704366364",
      |        "ogrn": "1167746714156",
      |        "okpo": "03751534",
      |        "okato": "45286560000",
      |        "oktmo": "45376000000",
      |        "okogu": "4210014",
      |        "okfs": "16",
      |        "okved": "62.01",
      |        "okveds": [
      |          {
      |            "main": true,
      |            "type": "2014",
      |            "code": "62.01",
      |            "name": "Разработка компьютерного программного обеспечения"
      |          },
      |          {
      |            "main": false,
      |            "type": "2014",
      |            "code": "62.02",
      |            "name": "Деятельность консультативная и работы в области компьютерных технологий"
      |          },
      |          {
      |            "main": false,
      |            "type": "2014",
      |            "code": "62.03",
      |            "name": "Деятельность по управлению компьютерным оборудованием"
      |          },
      |          {
      |            "main": false,
      |            "type": "2014",
      |            "code": "63.11.1",
      |            "name": "Деятельность по созданию и использованию баз данных и информационных ресурсов"
      |          },
      |          {
      |            "main": false,
      |            "type": "2014",
      |            "code": "63.99",
      |            "name": "Деятельность информационных служб прочая, не включенная в другие группировки"
      |          },
      |          {
      |            "main": false,
      |            "type": "2014",
      |            "code": "70.22",
      |            "name": "Консультирование по вопросам коммерческой деятельности и управления"
      |          },
      |          {
      |            "main": false,
      |            "type": "2014",
      |            "code": "73.20",
      |            "name": "Исследование конъюнктуры рынка и изучение общественного мнения"
      |          }
      |        ],
      |        "authorities": {
      |          "fts_registration": {
      |            "type": "FEDERAL_TAX_SERVICE",
      |            "code": "7746",
      |            "name": "Межрайонная инспекция Федеральной налоговой службы № 46 по г. Москве",
      |            "address": "125373, г.Москва, Походный проезд, домовладение 3, стр.2"
      |          },
      |          "fts_report": {
      |            "type": "FEDERAL_TAX_SERVICE",
      |            "code": "7705",
      |            "name": "Инспекция Федеральной налоговой службы № 5 по г. Москве",
      |            "address": null
      |          },
      |          "pf": {
      |            "type": "PENSION_FUND",
      |            "code": "087105",
      |            "name": "Государственное учреждение - Главное Управление Пенсионного фонда РФ №10 Управление №3 по г. Москве и Московской области муниципальный район Замоскворечье г.Москвы",
      |            "address": null
      |          },
      |          "sif": {
      |            "type": "SOCIAL_INSURANCE_FUND",
      |            "code": "7711",
      |            "name": "Филиал №11 Государственного учреждения - Московского регионального отделения Фонда социального страхования Российской Федерации",
      |            "address": null
      |          }
      |        },
      |        "documents": {
      |          "fts_registration": {
      |            "type": "FTS_REGISTRATION",
      |            "series": "77",
      |            "number": "017856662",
      |            "issue_date": 1469750400000,
      |            "issue_authority": "7746"
      |          },
      |          "pf_registration": {
      |            "type": "PF_REGISTRATION",
      |            "series": null,
      |            "number": "087105113873",
      |            "issue_date": 1502064000000,
      |            "issue_authority": "087105"
      |          },
      |          "sif_registration": {
      |            "type": "SIF_REGISTRATION",
      |            "series": null,
      |            "number": "771107712177111",
      |            "issue_date": 1470268800000,
      |            "issue_authority": "7711"
      |          },
      |          "smb": null
      |        },
      |        "licenses": null,
      |        "finance": {
      |          "tax_system": null,
      |          "income": 863926000,
      |          "expense": 828995000,
      |          "debt": null,
      |          "penalty": null
      |        },
      |        "address": {
      |          "value": "г Москва, ул Садовническая, д 82 стр 2, пом 3А",
      |          "unrestricted_value": "115035, г Москва, р-н Замоскворечье, ул Садовническая, д 82 стр 2, пом 3А",
      |          "data": {
      |            "postal_code": "115035",
      |            "country": "Россия",
      |            "country_iso_code": "RU",
      |            "federal_district": "Центральный",
      |            "region_fias_id": "0c5b2444-70a0-4932-980c-b4dc0d3f02b5",
      |            "region_kladr_id": "7700000000000",
      |            "region_iso_code": "RU-MOW",
      |            "region_with_type": "г Москва",
      |            "region_type": "г",
      |            "region_type_full": "город",
      |            "region": "Москва",
      |            "area_fias_id": null,
      |            "area_kladr_id": null,
      |            "area_with_type": null,
      |            "area_type": null,
      |            "area_type_full": null,
      |            "area": null,
      |            "city_fias_id": "0c5b2444-70a0-4932-980c-b4dc0d3f02b5",
      |            "city_kladr_id": "7700000000000",
      |            "city_with_type": "г Москва",
      |            "city_type": "г",
      |            "city_type_full": "город",
      |            "city": "Москва",
      |            "city_area": "Центральный",
      |            "city_district_fias_id": null,
      |            "city_district_kladr_id": null,
      |            "city_district_with_type": "р-н Замоскворечье",
      |            "city_district_type": "р-н",
      |            "city_district_type_full": "район",
      |            "city_district": "Замоскворечье",
      |            "settlement_fias_id": null,
      |            "settlement_kladr_id": null,
      |            "settlement_with_type": null,
      |            "settlement_type": null,
      |            "settlement_type_full": null,
      |            "settlement": null,
      |            "street_fias_id": "b0eb036a-e240-4dc3-9dfb-7b01d3ed8aa5",
      |            "street_kladr_id": "77000000000719100",
      |            "street_with_type": "ул Садовническая",
      |            "street_type": "ул",
      |            "street_type_full": "улица",
      |            "street": "Садовническая",
      |            "house_fias_id": "c718f386-2aed-46cf-8a74-08de8b6181dc",
      |            "house_kladr_id": "7700000000071910177",
      |            "house_type": "д",
      |            "house_type_full": "дом",
      |            "house": "82",
      |            "block_type": "стр",
      |            "block_type_full": "строение",
      |            "block": "2",
      |            "entrance": null,
      |            "floor": null,
      |            "flat_fias_id": null,
      |            "flat_type": "пом",
      |            "flat_type_full": "помещение",
      |            "flat": "3А",
      |            "flat_area": null,
      |            "square_meter_price": null,
      |            "flat_price": null,
      |            "postal_box": null,
      |            "fias_id": "c718f386-2aed-46cf-8a74-08de8b6181dc",
      |            "fias_code": "77000000000000071910177",
      |            "fias_level": "8",
      |            "fias_actuality_state": "0",
      |            "kladr_id": "7700000000071910177",
      |            "geoname_id": "524901",
      |            "capital_marker": "0",
      |            "okato": "45286560000",
      |            "oktmo": "45376000",
      |            "tax_office": "7705",
      |            "tax_office_legal": "7705",
      |            "timezone": "UTC+3",
      |            "geo_lat": "55.7355205",
      |            "geo_lon": "37.6425496",
      |            "beltway_hit": "IN_MKAD",
      |            "beltway_distance": null,
      |            "metro": [
      |              {
      |                "name": "Павелецкая",
      |                "line": "Кольцевая",
      |                "distance": 0.6
      |              },
      |              {
      |                "name": "Павелецкая",
      |                "line": "Замоскворецкая",
      |                "distance": 0.7
      |              },
      |              {
      |                "name": "Таганская",
      |                "line": "Таганско-Краснопресненская",
      |                "distance": 0.8
      |              }
      |            ],
      |            "qc_geo": "0",
      |            "qc_complete": null,
      |            "qc_house": null,
      |            "history_values": null,
      |            "unparsed_parts": null,
      |            "source": "115035, ГОРОД МОСКВА, УЛИЦА САДОВНИЧЕСКАЯ, ДОМ 82, СТРОЕНИЕ 2, ПОМЕЩЕНИЕ 3А08",
      |            "qc": "0"
      |          }
      |        },
      |        "phones": null,
      |        "emails": null,
      |        "ogrn_date": 1469664000000,
      |        "okved_type": "2014",
      |        "employee_count": 274
      |      }
      |    }
      |  ]
      |}
      |""".stripMargin

  private val jsonCreditApplication: String =
    """
      |{
      |  "result": {
      |    "ok": {}
      |  },
      |  "creditApplication": {
      |    "id": "6f07818c-0256-462a-bef8-44343a28879a",
      |    "created": "2020-11-20T11:53:34.676Z",
      |    "updated": "2021-03-05T08:54:47.578Z",
      |    "scheduledAt": "2021-03-05T09:04:47.626Z",
      |    "schedulerLastUpdate": "2021-03-05T08:54:47.626Z",
      |    "userId": "auto_17830914",
      |    "domain": "DOMAIN_AUTO",
      |    "state": "ACTIVE",
      |    "requirements": {
      |      "maxAmount": "490000",
      |      "initialFee": "500000",
      |      "termMonths": 13,
      |      "geobaseIds": [
      |        "10884"
      |      ]
      |    },
      |    "payload": {
      |      "autoru": {
      |        "offers": [
      |          {
      |            "category": "CARS",
      |            "id": "1114417209-926f4502"
      |          }
      |        ]
      |      }
      |    },
      |    "claims": [
      |      {
      |        "id": "53dd1ac2-fc05-4fe8-af92-eb9dcc2f5f48",
      |        "created": "2020-12-11T09:09:01.473Z",
      |        "updated": "2020-12-14T15:16:52.346Z",
      |        "bankClaimId": "",
      |        "creditProductId": "tinkoff-2",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "NOT_SENT",
      |        "bankState": "",
      |        "approvedMaxAmount": "0",
      |        "approvedTermMonths": 0,
      |        "approvedInterestRate": 0,
      |        "approvedMinInitialFeeRate": 0
      |      },
      |      {
      |        "id": "84745b5f-26bf-43e4-8585-098108ec4223",
      |        "created": "2021-02-24T10:28:30.498Z",
      |        "updated": "2021-03-03T08:25:28.124Z",
      |        "bankClaimId": "",
      |        "creditProductId": "rosgosstrah-1",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "NOT_SENT",
      |        "bankState": "",
      |        "approvedMaxAmount": "0",
      |        "approvedTermMonths": 0,
      |        "approvedInterestRate": 0,
      |        "approvedMinInitialFeeRate": 0,
      |        "processAfter": "2021-02-24T10:29:30.498Z"
      |      },
      |      {
      |        "id": "cd80748b-7f2d-4f51-838d-dc06f583155d",
      |        "created": "2020-12-11T09:09:01.473Z",
      |        "updated": "2020-12-14T17:02:01.213Z",
      |        "bankClaimId": "e46986946021dd3f89e0d304dcd0dfcf",
      |        "creditProductId": "tinkoff-1",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "NEW",
      |        "bankState": "",
      |        "approvedMaxAmount": "0",
      |        "approvedTermMonths": 0,
      |        "approvedInterestRate": 0,
      |        "approvedMinInitialFeeRate": 0
      |      },
      |      {
      |        "id": "3b24eb64-2df0-4ed5-b8e8-b6718b3cdf18",
      |        "created": "2021-02-12T11:28:47.244Z",
      |        "updated": "2021-03-05T08:54:47.670Z",
      |        "bankClaimId": "0ceb6c6d-5412-445e-852c-64918243f5bd",
      |        "creditProductId": "gazprombank-1",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "PREAPPROVED",
      |        "bankState": "INPUT_INITIAL",
      |        "approvedMaxAmount": "0",
      |        "approvedTermMonths": 0,
      |        "approvedInterestRate": 0,
      |        "approvedMinInitialFeeRate": 0,
      |        "processAfter": "2021-02-12T11:29:47.244Z"
      |      },
      |      {
      |        "id": "9134999f-0287-49a6-95c3-4da929d2404e",
      |        "created": "2020-12-09T12:21:33.621Z",
      |        "updated": "2020-12-18T11:14:51.187Z",
      |        "bankClaimId": "OAPI20201218PLL001913097859956",
      |        "creditProductId": "raiffeisen-1",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "APPROVED",
      |        "bankState": "UNISSUED",
      |        "approvedMaxAmount": "2267000",
      |        "approvedTermMonths": 60,
      |        "approvedInterestRate": 9.99,
      |        "approvedMinInitialFeeRate": 0,
      |        "processAfter": "2020-12-09T12:21:33.621Z"
      |      },
      |      {
      |        "id": "test-id-sovcombank-111",
      |        "created": "2020-12-11T09:09:01.473Z",
      |        "updated": "2020-12-14T17:02:01.213Z",
      |        "bankClaimId": "_ulbPf4ltEA_Fij-Yulnk",
      |        "creditProductId": "sovcombank-1",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "NEW",
      |        "bankState": "",
      |        "approvedMaxAmount": "0",
      |        "approvedTermMonths": 0,
      |        "approvedInterestRate": 0,
      |        "approvedMinInitialFeeRate": 0
      |      },
      |      {
      |        "id": "test-claim-alfa-1",
      |        "created": "2021-05-25T10:45:01.473Z",
      |        "updated": "2021-05-25T10:45:01.473Z",
      |        "bankClaimId": "",
      |        "creditProductId": "alfabank-1",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "DRAFT",
      |        "bankState": "",
      |        "approvedMaxAmount": "0",
      |        "approvedTermMonths": 0,
      |        "approvedInterestRate": 0,
      |        "approvedMinInitialFeeRate": 0
      |      },
      |      {
      |        "id": "test-claim-alfa-2",
      |        "created": "2021-05-25T10:45:01.473Z",
      |        "updated": "2021-05-25T10:45:01.473Z",
      |        "bankClaimId": "",
      |        "creditProductId": "alfabank-2",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "DRAFT",
      |        "bankState": "",
      |        "approvedMaxAmount": "0",
      |        "approvedTermMonths": 0,
      |        "approvedInterestRate": 0,
      |        "approvedMinInitialFeeRate": 0
      |      },
      |      {
      |        "id": "test-claim-dealer-1",
      |        "created": "2022-01-18T10:45:01.473Z",
      |        "updated": "2022-01-18T10:45:01.473Z",
      |        "bankClaimId": "",
      |        "creditProductId": "dealer-1",
      |        "claimPayload": {
      |          "autoru": {
      |            "offerEntities": []
      |          }
      |        },
      |        "state": "DRAFT",
      |        "bankState": "",
      |        "approvedMaxAmount": "0",
      |        "approvedTermMonths": 0,
      |        "approvedInterestRate": 0,
      |        "approvedMinInitialFeeRate": 0
      |      }
      |    ],
      |    "scores": [],
      |    "notifications": [],
      |    "info": {
      |      "controlWord": {
      |        "word": "лужа"
      |      },
      |      "okbStatementAgreement": {
      |        "isAgree": true,
      |        "timestamp": "2020-11-20T11:53:34.600Z"
      |      },
      |      "advertStatementAgreement": {
      |        "isAgree": true,
      |        "timestamp": "2020-11-20T11:53:34.600Z"
      |      }
      |    },
      |    "borrowerPersonProfile": {
      |      "id": "auto_17830914-6f07818c-0256-462a-bef8-44343a28879a-0",
      |      "userId": "auto_17830914",
      |      "blockTypes": [
      |        "NAME",
      |        "OLD_NAME",
      |        "GENDER",
      |        "PASSPORT_RF",
      |        "OLD_PASSPORT_RF",
      |        "FOREIGN_PASSPORT",
      |        "DRIVER_LICENSE",
      |        "BIRTH_DATE",
      |        "BIRTH_PLACE",
      |        "RESIDENCE_ADDRESS",
      |        "REGISTRATION_ADDRESS",
      |        "EDUCATION",
      |        "MARITAL_STATUS",
      |        "DEPENDENTS",
      |        "INCOME",
      |        "EXPENSES",
      |        "PROPERTY_OWNERSHIP",
      |        "VEHICLE_OWNERSHIP",
      |        "EMPLOYMENT",
      |        "RELATED_PERSONS",
      |        "PHONES",
      |        "EMAILS"
      |      ],
      |      "name": {
      |        "nameEntity": {
      |          "id": "",
      |          "userId": "",
      |          "name": "Василий",
      |          "surname": "Иванов",
      |          "patronymic": ""
      |        }
      |      },
      |      "oldName": {
      |        "nameEntity": {
      |          "id": "",
      |          "userId": "",
      |          "name": "Василий",
      |          "surname": "Петров",
      |          "patronymic": ""
      |        }
      |      },
      |      "gender": {
      |        "genderType": "MALE"
      |      },
      |      "passportRf": {
      |        "passportRfEntity": {
      |          "id": "",
      |          "userId": "",
      |          "series": "1234",
      |          "number": "506789",
      |          "issueDate": "2005-02-02T00:00:00Z",
      |          "departCode": "987-654",
      |          "departName": "АВТОЗАВОДСКИМ РУВД Г. ТОЛЬЯТТИ САМАРСКОЙ ОБЛ."
      |        }
      |      },
      |      "oldPassportRf": {
      |        "no": {}
      |      },
      |      "foreignPassport": {
      |        "no": {}
      |      },
      |      "driverLicense": {
      |        "driverLicenseEntity": {
      |          "id": "",
      |          "userId": "",
      |          "number": "7701397000",
      |          "issueDate": "2017-12-13T00:00:00Z",
      |          "issuerName": "ГИБДД 5801"
      |        }
      |      },
      |      "birthDate": {
      |        "birthDate": "1982-11-25T00:00:00Z"
      |      },
      |      "birthPlace": {
      |        "country": "Россия",
      |        "city": "Самарская обл г Тольятти"
      |      },
      |      "residenceAddress": {
      |        "addressEntity": {
      |          "id": "",
      |          "userId": "",
      |          "region": "г Москва",
      |          "city": "г Москва",
      |          "district": "г Москва",
      |          "street": "пр-кт Вернадского",
      |          "building": "99",
      |          "corpus": "1",
      |          "construction": "",
      |          "apartment": "",
      |          "postCode": 119526,
      |          "settlement": "",
      |          "kladr": {
      |            "id": "7700000000009530230",
      |            "regionId": "7700000000000",
      |            "areaId": "",
      |            "cityId": "7700000000000",
      |            "cityDistrictId": "",
      |            "settlementId": "",
      |            "streetId": "77000000000095300",
      |            "houseId": "7700000000009530230"
      |          },
      |          "fias": {
      |            "regionId": "0c5b2444-70a0-4932-980c-b4dc0d3f02b5",
      |            "areaId": "",
      |            "cityId": "0c5b2444-70a0-4932-980c-b4dc0d3f02b5",
      |            "cityDistrictId": "",
      |            "settlementId": "",
      |            "streetId": "1e3e2818-9fe2-4981-89c7-612cec372a8c",
      |            "houseId": "89441876-8f58-4aa1-92ed-417eb8f07e79",
      |            "id": "89441876-8f58-4aa1-92ed-417eb8f07e79",
      |            "code": "77000000000000009530230",
      |            "level": "8",
      |            "actualityState": "0"
      |          }
      |        }
      |      },
      |      "registrationAddress": {
      |        "registrationDate": "1999-04-02T00:00:00Z",
      |        "addressEntity": {
      |          "id": "",
      |          "userId": "",
      |          "region": "Самарская обл",
      |          "city": "г Тольятти",
      |          "district": "г Тольятти",
      |          "street": "Обводное шоссе",
      |          "building": "3",
      |          "corpus": "",
      |          "construction": "",
      |          "apartment": "",
      |          "postCode": 143362,
      |          "settlement": "",
      |          "kladr": {
      |            "id": "63000007000079900",
      |            "regionId": "6300000000000",
      |            "areaId": "",
      |            "cityId": "6300000700000",
      |            "cityDistrictId": "",
      |            "settlementId": "",
      |            "streetId": "63000007000079900",
      |            "houseId": ""
      |          },
      |          "fias": {
      |            "regionId": "df3d7359-afa9-4aaa-8ff9-197e73906b1c",
      |            "areaId": "",
      |            "cityId": "242e87c1-584d-4360-8c4c-aae2fe90048e",
      |            "cityDistrictId": "",
      |            "settlementId": "",
      |            "streetId": "98c1babe-5633-4dad-8bd6-48846f8e29f3",
      |            "houseId": "",
      |            "id": "98c1babe-5633-4dad-8bd6-48846f8e29f3",
      |            "code": "6300000700000000799",
      |            "level": "7",
      |            "actualityState": "0"
      |          }
      |        }
      |      },
      |      "education": {
      |        "state": "INCOMPLETE_HIGHER"
      |      },
      |      "maritalStatus": {
      |        "state": "DIVORCED"
      |      },
      |      "dependents": {
      |        "amountOfChildren": 1
      |      },
      |      "income": {
      |        "avgMonthlyIncome": "100000",
      |        "incomeProof": "BY_2NDFL"
      |      },
      |      "expenses": {
      |        "avgMonthlyExpenses": "40000"
      |      },
      |      "propertyOwnership": {
      |        "hasProperty": false
      |      },
      |      "vehicleOwnership": {
      |        "no": {}
      |      },
      |      "employment": {
      |        "employed": {
      |          "orgName": "ООО \"ЯНДЕКС.ВЕРТИКАЛИ ТЕХНОЛОГИИ\"",
      |          "inn": "7704366364",
      |          "headCount": 274,
      |          "okveds": [
      |            "62.01"
      |          ],
      |          "addressEntity": {
      |            "id": "",
      |            "userId": "",
      |            "region": "г Москва",
      |            "city": "г Москва",
      |            "district": "р-н Замоскворечье",
      |            "street": "ул Садовническая",
      |            "building": "82",
      |            "corpus": "",
      |            "construction": "2",
      |            "apartment": "3А",
      |            "postCode": 115035,
      |            "settlement": "",
      |            "kladr": {
      |              "id": "7700000000071910177",
      |              "regionId": "7700000000000",
      |              "areaId": "",
      |              "cityId": "7700000000000",
      |              "cityDistrictId": "",
      |              "settlementId": "",
      |              "streetId": "77000000000719100",
      |              "houseId": "7700000000071910177"
      |            },
      |            "fias": {
      |              "regionId": "0c5b2444-70a0-4932-980c-b4dc0d3f02b5",
      |              "areaId": "",
      |              "cityId": "0c5b2444-70a0-4932-980c-b4dc0d3f02b5",
      |              "cityDistrictId": "",
      |              "settlementId": "",
      |              "streetId": "b0eb036a-e240-4dc3-9dfb-7b01d3ed8aa5",
      |              "houseId": "c718f386-2aed-46cf-8a74-08de8b6181dc",
      |              "id": "c718f386-2aed-46cf-8a74-08de8b6181dc",
      |              "code": "77000000000000071910177",
      |              "level": "8",
      |              "actualityState": "0"
      |            }
      |          },
      |          "employee": {
      |            "positionType": "IT_SPECIALIST",
      |            "lastExperienceMonths": 72,
      |            "phones": [
      |              "74950011111"
      |            ]
      |          }
      |        }
      |      },
      |      "relatedPersons": {
      |        "relatedPersons": [
      |          {
      |            "nameEntity": {
      |              "id": "",
      |              "userId": "",
      |              "name": "Незнайка",
      |              "surname": "Налуне",
      |              "patronymic": ""
      |            },
      |            "phoneEntity": {
      |              "id": "",
      |              "userId": "",
      |              "phone": "79267010002",
      |              "phoneType": "UNKNOWN_PHONE_TYPE"
      |            },
      |            "relatedPersonType": "FRIEND",
      |            "birthDate": "1982-11-25T00:00:00Z"
      |          }
      |        ]
      |      },
      |      "phones": {
      |        "phoneEntities": [
      |          {
      |            "id": "",
      |            "userId": "",
      |            "phone": "79267010001",
      |            "phoneType": "UNKNOWN_PHONE_TYPE"
      |          }
      |        ]
      |      },
      |      "emails": {
      |        "emailEntities": [
      |          {
      |            "id": "",
      |            "userId": "",
      |            "email": "vasyivanov@yandex.ru"
      |          }
      |        ]
      |      }
      |    },
      |    "communication": {
      |      "autoruExternal": {
      |        "updated": "2021-03-03T07:47:28.644Z",
      |        "eventScheduledAt": "2021-03-03T08:02:28.644Z",
      |        "completenessState": "ALMOST_COMPLETLY",
      |        "objectCommunicationState": "SELECTED",
      |        "claimEntities": [
      |          {
      |            "creditProductId": "tinkoff-2",
      |            "state": "SENT"
      |          },
      |          {
      |            "creditProductId": "rosgosstrah-1",
      |            "state": "SENT"
      |          },
      |          {
      |            "creditProductId": "tinkoff-1",
      |            "state": "SENT"
      |          },
      |          {
      |            "creditProductId": "gazprombank-1",
      |            "state": "SENT"
      |          },
      |          {
      |            "creditProductId": "raiffeisen-1",
      |            "state": "SENT"
      |          }
      |        ]
      |      }
      |    }
      |  }
      |}
      |""".stripMargin

  def sampleDadataOrganization: SuccessResponse[Organization] = {
    import baker.common.client.dadata.converter.JsonConverter._
    parse[SuccessResponse[Organization]](jsonDadataOrganization)
  }

  def sampleCreditApplication: AutoruCreditApplication = {
    import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoJson._
    import ru.yandex.vertis.shark.converter.protobuf.Implicits._
    parse[Api.CreditApplicationResponse](jsonCreditApplication).creditApplication.get.asAuto
  }

  private def parse[T: Decoder](json: String): T = {
    Try(decode[T](json)) match {
      case Success(Right(value)) => value
      case Success(Left(exception)) =>
        println(s"обсер парсера: $exception")
        throw exception
      case Failure(ValidationException(fields)) =>
        for (f <- fields) {
          println(s"${f.name}: ${f.message}")
        }
        throw new Exception("обсер валидатора, но где то там...")
      case Failure(exception) =>
        println(s"тупо обсер: $exception")
        throw exception
    }
  }

}
object TmsStaticSamples extends TmsStaticSamples
