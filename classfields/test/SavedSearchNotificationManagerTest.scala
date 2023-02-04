package ru.yandex.vertis.general.favorites.logic.test

import common.jwt.Jwt
import common.jwt.Jwt.HMACConfig
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.bonsai.category_model.Category
import general.favorites.notification_model.{NotificationChannel, NotificationSettings}
import general.search.api.SearchOffersResponse
import general.search.model.RawValue.Value
import general.search.model.SearchFilter.Operation.{Equal, NotEqual}
import general.search.model.SearchSortEnum.SearchSort
import general.search.model.{SearchFilter, SearchSnippet, ShuffleEnum}
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.favorites.logic.SavedSearchNotificationManager
import ru.yandex.vertis.general.favorites.model.PushNotification
import ru.yandex.vertis.general.favorites.model.searches.{SavedSearchesSchedulerConfig, ShardCount}
import ru.yandex.vertis.general.favorites.model.testkit.SavedSearchInvertedListItemGen
import ru.yandex.vertis.general.favorites.storage.ydb.inverted.YdbSavedSearchInvertedDao
import ru.yandex.vertis.general.favorites.storage.ydb.notifications.{
  YdbFavoritesNotificationQueueDao,
  YdbSavedSearchNotificationOffsetDao
}
import ru.yandex.vertis.general.favorites.storage.{FavoritesNotificationQueueDao, SavedSearchInvertedDao}
import ru.yandex.vertis.general.gateway.clients.router.testkit.RouterClientMock
import ru.yandex.vertis.general.search.testkit.TestSearchService
import common.zio.logging.Logging
import zio.clock.Clock
import zio.test.Assertion.{anything, equalTo, forall, isGreaterThan, isSubtype}
import zio.test.TestAspect._
import zio.test._
import zio.{Ref, UIO, ZIO, ZLayer}

import scala.concurrent.duration._

object SavedSearchNotificationManagerTest extends DefaultRunnableSpec {

  implicit private class SearchFilterOps(params: Seq[SearchFilter]) {

    def filterOutImpossible: Seq[SearchFilter] = {
      val isAllowedValueTypeForEqual: Value => Boolean = {
        case Value.String(_) => true
        case _ => false
      }

      params.filter { x =>
        x.operation match {
          case Equal(x) => isAllowedValueTypeForEqual(x.getValue.value)
          case NotEqual(x) => isAllowedValueTypeForEqual(x.getValue.value)
          case _ => true
        }
      }
    }
  }

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SavedSearchNotificationManagerTest")(
      testM("send notifications") {
        checkNM(1)(SavedSearchInvertedListItemGen.items(10)) { items =>
          val prepared = items.zipWithIndex.map { case (item, i) =>
            item.copy(savedSearchInverted =
              item.savedSearchInverted.copy(
                searchId = item.savedSearchInverted.searchId + i,
                lastSeenAt = 0,
                lastSentAt = 0,
                notificationSettings = NotificationSettings(Seq(NotificationChannel.EMAIL)),
                searchRequest = item.savedSearchInverted.searchRequest
                  .copy(
                    sort = SearchSort.BY_PRICE_ASC,
                    categoryIds = Seq("test_category"),
                    parameter = item.savedSearchInverted.searchRequest.parameter.filterOutImpossible,
                    shuffle = ShuffleEnum.Shuffle.DEFAULT
                  )
              )
            )
          }.toSet
          for {
            _ <- TestSearchService.setSearchOffersResponse(_ =>
              ZIO.succeed(SearchOffersResponse().withSnippet(Seq(SearchSnippet(offerId = "123"))))
            )
            _ <- runTx(SavedSearchInvertedDao.upsert(prepared.toList))
            _ <- SavedSearchNotificationManager.processShards(Set(0))
            notifications <- runTx(FavoritesNotificationQueueDao.peek(0, 100))
            updatedInverted <- ZIO.foreach(prepared.groupBy(_.ownerId)) { case (owner, ids) =>
              runTx(SavedSearchInvertedDao.getBatch(ownerId = owner, ids.map(_.savedSearchInverted.searchId).toSet))
                .map(owner -> _)
            }
          } yield assert(notifications.size)(equalTo(10)) &&
            assert(updatedInverted.values.flatten.map(_.savedSearchInverted.lastSeenAt))(forall(isGreaterThan(0L))) &&
            assert(updatedInverted.values.flatten.map(_.savedSearchInverted.lastSentAt))(forall(isGreaterThan(0L)))
        }
      },
      testM("skip when searcher failed") {
        checkNM(1)(SavedSearchInvertedListItemGen.items(10)) { items =>
          val prepared = items.map(item =>
            item.copy(savedSearchInverted =
              item.savedSearchInverted.copy(
                lastSeenAt = 0,
                lastSentAt = 0,
                notificationSettings = NotificationSettings(),
                searchRequest = item.savedSearchInverted.searchRequest
                  .copy(
                    sort = SearchSort.BY_PRICE_ASC,
                    categoryIds = Seq("test_category"),
                    parameter = item.savedSearchInverted.searchRequest.parameter.filterOutImpossible
                  )
              )
            )
          )
          for {
            _ <- TestSearchService.setSearchOffersResponse(_ => ZIO.fail(new RuntimeException("searcher failed")))
            _ <- runTx(SavedSearchInvertedDao.upsert(prepared))
            _ <- SavedSearchNotificationManager.processShards(Set(0))
            notifications <- runTx(FavoritesNotificationQueueDao.peek(0, 100))
          } yield assert(notifications.size)(equalTo(0))
        }
      },
      testM("skip when router failed") {
        checkNM(1)(SavedSearchInvertedListItemGen.items(10)) { items =>
          val prepared = items.map(item =>
            item.copy(savedSearchInverted =
              item.savedSearchInverted.copy(
                lastSeenAt = 0,
                lastSentAt = 0,
                notificationSettings = NotificationSettings(),
                searchRequest = item.savedSearchInverted.searchRequest
                  .copy(
                    sort = SearchSort.BY_PRICE_ASC,
                    categoryIds = Seq("test_category"),
                    parameter = item.savedSearchInverted.searchRequest.parameter.filterOutImpossible
                  )
              )
            )
          )
          for {
            _ <- RouterClientMock.setRouteResponse(_ => ZIO.fail(new RuntimeException("test")))
            _ <- runTx(SavedSearchInvertedDao.upsert(prepared))
            _ <- SavedSearchNotificationManager.processShards(Set(0))
            notifications <- runTx(FavoritesNotificationQueueDao.peek(0, 100))
            savedSearch <- runTx(SavedSearchInvertedDao.listing(shardId = 0, limit = 100, lastKey = None))
          } yield assert(notifications.size)(equalTo(0)) &&
            assert(savedSearch.map(_.savedSearchInverted.lastSeenAt))(forall(equalTo(0L))) &&
            assert(savedSearch.map(_.savedSearchInverted.lastSentAt))(forall(equalTo(0L)))
        }
      },
      testM("skip when category not found") {
        checkNM(1)(SavedSearchInvertedListItemGen.items(10)) { items =>
          val prepared = items.map(item =>
            item.copy(savedSearchInverted =
              item.savedSearchInverted.copy(
                lastSeenAt = 0,
                lastSentAt = 0,
                notificationSettings = NotificationSettings(),
                searchRequest = item.savedSearchInverted.searchRequest
                  .copy(
                    sort = SearchSort.BY_PRICE_ASC,
                    categoryIds = Seq("unknown_category"),
                    parameter = item.savedSearchInverted.searchRequest.parameter.filterOutImpossible
                  )
              )
            )
          )
          for {
            _ <- runTx(SavedSearchInvertedDao.upsert(prepared))
            _ <- SavedSearchNotificationManager.processShards(Set(0))
            notifications <- runTx(FavoritesNotificationQueueDao.peek(0, 100))
          } yield assert(notifications.size)(equalTo(0))
        }
      },
      testM("skip when delay time is not expired") {
        checkNM(1)(SavedSearchInvertedListItemGen.items(10)) { items =>
          for {
            now <- zio.clock.instant
            prepared1 = items
              .take(5)
              .map(item =>
                item.copy(savedSearchInverted =
                  item.savedSearchInverted.copy(
                    lastSeenAt = 0,
                    lastSentAt = now.toEpochMilli,
                    notificationSettings = NotificationSettings(Seq(NotificationChannel.EMAIL)),
                    searchRequest = item.savedSearchInverted.searchRequest
                      .copy(
                        sort = SearchSort.BY_PRICE_ASC,
                        categoryIds = Seq("test_category"),
                        parameter = item.savedSearchInverted.searchRequest.parameter.filterOutImpossible
                      )
                  )
                )
              )
            prepared2 = items
              .drop(5)
              .map(item =>
                item.copy(savedSearchInverted =
                  item.savedSearchInverted.copy(
                    lastSeenAt = 0,
                    lastSentAt = 0,
                    notificationSettings = NotificationSettings(Seq(NotificationChannel.EMAIL)),
                    searchRequest = item.savedSearchInverted.searchRequest
                      .copy(
                        sort = SearchSort.BY_PRICE_ASC,
                        categoryIds = Seq("test_category"),
                        parameter = item.savedSearchInverted.searchRequest.parameter.filterOutImpossible
                      )
                  )
                )
              )

            _ <- TestSearchService.setSearchOffersResponse(_ =>
              ZIO.succeed(SearchOffersResponse().withSnippet(Seq(SearchSnippet(offerId = "123"))))
            )
            _ <- runTx(SavedSearchInvertedDao.upsert(prepared1 ++ prepared2))
            _ <- SavedSearchNotificationManager.processShards(Set(0))
            notifications <- runTx(FavoritesNotificationQueueDao.peek(0, 100))
            updatedInverted1 <- ZIO.foreach(prepared1.groupBy(_.ownerId)) { case (owner, ids) =>
              runTx(SavedSearchInvertedDao.getBatch(ownerId = owner, ids.map(_.savedSearchInverted.searchId).toSet))
                .map(owner -> _)
            }
            updatedInverted2 <- ZIO.foreach(prepared2.groupBy(_.ownerId)) { case (owner, ids) =>
              runTx(SavedSearchInvertedDao.getBatch(ownerId = owner, ids.map(_.savedSearchInverted.searchId).toSet))
                .map(owner -> _)
            }
          } yield assert(notifications.size)(equalTo(5)) &&
            assert(updatedInverted1.values.flatten.map(_.savedSearchInverted.lastSentAt))(
              forall(equalTo(now.toEpochMilli))
            ) &&
            assert(updatedInverted2.values.flatten.map(_.savedSearchInverted.lastSentAt))(forall(isGreaterThan(0L)))
        }
      },
      testM("группировать нотификации по настройкам уведомления") {
        checkNM(1)(SavedSearchInvertedListItemGen.items(10)) { items =>
          for {
            now <- zio.clock.instant
            prepared1 = items
              .take(5)
              .map(item =>
                item.copy(savedSearchInverted =
                  item.savedSearchInverted.copy(
                    searchId = "id",
                    lastSeenAt = 0,
                    lastSentAt = 0,
                    notificationSettings = NotificationSettings(),
                    searchRequest = item.savedSearchInverted.searchRequest
                      .copy(
                        sort = SearchSort.BY_PRICE_ASC,
                        categoryIds = Seq("test_category"),
                        parameter = item.savedSearchInverted.searchRequest.parameter.filterOutImpossible
                      )
                  )
                )
              )
            prepared2 = items
              .drop(5)
              .map(item =>
                item.copy(savedSearchInverted =
                  item.savedSearchInverted.copy(
                    searchId = "id",
                    lastSeenAt = 0,
                    lastSentAt = 0,
                    notificationSettings = NotificationSettings(notificationChannels = Seq(NotificationChannel.PUSH)),
                    searchRequest = item.savedSearchInverted.searchRequest
                      .copy(
                        sort = SearchSort.BY_PRICE_ASC,
                        categoryIds = Seq("test_category"),
                        parameter = item.savedSearchInverted.searchRequest.parameter.filterOutImpossible
                      )
                  )
                )
              )

            _ <- TestSearchService.setSearchOffersResponse(_ =>
              ZIO.succeed(SearchOffersResponse().withSnippet(Seq(SearchSnippet(offerId = "123"))))
            )
            _ <- runTx(SavedSearchInvertedDao.upsert(prepared1 ++ prepared2))
            _ <- SavedSearchNotificationManager.processShards(Set(0))
            notifications <- runTx(FavoritesNotificationQueueDao.peek(0, 100))
          } yield assert(notifications.size)(equalTo(1)) &&
            assert(notifications.head)(isSubtype[PushNotification](anything))
        }
      }
    ) @@ before(
      runTx(YdbSavedSearchInvertedDao.clean) *> runTx(YdbSavedSearchNotificationOffsetDao.clean) *> runTx(
        YdbFavoritesNotificationQueueDao.clean
      )
    ) @@ sequential @@ shrinks(0)
  }.provideCustomLayer {
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val clock = Clock.live
    val logging = Logging.live

    val router = RouterClientMock.layer
    val offsetDao = ydb >>> YdbSavedSearchNotificationOffsetDao.live
    val invertedSearchShardCount = UIO(ShardCount.defaultTest).toLayer
    val invertedDao = (ydb ++ invertedSearchShardCount) >+> YdbSavedSearchInvertedDao.live
    val notificationsShardCount = UIO(ShardCount.defaultTest).toLayer
    val testBonsaiSnapshot = BonsaiSnapshot(Seq(Category(id = "test_category", name = "test category")), Seq.empty)
    val notificationQueueDao = (notificationsShardCount ++ ydb) >>> YdbFavoritesNotificationQueueDao.live
    val searcher = TestSearchService.layer
    val schedulerConfig = UIO(
      SavedSearchesSchedulerConfig(
        notificationDelay = 1.minute,
        batchSize = 10,
        siteUrl = "https://o.yandex.ru",
        emailTemplate = "o_saved_search",
        avatarsHost = "avatar"
      )
    ).toLayer
    val testBonsaiRef = Ref.make(testBonsaiSnapshot).toLayer
    val jwt = ZLayer.succeed(HMACConfig("secret")) ++ ZLayer.succeed(Jwt.Config(1.day)) ++ clock >>> Jwt.HMAC256
    val notificationManager =
      (offsetDao ++ testBonsaiRef ++ router ++ invertedDao ++ notificationQueueDao ++
        searcher ++ txRunner ++ clock ++ logging ++ schedulerConfig ++ jwt) >+> SavedSearchNotificationManager.live

    invertedDao ++ notificationManager
  }
}
