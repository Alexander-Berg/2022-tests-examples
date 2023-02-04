package ru.yandex.vertis.statistics.transformer.util

import ru.yandex.vertis.ops.prometheus.{Prometheus, PrometheusRegistry}

/**
  *
  * @author zvez
  */
trait TestPrometheus {
  implicit val prometheusRegistry: PrometheusRegistry = Prometheus.defaultRegistry()
}
