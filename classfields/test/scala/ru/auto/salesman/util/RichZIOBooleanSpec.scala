package ru.auto.salesman.util

import ru.auto.salesman.test.{BaseSpec, TestException}
import zio.{IO, ZIO}

class RichZIOBooleanSpec extends BaseSpec {

  private val fail = ZIO.fail(new TestException): IO[TestException, Boolean]

  "&&" should {

    "return true when both are true" in {
      (ZIO.succeed(true) && ZIO.succeed(true)).success.value shouldBe true
    }

    "return false for true and false" in {
      (ZIO.succeed(true) && ZIO.succeed(false)).success.value shouldBe false
    }

    "return failure for true and failure" in {
      (ZIO.succeed(true) && fail).failure.exception shouldBe a[TestException]
    }

    "return false when first is false" in {
      (ZIO.succeed(false) && fail).success.value shouldBe false
    }

    "return ZIO.fail when first is failure" in {
      (ZIO.fail(new TestException) && ZIO.succeed(
        true
      )).failure.exception shouldBe a[TestException]
    }

    "run second effect when first is true" in {
      val f = mockFunction[Unit]
      f.expects().returning(())
      (ZIO.succeed(true) && ZIO.effectTotal(f())).success.value shouldBe (())
    }

    "not run second effect when first is false" in {
      val f = mockFunction[Unit]
      f.expects().never()
      (ZIO.succeed(false) && ZIO.effectTotal(f())).success.value shouldBe (())
    }
  }
}
