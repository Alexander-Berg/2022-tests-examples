package auto.carfax.common.utils.collections

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration.Inf
import scala.concurrent.{Await, ExecutionContext, Future}

class RichIterableTest extends AnyWordSpecLike with Matchers {

  "groupedBy" should {

    "return empty Seq on empty Iterable" in {
      Iterable.empty[Int].groupedBy((_, _) => true) shouldBe Seq.empty[Seq[Int]]
    }

    "return same length seq when group condition is always false" in {
      Iterable(1, 2, 3).groupedBy((_, _) => false) shouldBe Seq(Seq(1), Seq(2), Seq(3))
    }

    "return seq with length = 1 when group condition is always true" in {
      Iterable(1, 2, 3).groupedBy((_, _) => true) shouldBe Seq(Seq(1, 2, 3))
    }

    "group elements as expected by arbitrary group condition" in {

      def maxDiffIsOne(group: Seq[Int], elem: Int): Boolean = {
        group.forall(groupElem => Math.abs(elem - groupElem) <= 1)
      }

      Iterable(1, 2, 3, 4, 4, 5).groupedBy(maxDiffIsOne) shouldBe Seq(Seq(1, 2), Seq(3, 4, 4), Seq(5))

      Iterable(5, 4, 4, 3, 2, 1).groupedBy(maxDiffIsOne) shouldBe Seq(Seq(5, 4, 4), Seq(3, 2), Seq(1))

      Iterable(1, 4, 2, 3, 4, 5).groupedBy(maxDiffIsOne) shouldBe Seq(Seq(1, 2), Seq(4, 3, 4), Seq(5))

      Iterable(5, 2, 3, 1, 4).groupedBy(maxDiffIsOne) shouldBe Seq(Seq(5, 4), Seq(2, 3), Seq(1))

      Iterable(1, 2, 3, 4, 5).groupedBy { case (group, elem) =>
        elem - group.last <= 1
      } shouldBe Seq(Seq(1, 2, 3, 4, 5))
    }
  }

  "runSequential" should {

    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    "return empty Vector on empty Iterable" in {
      def action(i: Int): Future[Double] = Future.successful(i + 0.5)
      Await.result(Iterable.empty[Int].runSequential(action), Inf) shouldBe Vector.empty[Double]
    }

    "run actions on Iterable elements one by one, only after all previous succeeded" in {
      var actionCounter = 0
      def action(i: Int): Future[Unit] = {
        if (i == 1) {
          actionCounter shouldBe 0
          Future {
            Thread.sleep(100)
            actionCounter += 1
          }
        } else if (i == 2) {
          actionCounter shouldBe 1
          Future {
            actionCounter += 1
          }
        } else {
          Future {
            actionCounter += 1
            throw new IllegalArgumentException
          }
        }
      }
      intercept[IllegalArgumentException] {
        Await.result(Iterable(1, 2, 3, 4).runSequential(action), Inf)
      }
      actionCounter shouldBe 3 // 4 step was not executed coz of failure on 3 step
    }
  }

  "collectSuccess" should {

    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

    "return empty Vector on empty Iterable" in {
      Await.result(Iterable.empty[Future[Int]].collectSuccess, Inf) shouldBe Vector.empty[Double]
    }

    "return empty Vector if all Iterable futures are failed" in {
      val futures = Iterable(
        Future.failed(new RuntimeException),
        Future(throw new RuntimeException)
      )
      Await.result(futures.collectSuccess, Inf) shouldBe Vector.empty[Double]
    }

    "collect only successful results of Iterable futures and call onFailure for all others" in {
      val futures = Iterable(
        Future.successful(1),
        Future(2),
        Future.failed(new RuntimeException),
        Future(3),
        Future.failed(new IllegalArgumentException),
        Future.failed(new ArrayIndexOutOfBoundsException)
      )
      val exceptionCounts = scala.collection.mutable.Map(
        "RuntimeException" -> 0,
        "IllegalArgumentException" -> 0,
        "Other" -> 0
      )
      def onFailure(ex: Throwable): Unit = {
        val exName = ex.getClass.getSimpleName
        exceptionCounts.get(exName) match {
          case Some(count) => exceptionCounts += exName -> (count + 1)
          case None => exceptionCounts += "Other" -> (exceptionCounts("Other") + 1)
        }
      }
      Await.result(futures.collectSuccess(onFailure), Inf) shouldBe Vector(1, 2, 3)
      exceptionCounts("RuntimeException") shouldBe 1
      exceptionCounts("IllegalArgumentException") shouldBe 1
      exceptionCounts("Other") shouldBe 1
    }
  }
}
