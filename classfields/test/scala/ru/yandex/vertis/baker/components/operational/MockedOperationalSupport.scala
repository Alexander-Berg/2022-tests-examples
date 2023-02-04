package ru.yandex.vertis.baker.components.operational

import io.prometheus.client.Collector
import ru.yandex.common.monitoring.{CompoundHealthCheckRegistry, HealthChecks, Metrics}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry

trait MockedOperationalSupport extends OperationalAware with MockitoSupport {

  implicit val operational: ops.OperationalSupport = new ops.OperationalSupport {
    override val healthChecks: CompoundHealthCheckRegistry = HealthChecks.compoundRegistry()
    implicit override val codahaleRegistry: CodahaleRegistry = Metrics.defaultRegistry()

    implicit override val prometheusRegistry: PrometheusRegistry = mock[PrometheusRegistry]
    stub(prometheusRegistry.register(_: Collector)) {
      case collector => collector
    }
  }
}
