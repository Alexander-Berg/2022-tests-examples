package ru.yandex.vertis.billing.banker.util

import org.scalacheck.{Gen, ShrinkLowPriority}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.banker.model.gens.{EmailGen, Producer}
import ru.yandex.vertis.billing.banker.util.email.EmailValidator

class EmailValidatorSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with ShrinkLowPriority {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 100)

  private def spoilEmail(email: String): String = {
    Gen.choose(0, 1).next match {
      case 0 =>
        val dotIndexes = email.zipWithIndex.filter { case (c, _) => c == '.' }
        val index = Gen.oneOf(dotIndexes.map(_._2)).next
        val (head, tail) = email.splitAt(index)
        head + "." + tail
      case _ =>
        val withoutEnd = email.reverse.dropWhile(_ != '.')
        withoutEnd.reverse :+ Gen.alphaChar.next
    }
  }

  private val badEmailsGen = Gen.oneOf(EmailGen.map(spoilEmail), Gen.const("----@mail.ru"))

  "EmailValidator" should {

    "check correctness" in {
      forAll(EmailGen) { email =>
        EmailValidator.isValid(email) shouldBe true
      }
      forAll(badEmailsGen) { email =>
        EmailValidator.isValid(email) shouldBe false
      }
    }

    "fail on emails with leading '-'" in {
      val badEmail = "-dmitriy-vk@bk.ru"
      EmailValidator.isValid(badEmail) shouldBe false
    }

    "not fail on emails without leading '-'" in {
      val emails =
        Iterable("dmitriy-vk@bk.ru", "dmitriy@bk.ru")
      emails.foreach { email =>
        EmailValidator.isValid(email) shouldBe true
      }
    }

    "not pass too long emails" in {
      val email = "bxbxbxbxbxwwuwjsjbdxbdjhxgzgahaiakatuewiwidytxxhxbb-81289@mail.ru"

      EmailValidator.isValid(email) shouldBe false
    }

  }

}
