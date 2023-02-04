package ru.yandex.vertis.chat.service

import java.util.concurrent.Executors

import ru.yandex.vertis.chat.SpecBase
import scala.language.reflectiveCalls
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Test for Async Limiter
  *
  * @author aborunov
  */
class AsyncLimiterSpec extends SpecBase {
  private val pool = Executors.newFixedThreadPool(3)
  implicit private val ec = ExecutionContext.fromExecutor(pool)

  private val limiter = new AsyncLimiter {
    override def maxSimultaneousActions: Int = 3

    def testFunc(i: Int): Future[Int] = {
      executeLimited(Future {
        Thread.sleep(100)
        1
      }).recover {
        case e: IllegalStateException => 0
      }
    }
  }

  "AsyncLimiter" should {
    "return only 3 sucessful results" in {
      val x = Future.sequence((1 to 6).map(i => limiter.testFunc(i)))
      val result: Seq[Int] = Await.result(x, 1.second)
      result.count(_ == 1) shouldBe 3
    }
  }
}
