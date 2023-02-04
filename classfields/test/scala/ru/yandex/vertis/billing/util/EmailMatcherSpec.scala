package ru.yandex.vertis.billing.util

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.DefaultPropertyChecks
import ru.yandex.vertis.billing.model_core.gens.EmailStrGen
import ru.yandex.vertis.billing.util.validators.EmailMatcher

class EmailMatcherSpec extends AnyWordSpec with Matchers with DefaultPropertyChecks {

  private def normalize(email: String): String =
    email.trim.toLowerCase

  private def retrieveLocalPart(email: String): String =
    normalize(email).takeWhile(_ != '@')

  private def retrieveDomain(email: String): String =
    normalize(email).dropWhile(_ != '@').tail.reverse.dropWhile(_ != '.').tail.reverse

  "EmailMatcher" should {

    "correctly retrieve localpart and domain" in {
      forAll(EmailStrGen) { email =>
        EmailMatcher(email).localPartMatch(_ == retrieveLocalPart(email)) shouldBe true
        EmailMatcher(email).domainMatch(_ == retrieveDomain(email)) shouldBe true
      }
    }

  }

}
