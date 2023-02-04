package ru.auto.api

import java.util.function.Predicate
import io.prometheus.client.{Collector, CollectorRegistry}
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry

trait DummyOperationalSupport {

  implicit val metricsSupport: MetricsSupport = new MetricsSupport {
    implicit override def codahaleRegistry: CodahaleRegistry = null

    implicit override def prometheusRegistry: PrometheusRegistry = prometheusRegistryDummy
  }

  implicit val prometheusRegistryDummy: PrometheusRegistry = new PrometheusRegistry {
    override def register[C <: Collector](c: C): C = c

    override def asCollectorRegistry(): CollectorRegistry = new CollectorRegistry()

    override def unregister(p: Predicate[Collector]): Unit = ()
  }
}
