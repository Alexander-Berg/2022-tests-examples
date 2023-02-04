package ru.yandex.vertis.personal.couchbase

import org.scalatest.Ignore
import ru.yandex.vertis.personal.favorites.couchbase.FavoriteItemConverter
import ru.yandex.vertis.personal.generators.Producer
import ru.yandex.vertis.personal.model.ModelGenerators.FavoritesCollectionGen
import ru.yandex.vertis.personal.model.UserRef
import ru.yandex.vertis.personal.model.favorites.FavoritesCollection
import ru.yandex.vertis.personal.{PropertyDao, PropertyDaoSpecBase}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/**
  * Runnable specs on [[FavoritesCollection]] backed with Couchbase.
  *
  * @author dimas
  */
@Ignore
class CouchbaseCollectionDaoSpec extends PropertyDaoSpecBase[FavoritesCollection] {

  lazy val dao: PropertyDao[FavoritesCollection] = new CouchbaseCollectionDao(
    CouchbaseBucketWrapperWithTtl(
      CouchbaseBucketWrapperImpl(CouchbaseTesting.TestBucket),
      3.minutes
    ),
    FavoriteItemConverter,
    FavoritesCollection.apply,
    DocumentPrefixes.Favorites,
    ttlProvider = None
  )(ExecutionContext.global)

  def emptyProperty(user: UserRef): FavoritesCollection =
    FavoritesCollection.empty(user)

  def nextProperty(user: UserRef): FavoritesCollection =
    FavoritesCollectionGen.next.copy(user = user)
}
