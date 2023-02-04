package ru.yandex.common.actor

import akka.actor.{ActorSystem, DeadLetter, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import ru.yandex.common.monitoring.error.ExpiringWarningErrorCounterReservoir

import scala.concurrent.duration._

/**
  * Tests for [[ActorMonitor]]
  *
  * @author roose
  */
class ActorMonitorSpec
  extends TestKit(ActorSystem("ActorMonitorSpec"))
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    system.terminate()
  }

  "ActorMonitor" should {
    "detect DeadLetters and Terminated messages" in {
      val deadLetterReservoir = new ExpiringWarningErrorCounterReservoir(
        warningCount = 0,
        errorCount = 8,
        windowSize = 8)
      val kenny = TestProbe()
      val monitor = system.actorOf(
        ActorMonitor.props(Seq(testActor, kenny.ref), deadLetterReservoir),
        "test-monitor"
      )
      system.eventStream.subscribe(monitor, classOf[DeadLetter])

      watch(kenny.ref)
      kenny.ref ! PoisonPill
      expectTerminated(kenny.ref, 500.millis)
      awaitCond(!deadLetterReservoir.toResult.isHealthy, 1.second, 100.millis)
      deadLetterReservoir.toResult.getMessage should startWith ("1")  // one error
      kenny.ref ! "you ok?"
      awaitCond(deadLetterReservoir.toResult.getMessage.startsWith("2"), 1.second, 100.millis)
    }
  }
}
