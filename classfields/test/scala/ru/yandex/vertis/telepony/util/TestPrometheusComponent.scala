package ru.yandex.vertis.telepony.util

import ru.yandex.vertis.ops.prometheus.{PrometheusRegistry, SimpleCompositeCollector}
import ru.yandex.vertis.telepony.component.PrometheusComponent

/**
  * @author evans
  */
trait TestPrometheusComponent extends PrometheusComponent {

  def prometheusRegistry: PrometheusRegistry = new SimpleCompositeCollector

}
