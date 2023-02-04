package ru.yandex.realty.ci.util

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase

import scala.concurrent.Future

/**
  * @author abulychev
  */
@RunWith(classOf[JUnitRunner])
class FSMSpec extends AsyncSpecBase {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "FSM" should {
    "correctly process" in {
      val source =
        FSM
          .newBuilder[Int]()
          .init(1)
          .handleFuture {
            case x if x <= 3 => Future.successful(x + 1)
          }
          .handleSource {
            case 4 => Source(4 to 6)
          }
          .handleSource {
            case 10 => Source(1 to 3) // Unreachable
          }
          .run()

      source.runWith(Sink.seq).futureValue should be(Seq(1, 2, 3, 4, 4, 5, 6))
    }

    "be infinite" in {
      val source =
        FSM
          .newBuilder[Int]()
          .init(1)
          .handleFuture {
            case x if x <= 3 => Future.successful(x + 1)
          }
          .handleSource {
            case 4 => Source.single(1)
          }
          .run()

      val result = source.take(100).runWith(Sink.seq).futureValue
      result.length should be(100)
      result.startsWith(Seq(1, 2, 3, 4, 1, 2, 3, 4)) should be(true)
    }
  }

}
