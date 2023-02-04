package ru.yandex.vertis.passport.util

import org.scalatest.{FreeSpec, Inspectors, Matchers, OptionValues}
import ru.yandex.vertis.passport.model.Identity.Email

/**
  *
  * @author zvez
  */
class IdentityUtilsSpec extends FreeSpec with Matchers with Inspectors with OptionValues {

  "IdentityUtils" - {
    "should parse phone" - {

      def shouldParse(src: String, res: String): Unit = {
        s"$src -> $res" in {
          IdentityUtils.parsePhone(src) shouldBe Some(res)
        }
      }

      def shouldFail(why: String, src: String): Unit = {
        s"$why: $src" in {
          IdentityUtils.parsePhone(src) shouldBe None
        }
      }

      shouldParse("+77012030645", "77012030645")
      shouldParse("87012030645", "77012030645")
      shouldParse("+7(905)257-29-11", "79052572911")
      shouldParse("+375-25-932-43-60", "375259324360")

      shouldFail("too short", "1234")
      shouldFail("not a phone", "abs")
    }

    "parseAndCheckPhone" - {

      "should accept Russia phones" in {
        IdentityUtils.parseAndCheckPhone("+7(905)257-29-11") shouldBe Some("79052572911")
      }

      "should accept non-federal" in {
        IdentityUtils.parseAndCheckPhone("73833190077") shouldBe Some("73833190077")
      }

      "should accept Belarus phones" in {
        IdentityUtils.parseAndCheckPhone("+375-25-932-43-60") shouldBe Some("375259324360")
      }

      "forbid anything else" in {
        IdentityUtils.parseAndCheckPhone("+9(905)257-29-11") shouldBe None
        IdentityUtils.parseAndCheckPhone("+376-25-932-43-60") shouldBe None
      }

    }

    "should accept valid emails" in {
      val emails = """email@example.com
                     |firstname.lastname@example.com
                     |email@subdomain.example.com
                     |firstname+lastname@example.com
                     |1234567890@example.com
                     |email@example-one.com
                     |_______@example.com
                     |email@example.name
                     |email@example.museum
                     |email@example.co.jp
                     |firstname-lastname@example.com
            """.stripMargin.split("\n").init
      forAll(emails) { email =>
        IdentityUtils.parseEmail(email) should be(defined)
      }
    }

    "should reject invalid emails" in {
      val emails =
        """plainaddress
          |#@%^%#$@#$@#.com
          |@example.com
          |Joe Smith <email@example.com>
          |email.example.com
          |email example.com
          |email@example@example.com
          |.email@example.com
          |email.@example.com
          |あいうえお@example.com
          |email@example.com (Joe Smith)
          |email@example
          |email@-example.com
          |email@example..com
        """.stripMargin.split("\n")
      forAll(emails) { email =>
        IdentityUtils.parseEmail(email) shouldBe None
      }
    }

    "should ignore plus in email addresses" in {
      val emailsWithSubAddresses = """email+something@example.com
                                     |firstname.lastname+something@example.com
                                     |email+subemail+address@subdomain.example.com
                                     |1234567890+0987+654+321@example.com
                                     |email+one+more-email@example-one.com
                                     |_______+_____+@example.com
                                     |email+@example.name
                                     |email+subaddress@example.co.jp
                                     |firstname-lastname+subaddress@example.com
                                     """.stripMargin.split("\n")

      val emailsWithoutSubAddresses = """email@example.com
                                     |firstname.lastname@example.com
                                     |email@subdomain.example.com
                                     |1234567890@example.com
                                     |email@example-one.com
                                     |_______@example.com
                                     |email@example.name
                                     |email@example.co.jp
                                     |firstname-lastname@example.com
                                     """.stripMargin.split("\n")

      forAll(emailsWithSubAddresses.zip(emailsWithoutSubAddresses)) { pairOfEmails =>
        Email(pairOfEmails._1).normalizeIdentity.login shouldBe pairOfEmails._2
      }
    }

    "should not ignore plus in email domains" in {
      val emailsWithSubDomains =
        """email@domain+subdomain.com
          |firstname-lastname@exa+mple.com
          |1232134123@gmail+mail.com
          |address@first+second+third.com
          |""".stripMargin.split("\n")

      forAll(emailsWithSubDomains) { email =>
        Email(email).normalizeIdentity.login shouldBe email
      }
    }

    "should take email with yandex-like domains as yandex.ru" in {
      val emailsWithYandexLikeDomains =
        """email@yandex.ru
          |email@ya.ru
          |email@first.second.yandex.de
          |example@first.second.ya.ru
          |email@subdomain.yandex.com
          |email.example.test@yandex.com
          |example-email@mail.yandex.museum
          |""".stripMargin.split("\n")

      val emailsWithNormalizedDomain =
        """email@yandex.ru
          |email@yandex.ru
          |email@yandex.ru
          |example@yandex.ru
          |email@yandex.ru
          |email.example.test@yandex.ru
          |example-email@yandex.ru
          |""".stripMargin.split("\n")

      forAll(emailsWithYandexLikeDomains.zip(emailsWithNormalizedDomain)) { pairOfEmails =>
        Email(pairOfEmails._1).normalizeIdentity.login shouldBe pairOfEmails._2
      }
    }
  }

}
