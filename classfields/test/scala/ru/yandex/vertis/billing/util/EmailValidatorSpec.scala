package ru.yandex.vertis.billing.util

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.DefaultPropertyChecks
import ru.yandex.vertis.billing.model_core.gens.{EmailStrGen, Producer}
import ru.yandex.vertis.billing.util.validators.EmailValidator

class EmailValidatorSpec extends AnyWordSpec with Matchers with DefaultPropertyChecks {

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

  private val badEmailsGen = Gen.oneOf(EmailStrGen.map(spoilEmail), Gen.const("----@mail.ru"))

  "EmailValidator" should {

    "check correctness" in {
      forAll(EmailStrGen) { email =>
        EmailValidator.isValid(email) shouldBe true
      }
      forAll(badEmailsGen) { email =>
        EmailValidator.isValid(email) shouldBe false
      }
    }

  }

}
