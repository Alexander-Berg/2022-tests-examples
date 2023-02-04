package common.secrets.masker.test

import common.secrets.masker.SecretsMasker
import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec

class SecretsMaskerSpec extends AnyWordSpec {

  "SecretsMasker" should {
    "mask oauth token" in {
      SecretsMasker.mask("blabla AQAD-mytoken-text blabla") shouldBe "blabla AQAD-<masked> blabla"
      SecretsMasker.mask("token", "AQAD-mytoken-text") shouldBe "AQAD-<masked>"
    }
    "mask tokens" in {
      SecretsMasker.mask("Bearer eyJraWQiOiJSNkoyWFdNVlIzIi") shouldBe "Bearer <masked>"
      SecretsMasker.mask("Authorization", "Bearer eyJraWQiOiJSNkoyWFdNVlIzIi") shouldBe "Bearer <masked>"
      SecretsMasker.mask("Authorization", "Basic cmVhbHR5LW9mW5nOnJlYW==") shouldBe "Basic <masked>=="
    }
    "mask session_id" in {
      SecretsMasker.mask("session_id", "1231") shouldBe "<masked>"
      SecretsMasker.mask("session_id2", "1231") shouldBe "1231"
    }
    "mask user and service tickets" in {
      SecretsMasker.mask("x-ya-user-ticket", "ticket") shouldBe "<masked>"
      SecretsMasker.mask("x-ya-service-ticket", "ticket") shouldBe "<masked>"
      SecretsMasker.mask("x-ya-unknown-ticket", "ticket") shouldBe "ticket"
    }
    "mask cookies" in {
      SecretsMasker.mask("cookie", "sessionid2=123; abc=321") shouldBe "sessionid2=<masked>; abc=321"
    }
  }
}
