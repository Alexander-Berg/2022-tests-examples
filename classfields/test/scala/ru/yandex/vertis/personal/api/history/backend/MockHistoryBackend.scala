package ru.yandex.vertis.personal.api.history.backend

import ru.yandex.vertis.personal.JvmPropertyDao
import ru.yandex.vertis.personal.history.CollectionHistoryBackend
import ru.yandex.vertis.personal.model.history.MultiDomainHistoryCollection
import ru.yandex.vertis.personal.model.{Domain, Service}

import scala.concurrent.ExecutionContext

class MockHistoryBackend(service: Service, domain: Domain)
  extends CollectionHistoryBackend(
    service,
    Set(domain),
    new JvmPropertyDao(MultiDomainHistoryCollection.empty),
    maxHistoryItems = 50
  )(ExecutionContext.global)
