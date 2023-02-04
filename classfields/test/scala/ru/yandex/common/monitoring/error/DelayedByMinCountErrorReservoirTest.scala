package ru.yandex.common.monitoring.error

import java.util.Random

import com.codahale.metrics.health.HealthCheckRegistry
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.common.monitoring.healthchecks.{beHealthy, beUnhealthy}

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 25.11.15
  */
@RunWith(classOf[JUnitRunner])
class DelayedByMinCountErrorReservoirTest extends FlatSpec with Matchers {
  "DelayedByMinCountErrorReservoir" should "just correct work" in {
    val r = new HealthCheckRegistry()
    val minCount = 2
    val reservoir = new DelayedByMinCountErrorReservoir(
      new ExpiringWarningErrorPercentileReservoir(0, 50, windowSize = 100),
        minCount
    )
    val name = "test" + new Random().nextInt()
    ErrorReservoirs.register(name, reservoir, r)
    r.runHealthChecks().get(name) should beHealthy

    reservoir.error()
    r.runHealthChecks().get(name) should beHealthy

    reservoir.error()
    r.runHealthChecks().get(name) should beUnhealthy
  }
}
