package ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.util

import akka.stream.scaladsl._
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.util.StreamTestBase

import scala.concurrent.duration._

/**
  * @author pnaydenov
  */
class FoldWithinSpec extends StreamTestBase with WordSpecBase {
  val flow = Flow[Int].via(new FoldWithin[Int, List[Int]](3, 10.millis, zero = Nil)(_ :: _))

  "FoldWithin" should {
    "emit after n-th messages" in {
      val (pub, sub) = createPubSub(flow)
      sub.request(2)
      pub.sendNext(1)
      pub.sendNext(2)
      sub.expectNoMessage(5.millis)
      pub.sendNext(3)
      sub.expectNext(10.millis) shouldEqual List(3, 2, 1)
      sub.expectNoMessage(10.millis)

      pub.sendNext(4)
      pub.sendNext(5)
      pub.sendNext(6)
      sub.expectNext(5.millis) shouldEqual List(6, 5, 4)
    }

    "emit after timeout" in {
      val (pub, sub) = createPubSub(flow)
      sub.request(2)
      pub.sendNext(1)
      pub.sendNext(2)
      sub.expectNext(50.millis) shouldEqual List(2, 1)
      sub.expectNoMessage(10.millis)

      pub.sendNext(3)
      pub.sendNext(4)
      sub.expectNext(50.millis) shouldEqual List(4, 3)
      sub.expectNoMessage(10.millis)
    }

    "don't emit empty group" in {
      val (pub, sub) = createPubSub(flow)
      sub.request(1)
      sub.expectNoMessage(10.millis)
    }
  }
}
