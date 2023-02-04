package ru.yandex.common.actor

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.common.actor.Batcher.BatchRequest

import scala.concurrent.duration._

/**
 * Unit tests for [[ru.yandex.common.actor.Batcher]]
 *
 * @author incubos
 */
class BatcherSpec
    extends TestKit(ActorSystem("BatcherSpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "Batcher" should {
    "accumulate and forward batch" in {
      val probe = TestProbe()
      val batchSize = 3
      val batcher =
        TestActorRef(
          Batcher.props[Int](
            probe.ref,
            batchSize,
            1.minute))

      val e = 1


      probe.within(5.seconds) {
        for (_ <- 0 until batchSize)
          batcher ! e

        probe.expectMsg(BatchRequest((0 until batchSize).map(_ => e)))
      }
    }

    "flush messages" in {
      val probe = TestProbe()
      val batchSize = 3
      val batcher =
        TestActorRef(
          Batcher.props[Int](
            probe.ref,
            batchSize,
            1.second))

      val e = 2

      probe.within(5.seconds) {
        batcher ! e

        probe.expectMsg(BatchRequest(IndexedSeq(e)))
      }
    }

    "send batch and flush" in {
      val probe = TestProbe()
      val batchSize = 3
      val batcher =
        TestActorRef(
          Batcher.props[Int](
            probe.ref,
            batchSize,
            1.second))

      val e = 3

      probe.within(5.seconds) {
        for (_ <- 0 until (batchSize + 1))
          batcher ! e

        probe.expectMsg(BatchRequest((0 until batchSize).map(_ => e)))
        probe.expectMsg(BatchRequest(IndexedSeq(e)))
      }
    }
  }
}
