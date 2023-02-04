package ru.yandex.vertis.scoring.util

import sttp.client.Response
import sttp.model.{StatusCode, Uri}

object PassportResponseUtil {

  def getResponse(uri: Uri): Response[String] =
    uri.toString match {
      case s if s contains "3000000001" => Response.ok(validSuperfluousPassportResponse)
      case s if s contains "3000000002" => Response.ok(passportResponseWithEmptyPhones)
      case s if s contains "3000000003" => Response.ok(passportMessageMissingPhonesAndAttributes)
      case s if s contains "3000000004" => Response.ok(passportResponseErrorDBExceptionMessage)
      case s if s contains "3000000005" => Response.ok(passportResponseUserNotFound)
      case s if s contains "3000000006" => Response.ok(passportResponseMissingKarma)
      case s if s contains "3000000007" => Response.ok(passportResponseMissingUidInfo)
      case s if s contains "3000000008" => Response.ok(passportResponseErrorInvalidParamsMessage)
      case s if s contains "9000000000" => Response.ok(passportResponseDifferentUid)
      case s if s contains "9990000000" => Response.ok(passportResponseEmptyUsers)
      case s if s contains "3000000500" => Response("Internal Server Error", StatusCode.InternalServerError)
      case s if s contains "3000000404" => Response("Not Found", StatusCode.NotFound)
    }

  val validSuperfluousPassportResponse: String =
    """{
      |  "users": [
      |    {
      |      "id":"3000000001",
      |      "uid":
      |      {
      |        "value":"3000000001",
      |        "hosted":false,
      |        "domid":"",
      |        "domain":"",
      |        "mx":"",
      |        "domain_ena":"",
      |        "catch_all":""
      |      },
      |      "login":"test",
      |      "aliases":
      |      {
      |        "6":"uid-sjywgxrn"
      |      },
      |      "karma":
      |      {
      |        "value":85,
      |        "allow-until":1321965947
      |      },
      |      "karma_status":
      |      {
      |        "value":3085
      |      },
      |      "regname":"test",
      |      "display_name":
      |      {
      |        "name":"Козьма Прутков",
      |        "public_name":"Козьма П.",
      |        "avatar":
      |        {
      |          "default":"4000217463",
      |          "empty":false
      |        },
      |        "social":
      |        {
      |          "profile_id":"5328",
      |          "provider":"tw",
      |          "redirect_target":"1323266014.26924.5328.9e5e3b502d5ee16abc40cf1d972a1c17"
      |        }
      |      },
      |      "public_id": "mcat26m4cb7z951vv46zcbzgqt",
      |      "pin_status" : true,
      |      "dbfields":
      |      {
      |        "accounts.login.uid":"test",
      |        "userinfo.firstname.uid":null
      |      },
      |      "attributes" :
      |      {
      |        "25" : "1:Девичья фамилия матери",
      |        "1" : "1294999198"
      |      },
      |      "phones" : [
      |        {
      |          "id" : "2",
      |          "attributes" : {
      |            "6" : "1412183145",
      |            "105": 0,
      |            "106": 1
      |          }
      |        }
      |      ],
      |      "emails" : [
      |        {
      |          "id" : "2",
      |          "attributes" : {
      |            "1" : "my_email@gmail.com"
      |          }
      |        }
      |      ],
      |      "address-list": [
      |        {
      |          "address":"test@yandex.ru",
      |          "validated":true,
      |          "default":true,
      |          "rpop":false,
      |          "unsafe":false,
      |          "native":true,
      |          "born-date":"2011-11-16 00:00:00"
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  val passportResponseWithEmptyPhones: String =
    """{
      |  "users": [
      |    {
      |      "id":"3000000002",
      |      "uid":
      |      {
      |        "value":"3000000002",
      |        "hosted":false
      |      },
      |      "karma":
      |      {
      |        "value":85,
      |        "allow-until":1321965947
      |      },
      |      "karma_status":
      |      {
      |        "value":3085
      |      },
      |      "attributes" :
      |      {
      |        "25" : "1:Девичья фамилия матери",
      |        "1" : "1294999198"
      |      },
      |      "phones" : []
      |    }
      |  ]
      |}""".stripMargin

  val passportMessageMissingPhonesAndAttributes: String =
    """{
      |  "users": [
      |    {
      |      "id":"3000000003",
      |      "uid":
      |      {
      |        "value":"3000000003",
      |        "hosted":false
      |      },
      |      "karma":
      |      {
      |        "value":85,
      |        "allow-until":1321965947
      |      },
      |      "karma_status":
      |      {
      |        "value":3085
      |      }
      |    }
      |  ]
      |}""".stripMargin

  val passportResponseErrorDBExceptionMessage: String =
    """{
      |  "users": [
      |    {
      |      "exception": {
      |        "value": "DB_EXCEPTION",
      |        "id": 10
      |      },
      |      "error": "Fatal BlackBox error: dbpool exception in sezam dbfields fetch",
      |      "id": "3000000004"
      |    }
      |  ]
      |}""".stripMargin

  val passportResponseUserNotFound: String =
    """{
      |  "users": [
      |    {
      |      "id": "3000000005",
      |      "uid": {},
      |      "karma": {
      |        "value": 0
      |      },
      |      "karma_status": {
      |        "value": 0
      |      }
      |    }
      |  ]
      |}""".stripMargin

  val passportResponseMissingKarma: String =
    """{
      |  "users": [
      |    {
      |      "id":"3000000006",
      |      "uid":
      |      {
      |        "value":"3000000006",
      |        "hosted":false
      |      },
      |      "attributes":
      |      {
      |        "25" : "1:Девичья фамилия матери",
      |        "1" : "1294999198"
      |      },
      |      "phones" : [
      |        {
      |          "id" : "2",
      |          "attributes" : {
      |            "6" : "1412183145",
      |            "105": 0,
      |            "106": 1
      |          }
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  val passportResponseMissingUidInfo: String =
    """{
      |  "users": [
      |    {
      |      "id":"3000000007",
      |      "karma":
      |      {
      |        "value":85,
      |        "allow-until":3000000007
      |      },
      |      "karma_status":
      |      {
      |        "value":3085
      |      },
      |      "attributes":
      |      {
      |        "25" : "1:Девичья фамилия матери",
      |        "1" : "1294999198"
      |      },
      |      "phones" : [
      |        {
      |          "id" : "2",
      |          "attributes" : {
      |            "6" : "1412183145",
      |            "105": 0,
      |            "106": 1
      |          }
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  val passportResponseErrorInvalidParamsMessage: String =
    """{
      |  "exception":
      |  {
      |    "value":"INVALID_PARAMS",
      |    "id":2
      |  },
      |  "error":"BlackBox error: Missing userip argument"
      |}""".stripMargin

  val passportResponseDifferentUid: String =
    """{
      |  "users": [
      |    {
      |      "id":"9900000000",
      |      "uid":
      |      {
      |        "value":"9900000000",
      |        "hosted":false
      |      },
      |      "karma":
      |      {
      |        "value":85,
      |        "allow-until":1321965947
      |      },
      |      "karma_status":
      |      {
      |        "value":3085
      |      },
      |      "attributes" :
      |      {
      |        "25" : "1:Девичья фамилия матери",
      |        "1" : "1294999198"
      |      },
      |      "phones" : [
      |        {
      |          "id" : "2",
      |          "attributes" : {
      |            "6" : "1412183145",
      |            "105": 0,
      |            "106": 1
      |          }
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  val passportResponseEmptyUsers: String =
    """{
      |  "users": []
      |}""".stripMargin
}
