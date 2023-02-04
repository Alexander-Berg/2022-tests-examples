package ru.yandex.vertis.personal.api.favorites.backend

import ru.yandex.vertis.personal.JvmPropertyDao
import ru.yandex.vertis.personal.favorites.FavoritesCollectionBackend
import ru.yandex.vertis.personal.model.favorites.MultiDomainFavoritesCollection
import ru.yandex.vertis.personal.model.{Domain, Service}

import scala.concurrent.ExecutionContext

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 15.06.16
  */
class MockFavoritesBackend(service: Service, domain: Domain)
  extends FavoritesCollectionBackend(
    service,
    Set(domain),
    new JvmPropertyDao(MultiDomainFavoritesCollection.empty)
  )(ExecutionContext.global)
