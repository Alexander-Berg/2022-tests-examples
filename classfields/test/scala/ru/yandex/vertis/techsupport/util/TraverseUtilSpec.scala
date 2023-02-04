package ru.yandex.vertis.vsquality.techsupport.util

import cats.instances.future._
import cats.instances.list._
import ru.yandex.vertis.vsquality.techsupport.util.TraverseUtil._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

/**
  * @author potseluev
  */
class TraverseUtilSpec extends SpecBase {

  "TraverseUtil" should {
    "provide correct sequential traverse" in {
      val n = 100
      val input = 1 to n
      val buffer = new mutable.ListBuffer[Int]

      def action(i: Int): Future[Int] = Future {
        val delayMillis = Random.nextInt(20)
        Thread.sleep(delayMillis)
        buffer += i
        i
      }

      val future = input.toList.sequentialTraverse(action)
      val result = Await.result(future, 3.seconds)
      result shouldBe input
      buffer shouldBe input
    }
  }
}
