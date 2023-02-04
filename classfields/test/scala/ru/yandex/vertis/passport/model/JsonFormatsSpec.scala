package ru.yandex.vertis.passport.model

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FreeSpec, Matchers}
import play.api.libs.json.Json
import ru.yandex.passport.model.api.ApiModel.SocialUserPayload
import ru.yandex.vertis.passport.model.JsonFormats._
import ru.yandex.vertis.passport.test.ModelGenerators._

class JsonFormatsSpec extends FreeSpec with Matchers with GeneratorDrivenPropertyChecks {

  "SocialUserPayloadGen" in {
    forAll(SocialUserPayloadGen) { obj =>
      Json.toJson(obj).as[SocialUserPayload] shouldBe obj
    }
  }
}
