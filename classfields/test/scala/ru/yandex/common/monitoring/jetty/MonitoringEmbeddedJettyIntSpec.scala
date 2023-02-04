package ru.yandex.common.monitoring.jetty

import ru.yandex.common.monitoring._
import ru.yandex.common.monitoring.error.{ErrorReservoirs, ExpiringWarningErrorCounterReservoir}

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result
import org.scalatest.{Matchers, WordSpec}

import scala.util.Random

/**
 * Specification for [[MonitoringEmbeddedJetty]]
 *
 * @author incubos
 */
class MonitoringEmbeddedJettyIntSpec
    extends WordSpec with Matchers {

  "A MonitoringEmbeddedJettySpec" should {
    "start and stop" in {
      val defaultRegistry: CompoundHealthCheckRegistry = new CompoundHealthCheckRegistry()
      val jetty = MonitoringEmbeddedJetty(port = 8081, healthChecks = defaultRegistry).server

      ErrorReservoirs.register("desribed reservoir",new ExpiringWarningErrorCounterReservoir() with Described{
        override def getGuideLine: String = "description of described reservoir"
      },defaultRegistry)

      defaultRegistry.register(
        "Healthy name",
        new DescribedHealthCheck("if it's not ok, something is terribly wrong.") {
          override def check(): Result = {
            Thread.sleep(LoggedHealthCheckRegistry.LONG_CHECK_MS * 2)
            WarningHealthCheck.healthy("Healthy message")
          }
        })
      defaultRegistry.register(
        "Not described name",
        new HealthCheck {
          override def check(): Result = {
            Thread.sleep(LoggedHealthCheckRegistry.LONG_CHECK_MS * 2)
            WarningHealthCheck.healthy("Healthy message")
          }
        })

      defaultRegistry.register(
        "Warning name",
        new DescribedHealthCheck("can be ignored forever") {
          override def check(): Result =
            WarningHealthCheck.warning("Warning message")
        })

      defaultRegistry.register(
        "Unhealthy name",
        new DescribedHealthCheck("It will never be ok, sorry.") {
          override def check(): Result =
            WarningHealthCheck.unhealthy("Unhealthy message")
        })

      val result = defaultRegistry.runHealthChecks()
      result.keySet() should contain ("Healthy name")
      result.keySet() should contain ("Warning name")
      result.keySet() should contain ("Unhealthy name")
      jetty.stop()
    }
    "should respond with 0;ok" in {
      val defaultRegistry: CompoundHealthCheckRegistry = new CompoundHealthCheckRegistry()
      val jetty = MonitoringEmbeddedJetty(port = 8081, healthChecks = defaultRegistry).server

      ErrorReservoirs.register("desribed reservoir",new ExpiringWarningErrorCounterReservoir() with Described{
        override def getGuideLine: String = "description of described reservoir"
      },defaultRegistry)

      defaultRegistry.register(
        "Healthy name",
        new DescribedHealthCheck("if it's not ok, something is terribly wrong.") {
          override def check(): Result = {
            Thread.sleep(LoggedHealthCheckRegistry.LONG_CHECK_MS * 2)
            WarningHealthCheck.healthy("Healthy message")
          }
        })
      defaultRegistry.register(
        "Not described name",
        new HealthCheck {
          override def check(): Result = {
            Thread.sleep(LoggedHealthCheckRegistry.LONG_CHECK_MS * 2)
            WarningHealthCheck.healthy("Healthy message")
          }
        })

      defaultRegistry.register(
        "Warning name",
        new DescribedHealthCheck("can be ignored forever") {
          override def check(): Result =
            WarningHealthCheck.warning("Warning message")
        },false)

      defaultRegistry.register(
        "Unhealthy name",
        new DescribedHealthCheck("It will never be ok, sorry.") {
          override def check(): Result =
            WarningHealthCheck.unhealthy("Unhealthy message")
        },false)

      val result = defaultRegistry.runHealthChecks()
      result.keySet() should contain ("Healthy name")
      result.keySet() should not contain "Warning name"
      result.keySet() should not contain "Unhealthy name"
      jetty.stop()
    }
  }
}
