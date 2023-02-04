package ru.auto.cabinet.service.passport

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import spray.json._

class PassportProtocolSpec
    extends FlatSpec
    with Matchers
    with PassportProtocol {

  "Passport protocol" should "parse user fullName" in {
    val s =
      """
        |{
        |  "user": {
        |    "id": "123456789",
        |    "profile": {
        |      "autoru": {
        |        "alias": "Фамилия Имя",
        |        "userpic": {
        |          "name": "string",
        |          "sizes": "{'orig': '//avatars.mds.yandex.net/get-autoru/1121/image_name/orig'}"
        |        },
        |        "clientId": "string",
        |        "clientGroup": "string",
        |        "birthday": "1983-02-28",
        |        "about": "string",
        |        "showCard": true,
        |        "showMail": true,
        |        "allowMessages": true,
        |        "drivingYear": 0,
        |        "countryId": 0,
        |        "regionId": 0,
        |        "cityId": 0,
        |        "fullName": "Фамилия Имя Отчество",
        |        "usePassword": true,
        |        "geoId": 0,
        |        "autoruExpertStatus": {
        |          "canRead": true,
        |          "canAdd": true
        |        }
        |      }
        |    },
        |    "registrationDate": "1983-02-28",
        |    "active": true,
        |    "emails": [
        |      {
        |        "email": "string",
        |        "confirmed": true,
        |        "added": "2020-08-31T13:19:46.124Z"
        |      }
        |    ],
        |    "phones": [
        |      {
        |        "phone": "string",
        |        "added": "2020-08-31T13:19:46.124Z"
        |      }
        |    ],
        |    "socialProfiles": [
        |      {
        |        "provider": "VK",
        |        "socialUserId": "string",
        |        "added": "2020-08-31T13:19:46.124Z",
        |        "nickname": "string",
        |        "firstName": "string",
        |        "lastName": "string"
        |      }
        |    ],
        |    "registrationIp": "192.168.1.1",
        |    "yandexStaffLogin": "string"
        |  },
        |  "lastSeen": "2020-08-31T13:19:46.124Z",
        |  "authTypes": {
        |    "allowEmailCodeLogin": true,
        |    "allowPhoneCodeLogin": true,
        |    "allowPasswordLogin": true
        |  }
        |}
      """.stripMargin
    val userResponse = userResponseJsonFormat.read(s.parseJson)
    userResponse.user.profile.autoru.alias shouldBe Some("Фамилия Имя")
  }

  "Passport protocol" should "parse user fullName and return None if fullName is absent " in {
    val s =
      """
        |{
        |  "user": {
        |    "id": "123456789",
        |    "profile": {
        |      "autoru": {
        |        "userpic": {
        |          "name": "string",
        |          "sizes": "{'orig': '//avatars.mds.yandex.net/get-autoru/1121/image_name/orig'}"
        |        },
        |        "clientId": "string",
        |        "clientGroup": "string",
        |        "birthday": "1983-02-28",
        |        "about": "string",
        |        "showCard": true,
        |        "showMail": true,
        |        "allowMessages": true,
        |        "drivingYear": 0,
        |        "countryId": 0,
        |        "regionId": 0,
        |        "cityId": 0,
        |        "usePassword": true,
        |        "geoId": 0,
        |        "autoruExpertStatus": {
        |          "canRead": true,
        |          "canAdd": true
        |        }
        |      }
        |    },
        |    "registrationDate": "1983-02-28",
        |    "active": true,
        |    "emails": [
        |      {
        |        "email": "string",
        |        "confirmed": true,
        |        "added": "2020-08-31T13:19:46.124Z"
        |      }
        |    ],
        |    "phones": [
        |      {
        |        "phone": "string",
        |        "added": "2020-08-31T13:19:46.124Z"
        |      }
        |    ],
        |    "socialProfiles": [
        |      {
        |        "provider": "VK",
        |        "socialUserId": "string",
        |        "added": "2020-08-31T13:19:46.124Z",
        |        "nickname": "string",
        |        "firstName": "string",
        |        "lastName": "string"
        |      }
        |    ],
        |    "registrationIp": "192.168.1.1",
        |    "yandexStaffLogin": "string"
        |  },
        |  "lastSeen": "2020-08-31T13:19:46.124Z",
        |  "authTypes": {
        |    "allowEmailCodeLogin": true,
        |    "allowPhoneCodeLogin": true,
        |    "allowPasswordLogin": true
        |  }
        |}
      """.stripMargin
    val userResponse = userResponseJsonFormat.read(s.parseJson)
    userResponse.user.profile.autoru.alias shouldBe None
  }
}
