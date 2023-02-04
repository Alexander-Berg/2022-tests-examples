package ru.yandex.vos2.util

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class FutureUtilSpec extends AnyWordSpec with Matchers {

  "batchTraverse" should {
    "valid result" in {
      def testFunc(src: Int): Future[Int] = Future(src * 2)
      val future = FutureUtil.batchTraverse(1 to 5, 2)(testFunc)
      val actual = Await.result(future, 7.seconds)
      actual shouldEqual Seq(2, 4, 6, 8, 10)
    }

    "stop next batch on fail" in {
      @volatile var counter = 0
      def testFunc(src: Int): Future[Unit] = Future {
        counter = counter + 1
        if (src == 45) throw new Exception
      }
      val future = FutureUtil
        .batchTraverse(1 to 100, 10)(testFunc)
        .recover {
          case _ => Future.unit
        }
      Await.result(future, 5.seconds)
      counter should be <= 50
    }
  }

  "waitingTraverse" should {
    "continue on fail" in {
      @volatile var counter = 0
      def testFunc(src: Int): Future[Unit] = Future {
        counter = counter + 1
        if (src == 45) throw new Exception
      }
      val future = FutureUtil
        .waitingTraverse(1 to 100)(testFunc)
        .recover {
          case _ => Future.unit
        }
      Await.result(future, 5.seconds)
      counter should be > 45
    }
  }
}
