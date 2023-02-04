package ru.yandex.vertis.general.favorites.logic.test

import common.jwt.Jwt
import common.jwt.Jwt.HMACConfig
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.favorites.notification_model.{NotificationChannel, NotificationSettings}
import ru.yandex.vertis.general.common.model.pagination.{LimitOffset, Order}
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.favorites.api.mappers.SearchIdMapper
import ru.yandex.vertis.general.favorites.logic.{FavoritesManager, FavoritesStore, SavedSearchManager}
import ru.yandex.vertis.general.favorites.model.searches.{SavedSearch, ShardCount}
import ru.yandex.vertis.general.favorites.model.{FavoriteType, UnsubscribeSearchPayload}
import ru.yandex.vertis.general.favorites.storage.ydb.counts.YdbFavoritesCountDao
import ru.yandex.vertis.general.favorites.storage.ydb.favorites.YdbFavoritesDao
import ru.yandex.vertis.general.favorites.storage.ydb.inverted.YdbSavedSearchInvertedDao
import ru.yandex.vertis.general.search.model.testkit.SearchOffersRequestGen
import zio.clock.Clock
import zio.test.Assertion.{contains, equalTo, forall, not}
import zio.test.TestAspect.{before, sequential, shrinks}
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec}
import zio.{UIO, ZIO, ZLayer}

import scala.concurrent.duration._

object SavedSearchesManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SavedSearchesManagerTest")(
      testM("save & get & delete SavedSearches") {
        checkNM(1)(SearchOffersRequestGen.anySearchOffersRequests(15), OwnerIdGen.anyUserId) {
          (searchRequests, ownerId) =>
            for {
              savedSearches <- ZIO.foreach(searchRequests)(request =>
                SearchIdMapper
                  .toSearchId(request)
                  .map(id =>
                    SavedSearch(searchId = id, searchRequest = request, notificationSettings = NotificationSettings())
                  )
              )
              _ <- SavedSearchManager.createOrUpdateSavedSearches(userId = ownerId, savedSearches)
              storesSearches <- SavedSearchManager.getSavedSearches(ownerId, savedSearches.map(_.searchId).toSet)
              _ <- SavedSearchManager.deleteSavedSearches(ownerId, savedSearches.map(_.searchId).toSet)
              storesSearches2 <- SavedSearchManager.getSavedSearches(ownerId, savedSearches.map(_.searchId).toSet)
            } yield assert(savedSearches.toSet)(equalTo(storesSearches.toSet)) &&
              assert(storesSearches2)(equalTo(List.empty))
        }
      },
      testM("listing SavedSearches") {
        checkNM(1)(SearchOffersRequestGen.anySearchOffersRequests(15), OwnerIdGen.anyUserId) {
          (searchRequests, ownerId) =>
            for {
              savedSearches <- ZIO.foreach(searchRequests)(request =>
                SearchIdMapper
                  .toSearchId(request)
                  .map(id =>
                    SavedSearch(searchId = id, searchRequest = request, notificationSettings = NotificationSettings())
                  )
              )
              _ <- SavedSearchManager.createOrUpdateSavedSearches(userId = ownerId, savedSearches)
              page1 <- SavedSearchManager.getSavedSearchesListing(
                ownerId,
                LimitOffset(limit = 10, offset = 0),
                order = Order.Desc
              )
              page2 <- SavedSearchManager.getSavedSearchesListing(
                ownerId,
                LimitOffset(limit = 10, offset = 10),
                order = Order.Desc
              )
            } yield assert(page1.size)(equalTo(10)) &&
              assert(page2.size)(equalTo(5))
        }
      },
      testM("SavedSearches count") {
        checkNM(1)(SearchOffersRequestGen.anySearchOffersRequests(15), OwnerIdGen.anyUserId) {
          (searchRequests, ownerId) =>
            for {
              savedSearches <- ZIO.foreach(searchRequests)(request =>
                SearchIdMapper
                  .toSearchId(request)
                  .map(id =>
                    SavedSearch(searchId = id, searchRequest = request, notificationSettings = NotificationSettings())
                  )
              )
              _ <- SavedSearchManager.createOrUpdateSavedSearches(userId = ownerId, savedSearches)
              counts <- FavoritesManager.getFavoritesCount(ownerId)
              _ <- SavedSearchManager.deleteSavedSearches(ownerId, savedSearches.take(10).map(_.searchId).toSet)
              counts2 <- FavoritesManager.getFavoritesCount(ownerId)
            } yield assert(counts(FavoriteType.SavedSearch))(equalTo(savedSearches.size)) &&
              assert(counts2(FavoriteType.SavedSearch))(equalTo(savedSearches.size - 10))
        }
      },
      testM("Unsubscribe") {
        checkNM(1)(SearchOffersRequestGen.anySearchOffersRequests(100), OwnerIdGen.anyUserId) {
          (searchRequests, ownerId) =>
            for {
              jwt <- ZIO.service[Jwt.SignService]
              savedSearches <- ZIO.foreach(searchRequests)(request =>
                SearchIdMapper
                  .toSearchId(request)
                  .map(id =>
                    SavedSearch(
                      searchId = id,
                      searchRequest = request,
                      notificationSettings =
                        NotificationSettings(List(NotificationChannel.EMAIL, NotificationChannel.PUSH))
                    )
                  )
              )
              first <- ZIO.fromOption(savedSearches.headOption)
              _ <- SavedSearchManager.createOrUpdateSavedSearches(ownerId, savedSearches)
              firstToken <- jwt.sign(UnsubscribeSearchPayload.One(ownerId, first.searchId), None)
              _ <- SavedSearchManager.unsubscribe(firstToken)
              first <- SavedSearchManager
                .getSavedSearches(ownerId, Set(first.searchId))
                .flatMap(found => ZIO.fromOption(found.headOption))
              allToken <- jwt.sign(UnsubscribeSearchPayload.All(ownerId), None)
              _ <- SavedSearchManager.unsubscribe(allToken)
              all <- SavedSearchManager.getSavedSearches(ownerId, savedSearches.tail.map(_.searchId).toSet)
            } yield assert(first.notificationSettings.notificationChannels)(
              not(contains(NotificationChannel.EMAIL))
            ) &&
              assert(all.map(_.notificationSettings.notificationChannels))(
                forall(not(contains(NotificationChannel.EMAIL)))
              )
        }
      }
    ) @@ before(
      runTx(YdbFavoritesDao.clean) *> runTx(YdbFavoritesCountDao.clean) *> runTx(YdbSavedSearchInvertedDao.clean)
    ) @@ sequential @@ shrinks(0)
  }.provideCustomLayerShared {
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val clock = Clock.live
    val favoritesDao = ydb >+> YdbFavoritesDao.live
    val favoritesCountDao = ydb >+> YdbFavoritesCountDao.live
    val favoritesStore = (favoritesDao ++ favoritesCountDao) >+> FavoritesStore.live
    val favoritesManager = (favoritesStore ++ txRunner ++ clock) >+> FavoritesManager.live
    val shardCount = UIO(ShardCount(1)).toLayer
    val savedSearchInvertedDao = (ydb ++ shardCount) >+> YdbSavedSearchInvertedDao.live
    val jwt = ZLayer.succeed(HMACConfig("secret")) ++ ZLayer.succeed(Jwt.Config(1.day)) ++ clock >>> Jwt.HMAC256
    val savedSearchesManager =
      (favoritesStore ++ savedSearchInvertedDao ++ txRunner ++ clock ++ jwt) >+> SavedSearchManager.live
    favoritesStore ++ txRunner ++ clock ++ savedSearchesManager ++ favoritesManager
  }

}
