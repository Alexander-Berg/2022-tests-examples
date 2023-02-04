package ru.yandex.vertis.general.favorites.logic.test

import common.jwt.Jwt
import common.jwt.Jwt.HMACConfig
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.favorites.notification_model.{NotificationChannel, NotificationSettings}
import ru.yandex.vertis.general.common.model.pagination.{LimitOffset, Order}
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.favorites.logic.{FavoritesManager, FavoritesStore, SavedSellersManager}
import ru.yandex.vertis.general.favorites.model.{FavoriteType, UnsubscribeSellerPayload}
import ru.yandex.vertis.general.favorites.model.testkit.SavedSellerGen
import ru.yandex.vertis.general.favorites.storage.ydb.counts.YdbFavoritesCountDao
import ru.yandex.vertis.general.favorites.storage.ydb.favorites.YdbFavoritesDao
import ru.yandex.vertis.general.favorites.storage.ydb.inverted.{
  YdbSavedSellerInvertedCountDao,
  YdbSavedSellerInvertedDao
}
import zio.{ZIO, ZLayer}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.{before, sequential, shrinks}
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec}

import scala.concurrent.duration._

object SavedSellerManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SavedSellerManagerTest")(
      testM("save & get & delete SavedOffers") {
        checkNM(1)(SavedSellerGen.anySavedSeller(15), OwnerIdGen.anyUserId) { (savedSellers, ownerId) =>
          for {
            _ <- SavedSellersManager.createOrUpdateSavedSellers(userId = ownerId, savedSellers)
            stored <- SavedSellersManager.getSavedSellersListing(
              ownerId,
              LimitOffset(limit = 20, offset = 0),
              order = Order.Desc
            )
            _ <- SavedSellersManager.deleteSavedSeller(ownerId, savedSellers.map(_.sellerId).toSet)
            afterDelete <- SavedSellersManager.getSavedSellersListing(
              ownerId,
              LimitOffset(limit = 20, offset = 0),
              order = Order.Desc
            )
          } yield assert(stored.toSet)(equalTo(savedSellers.toSet)) &&
            assert(afterDelete)(equalTo(List.empty))
        }
      },
      testM("listing SavedOffer") {
        checkNM(1)(SavedSellerGen.anySavedSeller(15), OwnerIdGen.anyUserId) { (savedSellers, ownerId) =>
          for {
            _ <- SavedSellersManager.createOrUpdateSavedSellers(userId = ownerId, savedSellers)
            page1 <- SavedSellersManager.getSavedSellersListing(
              ownerId,
              LimitOffset(limit = 10, offset = 0),
              order = Order.Desc
            )
            page2 <- SavedSellersManager.getSavedSellersListing(
              ownerId,
              LimitOffset(limit = 10, offset = 10),
              order = Order.Desc
            )
          } yield assert(page1.size)(equalTo(10)) &&
            assert(page2.size)(equalTo(5))
        }
      },
      testM("favorites count") {
        checkNM(1)(SavedSellerGen.anySavedSeller(15), OwnerIdGen.anyUserId) { (savedSellers, ownerId) =>
          for {
            _ <- SavedSellersManager.createOrUpdateSavedSellers(userId = ownerId, savedSellers)
            counts <- FavoritesManager.getFavoritesCount(ownerId)
            _ <- SavedSellersManager.deleteSavedSeller(ownerId, savedSellers.take(10).map(_.sellerId).toSet)
            counts2 <- FavoritesManager.getFavoritesCount(ownerId)
          } yield assert(counts(FavoriteType.SavedSeller))(equalTo(savedSellers.size)) &&
            assert(counts2(FavoriteType.SavedSeller))(equalTo(savedSellers.size - 10))
        }
      },
      testM("followers count") {
        checkNM(1)(SavedSellerGen.anySavedSeller, SavedSellerGen.anySavedSeller, OwnerIdGen.anyUserId) {
          case (seller1, seller2, ownerId) =>
            for {
              _ <- SavedSellersManager.createOrUpdateSavedSellers(userId = ownerId, List(seller1))
              counts <- SavedSellersManager.getFollowersCount(Set(seller1.sellerId, seller2.sellerId))
              _ <- SavedSellersManager.deleteSavedSeller(ownerId, Set(seller1.sellerId))
              counts2 <- SavedSellersManager.getFollowersCount(Set(seller1.sellerId, seller2.sellerId))
            } yield assert(counts)(equalTo(Map(seller1.sellerId -> 1, seller2.sellerId -> 0))) &&
              assert(counts2)(equalTo(Map(seller1.sellerId -> 0, seller2.sellerId -> 0)))
        }
      },
      testM("Unsubscribe") {
        checkNM(1)(SavedSellerGen.anySavedSeller(10), OwnerIdGen.anyUserId) { (sellers, ownerId) =>
          for {
            jwt <- ZIO.service[Jwt.SignService]
            savedSellers = sellers.map(
              _.copy(
                notificationSettings = NotificationSettings(List(NotificationChannel.EMAIL, NotificationChannel.PUSH))
              )
            )
            first <- ZIO.fromOption(savedSellers.headOption)
            _ <- SavedSellersManager.createOrUpdateSavedSellers(ownerId, savedSellers)
            firstToken <- jwt.sign(UnsubscribeSellerPayload.One(ownerId, first.sellerId), None)
            _ <- SavedSellersManager.unsubscribe(firstToken)
            first <- SavedSellersManager
              .getSavedSellers(ownerId, Set(first.sellerId))
              .flatMap(found => ZIO.fromOption(found.headOption))
            allToken <- jwt.sign(UnsubscribeSellerPayload.All(ownerId), None)
            _ <- SavedSellersManager.unsubscribe(allToken)
            all <- SavedSellersManager.getSavedSellers(ownerId, savedSellers.tail.map(_.sellerId).toSet)
          } yield assert(first.notificationSettings.notificationChannels)(
            not(contains(NotificationChannel.EMAIL))
          ) &&
            assert(all.map(_.notificationSettings.notificationChannels))(
              forall(not(contains(NotificationChannel.EMAIL)))
            )
        }
      }
    ) @@ before(
      runTx(YdbFavoritesDao.clean) *> runTx(YdbFavoritesCountDao.clean) *>
        runTx(YdbSavedSellerInvertedDao.clean) *> runTx(YdbSavedSellerInvertedCountDao.clean)
    ) @@ sequential @@ shrinks(0)
  }.provideCustomLayerShared {
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val clock = Clock.live
    val favoritesDao = TestYdb.ydb >+> YdbFavoritesDao.live
    val favoritesCountDao = TestYdb.ydb >+> YdbFavoritesCountDao.live
    val savedSellerInvertedDao = TestYdb.ydb >+> YdbSavedSellerInvertedDao.live
    val savedSellerInvertedCountDao = TestYdb.ydb >+> YdbSavedSellerInvertedCountDao.live
    val favoritesStore = (favoritesDao ++ favoritesCountDao) >+> FavoritesStore.live
    val favoritesManager = (favoritesStore ++ txRunner ++ clock) >+> FavoritesManager.live
    val jwt = ZLayer.succeed(HMACConfig("secret")) ++ ZLayer.succeed(Jwt.Config(1.day)) ++ clock >>> Jwt.HMAC256
    val savedSellersManager =
      (favoritesStore ++ txRunner ++ clock ++ savedSellerInvertedDao ++ savedSellerInvertedCountDao ++ jwt) >+> SavedSellersManager.live
    favoritesManager ++ savedSellersManager
  }
}
