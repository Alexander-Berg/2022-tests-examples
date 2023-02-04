package ru.yandex.realty.ci.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase

import scala.concurrent.Future
import scala.util.control.NoStackTrace
import scala.util.{Failure, Success}

/**
  * @author abulychev
  */
@RunWith(classOf[JUnitRunner])
class StreamUtilsSpec extends AsyncSpecBase {
  import StreamUtils._

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "StreamUtils" should {

    "safely create source from success future" in {
      val f = Future.successful(1)
      safeSourceFromFuture(f).runWith(Sink.seq).futureValue should be(Seq(Success(1)))
    }

    "safely create source from failed future" in {
      val ex = new CustomException()
      val f = Future.failed(ex)
      safeSourceFromFuture(f).runWith(Sink.seq).futureValue should be(Seq(Failure(ex)))
    }

    "create source from success future" in {
      val f = Future.successful(1)
      sourceFromFuture(f).runWith(Sink.seq).futureValue should be(Seq(1))
    }

    "fail stream on failed future" in {
      val ex = new CustomException()
      val f = Future.failed(ex)
      val s = sourceFromFuture(f)
      interceptCause[CustomException] {
        s.runWith(Sink.seq).futureValue
      }
    }

  }

  "RichSource" should {
    "chain on last element" in {
      val s1 = Source(1 to 3)
      val s2 = s1.chainOnLast { el =>
        Source((el * 2) :: (el * 3) :: Nil)
      }
      s2.runWith(Sink.seq).futureValue should be(Seq(1, 2, 3, 6, 9))
    }

    "chain on last element in empty source" in {
      val s1 = Source.empty[Int]
      val s2 = s1.chainOnLast { el =>
        Source((el * 2) :: (el * 3) :: Nil)
      }
      s2.runWith(Sink.seq).futureValue should be(Seq.empty)
    }
  }

  private class CustomException extends Exception with NoStackTrace

}
