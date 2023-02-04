package ru.yandex.common.monitoring

import java.util.Random
import org.joda.time.{DateTime, Duration}
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import ru.yandex.common.monitoring.WarningHealthCheck._

import org.scalatestplus.junit.JUnitRunner

import scala.collection.JavaConverters._

/**
 * Tests for [[LastErrorChecks]]
 *
 * @author incubos
 */
@RunWith(classOf[JUnitRunner])
class LastErrorChecksSpec
  extends WordSpec
  with Matchers
  with BeforeAndAfterEach {

  val r = HealthChecks.defaultRegistry()

  override protected def beforeEach(): Unit = {
    // Unregister all
    r.runHealthChecks().asScala.toMap.foreach(kv => r.unregister(kv._1))
  }

  "LastErrorChecks" should {
    "check last error" in {
      @volatile
      var lastError = DateTime.now().minusDays(1)

      val name = "test" + new Random().nextInt()
      LastErrorChecks.lastError(
        name,
        () => lastError,
        Duration.standardHours(1),
        Duration.standardHours(3))

      isHealthy(r.runHealthChecks().get(name)) should be(true)

      lastError = DateTime.now().minusMinutes(1)

      isUnhealthy(r.runHealthChecks().get(name)) should be(true)

      lastError = DateTime.now().minusHours(2)

      isWarning(r.runHealthChecks().get(name)) should be(true)

      lastError = DateTime.now().minusHours(4)

      isHealthy(r.runHealthChecks().get(name)) should be(true)
    }

    "check last error via marker" in {
      val name = "test" + new Random().nextInt()
      val marker = LastErrorChecks.lastErrorWithMarker(
        name,
        Duration.standardHours(1),
        Duration.standardHours(3))

      isHealthy(r.runHealthChecks().get(name)) should be(true)

      marker.mark(DateTime.now().minusMinutes(1))

      isUnhealthy(r.runHealthChecks().get(name)) should be(true)

      marker.mark(DateTime.now().minusHours(2))

      isWarning(r.runHealthChecks().get(name)) should be(true)

      marker.mark(DateTime.now().minusHours(4))

      isHealthy(r.runHealthChecks().get(name)) should be(true)
    }

    "check last error as warning" in {
      @volatile
      var lastError = DateTime.now().minusDays(1)

      val name = "test" + new Random().nextInt()
      LastErrorChecks.lastErrorAsWarning(
        name,
        () => lastError,
        Duration.standardHours(1))

      isHealthy(r.runHealthChecks().get(name)) should be(true)

      lastError = DateTime.now().minusMinutes(1)

      isWarning(r.runHealthChecks().get(name)) should be(true)

      lastError = DateTime.now().minusHours(2)

      isHealthy(r.runHealthChecks().get(name)) should be(true)
    }

    "check last error as warning via marker" in {
      val name = "test" + new Random().nextInt()
      val marker = LastErrorChecks.lastErrorAsWarningWithMarker(
        name,
        Duration.standardHours(1))

      isHealthy(r.runHealthChecks().get(name)) should be(true)

      marker.mark(DateTime.now().minusMinutes(1))

      isWarning(r.runHealthChecks().get(name)) should be(true)

      marker.mark(DateTime.now().minusHours(2))

      isHealthy(r.runHealthChecks().get(name)) should be(true)
    }

    "check last event as warning" in {
      @volatile
      var lastEvent = DateTime.now()

      val name = "test" + new Random().nextInt()
      LastErrorChecks.lastEventAsWarning(
        name,
        () => lastEvent,
        Duration.standardHours(1))

      isHealthy(r.runHealthChecks().get(name)) should be(true)

      lastEvent = DateTime.now().minusHours(2)

      isWarning(r.runHealthChecks().get(name)) should be(true)

      lastEvent = DateTime.now().minusMinutes(10)

      isHealthy(r.runHealthChecks().get(name)) should be(true)
    }

    "check last event as warning via marker" in {
      val name = "test" + new Random().nextInt()
      val marker = LastErrorChecks.lastEventAsWarningWithMarker(
        name,
        Duration.standardHours(1))

      isHealthy(r.runHealthChecks().get(name)) should be(true)

      marker.mark(DateTime.now().minusHours(2))

      isWarning(r.runHealthChecks().get(name)) should be(true)

      marker.mark(DateTime.now().minusMinutes(10))

      isHealthy(r.runHealthChecks().get(name)) should be(true)
    }
  }
}
