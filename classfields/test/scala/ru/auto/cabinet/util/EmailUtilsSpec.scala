package ru.auto.cabinet.util

import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers

class EmailUtilsSpec extends FlatSpec with Matchers {
  it should "return true for valid email addresses" in {
    EmailUtils.isValid("user@abc.com") shouldBe true
    EmailUtils.isValid("user@localhost-auto.com") shouldBe true
    EmailUtils.isValid("123@abc.com") shouldBe true
    EmailUtils.isValid("_1_a_2_b_3_c_@abc.com") shouldBe true
    EmailUtils.isValid("name-secondname@abc.com") shouldBe true
    EmailUtils.isValid("name-secondname@abc123.com") shouldBe true
    EmailUtils.isValid("name.secondname@abc.ru") shouldBe true
    EmailUtils.isValid("name.secondname@abc.bk") shouldBe true
  }

  it should "return false for email address which ends with localhost" in {
    EmailUtils.isValid("user@localhost") shouldBe false
  }

  it should "return false for email address which contains cyrillic symbols" in {
    EmailUtils.isValid("пользователь@abc.com") shouldBe false
    EmailUtils.isValid("user@почта.com") shouldBe false
    EmailUtils.isValid("user@mail.ком") shouldBe false
  }

  it should "return false for email address which contains symbols in upper case (should be converted to lower case before validation)" in {
    EmailUtils.isValid("ABC@abc.com") shouldBe false
    EmailUtils.isValid("abc@ABC.com") shouldBe false
    EmailUtils.isValid("abc@abc.COM") shouldBe false
  }

  it should "return false for empty email addresses" in {
    EmailUtils.isValid("") shouldBe false
    EmailUtils.isValid("     ") shouldBe false
    EmailUtils.isValid("\t") shouldBe false
  }

  it should "return false for incorrect email addresses" in {
    EmailUtils.isValid("qwerty") shouldBe false
    EmailUtils.isValid("qwerty.com") shouldBe false
    EmailUtils.isValid("test@abc") shouldBe false
    EmailUtils.isValid("test@.com") shouldBe false
    EmailUtils.isValid("user.test.com") shouldBe false
    EmailUtils.isValid("@@@") shouldBe false
    EmailUtils.isValid("test@abc.com <some info>") shouldBe false
    EmailUtils.isValid("test test@abc.com") shouldBe false
    EmailUtils.isValid("test@ abc.com") shouldBe false
    EmailUtils.isValid("test@abc .com") shouldBe false
  }
}
