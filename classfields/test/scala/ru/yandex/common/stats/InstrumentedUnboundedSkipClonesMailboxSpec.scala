package ru.yandex.common.stats

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}

/** Tests for [[ru.yandex.common.stats.InstrumentedUnboundedMailbox]]
 *
 * @author incubos
 */
class InstrumentedUnboundedSkipClonesMailboxSpec(_system: ActorSystem)
    extends TestKit(_system)
    with WordSpecLike
    with Matchers {

  def this() = this(
    ActorSystem(
      "testsystem",
      ConfigFactory.parseString( """
akka.event-handlers = ["akka.testkit.TestEventListener"]

dispatcher.forwarder {
  type = PinnedDispatcher
  executor = "thread-pool-executor"
  throughput = 1
  mailbox-type = ru.yandex.common.stats.InstrumentedUnboundedSkipClonesMailbox
}
                                 """)))

  import concurrent.duration._

  val probe = TestProbe()
  val forwarder = _system.actorOf(Props(
    new Actor {
      def receive = {
        case msg: AnyRef => probe.ref ! msg
      }
    }).withDispatcher("dispatcher.forwarder"),
    "forwarder")

  "InstrumentedUnboundedSkipClonesMailbox" should {
    "work as an ordinary unbounded mailbox processing 1K different messages" in {
      probe.within(10.seconds) {
        val msgs = 1 to 1000

        for (i <- msgs)
          forwarder ! i

        probe.expectMsgAllOf(msgs: _*)
      }
    }
  }

  val slower = _system.actorOf(Props(
    new Actor {
      def receive = {
        case msg: AnyRef =>
          Thread.sleep(5000)
          probe.ref ! msg
      }
    }).withDispatcher("dispatcher.forwarder"),
    "slower")

  "InstrumentedUnboundedSkipClonesMailbox" should {
    "skip some message clones" in {
      probe.within(15.seconds) {
        slower ! 0
        slower ! 1
        slower ! 1

        probe.expectMsg(0)
        probe.expectMsg(1)
        probe.expectNoMessage()
      }
    }
  }
}
