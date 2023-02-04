package ru.auto.api.util

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.util.ErrorUtils.RichTry

import scala.util.{Failure, Random, Success}

class ErrorUtilsSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "RichTry" should {
    "return success for sequence()" in {
      forAll(Gen.listOf(Gen.posNum[Int])) { elems =>
        RichTry.sequence(elems.map(Success.apply)) shouldBe Success(elems)
      }
    }

    "return first failure for sequence()" in {
      forAll(Gen.listOf(Gen.posNum[Int]), Gen.nonEmptyListOf(Gen.alphaNumStr)) { (elems, messages) =>
        val successes = elems.map(Success.apply)
        val failures = messages.map(msg => Failure(new Exception(msg)))
        val all = Random.shuffle(successes ++ failures)
        all.find(_.isFailure) should contain(RichTry.sequence(all))
      }
    }

    "return success for success()" in {
      forAll(Gen.posNum[Int])(elem => RichTry.success(elem) shouldBe Success(elem))
    }

    "return failure for failure()" in {
      forAll(Gen.alphaNumStr) { msg =>
        val e = new Exception(msg)
        RichTry.failure(e) shouldBe Failure(e)
      }
    }
  }
}
