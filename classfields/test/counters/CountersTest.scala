package ru.yandex.vertis.general.gost.logic.test.counters

import common.id.IdGenerator
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import common.zio.ydb.testkit.TestYdb.runTx
import general.gost.storage.ydb.feed.YdbFeedIdsMappingDao
import ru.yandex.vertis.general.common.dictionaries.testkit.TestBansDictionaryService
import ru.yandex.vertis.general.common.model.editor.testkit.Editors
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.gost.logic.OfferManager.OfferManager
import ru.yandex.vertis.general.gost.logic.counters.CountersManager
import ru.yandex.vertis.general.gost.logic.counters.CountersManager.CountersManager
import ru.yandex.vertis.general.gost.logic.{
  ChangeCategoryEventSender,
  OfferIdGenerator,
  OfferManager,
  OfferPresetsStore,
  OfferStore,
  SellingAddressEnricher,
  TotalCountersStore
}
import ru.yandex.vertis.general.gost.logic.testkit.TestValidationManager
import ru.yandex.vertis.general.gost.model.OfferStatus
import ru.yandex.vertis.general.gost.model.Price.InCurrency
import ru.yandex.vertis.general.gost.model.counters.{Counter, CountersAggregate}
import ru.yandex.vertis.general.gost.model.inactive.recall.SellerRecallReason.SoldOnYandex
import ru.yandex.vertis.general.gost.model.testkit.{Data, OfferGen}
import ru.yandex.vertis.general.gost.storage.OfferDao
import ru.yandex.vertis.general.gost.storage.ydb.YdbQueueDao
import ru.yandex.vertis.general.gost.storage.ydb.counters.YdbTotalCountersDao
import ru.yandex.vertis.general.gost.storage.ydb.feed.{YdbActiveFeedOfferDao, YdbFeedOfferDao}
import ru.yandex.vertis.general.gost.storage.ydb.offer.YdbOfferDao
import ru.yandex.vertis.general.gost.storage.ydb.preset.{YdbOfferPresetDao, YdbOfferPresetsCountDao}
import common.zio.logging.Logging
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import zio.{Ref, ZIO}
import zio.clock.Clock
import zio.random.Random
import zio.test._
import zio.test.Assertion._

object CountersTest extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Counters")(
      testM("Create offer and get user counters") {
        checkM(OfferGen.anyBannedOffer, SellerGen.anyUserId) { case (offer, user) =>
          val withSeller = offer.copy(sellerId = user)
          for {
            _ <- runTx(OfferDao.createOrUpdateOffer(withSeller))
            _ <- ZIO.accessM[OfferManager](
              _.get.updateOffer(withSeller.offerId, user, Editors.seller(user))(offer =>
                ZIO.succeed(offer.copy(status = OfferStatus.Active))
              )
            )
            counters <- ZIO.accessM[CountersManager](_.get.getTotalOffersCounters(user))
            expected = CountersAggregate.forCounter(Counter.Active, 1L)
          } yield assert(counters)(equalTo(expected))
        }
      },
      testM("Create offer, sell it and then remove") {
        checkM(OfferGen.anyBannedOffer, SellerGen.anyUserId) { case (offer, user) =>
          val withSeller = offer.copy(sellerId = user, price = InCurrency(7000L))
          for {
            _ <- runTx(OfferDao.createOrUpdateOffer(withSeller))
            _ <- ZIO.accessM[OfferManager](
              _.get.updateOffer(withSeller.offerId, user, Editors.seller(user))(offer =>
                ZIO.succeed(offer.copy(status = OfferStatus.Active))
              )
            )
            afterActivation <- ZIO.accessM[CountersManager](_.get.getTotalOffersCounters(user))
            expectedAfterActivation = CountersAggregate.forCounter(Counter.Active, 1L)
            _ <- ZIO.accessM[OfferManager](
              _.get.hideOffer(withSeller.offerId, user, Editors.seller(user), SoldOnYandex)
            )
            afterHiding <- ZIO.accessM[CountersManager](_.get.getTotalOffersCounters(user))
            expectedAfterHiding = CountersAggregate(Map(Counter.Sold -> 1L, Counter.MoneyEarned -> 7000L))
            _ <- ZIO.accessM[OfferManager](_.get.deleteOffer(withSeller.offerId, user, Editors.seller(user)))
            afterRemoving <- ZIO.accessM[CountersManager](_.get.getTotalOffersCounters(user))
            expectedAfterRemoving = CountersAggregate(Map(Counter.Sold -> 1L, Counter.MoneyEarned -> 7000L))
          } yield assert(afterActivation)(equalTo(expectedAfterActivation)) &&
            assert(afterHiding)(equalTo(expectedAfterHiding)) &&
            assert(afterRemoving)(equalTo(expectedAfterRemoving))
        }
      }
    ).provideCustomLayer {
      val dict = TestBansDictionaryService.layer
      val logging = Logging.live
      val ydb = TestYdb.ydb
      val txRunner = ydb >>> Ydb.txRunner
      val clock = Clock.live
      val random = Random.live
      val offerDao = YdbOfferDao.live
      val activeFeedOfferDao = YdbActiveFeedOfferDao.live
      val ydbFeedIdsMappingDao = YdbFeedIdsMappingDao.live
      val presetsDao = YdbOfferPresetDao.live
      val presetsCountDao = YdbOfferPresetsCountDao.live
      val presetManager = (presetsDao ++ presetsCountDao) >+> OfferPresetsStore.live
      val totalCountersDao = YdbTotalCountersDao.live
      val totalCountersStore = totalCountersDao >>> TotalCountersStore.live
      val offerStore =
        (offerDao ++ YdbQueueDao.live ++ ydbFeedIdsMappingDao ++ presetManager ++ totalCountersStore ++ clock) >>> OfferStore.live
      val cacher = Cache.noop ++ logging >>> RequestCacher.live
      val sellingAddressEnricher = TestGeoService.layer ++ cacher >>> SellingAddressEnricher.live
      val offerIdGenerator = IdGenerator.random >>> OfferIdGenerator.live
      val bonsaiSnapshot = Ref.make(Data.bonsaiSnapshotMock).toLayer
      val deps =
        ydb >>> (offerDao ++ activeFeedOfferDao ++ ydbFeedIdsMappingDao ++ totalCountersDao ++ presetManager ++ offerStore ++ txRunner) ++ clock ++ TestValidationManager.layer ++
          dict ++ ((clock ++ random) >>> offerIdGenerator) ++
          logging ++ sellingAddressEnricher ++ bonsaiSnapshot ++ ChangeCategoryEventSender.noop
      val countersManager = deps >>> CountersManager.live
      (deps >>> OfferManager.live) ++ (ydb >>> offerDao) ++ txRunner ++ Random.live ++ deps ++ countersManager
    }
}
