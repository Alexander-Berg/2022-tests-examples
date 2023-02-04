package ru.yandex.realty.developer.chat.service.processing.stages

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.TableDrivenPropertyChecks

@RunWith(classOf[JUnitRunner])
class BillingProcessorSpec extends FlatSpec with Matchers with TableDrivenPropertyChecks {

  it should "detect phones" in {
    val phones = Table(
      "phone",
      "мой телефон +7 (921) 123-45-67",
      "+79211234567",
      "8 921 123 45 67"
    )

    forAll(phones) { phone =>
      BillingProcessor.containsPhone(phone) shouldBe true
    }
  }

  it should "detect emails" in {
    val emails = Table(
      "email",
      "наш email email@yandex-team.ru, ответим в течение суток",
      "aba.c.aba@gmail.com"
    )

    forAll(emails) { email =>
      BillingProcessor.containsEmail(email) shouldBe true
    }
  }

}
