package ru.yandex.common.actor

import akka.actor.{Actor, ActorSystem}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FlatSpecLike, Matchers}

/**
 * Specification for [[ru.yandex.common.actor.AdaptiveBalancer]] actor
 *
 * @author incubos
 */
class AdaptiveBalancerSpec(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with FlatSpecLike
    with Matchers
    with BeforeAndAfter {

  def this() = this(
    ActorSystem(
      "testsystem",
      ConfigFactory.parseString( """
akka.event-handlers = ["akka.testkit.TestEventListener"]
                                 """)))

  import scala.concurrent.duration._

  val probe = new TestProbe(_system)

  var counter1 = 0
  val target1 = TestActorRef(new Actor {
    def receive = {
      case x =>
        counter1 += 1
        probe.ref ! x
    }
  })

  var counter2 = 0
  val target2 = TestActorRef(new Actor {
    def receive = {
      case x =>
        counter2 += 1
        probe.ref ! x
    }
  })

  val balancer = TestActorRef(
    new AdaptiveBalancer(List(system.actorSelection(target1.path),
      system.actorSelection(target2.path))))

  ignore should "almost evenly distribute messages" in {
    val MESSAGES = 1000

    probe.within(10.second) {
      for (i <- 1 to MESSAGES)
        balancer ! i

      probe.expectMsgAllOf(1 to MESSAGES: _*)
    }

    counter1 should be < (MESSAGES / 2 + 200)
    counter1 should be > (MESSAGES / 2 - 200)
    counter2 should be < (MESSAGES / 2 + 200)
    counter2 should be > (MESSAGES / 2 - 200)
  }
}
