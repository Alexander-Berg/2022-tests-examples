package ru.yandex.vertis.phonoteka.model

import ru.yandex.vertis.quality.test_utils.SpecBase

import scala.util.Try

class PhoneSpec extends SpecBase {

  private case class TestCase(source: String, expected: Option[String])

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase("+79313540464", Some("79313540464")),
      TestCase("89313540464", Some("79313540464")),
      TestCase("79313540464", Some("79313540464")),
      TestCase("  +79313540464", Some("79313540464")),
      TestCase("+7 931 354 04 64", Some("79313540464")),
      TestCase("+7(931)354-04-64", Some("79313540464")),
      TestCase("-79313540464", Some("79313540464")),
      TestCase("1", None),
      TestCase("word", None),
      TestCase("", None),
      TestCase("375447375183", None),
      TestCase("9652861016", Some("79652861016")),
      TestCase("2861016", None)
    )

  "Phone.normalize" should {
    testCases.foreach { case TestCase(source, expected) =>
      s"source value '$source'" in {
        Try(Phone(source).normalized).toOption shouldBe expected
      }
    }
  }
}
