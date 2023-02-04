package ru.yandex.vertis.billing.monitoring

import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, PoisonPill, Props, SupervisorStrategy, Terminated}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.FixtureAnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, Outcome}
import ru.yandex.common.monitoring.{CompoundHealthCheckRegistry, HealthChecks}
import ru.yandex.vertis.billing.actor.monitoring.ActorLivelinessMonitor
import ru.yandex.vertis.billing.monitoring.ActorLivelinessMonitorSpec._

import scala.annotation.nowarn
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

/**
  * Runnable specs on [[ActorLivelinessMonitor]]
  *
  * @author alex-kovalenko
  */
class ActorLivelinessMonitorSpec
  extends TestKit(ActorSystem("ActorLivelinessMonitorSpec"))
  with ImplicitSender
  with Matchers
  with FixtureAnyWordSpecLike
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val supervisor = system.actorOf(Props(new Supervisor))

  implicit val timeout = Timeout(1.second)

  type FixtureParam = ActorRef

  def withFixture(test: OneArgTest): Outcome = {
    val actor = TestActorRef(Props(new TestActor with Monitored), supervisor, actorName)
    try {
      withFixture(test.toNoArgTest(actor))
    } finally {
      actor ? PoisonPill
      healthChecks.unregister(checkName)
      assert(healthChecks.runHealthChecks().isEmpty): @nowarn("msg=discarded non-Unit value")
    }
  }

  "ActorLivelinessMonitor" should {
    "register health check by name" in { actor =>
      healthChecks.runDeveloperChecks().asScala.get(checkName) match {
        case Some(result) if result.isHealthy =>
        case other => fail(s"Unexpected $other")
      }
    }

    "report about failed actors" in { actor =>
      watch(actor)
      actor ! "stop"
      expectMsgPF() { case Terminated(`actor`) => }
      healthChecks.runDeveloperChecks().asScala.get(checkName) match {
        case Some(r) if !r.isHealthy =>
        case other => fail(s"Unexpected $other")
      }
    }

    "correctly process on restart" in { actor =>
      watch(actor)
      actor ? "restart"
      healthChecks.runDeveloperChecks().asScala.get(checkName) match {
        case Some(result) if result.isHealthy =>
        case other => fail(s"Unexpected $other")
      }
    }
  }
}

object ActorLivelinessMonitorSpec {

  class TestActor extends Actor {

    def receive: Receive = {
      case "stop" => throw new NoSuchElementException("Artificial")
      case "restart" => throw new IllegalArgumentException("Artificial")
    }
  }

  val healthChecks = HealthChecks.compoundRegistry()

  trait Monitored extends ActorLivelinessMonitor {

    def healthChecks: CompoundHealthCheckRegistry =
      ActorLivelinessMonitorSpec.this.healthChecks

    override def getName(actor: Actor): String = actorName
  }

  val actorName = "test-actor"
  val checkName = s"$actorName-alive"

  class Supervisor extends Actor {

    override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
      case _: IllegalArgumentException => Restart
      case _: NoSuchElementException => Stop
      case t => super.supervisorStrategy.decider.applyOrElse(t, (_: Any) => Escalate)
    }

    override def receive: Receive = { case _ =>
      ()
    }
  }
}
