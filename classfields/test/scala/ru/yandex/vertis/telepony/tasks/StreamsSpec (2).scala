package ru.yandex.vertis.telepony.tasks

import java.util.concurrent.atomic.AtomicInteger
import akka.actor.ActorSystem
import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorAttributes, ActorMaterializerSettings, Materializer, Supervision}
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.vertis.telepony.server.env.ConfigHelper
import ru.yandex.vertis.telepony.util.akka.Streams

import scala.concurrent.duration._

/**
  * @author evans
  */
class StreamsSpec extends AnyWordSpecLike {

  val config = ConfigHelper.load(Seq("application-test.conf"))

  implicit val ac = ActorSystem("test", config)
  implicit val mat = Materializer(ac)

  "Streams" should {
    "produce all elements" in {
      val elems = 1.to(10)
      val src = Source(elems)
      val limiter = Streams.limiterFlow[Int](10.millis)(() => true)
      src
        .via(limiter)
        .runWith(TestSink.probe[Int])
        .request(10)
        .expectNextN(elems)
        .expectComplete()
    }
    "produce nothing" in {
      val elems = 1.to(10)
      val src = Source(elems)
      val limiter = Streams.limiterFlow[Int](10.millis)(() => false)
      src
        .via(limiter)
        .runWith(TestSink.probe[Int])
        .request(10)
        .expectNoMessage(500.millis)
    }
    "produce with limiting" in {
      val elems = 1.to(10)
      val src = Source(elems)
      val counter = new AtomicInteger()
      def resourceExits(): Boolean = counter.getAndIncrement() != 5
      val limiter = Streams.limiterFlow[Int](1000.millis)(resourceExits _)
      src
        .via(limiter)
        .runWith(TestSink.probe[Int])
        .request(10)
        .expectNextN(elems.take(5))
        .expectNoMessage(400.millis)
        .expectNextN(elems.drop(5))
        .expectComplete()
    }
    "use different supervision strategies" in {
      val actorMaterializer: Materializer = Materializer(ac)
      val elems = 1.to(10).map(_ => 0)
      val src = Source(elems)
      val limiter = Streams.limiterFlow[Int](10.millis)(() => true)
      val source = src.via(limiter).map(1 / _)
      // resume
      source
        .withAttributes(supervisionStrategy(Supervision.resumingDecider))
        .runWith(TestSink.probe[Int])(actorMaterializer)
        .request(10)
        .expectComplete()
      // stop
      source
        .withAttributes(supervisionStrategy(Supervision.stoppingDecider))
        .runWith(TestSink.probe[Int])(actorMaterializer)
        .request(10)
        .expectError()
    }
  }
}
