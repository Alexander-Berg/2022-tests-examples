package ru.yandex.vertis.chat.service.impl

import ru.yandex.vertis.chat.components.domains.DomainAware
import ru.yandex.vertis.chat.{Domain, Domains}

trait TestDomainAware extends DomainAware {

  override def domain: Domain = Domains.Auto
}
