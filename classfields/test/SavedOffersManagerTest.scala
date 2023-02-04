package ru.yandex.vertis.general.favorites.logic.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import ru.yandex.vertis.general.common.model.pagination.{LimitOffset, Order}
import ru.yandex.vertis.general.common.model.user.testkit.OwnerIdGen
import ru.yandex.vertis.general.favorites.logic.{FavoritesManager, FavoritesStore, SavedOffersManager}
import ru.yandex.vertis.general.favorites.model.FavoriteType
import ru.yandex.vertis.general.favorites.model.testkit.SavedOfferGen
import ru.yandex.vertis.general.favorites.storage.ydb.counts.YdbFavoritesCountDao
import ru.yandex.vertis.general.favorites.storage.ydb.favorites.YdbFavoritesDao
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion.equalTo
import zio.test.TestAspect.{before, sequential, shrinks}
import zio.test.{assert, checkNM, DefaultRunnableSpec, ZSpec}

object SavedOffersManagerTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("SavedOffersManagerTest")(
      testM("save & get & delete SavedOffers") {
        checkNM(1)(SavedOfferGen.anySavedOffers(15), OwnerIdGen.anyUserId) { (savedOffers, ownerId) =>
          for {
            _ <- SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, savedOffers)
            storedSavedOffer <-
              SavedOffersManager.getSavedOffersListing(ownerId, LimitOffset(limit = 20, offset = 0), order = Order.Desc)
            _ <- SavedOffersManager.deleteSavedOffers(ownerId, savedOffers.map(_.offerId))
            afterDeletedSavedOffer <-
              SavedOffersManager.getSavedOffersListing(ownerId, LimitOffset(limit = 20, offset = 0), order = Order.Desc)
          } yield assert(storedSavedOffer.toSet)(equalTo(savedOffers.toSet)) &&
            assert(afterDeletedSavedOffer)(equalTo(List.empty))
        }
      },
      testM("listing SavedOffer") {
        checkNM(1)(SavedOfferGen.anySavedOffers(15), OwnerIdGen.anyUserId) { (savedOffers, ownerId) =>
          for {
            _ <- SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, savedOffers)
            page1 <-
              SavedOffersManager.getSavedOffersListing(ownerId, LimitOffset(limit = 10, offset = 0), order = Order.Desc)
            page2 <- SavedOffersManager.getSavedOffersListing(
              ownerId,
              LimitOffset(limit = 10, offset = 10),
              order = Order.Desc
            )
          } yield assert(page1.size)(equalTo(10)) &&
            assert(page2.size)(equalTo(5))
        }
      },
      testM("listing SavedOffer in desc order") {
        checkNM(1)(SavedOfferGen.anySavedOffers(15), OwnerIdGen.anyUserId) { (savedOffers, ownerId) =>
          for {
            _ <- ZIO.foreach_(savedOffers) { offer =>
              SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, List(offer))
            }
            listing <-
              SavedOffersManager.getSavedOffersListing(ownerId, LimitOffset(limit = 15, offset = 0), order = Order.Desc)
          } yield assert(listing)(equalTo(savedOffers.reverse))
        }
      },
      testM("listing SavedOffer in asc order") {
        checkNM(1)(SavedOfferGen.anySavedOffers(15), OwnerIdGen.anyUserId) { (savedOffers, ownerId) =>
          for {
            _ <- ZIO.foreach_(savedOffers) { offer =>
              SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, List(offer))
            }
            listing <-
              SavedOffersManager.getSavedOffersListing(ownerId, LimitOffset(limit = 15, offset = 0), order = Order.Asc)
          } yield assert(listing)(equalTo(savedOffers))
        }
      },
      testM("favorites count") {
        checkNM(1)(SavedOfferGen.anySavedOffers(15), OwnerIdGen.anyUserId) { (savedOffers, ownerId) =>
          for {
            _ <- SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, savedOffers)
            counts <- FavoritesManager.getFavoritesCount(ownerId)
            _ <-
              SavedOffersManager.deleteSavedOffers(ownerId, savedOffers.take(10).map(_.offerId))
            counts2 <- FavoritesManager.getFavoritesCount(ownerId)
          } yield assert(counts(FavoriteType.SavedOffer))(equalTo(savedOffers.size)) &&
            assert(counts2(FavoriteType.SavedOffer))(equalTo(savedOffers.size - 10))
        }
      },
      testM("increase & decrease count only once") {
        checkNM(1)(SavedOfferGen.anySavedOffers(20), OwnerIdGen.anyUserId) { (savedOffers, ownerId) =>
          val savedOffers1 = savedOffers.take(10)
          val savedOffers2 = savedOffers.drop(10)
          for {
            _ <- SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, savedOffers1)
            _ <- SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, savedOffers1)
            storedCount1 <- FavoritesManager.getFavoritesCount(ownerId)
            _ <- SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, savedOffers1 ++ savedOffers2)
            storedCount2 <- FavoritesManager.getFavoritesCount(ownerId)
            _ <- SavedOffersManager.deleteSavedOffers(ownerId, savedOffers1.map(_.offerId))
            _ <- SavedOffersManager.deleteSavedOffers(ownerId, savedOffers1.map(_.offerId))
            afterDeleteCount1 <- FavoritesManager.getFavoritesCount(ownerId)
            _ <- SavedOffersManager.deleteSavedOffers(
              ownerId,
              savedOffers1.map(_.offerId) ++ savedOffers2.map(_.offerId)
            )
            afterDeleteCount2 <- FavoritesManager.getFavoritesCount(ownerId)
          } yield assert(storedCount1(FavoriteType.SavedOffer))(equalTo(savedOffers1.size)) &&
            assert(storedCount2(FavoriteType.SavedOffer))(equalTo(savedOffers1.size + savedOffers2.size)) &&
            assert(afterDeleteCount1(FavoriteType.SavedOffer))(equalTo(savedOffers2.size)) &&
            assert(afterDeleteCount2(FavoriteType.SavedOffer))(equalTo(0))
        }
      },
      testM("increase & decrease count only once 2") {
        checkNM(1)(SavedOfferGen.anySavedOffers(20), OwnerIdGen.anyUserId) { (savedOffers, ownerId) =>
          val ids = savedOffers.map(_.offerId)
          for {
            _ <- SavedOffersManager.createOrUpdateSavedOffers(userId = ownerId, savedOffers)
            storedCount <- FavoritesManager.getFavoritesCount(ownerId)
            _ <- SavedOffersManager.deleteSavedOffers(ownerId, ids ++ ids)
            afterDeleteCount <- FavoritesManager.getFavoritesCount(ownerId)
          } yield assert(storedCount(FavoriteType.SavedOffer))(equalTo(savedOffers.size)) &&
            assert(afterDeleteCount(FavoriteType.SavedOffer))(equalTo(0))
        }
      }
    ) @@ before(runTx(YdbFavoritesDao.clean) *> runTx(YdbFavoritesCountDao.clean)) @@ sequential @@ shrinks(0)
  }.provideCustomLayerShared {
    val ydb = TestYdb.ydb
    val txRunner = ydb >+> Ydb.txRunner
    val clock = Clock.live
    val favoritesDao = TestYdb.ydb >+> YdbFavoritesDao.live
    val favoritesCountDao = TestYdb.ydb >+> YdbFavoritesCountDao.live
    val favoritesStore = (favoritesDao ++ favoritesCountDao) >+> FavoritesStore.live
    val favoritesManager = (favoritesStore ++ txRunner ++ clock) >+> FavoritesManager.live
    val savedOffersManager = (favoritesStore ++ txRunner ++ clock) >+> SavedOffersManager.live
    favoritesStore ++ txRunner ++ clock ++ favoritesManager ++ savedOffersManager
  }

}
