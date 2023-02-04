package ru.yandex.realty.amohub.clients.amocrm.model

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.realty.SpecBase
import ru.yandex.realty.amohub.clients.amocrm.AmocrmClient.RefreshToken

@RunWith(classOf[JUnitRunner])
class ExchangeTokensRequestSpec extends SpecBase {
  "ExchangeTokensRequest" should {
    "print to json" in {
      val obj = ExchangeTokensRequest(
        clientId = "000",
        clientSecret = "abc",
        grantType = "none",
        refreshToken = RefreshToken("1234567890")
      )
      Json.toJson(obj).toString() should be(
        """{"client_id":"000","client_secret":"abc","grant_type":"none","refresh_token":"1234567890"}"""
      )
    }
  }
}
