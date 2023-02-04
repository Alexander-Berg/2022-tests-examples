package ru.yandex.vertis.billing.service.metered

import com.codahale.metrics.MetricRegistry
import ru.yandex.vertis.billing.OpsSpecBase
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry

trait MeteredStub extends Metered {
  override def serviceName: String = "test"

  override def metricRegistry: MetricRegistry = OpsSpecBase.codahaleRegistry

  override def clazz: Class[_] = classOf[MeteredStub]

  override def prometheusRegistry: PrometheusRegistry = OpsSpecBase.prometheusRegistry
}
