package ru.yandex.vertis.ops.test

import com.codahale.metrics.MetricRegistry
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.{CompositeCollector, PrometheusRegistry}

/**
  * Provides local registries for test
  *
  * @author Ilya Gerasimov (747mmHg@yandex-team.ru)
  */
trait TestOperationalSupport
  extends OperationalSupport {

  final val healthChecks: CompoundHealthCheckRegistry =
    new CompoundHealthCheckRegistry()

  final implicit val prometheusRegistry: PrometheusRegistry =
    new CompositeCollector()

  final implicit val codahaleRegistry: CodahaleRegistry =
    new MetricRegistry()
}

object TestOperationalSupport extends TestOperationalSupport