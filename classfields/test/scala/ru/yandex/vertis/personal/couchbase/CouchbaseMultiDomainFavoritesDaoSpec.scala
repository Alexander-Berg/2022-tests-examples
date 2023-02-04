package ru.yandex.vertis.personal.couchbase

import org.scalatest.Ignore
import ru.yandex.vertis.personal.PropertyDaoSpecBase
import ru.yandex.vertis.personal.favorites.couchbase.FavoriteItemConverter
import ru.yandex.vertis.personal.generators.Producer
import ru.yandex.vertis.personal.model.ModelGenerators.MultiDomainFavoritesCollectionGen
import ru.yandex.vertis.personal.model.UserRef
import ru.yandex.vertis.personal.model.favorites.MultiDomainFavoritesCollection

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

/**
  * Runnable specs on [[MultiDomainFavoritesCollection]] backed with Couchbase.
  *
  * @author dimas
  */
@Ignore
class CouchbaseMultiDomainFavoritesDaoSpec extends PropertyDaoSpecBase[MultiDomainFavoritesCollection] {

  lazy val dao = new CouchbaseMultiDomainCollectionDao(
    CouchbaseBucketWrapperWithTtl(
      CouchbaseBucketWrapperImpl(CouchbaseTesting.TestBucket),
      3.minutes
    ),
    FavoriteItemConverter,
    DocumentPrefixes.Favorites,
    ttlProvider = None,
    new MultiDomainCollectionLayout(
      FavoriteItemConverter,
      MultiDomainFavoritesCollection.apply,
      DocumentPrefixes.Favorites
    )
  )(ExecutionContext.global)

  def nextProperty(user: UserRef): MultiDomainFavoritesCollection =
    MultiDomainFavoritesCollectionGen.next.copy(user = user)

  def emptyProperty(user: UserRef): MultiDomainFavoritesCollection =
    MultiDomainFavoritesCollection.empty(user)
}
