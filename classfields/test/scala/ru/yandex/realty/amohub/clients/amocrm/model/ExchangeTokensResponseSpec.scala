package ru.yandex.realty.amohub.clients.amocrm.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsResultException, Json}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.amohub.clients.amocrm.AmocrmClient.{AccessToken, RefreshToken}
import ru.yandex.realty.amohub.clients.amocrm.model

@RunWith(classOf[JUnitRunner])
class ExchangeTokensResponseSpec extends SpecBase {
  "ExchangeTokensResponse" should {
    "parce from json" in {
      val res = Json
        .parse(
          """{
            |  "expires_in": 123456,
            |  "access_token": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            |  "refresh_token": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
            |}""".stripMargin
        )
        .as[ExchangeTokensResponse]
      res should be(
        model.ExchangeTokensResponse(
          expiresIn = 123456,
          accessToken = AccessToken("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"),
          refreshToken = RefreshToken("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
        )
      )
    }
    "parce from json with invalid token" in {
      def res: ExchangeTokensResponse =
        Json
          .parse(
            """{
            |  "expires_in": 123456,
            |  "access_token": 1,
            |  "refresh_token": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
            |}""".stripMargin
          )
          .as[ExchangeTokensResponse]
      the[JsResultException] thrownBy res should have message
        "JsResultException(errors:List((/access_token,List(JsonValidationError(List(error.expected.jsstring),WrappedArray())))))"
    }
  }
}
