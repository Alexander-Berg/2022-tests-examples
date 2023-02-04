package ru.yandex.vertis.promocoder

import org.scalatest.matchers.should.Matchers
import org.scalatest.exceptions.TestFailedException

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

/** Combines traits used in most tests
  *
  * @author alex-kovalenko
  */
trait SpecBase extends Matchers with SpecWithFutures {

  def asyncTest(test: => Future[Unit]): Unit =
    try {
      test.futureValue
    } catch {
      case e: TestFailedException if e.cause.isDefined =>
        throw e.cause.get
    }

  implicit class RichFutureTest[T](future: Future[T]) {

    def await: T = {
      Await.result(future, 1.seconds)
    }
  }

}
