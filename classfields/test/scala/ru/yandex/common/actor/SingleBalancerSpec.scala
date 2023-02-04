package ru.yandex.common.actor

import akka.actor.{Actor, ActorSystem, DeadLetter, Props}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfter, FlatSpecLike, Matchers}
import ru.yandex.common.monitoring.HealthChecks

import scala.concurrent.duration._

/**
 * User: daedra
 * Date: 14.08.13
 * Time: 23:45
 */
class SingleBalancerSpec(_system: ActorSystem)
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


  ignore should "almost evenly distribute messages" in {
    val probe = new TestProbe(_system)

    val MESSAGES = 1000

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
      new SingleBalancer(List(system.actorSelection(target1.path), system.actorSelection(target2.path))))


    probe.within(10.second) {
      for (i <- 1 to MESSAGES)
        balancer ! i

      probe.expectMsgAllOf(1 to MESSAGES: _*)
    }

    counter1 should be(MESSAGES)
    counter2 should be(0)
  }

  "Balancer" should "distribute messages to only one alive actor" in {
    val probe = new TestProbe(_system)
    val MESSAGES = 10

    val target1 = _system.actorSelection("/inexistingActor")

    var counter2 = 0
    val target2 = TestActorRef(new Actor {
      def receive = {
        case x =>
          counter2 += 1
          probe.ref ! x
      }
    })

    val balancer = TestActorRef(
      new SingleBalancer(List(target1, system.actorSelection(target2.path))))

    val deadLetterNotifier = _system
        .actorOf(Props(new DeadLetterNotifier(List(balancer))),
      "deadLetterNotifier")
    _system.eventStream.subscribe(deadLetterNotifier, classOf[DeadLetter])


    probe.within(10.seconds) {
      for (i <- 1 to MESSAGES)
        balancer ! i

      probe.expectMsgAllOf(1 to MESSAGES: _*)
    }

    counter2 should be(MESSAGES)

    HealthChecks.defaultRegistry().runHealthChecks().get("single-balancer-accessible-targets")
        .isHealthy should be(right = true)
  }

  "Balancer" should "distribute no messages" in {
    val MESSAGES = 10

    val target = _system.actorSelection("/inexistingActor")

    val balancer = TestActorRef(
      new SingleBalancer(List(target)))

    val deadLetterNotifier = _system
        .actorOf(Props(new DeadLetterNotifier(List(balancer))),
      "deadLetterNotifier2")
    system.eventStream.subscribe(deadLetterNotifier, classOf[DeadLetter])


    for (i <- 1 to MESSAGES)
      balancer ! i
    Thread.sleep(10000)

    HealthChecks.defaultRegistry().runHealthChecks().get("single-balancer-accessible-targets")
        .isHealthy should be(right = false)
  }
}
