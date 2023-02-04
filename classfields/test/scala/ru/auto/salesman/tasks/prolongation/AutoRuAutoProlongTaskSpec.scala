package ru.auto.salesman.tasks.prolongation

import ru.auto.salesman.model.DeprecatedDomain
import ru.auto.salesman.model.DeprecatedDomains.AutoRu

class AutoRuAutoProlongTaskSpec extends AutoProlongTaskSpec {
  implicit override def domain: DeprecatedDomain = AutoRu
}
