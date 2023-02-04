package ru.yandex.common.monitoring.error

import java.util.Random

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.common.monitoring.HealthChecks
import ru.yandex.common.monitoring.healthchecks.{beHealthy, beUnhealthy, beWarning}

import scala.collection.JavaConverters._
import scala.concurrent.duration._

/**
 * Tests for [[ExpiringWarningErrorPercentileReservoir]]
 *
 * @author incubos
 */
@RunWith(classOf[JUnitRunner])
class ExpiringWarningErrorCounterReservoirSpec
    extends WordSpec
    with Matchers {

  "ExpiringWarningErrorPercentileReservoir" should {
    "check last error" in {
      val r = HealthChecks.defaultRegistry()

      // Unregister all
      r.runHealthChecks().asScala.toMap.foreach(kv => r.unregister(kv._1))

      r.runHealthChecks().asScala.foreach(_._2 should beHealthy)

      val reservoir =
        new ExpiringWarningErrorCounterReservoir(5, 10, 3.seconds, 100)
      val name = "test" + new Random().nextInt()
      ErrorReservoirs.register(name, reservoir)

      // No results

      r.runHealthChecks().get(name) should beHealthy

      // 1/1 errors => critical

      reservoir.error()

      r.runHealthChecks().get(name) should beHealthy

      // 0/100 errors => OK

      (0 until 100).foreach(_ => reservoir.ok())

      r.runHealthChecks().get(name) should beHealthy

      // 11/100 errors => warning

      (0 until 6).foreach(_ => reservoir.error())

      r.runHealthChecks().get(name) should beWarning

      // 0/100 errors => OK

      (0 until 100).foreach(_ => reservoir.ok())

      r.runHealthChecks().get(name) should beHealthy

      // 51/100 errors => critical

      (0 until 11).foreach(_ => reservoir.error())

      r.runHealthChecks().get(name) should beUnhealthy

      (0 until 100).foreach(_ => reservoir.ok())

      r.runHealthChecks().get(name) should beHealthy

      // 5/100 errors => OK

      (0 until 5).foreach(_ => reservoir.ok())

      r.runHealthChecks().get(name) should beHealthy
    }

    "support expire" in {
      val r = HealthChecks.defaultRegistry()

      // Unregister all
      r.runHealthChecks().asScala.toMap.foreach(kv => r.unregister(kv._1))

      r.runHealthChecks().asScala.foreach(_._2 should beHealthy)

      val reservoir =
        new ExpiringWarningErrorCounterReservoir(5, 10, 3.seconds, 100)
      val name = "test" + new Random().nextInt()
      ErrorReservoirs.register(name, reservoir)

      // No results

      r.runHealthChecks().get(name) should beHealthy

      // 100% errors => OK

      (0 until 100).foreach(_ => reservoir.error())

      r.runHealthChecks().get(name) should beUnhealthy

      // 0% errors => OK

      Thread.sleep(5.seconds.toMillis)

      r.runHealthChecks().get(name) should beHealthy
    }
  }
}
