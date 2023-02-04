package ru.yandex.verba.core.util

import org.scalatest.Ignore
import org.scalatest.freespec.AnyFreeSpec
import ru.yandex.verba.core.util.FutureUtils._
import org.scalatest.matchers.should.Matchers._

import scala.concurrent._
import scala.concurrent.duration.Duration

/**
 * Author: Vladislav Dolbilov (darl@yandex-team.ru)
 * Created: 08.11.13 16:17
 */
@Ignore
class FutureUtilsTest extends AnyFreeSpec {

  implicit val ec = ExecutionContext.global
  implicit val timeout = Duration("150 ms")

  class TestException extends Exception

  "FutureSeq" - {
    "join() should return future which" - {
      "completes when all futures in sequence will be completed" in {
        val p1 = Promise[Int]()
        val p2 = Promise[Int]()
        val p3 = Promise[Int]()

        val seqF = Seq(p1.future, p2.future, p3.future).join

        p1.success(1)
        p2.success(2)

        intercept[TimeoutException] {
          seqF.await
        }

        p3.success(3)
        seqF.awaitReady.isCompleted shouldEqual true
      }

      "contains exception if any future in sequence completed with exception" in {
        val p1 = Promise[Int]()
        val p2 = Promise[Int]()

        val seqF = Seq(p1.future, p2.future).join

        intercept[TimeoutException] {
          seqF.await
        }

        p2.failure(new TestException)
        intercept[TestException] {
          seqF.await
        }
      }
    }
  }
}
