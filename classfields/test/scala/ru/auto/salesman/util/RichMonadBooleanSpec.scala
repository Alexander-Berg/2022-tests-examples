package ru.auto.salesman.util

import cats.implicits._
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.yandex.vertis.util.collection.TryUtil._

import scala.util.Try

class RichMonadBooleanSpec extends BaseSpec {

  "&&" should {

    "return true when both are true" in {
      (success(true) && success(true)).success.value shouldBe true
    }

    "return false for true and false" in {
      (success(true) && success(false)).success.value shouldBe false
    }

    "return failure for true and failure" in {
      (success(true) && failure[Boolean](
        new TestException
      )).failure.exception shouldBe a[TestException]
    }

    "return false when first is false" in {
      (success(false) && failure[Boolean](
        new TestException
      )).success.value shouldBe false
    }

    "return failure when first is failure" in {
      (failure[Boolean](new TestException) && success(
        true
      )).failure.exception shouldBe a[TestException]
    }

    "run second effect when first is true" in {
      val f = mockFunction[Try[Unit]]
      f.expects().returningT(())
      (success(true) && f()).success.value shouldBe (())
    }

    "not run second effect when first is false" in {
      val f = mockFunction[Try[Unit]]
      f.expects().never()
      (success(false) && f()).success.value shouldBe (())
    }
  }
}
