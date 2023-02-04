package ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.util

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink}
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.util.StreamUtils._
import ru.yandex.vertis.feedprocessor.util.StreamTestBase

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author pnaydenov
  */
class StreamUtilsTest extends StreamTestBase {

  "StreamUtils.mapAsyncWithRecovery" should {
    "pass initial message in case of error" in {
      val errors = collection.mutable.ArrayBuffer.empty[(Throwable, Int)]

      val flow: Flow[Int, Long, NotUsed] = Flow[Int]
        .mapAsyncWithRecovery(1, Sink.foreach(errors += _), recoverMessage = (ex: Throwable, msg: Int) => msg.toLong)(
          evenAllowedAsync
        )
      val (pub, sub) = createPubSub(flow)

      sub.request(3)
      pub.sendNext(2)
      sub.expectNext() shouldEqual 20L
      pub.sendNext(3)
      sub.expectNext() shouldEqual 3L
      pub.sendNext(4)
      sub.expectNext() shouldEqual 40L

      errors.toList should have size 1
      errors.head._2 shouldEqual 3
    }
  }

  "StreamUtils.mapAsyncFilterFailed" should {
    "silently filter failures" in {
      val errors = collection.mutable.ArrayBuffer.empty[(Throwable, Int)]

      val flow: Flow[Int, Long, NotUsed] = Flow[Int]
        .mapAsyncFilterFailed(1, Sink.foreach(errors += _))(evenAllowedAsync)
      val (pub, sub) = createPubSub(flow)
      sub.request(3)
      pub.sendNext(3)
      sub.expectNoMessage(100.millis)
      pub.sendNext(2)
      sub.expectNext() shouldEqual 20L

      errors.toList should have size 1
      errors.head._2 shouldEqual 3
    }
  }

  "StreamUtils.mapWithRecovery" should {
    "pass initial message in case of error" in {
      val errors = collection.mutable.ArrayBuffer.empty[(Throwable, Int)]

      val flow: Flow[Int, Long, NotUsed] = Flow[Int]
        .mapWithRecovery(Sink.foreach(errors += _), recoverMessage = (ex: Throwable, msg: Int) => msg.toLong)(
          evenAllowed
        )
      val (pub, sub) = createPubSub(flow)

      sub.request(3)
      pub.sendNext(2)
      sub.expectNext() shouldEqual 20L
      pub.sendNext(3)
      sub.expectNext() shouldEqual 3L
      pub.sendNext(4)
      sub.expectNext() shouldEqual 40L

      errors.toList should have size 1
      errors.head._2 shouldEqual 3
    }
  }

  def evenAllowedAsync(i: Int): Future[Long] =
    Future {
      evenAllowed(i)
    }

  def evenAllowed(i: Int): Long = {
    if (i % 2 == 0) (i * 10).toLong
    else throw new RuntimeException(s"Only even allowed but $i given")
  }
}
