package ru.yandex.vertis.personal.api.settings

import ru.yandex.vertis.personal.{JvmPropertyDao, ServiceRegistry}
import ru.yandex.vertis.personal.generic.{GenericCollectionBackend, GenericCollectionBackendImpl}
import ru.yandex.vertis.personal.model.{Domains, Services, SingleRef}
import ru.yandex.vertis.personal.model.generic.GenericMultiDomainCollection
import ru.yandex.vertis.personal.model.settings.SettingsPayload
import ru.yandex.vertis.personal.util.HandlerSpec

trait BaseSettingsVersionedHandlerSpec extends HandlerSpec {

  val version = "1.0"
  val service = Services.Realty
  val domain = Domains.Settings
  val settingsDomain = SingleRef(service, domain)

  val registry = new ServiceRegistry[GenericCollectionBackend[SettingsPayload]]

  val f = (a: Option[SettingsPayload], b: Option[SettingsPayload]) => b.orElse(a)

  val propertyDao =
    new JvmPropertyDao[GenericMultiDomainCollection[SettingsPayload]](
      GenericMultiDomainCollection.empty[SettingsPayload]
    )

  val settingsBackend = new GenericCollectionBackendImpl[SettingsPayload](
    service,
    Set(domain),
    f,
    propertyDao
  )

  registry.register(settingsBackend)

}
