package ru.yandex.common.actor

import java.util.concurrent.atomic.AtomicInteger

import akka.testkit._
import org.scalatest._
import org.slf4j.LoggerFactory
import ru.yandex.common.monitoring.HealthChecks

import scala.collection.JavaConverters._


/**
 * Specification for [[ru.yandex.common.actor.BurstScaler]] actor
 *
 * @author incubos
 */
trait BurstScalerSpec
    extends ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterEach {
  this: TestKit =>

  val log = LoggerFactory.getLogger(this.getClass)

  import scala.concurrent.duration._

  def scaler(probe: TestProbe, initialDelay: Boolean): () => BurstScaler[Long]

  def burstScaler(initialDelay: Boolean = false)
      (implicit probe: TestProbe): (TestActorRef[BurstScaler[Long]], String) = {
    val name = "burst-scaler-" + BurstScalerSpec.actorCounter.getAndIncrement
    val actor = TestActorRef(scaler(probe, initialDelay)(), name)
    (actor, name)
  }


  "A BurstScaler" should {
    "always forward the first message" in {
      implicit val probe = TestProbe()
      val (scaler, _) = burstScaler()
      scaler ! 1L
      probe.expectMsg(3.seconds, 1L)
      probe.expectNoMessage(5.second)
    }

    "block first message in initial delay mode" in {
      implicit val probe = TestProbe()
      val (scaler, _) = burstScaler(true)
      probe.within(5.seconds) {
        scaler ! 1L
        probe.expectNoMessage()
      }
      probe.within(5.second) {
        scaler ! 1L

        probe.expectMsg(1L)
      }
    }

    "suppress messages withing delay" in {
      implicit val probe = TestProbe()
      val (scaler, _) = burstScaler()
      probe.within(5.second) {
        scaler ! 1L

        probe.expectMsg(1L)
      }

      scaler ! 1L

      probe.expectNoMessage(6.second)

      probe.within(5.second) {
        scaler ! 1L

        probe.expectMsg(1L)
      }

      probe.expectNoMessage(5.second)
    }

    "forward all the messages of other types" in {
      implicit val probe = TestProbe()
      val (scaler, _) = burstScaler()
      probe.within(5.second) {
        scaler ! "Test"

        probe.expectMsg("Test")
      }

      probe.within(5.second) {
        scaler ! "Test"

        probe.expectMsg("Test")
      }
    }

    "checks be not ok" in {
      implicit val probe = TestProbe()
      val (scaler, name) = burstScaler()
      probe.within(5.second) {
        scaler ! 1L

        probe.expectMsg(1L)
      }

      probe.expectNoMessage(6.seconds)

      val failedChecks =
        HealthChecks
            .defaultRegistry()
            .runHealthChecks()
            .asScala
            .filterNot(_._2.isHealthy)

      log.info("failed checks:" + failedChecks)

      HealthChecks.defaultRegistry().runHealthChecks()
          .get(s"$name-events").isHealthy should be(false)
    }

  }
}

object BurstScalerSpec {

  private val actorCounter = new AtomicInteger()
}
