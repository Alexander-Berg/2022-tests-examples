package ru.yandex.vertis.billing

import com.codahale.metrics.MetricRegistry
import ru.yandex.common.monitoring.HealthChecks
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.{CompositeCollector, PrometheusRegistry}

trait OpsSpecBase extends OperationalSupport {
  override val healthChecks = HealthChecks.compoundRegistry()

  implicit override val codahaleRegistry: CodahaleRegistry = new MetricRegistry

  implicit override val prometheusRegistry: PrometheusRegistry = new CompositeCollector
}

object OpsSpecBase extends OpsSpecBase
