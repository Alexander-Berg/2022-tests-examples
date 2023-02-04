package ru.yandex.common.monitoring.healthcheck

import java.lang.Thread.sleep
import java.util.concurrent.Executors.newScheduledThreadPool
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

import com.codahale.metrics.health.HealthCheck
import com.codahale.metrics.health.HealthCheck.Result
import com.codahale.metrics.health.HealthCheck.Result.{healthy, unhealthy}
import org.scalatest.{BeforeAndAfter, Ignore, Matchers, WordSpecLike}
import ru.yandex.common.monitoring.healthcheck.AsyncCachedHealthCheck.DaemonNamedThreadFactory
import ru.yandex.common.monitoring.healthcheck.AsyncCachedHealthCheckSpec._
import ru.yandex.common.monitoring.healthchecks.{beHealthy, beUnhealthy, containMessage}

import scala.concurrent.duration._
import scala.language.reflectiveCalls

/**
  * @author korvit
  */
@Ignore
class AsyncCachedHealthCheckSpec
  extends WordSpecLike with Matchers with BeforeAndAfter {

  def fixture = new {
    val executor: ScheduledExecutorService = newScheduledThreadPool(1, DaemonNamedThreadFactory)
  }

  "async healthcheck" should {
    "always return `NoResult` if healthcheck doesn't work" in {
      val f = fixture
      try {
        val healthCheck = AsyncCachedHealthCheck(
          healthCheck = healthCheckFromStates(Set(
            HealthyState(1 until 10, 20.seconds))),
          initialDelay = DefaultHealthCheckFrequency,
          checkFrequency = DefaultHealthCheckFrequency,
          guideLine = TestHealthCheckName,
          executor = f.executor)

        mustBeNoResult(healthCheck)
        sleep((DefaultHealthCheckFrequency * 1.5).toMillis)
        for (_ <- 1 to 9) {
          val result = healthCheck.execute()
          result should (beUnhealthy and containMessage(NoResultMessage))
          sleep((DefaultHealthCheckFrequency / 2).toMillis)
        }
      } finally f.executor.shutdown()
    }

    "always return actual result if healthcheck work correctly and always healthy" in {
      val f = fixture
      try {
        val healthCheck = AsyncCachedHealthCheck(
          healthCheck = healthCheckFromStates(Set(
            HealthyState(1 until 10))),
          initialDelay = DefaultHealthCheckFrequency,
          checkFrequency = DefaultHealthCheckFrequency,
          guideLine = TestHealthCheckName,
          executor = f.executor)

        mustBeNoResult(healthCheck)
        sleep((DefaultHealthCheckFrequency * 1.5).toMillis)
        for (i <- 1 to 9) {
          val result = healthCheck.execute()
          result should (beHealthy and containMessage(i.toString))
          sleep(DefaultHealthCheckFrequency.toMillis)
        }
      } finally f.executor.shutdown()
    }

    "always return actual result if healthcheck work correctly with alternate result" in {
      val f = fixture
      try {
        val healthCheck = AsyncCachedHealthCheck(
          healthCheck = healthCheckFromStates(Set(
            HealthyState(1 until 2),
            UnhealthyState(2 until 3),
            HealthyState(3 until 4),
            UnhealthyState(4 until 5),
            HealthyState(5 until 6))),
          initialDelay = DefaultHealthCheckFrequency,
          checkFrequency = DefaultHealthCheckFrequency,
          guideLine = TestHealthCheckName,
          executor = f.executor)

        mustBeNoResult(healthCheck)
        sleep((DefaultHealthCheckFrequency * 1.5).toMillis)
        for (i <- 1 to 5) {
          val result = healthCheck.execute()
          if (i % 2 == 1) {
            result should beHealthy
          } else {
            result should beUnhealthy
          }
          result should containMessage(i.toString)
          sleep(DefaultHealthCheckFrequency.toMillis)
        }
      } finally f.executor.shutdown()
    }

    "correctly restore after result expiration" in {
      val f = fixture
      try {
        val healthCheck: HealthCheck = new AsyncCachedHealthCheck(
          healthCheck = healthCheckFromStates(Set(
            HealthyState(1 until 2),
            HealthyState(2 until 3, DefaultHealthCheckFrequency * 4),
            HealthyState(3 until 4),
            HealthyState(4 until 5),
            HealthyState(5 until 6),
            HealthyState(6 until 7),
            HealthyState(7 until 8),
            HealthyState(8 until 9))),
          ttlFactor = 3,
          initialDelay = DefaultHealthCheckFrequency,
          checkFrequency = DefaultHealthCheckFrequency,
          guideLine = TestHealthCheckName,
          executor = f.executor)

        mustBeNoResult(healthCheck)
        sleep((DefaultHealthCheckFrequency * 1.5).toMillis)
        for (i <- 1 to 12) {
          val result = healthCheck.execute()
          if (i <= 3) {
            result should (beHealthy and containMessage("1"))
          } else if (i <= 5) {
            result should beUnhealthy
            result.getMessage should (include(expiredMessagePrefix(1)) and include(expiredMessageSuffix))
          } else {
            result should (beHealthy and containMessage((i - 4).toString))
          }
          sleep(DefaultHealthCheckFrequency.toMillis)
        }
      } finally f.executor.shutdown()
    }

    "correctly handle exception" in {
      val f = fixture
      try {
        val expectedErrorMessage = "expected error"
        val healthCheck = AsyncCachedHealthCheck(
          healthCheck = healthCheckFromStates(Set(
            HealthyState(1 until 2),
            FailedState(2 until 3, error = new HealthCheckError(expectedErrorMessage)),
            HealthyState(3 until 4),
            FailedState(4 until 5, error = new HealthCheckError(expectedErrorMessage)),
            HealthyState(5 until 6))),
          initialDelay = DefaultHealthCheckFrequency,
          checkFrequency = DefaultHealthCheckFrequency,
          guideLine = TestHealthCheckName,
          executor = f.executor)

        mustBeNoResult(healthCheck)
        sleep((DefaultHealthCheckFrequency * 1.5).toMillis)
        for (i <- 1 to 5) {
          val result = healthCheck.execute()
          if (i % 2 == 1) {
            result should (beHealthy and containMessage(i.toString))
          } else {
            result should beUnhealthy
            result.getError.getClass shouldEqual classOf[HealthCheckError]
            result.getError.getMessage shouldEqual expectedErrorMessage
          }
          sleep(DefaultHealthCheckFrequency.toMillis)
        }
      } finally f.executor.shutdown()
    }
  }

  private def mustBeNoResult(healthCheck: HealthCheck) = {
    val result = healthCheck.execute()
    result.isHealthy shouldEqual false
    result.getMessage shouldEqual NoResultMessage
  }
}

object AsyncCachedHealthCheckSpec {
  val TestHealthCheckName = "test healthcheck"
  val NoResultMessage = "no result"

  val DefaultHealthCheckFrequency: FiniteDuration = 1.second

  def expiredMessagePrefix(lastCallNumber: Int) =
    s"result expired. previous result is Result{isHealthy=true, message=$lastCallNumber"

  val expiredMessageSuffix = "}, expired at"

  private trait HealthCheckState {

    def callInterval: Range

    def sleepTime: FiniteDuration

    def result: Int => HealthCheck.Result
  }

  private case class HealthyState(callInterval: Range,
                                  sleepTime: FiniteDuration = Duration.Zero)
    extends HealthCheckState {

    override def result: (Int) => Result = callNumber => healthy(callNumber.toString)
  }

  private case class UnhealthyState(callInterval: Range,
                                    sleepTime: FiniteDuration = Duration.Zero)
    extends HealthCheckState {

    override def result: (Int) => Result = callNumber => unhealthy(callNumber.toString)

  }

  private case class FailedState(callInterval: Range,
                                 sleepTime: FiniteDuration = Duration.Zero,
                                 error: Throwable)
    extends HealthCheckState {

    override def result: (Int) => Result = _ => throw error
  }

  private class HealthCheckError(message: String) extends Throwable(message)

  private def healthCheckFromStates(states: Set[HealthCheckState]): () => HealthCheck.Result = {
    require(states.nonEmpty, "`states` must be non empty")
    require(
      states forall (!_.callInterval.isInclusive),
      "all `state.callInterval` must be exclusive")
    require(
      isContinuousWithoutOverlaps(states map (_.callInterval)),
      "`states` must form continuous interval without overlaps")

    val callCounter = new AtomicInteger(0)
    () => {
      val callNumber = callCounter.incrementAndGet()
      val state = states
        .find(_.callInterval.contains(callNumber))
        .getOrElse(throw new IllegalArgumentException(s"illegal `callNumber`: $callNumber"))

      sleep(state.sleepTime.toMillis)
      state.result(callNumber)
    }
  }

  private def isContinuousWithoutOverlaps(ranges: Set[Range]): Boolean =
    if (ranges.size < 2) true
    else {
      val sorted = ranges.toSeq sortBy (_.start)
      (sorted dropRight 1) zip sorted.tail forall { case (r1, r2) => r1.end == r2.start }
    }
}
