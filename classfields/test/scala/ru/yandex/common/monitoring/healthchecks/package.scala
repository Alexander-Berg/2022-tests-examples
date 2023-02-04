package ru.yandex.common.monitoring

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result
import org.scalatest.matchers.{MatchResult, Matcher}

/**
 * Stuff for testing
 *
 * @author ruslansd
 */
package object healthchecks {

  object HealthyCheck extends HealthCheck {
    val name = "healthy"
    def check(): Result = Result.healthy()
  }

  object UnhealthyCheck extends HealthCheck {
    val name = "unhealthy"
    def check(): Result = Result.unhealthy("fail")
  }

  object WarningCheck extends HealthCheck {
    val name = "warning"
    def check(): Result = WarningHealthCheck.warning("bla")
  }

  val beHealthy: Matcher[HealthCheck.Result] =
    new Matcher[Result] {
      override def apply(left: Result): MatchResult =
        MatchResult(
          WarningHealthCheck.isHealthy(left),
          s"Expected healthy check result, but got [$left]",
          "CheckResult is healthy"
        )
    }

  val beUnhealthy: Matcher[HealthCheck.Result] =
    new Matcher[Result] {
      override def apply(left: Result): MatchResult =
        MatchResult(
          WarningHealthCheck.isUnhealthy(left),
          s"Expected unhealthy check result, but got [$left]",
          "CheckResult is unhealthy"
        )
    }

  val beWarning: Matcher[HealthCheck.Result] =
    new Matcher[Result] {
      override def apply(left: Result): MatchResult =
        MatchResult(
          WarningHealthCheck.isWarning(left),
          s"Expected warning check result, but got [$left]",
          "CheckResult is warning"
        )
    }

  def containMessage(msg: String): Matcher[HealthCheck.Result] =
    new Matcher[Result] {
      override def apply(left: Result): MatchResult =
        MatchResult(
          left.getMessage.contains(msg),
          s"Expected health check result with message [$msg], but got [${left.getMessage}]",
          s"Message matches $msg"
        )
    }

}
