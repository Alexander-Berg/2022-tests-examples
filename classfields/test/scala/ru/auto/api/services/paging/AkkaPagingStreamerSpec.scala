package ru.auto.api.services.paging

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import ru.auto.api.BaseSpec
import ru.auto.api.model.Paging
import ru.auto.api.services.paging.AkkaPagingStreamerSpec.Ints

import scala.concurrent.Future

class AkkaPagingStreamerSpec extends BaseSpec {

  private val streamer = new AkkaPagingStreamer[Int, Ints](_.ints, _.pageCount)

  implicit private val system: ActorSystem = ActorSystem("test")
  implicit private val mat: Materializer = Materializer.createMaterializer(system)

  "Akka paging streamer" should {

    "fetch all elements" in {
      val result = streamer
        .fetchAllPages(PageSize(2)) {
          case Paging(1, 2) => Future.successful(Ints(List(0, 1), PageCount(3)))
          case Paging(2, 2) => Future.successful(Ints(List(3, 4), PageCount(3)))
          case Paging(3, 2) => Future.successful(Ints(List(2), PageCount(3)))
          case other => fail(s"Unexpected paging == $other")
        }
        .runWith(Sink.seq)
        .futureValue
      result shouldBe Seq(0, 1, 3, 4, 2)
    }

    "fetch no elements" in {
      val result = streamer
        .fetchAllPages(PageSize(10)) {
          case Paging(1, 10) => Future.successful(Ints(List(), PageCount(0)))
          case other => fail(s"Unexpected page == $other")
        }
        .runWith(Sink.seq)
        .futureValue
      result shouldBe Nil
    }
  }
}

object AkkaPagingStreamerSpec {

  case class Ints(ints: List[Int], pageCount: PageCount)
}
