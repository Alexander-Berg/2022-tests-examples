package ru.yandex.common.stats

import com.codahale.metrics.MetricRegistry
import org.scalatest.{Matchers, WordSpecLike}

import scala.util.Success

/** Specs on utils from [[InstrumentedMailbox]]
  */
class InstrumentedMailboxSpec
  extends Matchers
  with WordSpecLike {

  "findMetricsRegistry" should {
    "find registry by fqcn of MetricRegistry object" in {
      InstrumentedMailbox.tryMetricRegistry(
        "ru.yandex.common.stats.MetricRegistryInstance"
      ) should be(Success(MetricRegistryInstance))
    }
    "find registry by fqcn of MetricRegistryProvider object 1" in {
      InstrumentedMailbox.tryMetricRegistry(
        "ru.yandex.common.stats.MetricRegistryProviderInstance1"
      ) should be(Success(MetricRegistryInstance))
    }
    "find registry by fqcn of MetricRegistryProvider object 2" in {
      InstrumentedMailbox.tryMetricRegistry(
        "ru.yandex.common.stats.MetricRegistryProviderInstance2"
      ) should be(Success(MetricRegistryInstance))
    }
    "not find registry by fqcn of MetricRegistryProvider" in {
      val result = InstrumentedMailbox.tryMetricRegistry(
        "ru.yandex.vertis.subscriptions.util.akka.NotMetricsRegistry"
      )
      assert(result.isFailure)
    }
  }
}

object MetricRegistryInstance extends MetricRegistry

object MetricRegistryProviderInstance1 extends MetricRegistryProvider {
  def metrics = MetricRegistryInstance
}

object MetricRegistryProviderInstance2 extends MetricRegistryProvider {
  val metrics = MetricRegistryInstance
}

