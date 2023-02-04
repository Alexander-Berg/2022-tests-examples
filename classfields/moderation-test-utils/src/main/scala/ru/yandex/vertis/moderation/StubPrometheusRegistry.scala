package ru.yandex.vertis.moderation

import io.prometheus.client.{Collector, CollectorRegistry}
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry

import java.util.function.Predicate

/**
  * @author mpoplavkov
  */
object StubPrometheusRegistry extends PrometheusRegistry {
  override def register[C <: Collector](c: C): C = c

  override def asCollectorRegistry(): CollectorRegistry = new CollectorRegistry()

  override def unregister(predicate: Predicate[Collector]): Unit = ()
}
