package ru.yandex.realty.amohub.clients.amocrm.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class PaginatedResponseSpec extends SpecBase {
  "PaginatedResponseEx" when {
    "FormatCrmUsersResponse" should {
      "parce from json" in {
        val res = Json
          .parse(
            """{
            |    "_total_items": 118,
            |    "_page": 1,
            |    "_page_count": 40,
            |    "_links": {
            |        "self": {
            |            "href": "https://yandexarenda.amocrm.ru/api/v4/users/?limit=3&page=1"
            |        },
            |        "next": {
            |            "href": "https://yandexarenda.amocrm.ru/api/v4/users/?limit=3&page=2"
            |        },
            |        "last": {
            |            "href": "https://yandexarenda.amocrm.ru/api/v4/users/?limit=3&page=40"
            |        }
            |    },
            |    "_embedded": {
            |        "users": [
            |            {
            |                "id": 6011248,
            |                "name": "Святослав",
            |                "email": "svyatoslav@yandex-team.ru",
            |                "lang": "ru"
            |            },
            |            {
            |                "id": 6028309,
            |                "name": "Александр Смычков",
            |                "email": "a-smychkov@yandex-team.ru",
            |                "lang": "ru"
            |            },
            |            {
            |                "id": 6431002,
            |                "name": "Екатерина Овчинникова",
            |                "lang": "ru"
            |            }
            |        ]
            |    }
            |}""".stripMargin
          )
          .as[PaginatedResponseEx[CrmUsersEmbedded]]
        res should be(
          PaginatedResponseEx(
            _total_items = 118,
            _page = 1,
            _page_count = 40,
            _embedded = CrmUsersEmbedded(
              List(
                ApiCrmUser(6011248, "Святослав", Some("svyatoslav@yandex-team.ru")),
                ApiCrmUser(6028309, "Александр Смычков", Some("a-smychkov@yandex-team.ru")),
                ApiCrmUser(6431002, "Екатерина Овчинникова", None)
              )
            )
          )
        )
      }
    }
  }
}
