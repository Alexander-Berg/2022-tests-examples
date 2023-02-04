package ru.yandex.vertis.general.favorites.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.favorites.notification_model.NotificationSettings
import general.search.api.SearchOffersRequest
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.favorites.model.searches.{
  SavedSearch,
  SavedSearchInvertedKey,
  SavedSearchInvertedListItem,
  ShardCount
}
import ru.yandex.vertis.general.favorites.storage.SavedSearchInvertedDao
import ru.yandex.vertis.general.favorites.storage.ydb.inverted.YdbSavedSearchInvertedDao
import zio.UIO
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.TestAspect.{before, sequential, shrinks}
import zio.test.{assert, checkNM, suite, testM, DefaultRunnableSpec, ZSpec}

object YdbSavedSearchInvertedDaoTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("YdbSavedSearchInvertedDaoTest")(
      testM("create SavedSearch") {
        checkNM(1)(OwnerIdGen.anyUserId) { ownerId =>
          val shardId = 0
          val searches = List(
            SavedSearch(searchId = "id", SearchOffersRequest(), NotificationSettings())
          ).toSet
          for {
            now <- zio.clock.instant
            _ <- runTx(
              SavedSearchInvertedDao.upsert(
                searches
                  .map(search =>
                    SavedSearchInvertedListItem.fromSavedSearch(
                      ownerId = ownerId,
                      savedSearch = search,
                      lastSeenAt = now.getEpochSecond,
                      lastSentAt = now.getEpochSecond
                    )
                  )
                  .toList
              )
            )
            savedSearches <- runTx(SavedSearchInvertedDao.listing(shardId, limit = 10, lastKey = None))
            size <- SavedSearchInvertedDao.size
            _ <- runTx(
              SavedSearchInvertedDao.delete(ownerId, searches.map(_.searchId))
            )
            savedSearches2 <- runTx(SavedSearchInvertedDao.listing(shardId, limit = 10, lastKey = None))
          } yield assert(savedSearches.map(_.savedSearchInverted.searchId).toSet)(equalTo(searches.map(_.searchId))) &&
            assert(size)(equalTo(1)) &&
            assert(savedSearches2)(equalTo(List.empty))
        }
      },
      testM("list SavedSearches") {
        checkNM(1)(OwnerIdGen.anyUserId, OwnerIdGen.anyUserId) { (ownerId1, ownerId2) =>
          val shardId = 0
          val search1 =
            SavedSearch(searchId = "id1", SearchOffersRequest(), NotificationSettings())
          val search2 =
            SavedSearch(searchId = "id2", SearchOffersRequest(), NotificationSettings())
          val searches = List(search1, search2)
          for {
            now <- zio.clock.instant
            _ <- runTx(
              SavedSearchInvertedDao.upsert(
                searches.map(search =>
                  SavedSearchInvertedListItem.fromSavedSearch(
                    ownerId = ownerId1,
                    savedSearch = search,
                    lastSeenAt = now.getEpochSecond,
                    lastSentAt = now.getEpochSecond
                  )
                )
              )
            )
            _ <- runTx(
              SavedSearchInvertedDao.upsert(
                searches.map(search =>
                  SavedSearchInvertedListItem.fromSavedSearch(
                    ownerId = ownerId2,
                    savedSearch = search,
                    lastSeenAt = now.getEpochSecond,
                    lastSentAt = now.getEpochSecond
                  )
                )
              )
            )
            savedSearches <- runTx(SavedSearchInvertedDao.listing(shardId, limit = 10, lastKey = None))
            savedSearches2 <- runTx(
              SavedSearchInvertedDao.listing(
                shardId,
                limit = 10,
                lastKey = savedSearches
                  .map(ss =>
                    SavedSearchInvertedKey(shardId = shardId, searchId = ss.savedSearchInverted.searchId, ss.ownerId)
                  )
                  .headOption
              )
            )
          } yield assert(savedSearches.map(_.savedSearchInverted.searchId))(
            equalTo(List(search1.searchId, search1.searchId, search2.searchId, search2.searchId))
          ) &&
            assert(savedSearches.map(_.ownerId))(
              equalTo(List(ownerId1, ownerId2, ownerId1, ownerId2))
            ) &&
            assert(savedSearches2.map(_.savedSearchInverted.searchId))(
              equalTo(List(search1.searchId, search2.searchId, search2.searchId))
            )
        }
      },
      testM("get batch SavedSearch") {
        checkNM(1)(OwnerIdGen.anyUserId) { ownerId =>
          val searches = List(
            SavedSearch(searchId = "id", SearchOffersRequest(), NotificationSettings())
          ).toSet
          for {
            now <- zio.clock.instant
            _ <- runTx(
              SavedSearchInvertedDao.upsert(
                searches
                  .map(search =>
                    SavedSearchInvertedListItem.fromSavedSearch(
                      ownerId = ownerId,
                      savedSearch = search,
                      lastSeenAt = now.getEpochSecond,
                      lastSentAt = now.getEpochSecond
                    )
                  )
                  .toList
              )
            )
            savedSearches <- runTx(
              SavedSearchInvertedDao.getBatch(ownerId, searches.map(_.searchId))
            )
            _ <- runTx(
              SavedSearchInvertedDao.delete(ownerId, searches.map(_.searchId))
            )
            savedSearches2 <- runTx(
              SavedSearchInvertedDao.getBatch(ownerId, searches.map(_.searchId))
            )

          } yield assert(savedSearches.map(_.savedSearchInverted.searchId).toSet)(equalTo(searches.map(_.searchId))) &&
            assert(savedSearches2)(equalTo(List.empty))
        }
      }
    ) @@ before(runTx(YdbSavedSearchInvertedDao.clean)) @@ sequential @@ shrinks(0)
  }.provideCustomLayerShared {
    val shardCount = UIO(ShardCount(1)).toLayer
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val dao = (shardCount ++ txRunner) >+> YdbSavedSearchInvertedDao.live
    dao ++ Clock.live
  }
}
