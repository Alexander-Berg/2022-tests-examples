package ru.yandex.vertis.passport.service.identity

import org.scalatest.{Matchers, WordSpec}
import ru.yandex.vertis.passport.model.RequestContext
import ru.yandex.vertis.passport.util.crypt.HmacSigner

/**
  * Tests for IdentityService
  *
  * @author zvez
  */
class IdentityServiceSpec extends WordSpec with Matchers {

  val service =
    new IdentityServiceImpl(new HmacSigner("secret")) with LegacyIdentitySupport

  implicit val ctx = RequestContext("123")

  "IdentityService" should {
    "generate unique values" in {
      val generated = (1 to 10000).map(_ => service.generate())
      generated.size shouldBe generated.distinct.size
    }

    "be able to validate uid" in {
      service.validate("fake") shouldBe false
      service.validate("fake.for-real") shouldBe false
      service.validate(service.generate()) shouldBe true
      service.validate(service.generate().drop(1)) shouldBe false
    }

    "validate legacy (php) identities" in {
      val realUid = "93ad226bd64c7d2b7a0e331f4ac991a6.b5c269b6d3123d42f9a28ed0a6fb5554"
      service.validate(realUid) shouldBe true

      service.validate(LegacyIdentityService.generate()) shouldBe true
    }
  }

}
