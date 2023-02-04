package ru.yandex.common.monitoring.error

import java.util.Random

import com.codahale.metrics.health.HealthCheckRegistry
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.healthchecks.{beHealthy, beUnhealthy, beWarning}

import scala.concurrent.duration._

/**
 * User: daedra 
 * Date: 21.08.14
 * Time: 20:04
 */
@RunWith(classOf[JUnitRunner])
class AlwaysWarningErrorPercentileTimeWindowReservoirSpec
  extends WordSpec
  with Matchers {

  "AlwaysWarningErrorPercentileTimeWindowReservoir" should {
    "check errors in last 5 seconds" in {
      val r = new HealthCheckRegistry()

      val reservoir = new AlwaysWarningErrorPercentileTimeWindowReservoir(50, 5.seconds)
      val name = "test" + new Random().nextInt()
      ErrorReservoirs.register(name, reservoir, r)

      r.runHealthChecks().get(name) should beHealthy

      reservoir.ok()

      Thread.sleep(4050)

      r.runHealthChecks().get(name) should beHealthy

      reservoir.error()

      r.runHealthChecks().get(name) should beWarning

      reservoir.error()

      r.runHealthChecks().get(name) should beUnhealthy

      reservoir.ok()

      r.runHealthChecks().get(name) should beWarning

      Thread.sleep(1050)

      r.runHealthChecks().get(name) should beUnhealthy

      Thread.sleep(4050)

      r.runHealthChecks().get(name) should beHealthy

      (0 until 50).foreach(_ => reservoir.ok())

      r.runHealthChecks().get(name) should beHealthy

      (0 until 51).foreach(_ => reservoir.error())

      r.runHealthChecks().get(name) should beUnhealthy

      Thread.sleep(1050)

      reservoir.ok()

      r.runHealthChecks().get(name) should beWarning

      Thread.sleep(4050)

      r.runHealthChecks().get(name) should beHealthy
    }
  }

}
