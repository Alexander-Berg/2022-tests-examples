package ru.auto.salesman.service.impl

import ru.auto.salesman.service.DynamicConfigurationsFeatureService
import ru.auto.salesman.service.DynamicConfigurationsFeatureService._

object TestDynamicConfigurationsFeatureService
    extends DynamicConfigurationsFeatureService {

  def isConsumerEnabled(name: String): Boolean =
    defaultConsumersMap.getOrElse(name, true)
}
