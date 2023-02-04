package ru.yandex.vertis.general.gost.storage.test

import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.gost.storage.ydb.feed.YdbFeedIdsMappingDao
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.gost.model.Offer.OfferId
import ru.yandex.vertis.general.gost.model.testkit.{FeedInfoGen, OfferGen, StateGen}
import ru.yandex.vertis.general.gost.storage.FeedIdsMappingDao
import ru.yandex.vertis.general.gost.storage.FeedIdsMappingDao.FeedIdsMapping
import ru.yandex.vertis.general.gost.storage.testkit.FeedIdsMappingGen
import zio.ZIO
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect.{sequential, shrinks}
import zio.test.{Gen, _}

object YdbFeedIdsMappingDaoSpec extends DefaultRunnableSpec {

  import TestYdb._

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("YdbFeedIdsMappingDao")(
      testM("Добавить новые записи и получить id офферов") {
        (checkNM(1): CheckVariants.CheckNM)(
          Gen.setOfN(2)(SellerGen.anySellerId).noShrink,
          Gen.setOfN(2)(FeedInfoGen.anyNamespaceIdGen).noShrink,
          Gen.setOfN(10)(OfferGen.anyOfferId).noShrink,
          Gen.setOfN(10)(FeedIdsMappingGen.anyFeedIdsMapping).noShrink,
          Gen.setOfN(20)(StateGen.genInstantMs.derive).noShrink.map(_.iterator)
        ) { (sellerIds, namespaceIds, offerIds, feedInfos, timestamp) =>
          for {
            elements <- ZIO
              .effectTotal {
                for {
                  offerId <- offerIds
                  info <- feedInfos
                  sellerId <- sellerIds
                  namespaceId <- namespaceIds
                } yield FeedIdsMapping(
                  sellerId,
                  namespaceId,
                  info.externalOfferId,
                  info.taskId,
                  offerId,
                  timestamp.nextOption()
                )
              }
              .map(_.toList)
            _ <- runTx(FeedIdsMappingDao.upsert(elements))
            queriedExternalIds = feedInfos.map(_.externalOfferId)
            externalIdToOfferId <- runTx(
              FeedIdsMappingDao.listOfferIds(sellerIds.head, namespaceIds.head, queriedExternalIds.toList)
            )
            expectedMap = elements
              .filter(element =>
                element.sellerId == sellerIds.head &&
                  element.namespaceId == namespaceIds.head &&
                  queriedExternalIds.contains(element.externalOfferId)
              )
              .map(element => element.externalOfferId -> element.offerId)
              .toMap
          } yield assert(externalIdToOfferId)(equalTo(expectedMap))
        }
      },
      testM("Добавить новые записи и сделать листинг") {
        (checkNM(1): CheckVariants.CheckNM)(
          Gen.setOfN(2)(SellerGen.anySellerId).noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink,
          Gen.setOfN(10)(OfferGen.anyOfferId).noShrink,
          Gen.setOfN(10)(FeedInfoGen.anyExternalOfferId).noShrink,
          Gen.setOfN(20)(StateGen.genInstantMs.derive).noShrink.map(_.iterator)
        ) { (sellerIds, namespaceId, namespaceId2, offerIds, externalIds, timestamp) =>
          for {
            elements <- ZIO.effectTotal {
              offerIds.zip(externalIds).zipWithIndex.toList.flatMap { case ((offerId, externalId), ind) =>
                List(
                  FeedIdsMapping(sellerIds.head, namespaceId, externalId, ind, offerId, None),
                  FeedIdsMapping(sellerIds.head, namespaceId2, externalId, ind, offerId, Some(timestamp.next())),
                  FeedIdsMapping(
                    sellerIds.drop(1).head,
                    namespaceId,
                    externalId,
                    ind,
                    offerId,
                    None
                  )
                )
              }
            }
            _ <- runTx(FeedIdsMappingDao.upsert(elements))
            firstStoredInfo <-
              FeedIdsMappingDao.listLessThanTaskId(sellerIds.head, namespaceId, 5).take(1).runCollect
            allStoredInfo <-
              FeedIdsMappingDao.listLessThanTaskId(sellerIds.head, namespaceId, 5).runCollect
            listingOfRemovedMappings <-
              FeedIdsMappingDao.listLessThanTaskId(sellerIds.head, namespaceId2, 100).runCollect
            expectedElements = elements
              .filter(element =>
                element.sellerId == sellerIds.head && element.namespaceId == namespaceId && element.taskId < 5
              )
          } yield assert(firstStoredInfo)(hasSize(equalTo(1))) &&
            assert(allStoredInfo)(hasSubset(firstStoredInfo)) &&
            assert(allStoredInfo)(hasSize(equalTo(5))) && // not really necessary
            assert(allStoredInfo)(hasSameElements(expectedElements)) &&
            assert(listingOfRemovedMappings)(isEmpty)
        }
      },
      testM("Добавить запись и перетереть ее") {
        (checkNM(1): CheckVariants.CheckNM)(
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink,
          FeedInfoGen.anyExternalOfferId.noShrink
        ) { (sellerId, namespaceId, externalId) =>
          for {
            initialRecord <- ZIO.effectTotal(
              FeedIdsMapping(sellerId, namespaceId, externalId, 1, OfferId("1"), None)
            )
            newRecord = FeedIdsMapping(
              sellerId,
              namespaceId,
              externalId,
              2,
              OfferId("2"),
              None
            )
            _ <- runTx(FeedIdsMappingDao.upsert(List(initialRecord)))
            mapBeforeUpsert <- runTx(FeedIdsMappingDao.listOfferIds(sellerId, namespaceId, List(externalId)))
            listingBeforeUpsert <- FeedIdsMappingDao
              .listLessThanTaskId(sellerId, namespaceId, lessThanTaskId = 5)
              .runCollect
              .map(_.toList)
            _ <- runTx(FeedIdsMappingDao.upsert(List(newRecord)))
            mapAfterUpsert <- runTx(FeedIdsMappingDao.listOfferIds(sellerId, namespaceId, List(externalId)))
            listingAfterUpsert <- FeedIdsMappingDao
              .listLessThanTaskId(sellerId, namespaceId, lessThanTaskId = 5)
              .runCollect

          } yield assert(mapBeforeUpsert)(equalTo(Map(externalId -> initialRecord.offerId))) &&
            assert(listingBeforeUpsert)(equalTo(List(initialRecord))) &&
            assert(mapAfterUpsert)(equalTo(Map(externalId -> newRecord.offerId))) &&
            assert(listingAfterUpsert)(hasSameElements(List(newRecord)))
        }
      }
    ) @@ sequential @@ shrinks(1)).provideCustomLayerShared {
      TestYdb.ydb >>> (YdbFeedIdsMappingDao.live ++ Ydb.txRunner) ++ Clock.live ++ Random.live
    }
  }
}
