package ru.yandex.vertis.feedprocessor.util

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.stream.testkit.{TestPublisher, TestSubscriber}
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext

/**
  * Base class for all tests of Akka Stream stages
  * @see ru.yandex.vertis.feedprocessor.util.StreamTestBase
  */
abstract class StreamTestBase
  extends TestKit(ActorSystem("Test"))
  with Matchers
  with AnyWordSpecLike
  with ScalaFutures {

  val decider: Supervision.Decider = { e =>
    fail("Unhandled exception in stream", e)
    Supervision.Stop
  }

  implicit val materalizer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider)
  )(system)

  implicit protected val executionContext: ExecutionContext = system.dispatcher

  def createPubSub[I, O, Mat](flow: Graph[FlowShape[I, O], Mat]): (TestPublisher.Probe[I], TestSubscriber.Probe[O]) = {
    val source = TestSource.probe[I]
    source.via(flow).toMat(TestSink.probe)(Keep.both).run()
  }
}
