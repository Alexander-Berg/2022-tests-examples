package ru.yandex.common.monitoring.error

import java.util.Random

import com.codahale.metrics.health.HealthCheckRegistry
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.healthchecks.{beHealthy, beUnhealthy, beWarning}

/**
 * Tests for [[AlwaysWarningErrorCounterReservoir]]
 */
@RunWith(classOf[JUnitRunner])
class AlwaysWarningErrorCounterReservoirSpec
  extends WordSpec
  with Matchers {

  "AlwaysWarningErrorCounterReservoir" should {
    "check last error" in {
      val r = new HealthCheckRegistry()

      val reservoir = new AlwaysWarningErrorCounterReservoir(50, 100)
      val name = "test" + new Random().nextInt()
      ErrorReservoirs.register(name, reservoir, r)

      r.runHealthChecks().get(name) should beHealthy

      reservoir.error()

      r.runHealthChecks().get(name) should beWarning

      (0 until 100).foreach(_ => reservoir.ok())

      r.runHealthChecks().get(name) should beHealthy

      (0 until 51).foreach(_ => reservoir.error())

      r.runHealthChecks().get(name) should beUnhealthy

      (0 until 50).foreach(_ => reservoir.ok())

      r.runHealthChecks().get(name) should beWarning

      (0 until 50).foreach(_ => reservoir.ok())

      r.runHealthChecks().get(name) should beHealthy
    }
  }
}
