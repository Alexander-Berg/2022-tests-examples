package ru.yandex.realty.rent.clients.spectrumdata

import play.api.libs.json.Json
import ru.yandex.realty.util.Randoms

import scala.util.Random

object SpectrumDataJsonGen extends Randoms {
  val domainUid: String = nextString(5)
  val reportUid: String = nextString(5) + s"@$domainUid"
  val reportTypeUid: String = nextString(5) + s"@$domainUid"
  val isNew: Boolean = Random.nextBoolean()
  val suggestedGet = "2018-11-20T11:06:37.569Z"
  val processRequestUid: String = nextString(5)
  val passportSeries: String = nextStringifyInt(4)
  val passportNumber: String = nextStringifyInt(6)
  val expired: Boolean = Random.nextBoolean()
  val passportDetails: String = nextString(10)
  val stamp = "2018-11-20T13:59:27.141Z"
  val phone = "9001234567"
  val firstName: String = nextString(5)
  val lastName: String = nextString(5)
  val patronymic: String = nextString(5)
  val birthDay = "19.12.1983"
  val birthDate = "1983-12-19"
  val verifyPassportDescription: String = nextString(15)
  val place: String = nextString(15)
  val gender = "МУЖ"
  val nationality = "РУССКИЙ"
  val investigation: String = nextString(15)
  val region: String = nextString(15)
  val version = "2021-06-15T11:13:08.215+0000"
  val url = "http://www.fedsfm.ru/documents/terrorists-catalog-portal-act"
  val itemNumber: Int = Random.nextInt(9999)
  val itemId: String = nextString(15)
  val score: Double = Random.nextDouble()
  val date = "2001-01-01"
  val number = "43535/53/53643-ИП"
  val name = s"$lastName $firstName $patronymic"
  val numberComp = "5547/17/21003-СД"
  val typeCode = 3
  val typeName: String = nextString(15)
  val weNumber = "2-573/15"
  val riseDate = "2016-11-08"
  val subjectType: Int = Random.nextInt(100)
  val subjectTypeName: String = nextString(15)
  val issuer: String = nextString(15)
  val bailiffOfficeCode: Int = Random.nextInt(9999)
  val bailiffOfficeName: String = nextString(15)
  val bailiffOfficeAddress: String = nextString(15)
  val bailiffName: String = nextString(15)
  val bailiffPhone = "74833644001"
  val balance: Double = Random.nextDouble()
  val balanceFine: Double = Random.nextDouble()
  val balanceDuty: Double = Random.nextDouble()
  val balanceBudget: Double = Random.nextDouble()
  val balanceOther: Double = Random.nextDouble()
  val endDate = "2800-11-08"

  def createPassportReportRequest: String = Json.stringify(
    Json.parse(
      s"""
         |{
         |    "data": {
         |        "passport": "$passportSeries $passportNumber"
         |    },
         |    "queryType": "MULTIPART",
         |    "query": " "
         |}
         |""".stripMargin
    )
  )

  def createReportResponse: String = Json.stringify(
    Json.parse(
      s"""
         |{
         |    "state": "ok",
         |    "size": 1,
         |    "stamp": "$stamp",
         |    "data":
         |        [
         |         {
         |            "uid":"$reportUid",
         |            "isnew": $isNew,
         |            "process_request_uid": "$processRequestUid",
         |            "suggest_get": "$suggestedGet"
         |         }
         |        ]
         |}
         |""".stripMargin
    )
  )

  def spectrumReportResponse(content: String): String =
    s"""
       |{
       |  "state": "ok",
       |  "size": 1,
       |  "stamp": "$stamp",
       |  "data": [
       |    {
       |      "domain_uid": "$domainUid",
       |      "report_type_uid": "$reportTypeUid",
       |      "query": {
       |        "type": "MULTIPART",
       |        "body": " ",
       |        "data": {
       |          "passport": "$passportSeries $passportNumber"
       |        }
       |      },
       |      "progress_ok": 1,
       |      "progress_wait": 0,
       |      "progress_error": 0,
       |      "state": {
       |        "sources": [
       |          {
       |            "_id": "some_source",
       |            "state": "OK",
       |            "data": {}
       |          }
       |        ],
       |        "data": {}
       |      },
       |      "content": {
       |        $content
       |      }
       |    }
       |  ]
       |}
       |""".stripMargin

  def emptyReport: String = spectrumReportResponse(s"")

  def passportReport: String = spectrumReportResponse(s"""
       |"check_person": {
       | "passport": {
       |   "number": "$passportNumber",
       |   "series": "$passportSeries",
       |   "expired": $expired,
       |   "details": "$passportDetails"
       | }
       |}""")

  def verifyPassportRequest: String = Json.stringify(
    Json.parse(
      s"""
         |{
         |  "data": {
         |    "last_name": "$lastName",
         |    "first_name": "$firstName",
         |    "patronymic": "$patronymic",
         |    "birth": "$birthDay",
         |    "passport": "$passportSeries$passportNumber",
         |    "phone": "$phone"
         |  },
         |  "queryType": "MULTIPART",
         |  "query": " "
         |}
         |""".stripMargin
    )
  )

  def verifyPassportReport: String = spectrumReportResponse(s"""
       |  "check_person": {
       |    "verify_person": {
       |      "matchResult": "MATCH_FOUND",
       |      "description": "$verifyPassportDescription",
       |      "rawQuery": {
       |        "lastName": "$lastName",
       |        "firstName": "$firstName",
       |        "middleName": "$patronymic",
       |        "phoneNumber": "$phone",
       |        "passportNumber": "$passportSeries$passportNumber",
       |        "birthDate": "$birthDate"
       |      }
       |    }
       |  }""")

  def createPersonRequest: String = Json.stringify(
    Json.parse(
      s"""
         |{
         |  "data": {
         |    "first_name": "$firstName",
         |    "last_name": "$lastName",
         |    "patronymic": "$patronymic",
         |    "birth": "$birthDay"
         |  },
         |  "queryType": "MULTIPART",
         |  "query": " "
         |}
         |""".stripMargin
    )
  )

  def wantedReport: String = spectrumReportResponse(s"""
       |"check_person": {
       | "wanted": {
       |   "count": 1,
       |   "items": [{
       |     "person": {
       |        "first_name": "$firstName",
       |        "last_name": "$lastName",
       |        "patronymic": "$patronymic",
       |        "gender": "$gender",
       |        "nationality": "$nationality",
       |        "birth": {
       |          "date": "$birthDay",
       |          "place": "$place"
       |        },
       |        "image": {
       |          "url": ""
       |        }
       |     },
       |     "contact": {
       |        "information": "$phone"
       |     },
       |     "base": {
       |        "investigation": "$investigation"
       |     },
       |     "region": "$region"
       |   }]
       | }
       |}""")

  def extremistsReport: String = spectrumReportResponse(s"""
       |"check_person": {
       | "extremists": {
       |   "found": true,
       |   "isActive": false,
       |   "version": "$version",
       |   "score": $score,
       |   "items": [{
       |      "matched_middle_name": true,
       |      "matched_region": false,
       |      "matched_birth_date": true,
       |      "score": $score,
       |      "isActive": false,
       |      "isAdded": false,
       |      "isDeleted": true,
       |      "item": {
       |        "evidence": {
       |                "version": "$version",
       |                "url": "$url"
       |        },
       |        "number": $itemNumber,
       |        "last_name": "$lastName",
       |        "first_name": "$firstName",
       |        "middle_name": "$patronymic",
       |        "birth_date": "$birthDate",
       |        "address": "$place"
       |       }
       |   }]
       | }
       |}""")

  def proceedingExecutiveReport: String = spectrumReportResponse(s"""
       |"check_person": {
       | "proceeding_v2": {
       |    "executive_v2": {
       |      "items": [{
       |        "entBdate": "$date",
       |        "epNumber": "$number",
       |        "name": "$name",
       |        "epNumberComp": "$numberComp",
       |        "entBplace": "$place",
       |        "weType": $typeCode,
       |        "weTypeName": "$typeName",
       |        "weNumber": "$number",
       |        "weDate": "$date",
       |        "weRisedate": "$riseDate",
       |        "weSubjType": $subjectType,
       |        "weSubjTypeName": "$subjectTypeName",
       |        "weIssuer": "$issuer",
       |        "bailiffOfficeCode": $bailiffOfficeCode,
       |        "bailiffOfficeAddress": "$bailiffOfficeAddress",
       |        "bailiffName": "$bailiffName",
       |        "bailiffPhone": "$bailiffPhone",
       |        "debtBalanceEp": $balance,
       |        "debtBalanceFine": $balanceFine,
       |        "debtBalanceDuty": $balanceDuty,
       |        "debtBalanceBudg": $balanceBudget,
       |        "debtBalanceOther": $balanceOther,
       |        "unloadStatus": "C",
       |        "endDate": "$endDate"
       |      }]
       |    }
       | }
       |}""")
}
