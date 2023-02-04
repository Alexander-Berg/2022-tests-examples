package ru.yandex.vertis.subscriptions.util.zio

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.subscriptions.{SpecBase, TestExecutionContext}
import zio._
import zio.stream.Stream

import scala.concurrent.{Future, Promise}

/**
  *
  * @author zvez
  */
@RunWith(classOf[JUnitRunner])
class BlockingStreamIteratorSpec extends SpecBase with TestExecutionContext {

  private val runtime = Runtime.default

  "BlockingStreamIterator" should {
    "work on empty streams" in {
      val iter = new BlockingStreamIterator(Stream(), runtime)
      iter.toSeq shouldBe empty
    }

    "return all elements" in {
      val elements = Seq(1, 2, 3)
      val iter = new BlockingStreamIterator(Stream(elements: _*), runtime)
      (iter.toSeq should contain).theSameElementsInOrderAs(elements)
    }

    "block till next element is available" in {
      val cbPromise = Promise[IO[Option[Throwable], Int] => Unit]()
      val stream = Stream.effectAsync[Throwable, Int](cb => cbPromise.success(cb))
      val xsF = Future {
        new BlockingStreamIterator(stream, runtime).toArray
      }
      xsF.isCompleted shouldBe false
      val cb = cbPromise.future.futureValue
      cb(ZIO(1).mapError(Some(_)))
      xsF.isCompleted shouldBe false
      cb(ZIO(2).mapError(Some(_)))
      xsF.isCompleted shouldBe false
      cb(ZIO(3).mapError(Some(_)))
      xsF.isCompleted shouldBe false
      cb(ZIO.fail(None))

      (xsF.futureValue should contain).theSameElementsInOrderAs(Seq(1, 2, 3))
    }

    /* "back pressure" in {
      val infinity = ZStream.effectAsyncInterrupt[Any, Throwable, Int] { cb =>
        Future {
          (0 to Int.MaxValue).foreach { i =>
            println(i)
            cb(ZIO(i).mapError(Some(_)))
          }
        }
        Left(UIO[Any](Unit))
      }
      /*val infinity = Stream.iterate(0) { v =>
        println(v)
        v + 1
      }*/
      val iter = new BlockingStreamIterator(infinity.buffer(1024), runtime)
      iter.take(3).toSeq should contain theSameElementsInOrderAs Seq(0, 1, 2)
      Thread.sleep(100000)
      iter.take(3).toSeq should contain theSameElementsInOrderAs Seq(3, 4, 5)
    } */
  }

}
