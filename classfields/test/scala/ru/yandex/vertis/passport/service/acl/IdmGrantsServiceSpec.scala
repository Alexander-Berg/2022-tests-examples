package ru.yandex.vertis.passport.service.acl

import org.scalatest.FreeSpec
import play.api.libs.json.Json
import ru.yandex.vertis.passport.model.UserGrantsSet
import ru.yandex.vertis.passport.test.SpecBase

class IdmGrantsServiceSpec extends FreeSpec with SpecBase {
  "IdmGrantsService" - {
    "parseGrants" - {
      "should return correct data" in {
        val json = Json.parse("""["dcversus","blazhievskaya","dinoskova"]""")
        IdmGrantsService.parseAutoruGrants(json) shouldBe Map(
          "dcversus" -> UserGrantsSet(List("MODERATOR_AUTORU", "MODERATOR_USERS_AUTORU")),
          "blazhievskaya" -> UserGrantsSet(List("MODERATOR_AUTORU", "MODERATOR_USERS_AUTORU")),
          "dinoskova" -> UserGrantsSet(List("MODERATOR_AUTORU", "MODERATOR_USERS_AUTORU"))
        )
      }
    }
  }
}
