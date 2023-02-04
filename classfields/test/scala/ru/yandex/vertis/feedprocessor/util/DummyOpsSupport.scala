package ru.yandex.vertis.feedprocessor.util

import io.prometheus.client.{Collector, CollectorRegistry}
import ru.yandex.common.monitoring.CompoundHealthCheckRegistry
import ru.yandex.vertis.feedprocessor.app.{Application, OpsSupport}
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry

import java.util.function.Predicate

/**
  * @author pnaydenov
  */
trait DummyOpsSupport extends OpsSupport { this: Application with Logging =>

  implicit override lazy val operationalSupport: OperationalSupport = new OperationalSupport {
    override def healthChecks: CompoundHealthCheckRegistry = new CompoundHealthCheckRegistry

    implicit override def codahaleRegistry: CodahaleRegistry = ???

    implicit override def prometheusRegistry: PrometheusRegistry =
      new PrometheusRegistry {
        override def asCollectorRegistry(): CollectorRegistry = ???

        override def register[C <: Collector](c: C): C = c

        override def unregister(predicate: Predicate[Collector]): Unit = ()
      }
  }
}
