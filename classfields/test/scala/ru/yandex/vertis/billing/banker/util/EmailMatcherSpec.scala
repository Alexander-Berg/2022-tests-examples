package ru.yandex.vertis.billing.banker.util

import org.scalacheck.ShrinkLowPriority
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.banker.model.gens.EmailGen
import ru.yandex.vertis.billing.banker.util.email.EmailMatcher
import ru.yandex.vertis.billing.banker.util.email.EmailUtils.RichEmailString

class EmailMatcherSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with ShrinkLowPriority {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)

  private def normalize(email: String): String =
    email.trim.toLowerCase

  "EmailMatcher" should {

    "correctly retrieve localpart and domain" in {
      forAll(EmailGen) { email =>
        val expectedLocalPart = normalize(email).localPart
        EmailMatcher(email).localPartMatch(_ == expectedLocalPart) shouldBe true

        val expectedDomain = normalize(email).domain
        EmailMatcher(email).domainMatch(_ == expectedDomain) shouldBe true
      }
    }

  }

}
