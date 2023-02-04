package ru.yandex.common.tokenization.instrumented

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.codahale.metrics.health.HealthCheckRegistry
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.common.ZooKeeperAware
import ru.yandex.common.monitoring.WarningHealthCheck
import ru.yandex.common.tokenization.TokensDistributor.Config
import ru.yandex.common.tokenization.{BlockingTokensFilter, IntTokens}

import scala.concurrent.duration._

/**
 * Specs on [[ru.yandex.common.tokenization.instrumented.MonitoredTokensFilter]]
 */
class MonitoredTokensFilterIntSpec
  extends TestKit(ActorSystem("unit-test"))
  with Matchers
  with WordSpecLike
  with ZooKeeperAware {

  val tokens = new IntTokens(32)

  val curator = curatorBase.usingNamespace("monitored-tokens-filter-spec")

  val distributionConfig = Config(
    distributePeriod = 100.milliseconds,
    redistributionStateTimeout = 300.milliseconds,
    nearlyDistributedTimeout = 300.milliseconds
  )

  val registry = new HealthCheckRegistry

  def blockedFilter(name: String, id: String, warning: Boolean) =
    new BlockingTokensFilter(
      name,
      id,
      tokens,
      curator.usingNamespace(s"monitored-tokens-filter"),
      distributionConfig
    )(system)
      with MonitoredTokensFilter {
      override def healthChecks = registry
      override def isWarning = warning
    }

  override protected def afterAll() = {
    shutdown(system)
    super.afterAll()
  }

  "BlockedTokensFilter" should {
    "produce warning check result" in {
      val filter = blockedFilter("foo", "1", warning = true)
      WarningHealthCheck.isWarning(registry.runHealthCheck("tokens-filter-foo-1")) should be(true)
      filter.distribution.getTokens
      WarningHealthCheck.isHealthy(registry.runHealthCheck("tokens-filter-foo-1")) should be(true)
    }
    "produce unhealthy check result" in {
      val filter = blockedFilter("foo", "2", warning = false)
      WarningHealthCheck.isUnhealthy(registry.runHealthCheck("tokens-filter-foo-2")) should be(true)
      filter.distribution.getTokens
      WarningHealthCheck.isHealthy(registry.runHealthCheck("tokens-filter-foo-2")) should be(true)
    }
  }
}
