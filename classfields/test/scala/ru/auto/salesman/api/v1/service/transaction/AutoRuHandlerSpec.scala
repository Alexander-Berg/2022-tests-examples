package ru.auto.salesman.api.v1.service.transaction

import ru.auto.salesman.model.DeprecatedDomain
import ru.auto.salesman.model.DeprecatedDomains.AutoRu

class AutoRuHandlerSpec extends HandlerSpec {

  implicit override def domain: DeprecatedDomain = AutoRu
}
