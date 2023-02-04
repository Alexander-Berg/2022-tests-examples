package ru.yandex.vertis.feedprocessor.util

import scala.concurrent.{Await, Future, Promise, TimeoutException}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * @author pnaydenov
  */
class FutureUtilsSpec extends StreamTestBase {
  "FutureTimeoutManager" should {
    pending

    "complete hanging futures" in {
      val promise = Promise[Int]()
      val future = FutureUtils.withTimeout(promise, 1.second)
      Await.ready(future, 3.second)
      future.value.get match {
        case Failure(ex) =>
          ex shouldBe a[TimeoutException]
          ex.getMessage shouldEqual "Not completed in 1 second"
        case Success(x) =>
          throw new IllegalStateException("test failed")
      }
    }

    "don't complete hanging future before 2 x timeout" in {
      val promise = Promise[Int]()
      val future = FutureUtils.withTimeout(promise, 1.second)
      intercept[TimeoutException] {
        Await.ready(future, 1.second)
        // future not complete (not timeouted) after 1 second
      }
      Await.ready(future, 2.second)
    }

    "don't touch futures before timeout" in {
      val promise = Promise[Int]()
      val future = FutureUtils.withTimeout(promise, 1.second)
      Future {
        Thread.sleep(500)
        promise.success(5)
      }
      Await.ready(future, 3.second)
      future.value.get match {
        case Success(5) =>
        case Success(_) => throw new IllegalStateException("test failed (not 5)")
        case Failure(_) => throw new IllegalStateException("test failed (failure)")
      }
    }
  }
}
