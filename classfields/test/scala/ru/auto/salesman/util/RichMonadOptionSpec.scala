package ru.auto.salesman.util

import cats.implicits._
import ru.auto.salesman.test.{BaseSpec, TestException}
import ru.yandex.vertis.util.collection.TryUtil._

import scala.util.{Failure, Success}

class RichMonadOptionSpec extends BaseSpec {

  "flatMapSome" should {

    "return flatmapped option value" in {
      success(Option(42))
        .flatMapSome(x => Success(x * 2))
        .success
        .value
        .value shouldBe 84
    }

    "return failure if flatmapped to failure" in {
      success(Option(42))
        .flatMapSome(_ => Failure(new TestException))
        .failure
        .exception shouldBe a[TestException]
    }

    "return same none" in {
      success(Option.empty[Int])
        .flatMapSome(_ => ???)
        .success
        .value shouldBe None
    }

    "return same failure" in {
      failure[Option[Int]](new TestException)
        .flatMapSome(_ => ???)
        .failure
        .exception shouldBe a[TestException]
    }
  }

  "orRaiseError" should {

    "return same some" in {
      success(Option(42)).orRaiseError(???).success.value shouldBe 42
    }

    "return passed error if none" in {
      val e = new TestException
      success(Option.empty[Int])
        .orRaiseError(e)
        .failure
        .exception shouldBe e
    }

    "return same failure" in {
      val e = new TestException
      failure[Option[Int]](e).orRaiseError(???).failure.exception shouldBe e
    }
  }
}
