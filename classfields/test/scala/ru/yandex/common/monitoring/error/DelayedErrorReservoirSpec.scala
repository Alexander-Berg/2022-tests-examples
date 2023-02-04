package ru.yandex.common.monitoring.error

import java.util.Random

import com.codahale.metrics.health.HealthCheckRegistry
import scala.concurrent.duration._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}

import ru.yandex.common.monitoring.healthchecks.{beHealthy, beUnhealthy, beWarning}

/**
 * Tests for [[DelayedErrorReservoir]]
 *
 * @author rmuzhikov
 */
@RunWith(classOf[JUnitRunner])
class DelayedErrorReservoirSpec
  extends WordSpec
  with Matchers {

  "DelayedErrorReservoir" should {
    "change unhealthy health check result to warning during error delay interval" in {
      val r = new HealthCheckRegistry()
      val delay = 1.second

      val reservoir = new DelayedErrorReservoir(
        delay,
        new ExpiringWarningErrorPercentileReservoir(0, 50, windowSize = 100)
      )

      val name = "test" + new Random().nextInt()
      ErrorReservoirs.register(name, reservoir, r)
      r.runHealthChecks().get(name) should beHealthy

      reservoir.error()
      r.runHealthChecks().get(name) should beWarning

      Thread.sleep(delay.toMillis)
      r.runHealthChecks().get(name) should beUnhealthy

      (0 until 100).foreach(_ => reservoir.ok())
      r.runHealthChecks().get(name) should beHealthy

      (0 until 51).foreach(_ => reservoir.error())
      r.runHealthChecks().get(name) should beWarning

      Thread.sleep(delay.toMillis)
      r.runHealthChecks().get(name) should beUnhealthy

      (0 until 50).foreach(_ => reservoir.ok())
      r.runHealthChecks().get(name) should beWarning

      (0 until 50).foreach(_ => reservoir.ok())
      r.runHealthChecks().get(name) should beHealthy
    }
  }
}
