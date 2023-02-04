package ru.auto.salesman.util

import cats.implicits._
import org.scalacheck.Gen
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.yandex.vertis.util.collection.TryUtil.{failure, success}

import scala.util.Try

class RichOptionSpec extends BaseSpec {

  "orElseA" should {

    "return option value" in {
      forAll(Gen.posNum[Int]) { value =>
        Some(value).orElseA[Try](???).success.value shouldBe value
      }
    }

    "return given success value" in {
      forAll(Gen.posNum[Int]) { value =>
        None.orElseA(success(value)).success.value shouldBe value
      }
    }

    "return given error" in {
      None
        .orElseA(failure(new TestException))
        .failure
        .exception shouldBe a[TestException]
    }
  }

  "orElseOptA" should {
    "return option value in Some" in {
      forAll(Gen.posNum[Int]) { value =>
        Some(value).orElseOptA[Try](???).success.value shouldBe Some(value)
      }
    }

    "return given success value in Some" in {
      forAll(Gen.posNum[Int]) { value =>
        None.orElseOptA[Try](success(Some(value))).success.value shouldBe Some(
          value
        )
      }
    }

    "return given error" in {
      None
        .orElseOptA[Try](failure(new TestException))
        .failure
        .exception shouldBe a[TestException]
    }
  }

  "orRaiseError" should {

    "return value if exists" in {
      forAll(Gen.posNum[Int]) { value =>
        Some(value).orRaiseError[Try](???).success.value shouldBe value
      }
    }

    "return given error if there is no value" in {
      val e = new TestException
      None.orRaiseError[Try](e).failure.exception shouldBe e
    }
  }
}
