package ru.yandex.common.monitoring

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.common.monitoring.healthchecks._

import scala.concurrent.duration.DurationInt
import scala.util.Failure

/**
 * Spec on [[SuppressHealthCheckRegistry]]
 *
 * @author ruslansd
 */
@RunWith(classOf[JUnitRunner])
class SuppressHealthCheckRegistrySpec
  extends Matchers
  with WordSpecLike {

  "SuppressCriticalHealthCheckRegistry" should {
    "don't suppress healthy and not suppressed unhealthy checks" in {
      val r = new SuppressHealthCheckRegistry

      r.register(UnhealthyCheck.name, UnhealthyCheck)
      r.register(HealthyCheck.name, HealthyCheck)

      r.runHealthCheck(UnhealthyCheck.name) should (beUnhealthy and containMessage("fail"))
      r.runHealthCheck(HealthyCheck.name) should beHealthy
    }

    "don't suppress not existing health checks" in {
      val r = new SuppressHealthCheckRegistry

      r.suppressFor(UnhealthyCheck.name, 10.minutes) match {
        case Failure(_: IllegalArgumentException) =>
        case other =>
          fail(s"Unexpected $other")
      }

      r.get(UnhealthyCheck.name) should be (None)
    }

    "suppress only unhealthy checks" in {
      val r = new SuppressHealthCheckRegistry

      r.register(UnhealthyCheck.name, UnhealthyCheck)
      r.register(HealthyCheck.name, HealthyCheck)

      r.suppressFor(UnhealthyCheck.name, 10.minutes)
      r.suppressFor(HealthyCheck.name, 10.minutes)

      val result = r.runHealthChecks()

      result.get(UnhealthyCheck.name) should beWarning
      result.get(HealthyCheck.name) should beHealthy
    }

    "undo suppress automatically after specified duration" in {
      val r = new SuppressHealthCheckRegistry

      r.register(UnhealthyCheck.name, UnhealthyCheck)
      r.suppressFor(UnhealthyCheck.name, 2.seconds)

      r.runHealthCheck(UnhealthyCheck.name) should beWarning
      Thread.sleep(2000)

      r.runHealthCheck(UnhealthyCheck.name) should (beUnhealthy and containMessage("fail"))
    }

    "undo suppress manually" in {
      val r = new SuppressHealthCheckRegistry

      r.register(UnhealthyCheck.name, UnhealthyCheck)
      r.suppressFor(UnhealthyCheck.name, 10.minutes)

      r.runHealthCheck(UnhealthyCheck.name) should beWarning
      r.unSuppress(UnhealthyCheck.name)

      r.runHealthCheck(UnhealthyCheck.name) should (beUnhealthy and containMessage("fail"))
    }

    "suppress warnings" in {
      val r = new SuppressHealthCheckRegistry
      r.register(WarningCheck.name, WarningCheck)
      r.suppressFor(WarningCheck.name, 10.minutes)
      val result = r.runHealthChecks().get(WarningCheck.name)
      result should (beWarning and containMessage("suppressed"))
    }

    "suppress healthy" in {
      val r = new SuppressHealthCheckRegistry
      r.register(HealthyCheck.name, HealthyCheck)
      r.suppressFor(HealthyCheck.name, 10.minutes)
      val (checkName, _) :: _ = r.getSuppressedChecks
      checkName shouldBe HealthyCheck.name
    }

  }

}
