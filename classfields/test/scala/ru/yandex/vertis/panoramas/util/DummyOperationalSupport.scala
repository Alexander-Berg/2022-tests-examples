package ru.yandex.vertis.panoramas.util

import java.util.function.Predicate

import io.prometheus.client.{Collector, CollectorRegistry}
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry

trait DummyOperationalSupport {

  implicit val prometheusRegistry: PrometheusRegistry = new PrometheusRegistry {
    override def register[C <: Collector](c: C): C = c

    override def asCollectorRegistry(): CollectorRegistry = new CollectorRegistry()

    override def unregister(p: Predicate[Collector]): Unit = ()
  }
}
