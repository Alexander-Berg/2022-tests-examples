package ru.yandex.common.stats

import akka.actor.{Actor, ActorSystem, Props}
import akka.routing.FromConfig
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpecLike}

/** Tests for [[ru.yandex.common.stats.InstrumentedUnboundedMailbox]]
 *
 * @author incubos
 */
class InstrumentedUnboundedMailboxSpec(_system: ActorSystem)
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
  mailbox-type = ru.yandex.common.stats.InstrumentedUnboundedMailbox
}

akka.actor.deployment {
  /forwarder {
    router = smallest-mailbox-pool
    nr-of-instances = 2
  }
}
                                 """)))

  import concurrent.duration._

  val probe = TestProbe()
  val forwarder = _system.actorOf(Props(
    new Actor {
      def receive = {
        case msg: AnyRef => probe.ref ! msg
      }
    }).withDispatcher("dispatcher.forwarder").withRouter(FromConfig()),
    "forwarder")

  "InstrumentedUnboundedMailbox" should {
    "work as an ordinary unbounded mailbox processing 1K messages" in {
      probe.within(10.seconds) {
        val msgs = 1 to 1000

        for (i <- msgs)
          forwarder ! i

        probe.expectMsgAllOf(msgs: _*)
      }
    }
  }
}
