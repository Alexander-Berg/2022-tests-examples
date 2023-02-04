package ru.yandex.vertis.personal.util

import com.couchbase.client.core.CouchbaseException
import ru.yandex.vertis.personal.model.exception.{CASModificationFailedException, ConflictException}

import scala.concurrent.Future
import scala.util.control.NonFatal

class CasLoopSpec extends BaseSpec {

  private def checkAction[T](maxRetries: Int)(aim: (Int, => Future[T]) => Future[T]): Unit = {

    var counter = 0

    def action: Future[T] = {
      counter = counter + 1
      Future.failed(
        new CASModificationFailedException(
          "test",
          "test",
          "test",
          new CouchbaseException()
        )
      )
    }

    aim(maxRetries, action).failed.futureValue match {
      case _: ConflictException =>
        counter shouldBe maxRetries
      case NonFatal(other) =>
        fail("Unexpected exception", other)
    }
  }

  "CasLoop" should {
    "stop after max retries" when {
      "use loop" in {
        checkAction[Int](10)(CasLoop.loop(_)(_))
      }
      "use loopWithoutResult" in {
        checkAction[Unit](10)(CasLoop.loopWithoutResult(_)(_))
      }
    }
  }

}
