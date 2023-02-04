package ru.yandex.common.monitoring.healthcheck

import com.codahale.metrics.health.HealthCheck.Result
import org.joda.time.DateTime
import org.scalatest.{Matchers, WordSpecLike}
import org.slf4j.LoggerFactory
import ru.yandex.common.monitoring.healthchecks.{beUnhealthy, beWarning}
import ru.yandex.common.monitoring.{LoggedHealthCheckRegistry, _}

/**
 * @author @logab
 */
class SuppressNightCriticalSpec
    extends WordSpecLike with Matchers {
  def timeWithHour(hourOfDay: Int) = {
    DateTime.now.withTimeAtStartOfDay().plusHours(hourOfDay)
  }

  val startNight: Int = 21

  val endNight: Int = 9

  trait HealthCheckComponent extends SuppressNightCritical {
    override def startNightHour: Int = startNight

    override def endNightHour: Int = endNight
  }

  val log = LoggerFactory
      .getLogger(classOf[SuppressNightCriticalSpec])

  "no critical at night healthcheck" should {
    "light warning from when it's night" in {
      (0 to 23) foreach {
        hour =>
          val hc = new WarningHealthCheck
            with SuppressNightCriticalHealthCheck
            with HealthCheckComponent {
            override def check(): Result =
              WarningHealthCheck.unhealthy("always error")

            override def now: DateTime = timeWithHour(hour)
          }
          val hcr = new LoggedHealthCheckRegistry()
          hcr.register("warning-at-night", hc)
          if (hour < endNight || hour >= startNight)
            hcr.runHealthCheck("warning-at-night") should beWarning
          else
            hcr.runHealthCheck("warning-at-night") should beUnhealthy
      }

    }
  }

}