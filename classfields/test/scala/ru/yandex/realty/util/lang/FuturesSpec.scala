package ru.yandex.realty.util.lang

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.util.thread.Threads

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class FuturesSpec extends AsyncSpecBase {

  val exception = new Exception()

  private def toFutureInt(s: String): Future[Int] = Future(s.toInt).recover { case _ => throw exception }

  "withRetry" should {
    "reevaluate the future on each retry" in {
      val evaluationCounter = new AtomicInteger(0)
      def evaluateAndReturnFuture(): Future[Unit] = {
        evaluationCounter.incrementAndGet()
        Future.failed(new RuntimeException())
      }
      val retryCount = 2
      Await.ready(Futures.withRetry(retryCount)(evaluateAndReturnFuture())(Threads.lightWeightTasksEc), 1.second)
      evaluationCounter.get() shouldBe (retryCount + 1)
    }
  }

  "validate" should {
    "drop elements with error" in {
      val res = Futures.validate(Seq("1", "l", "b", "4", "5"))(toFutureInt)
      res.futureValue shouldBe Seq(1, 4, 5)
    }
  }

  "tryTraverse" should {
    "transform each element of input and zip result with input" in {
      val res = Futures.tryTraverse(Seq("1", "l", "b", "4", "5"))(toFutureInt)
      res.futureValue shouldBe Seq(
        "1" -> Success(1),
        "l" -> Failure(exception),
        "b" -> Failure(exception),
        "4" -> Success(4),
        "5" -> Success(5)
      )
    }
  }
}
