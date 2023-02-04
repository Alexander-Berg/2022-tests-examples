package ru.yandex.vertis.general.gost.logic.test

import java.time.Instant
import java.util.concurrent.TimeUnit
import cats.data.NonEmptyList
import common.id.IdGenerator
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.HasTxRunner
import common.zio.ydb.testkit.TestYdb
import general.common.fail_policy.FailPolicy
import general.gost.storage.ydb.feed.YdbFeedIdsMappingDao
import ru.yandex.vertis.general.common.dictionaries.testkit.TestBansDictionaryService
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.gost.logic._
import ru.yandex.vertis.general.gost.logic.testkit.TestValidationManager
import ru.yandex.vertis.general.common.model.editor.testkit.Editors
import ru.yandex.vertis.general.gost.model.Offer.{CategoryInfo, OfferId}
import ru.yandex.vertis.general.gost.model._
import ru.yandex.vertis.general.gost.model.bans.BanInfo
import ru.yandex.vertis.general.gost.model.attributes.{Attribute, AttributeValue, Attributes}
import ru.yandex.vertis.general.gost.model.inactive.InactiveReason
import ru.yandex.vertis.general.gost.model.inactive.recall.SellerRecallReason
import ru.yandex.vertis.general.gost.model.inactive.recall.SellerRecallReason.{Other, Rethink, SoldOnYandex}
import ru.yandex.vertis.general.gost.model.moderation.DisplayApplicable.{Approved, NotApproved, Unprocessed}
import ru.yandex.vertis.general.gost.model.moderation.LockedField
import ru.yandex.vertis.general.gost.model.testkit.{Data, OfferGen, OfferUpdateGen}
import ru.yandex.vertis.general.gost.model.validation.ValidationError
import ru.yandex.vertis.general.gost.storage.OfferDao
import ru.yandex.vertis.general.gost.storage.ydb.YdbQueueDao
import ru.yandex.vertis.general.gost.storage.ydb.counters.YdbTotalCountersDao
import ru.yandex.vertis.general.gost.storage.ydb.feed.{YdbActiveFeedOfferDao, YdbFeedOfferDao}
import ru.yandex.vertis.general.gost.storage.ydb.offer.YdbOfferDao
import ru.yandex.vertis.general.gost.storage.ydb.preset.{YdbOfferPresetDao, YdbOfferPresetsCountDao}
import ru.yandex.vertis.ydb.zio.{Tx, TxRunner}
import common.zio.logging.Logging
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import zio.clock.Clock
import zio.duration.Duration
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect.{before, sequential, shrinks}
import zio.test._
import zio.{Ref, ZIO}
import zio.clock.instant

import scala.concurrent.duration._

object DefaultOfferManagerTest extends DefaultRunnableSpec {

  private def runTx[R <: Clock with HasTxRunner, E, A](action: Tx[R, E, A]): ZIO[R, E, A] =
    ZIO.service[TxRunner].flatMap(_.runTx(action).flatMapError(_.getOrDie))

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("DefaultOfferManager")(
      testM("Get offer") {
        checkNM(1)(
          OfferGen.anyActiveOffer.noShrink,
          SellerGen.anySellerId.noShrink,
          SellerGen.anySellerId.noShrink
        ) { (offer, sellerId, anotherSellerId) =>
          val withUserId = offer.copy(sellerId = sellerId)
          for {
            _ <- runTx(OfferDao.createOrUpdateOffer(withUserId))
            resultOffer <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = false,
              includeRestricted = true
            )
            anotherSellerResult <-
              OfferManager
                .getOffer(offer.offerId, Some(anotherSellerId), includeRemoved = true, includeRestricted = true)
                .flip
          } yield assert(resultOffer)(equalTo(withUserId)) &&
            assert(anotherSellerResult)(isSubtype[OfferNotFound](anything))
        }
      },
      testM("Hide offer") {
        checkNM(1)(OfferGen.anyActiveOffer, SellerGen.anySellerId) { (offer, sellerId) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffer = offer.copy(sellerId = sellerId, updatedAt = now)
            for {
              _ <- runTx(OfferDao.createOrUpdateOffer(fixedOffer))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- OfferManager.hideOffer(
                offer.offerId,
                sellerId,
                Editors.seller(sellerId),
                recallReason = SoldOnYandex
              )
              resultOffer <- OfferManager.getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = false,
                includeRestricted = true
              )
            } yield assert(OfferStatus.isInactive(resultOffer.status))(isTrue) &&
              assert(
                OfferStatus
                  .asInactive(resultOffer.status)
                  .flatMap(inactive => InactiveReason.asSellerRecall(inactive.reason))
                  .map(_.recallReason)
              )(
                equalTo(Option(SoldOnYandex))
              )
          }
        }
      },
      testM("Activate offer") {
        checkNM(1)(OfferGen.anyInactiveOffer.noShrink, SellerGen.anySellerId.noShrink) { (offer, sellerId) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffer = offer.copy(sellerId = sellerId, updatedAt = now)
            for {
              _ <- runTx(OfferDao.createOrUpdateOffer(fixedOffer))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- OfferManager.activateOffer(offer.offerId, sellerId, Editors.seller(sellerId))
              resultOffer <- OfferManager.getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = false,
                includeRestricted = true
              )
            } yield assert(resultOffer.status)(equalTo(OfferStatus.Active))
          }
        }
      },
      testM("Activate offer triggers actualization") {
        checkNM(1)(OfferGen.anyInactiveOffer.noShrink, SellerGen.anySellerId.noShrink) { (offer, sellerId) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffer = offer.copy(sellerId = sellerId, updatedAt = now)
            for {
              _ <- runTx(OfferDao.createOrUpdateOffer(fixedOffer))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- OfferManager.activateOffer(offer.offerId, sellerId, Editors.seller(sellerId))
              resultOffer <- OfferManager.getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = false,
                includeRestricted = true
              )
            } yield assert(resultOffer.actualizedAt.toEpochMilli)(isGreaterThan(offer.actualizedAt.toEpochMilli))
          }
        }
      },
      testM("Activate offer updates expireAt") {
        checkNM(1)(OfferGen.anyInactiveOffer.noShrink, SellerGen.anySellerId.noShrink) { (offer, sellerId) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffer = offer.copy(sellerId = sellerId, updatedAt = now, expireAt = Some(now))
            for {
              _ <- runTx(OfferDao.createOrUpdateOffer(fixedOffer))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- OfferManager.activateOffer(offer.offerId, sellerId, Editors.seller(sellerId))
              resultOffer <- OfferManager.getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = false,
                includeRestricted = true
              )
            } yield assert(resultOffer.expireAt)(isSome(isGreaterThan(now.plusSeconds(29.days.toSeconds))))
          }
        }
      },
      testM("Update offer") {
        import Data._
        checkNM(1)(
          OfferGen.anyActiveOffer.noShrink,
          SellerGen.anySellerId.noShrink,
          OfferUpdateGen.anyOfferUpdate.noShrink
        ) { case (offer, sellerId, update) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffer = offer.copy(
              sellerId = sellerId,
              updatedAt = now,
              status = OfferStatus.Active,
              category = CategoryInfo(mockCategory1.id, 0)
            )
            val fixedUpdate =
              update.copy(
                category = CategoryInfo(mockCategory1.id, 0),
                attributes = Attributes(Seq(Attribute(mockAttribute1.attributeId, 0, AttributeValue.Empty)))
              )
            for {
              _ <- runTx(OfferDao.createOrUpdateOffer(fixedOffer))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- OfferManager.updateOffer(
                offer.offerId,
                sellerId,
                Editors.seller(sellerId)
              )((offer: Offer) => ZIO.succeed(fixedUpdate(offer)))
              resultOffer <- OfferManager.getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = false,
                includeRestricted = true
              )
            } yield assert(resultOffer.description)(equalTo(fixedUpdate.description)) &&
              assert(resultOffer.category.version)(equalTo(mockCategory1.version)) &&
              assert(resultOffer.attributes.get(mockAttribute1.attributeId).get.version)(
                equalTo(mockAttribute1.version)
              )
          }
        }
      },
      testM("Update offer concurrently") {
        import Data._
        checkNM(1)(
          OfferGen.anyActiveOffer.noShrink,
          SellerGen.anySellerId.noShrink
        ) { case (offer, sellerId) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffer = offer.copy(
              title = "0",
              sellerId = sellerId,
              updatedAt = now,
              status = OfferStatus.Active
            )
            for {
              _ <- runTx(OfferDao.createOrUpdateOffer(fixedOffer))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- ZIO.foreachPar_(0 until 3)(_ =>
                OfferManager.updateOffer(
                  offer.offerId,
                  sellerId,
                  Editors.seller(sellerId)
                )((offer: Offer) => ZIO.succeed(offer.copy(title = (offer.title.toInt + 1).toString)))
              )
              resultOffer <- OfferManager.getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = false,
                includeRestricted = true
              )
            } yield assert(resultOffer.title.toInt)(equalTo(3))
          }
        }
      },
      testM("Update offers concurrently") {
        import Data._
        checkNM(1)(
          OfferGen.anyNonRemovedOffers(2),
          SellerGen.anySellerId.noShrink
        ) { case (offers, sellerId) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffers = offers.map(
              _.copy(
                title = "0",
                sellerId = sellerId,
                updatedAt = now,
                status = OfferStatus.Active
              )
            )
            val updatesFunc = fixedOffers
              .map(o =>
                o.offerId -> ((offer: Offer) => ZIO.succeed(offer.copy(title = (offer.title.toInt + 1).toString)))
              )
              .toMap
            for {
              _ <- runTx(OfferDao.createOrUpdateOffers(fixedOffers))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- ZIO.foreachPar_(0 until 3)(_ =>
                OfferManager.updateOffers(sellerId, Editors.seller(sellerId), updatesFunc, validateFields = true)
              )
              resultOffers <- OfferManager.getOffers(
                offers.map(_.offerId),
                Some(sellerId),
                includeRemoved = false,
                includeRestricted = true,
                failPolicy = FailPolicy.FAIL_FAST
              )
            } yield assert(resultOffers.map(_.title).toSet)(equalTo(Set("3")))
          }
        }
      },
      testM("Update offer triggers actualization") {
        import Data._
        checkNM(1)(
          OfferGen.anyActiveOffer.noShrink,
          SellerGen.anySellerId.noShrink,
          OfferUpdateGen.anyOfferUpdate.noShrink
        ) { case (offer, sellerId, update) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffer = offer.copy(
              sellerId = sellerId,
              updatedAt = now,
              status = OfferStatus.Active,
              category = CategoryInfo(mockCategory1.id, 0)
            )
            val fixedUpdate =
              update.copy(
                category = CategoryInfo(mockCategory1.id, 0),
                attributes = Attributes(Seq(Attribute(mockAttribute1.attributeId, 0, AttributeValue.Empty)))
              )
            for {
              _ <- runTx(OfferDao.createOrUpdateOffer(fixedOffer))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- OfferManager.updateOffer(
                offer.offerId,
                sellerId,
                Editors.seller(sellerId)
              )((offer: Offer) => ZIO.succeed(fixedUpdate(offer)))
              resultOffer <- OfferManager.getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = false,
                includeRestricted = true
              )
            } yield assert(resultOffer.actualizedAt.toEpochMilli)(isGreaterThan(fixedOffer.actualizedAt.toEpochMilli))
          }
        }
      },
      testM("Batch update offers") {
        checkNM(1)(
          OfferGen.anyNonRemovedOffers(10),
          SellerGen.anySellerId,
          OfferUpdateGen.anyOfferUpdates(10)
        ) { case (offers, sellerId, update) =>
          val withUserId = offers.map(_.copy(sellerId = sellerId, status = OfferStatus.Active))
          val updates = offers.map(_.offerId).zip(update).toMap
          val updatesFunc = updates.view.mapValues(update => (offer: Offer) => ZIO.succeed(update(offer))).toMap
          for {
            _ <- runTx(OfferDao.createOrUpdateOffers(withUserId))
            _ <- OfferManager.updateOffers(sellerId, Editors.seller(sellerId), updatesFunc, validateFields = true)
            resultOffer <- OfferManager.getOffers(
              offers.map(_.offerId),
              Some(sellerId),
              includeRemoved = false,
              includeRestricted = true,
              failPolicy = FailPolicy.FAIL_FAST
            )
          } yield assert(resultOffer.map(o => o.offerId -> o.price).toMap)(
            equalTo(updates.view.mapValues(_.price).toMap)
          )
        }
      },
      testM("Batch update someone else's offers") {
        checkNM(1)(
          OfferGen.anyActiveOffers(4).noShrink,
          SellerGen.anySellerId.noShrink,
          OfferGen.anyActiveOffers(4).noShrink,
          SellerGen.anySellerId.noShrink,
          OfferUpdateGen.anyOfferUpdates(4).noShrink
        ) { case (offers1, sellerId1, offers2, sellerId2, update) =>
          val withUserId1 = offers1.map(_.copy(sellerId = sellerId1))
          val withUserId2 = offers2.map(_.copy(sellerId = sellerId2))
          val mixedOffers = offers1.take(2) ++ offers2.take(2)
          val updates = mixedOffers.map(_.offerId).zip(update).toMap
          val updatesFunc = updates.view.mapValues(update => (offer: Offer) => ZIO.succeed(update(offer))).toMap
          for {
            _ <- runTx(OfferDao.createOrUpdateOffers(withUserId1))
            _ <- runTx(OfferDao.createOrUpdateOffers(withUserId2))
            result <- OfferManager
              .updateOffers(sellerId1, Editors.seller(sellerId1), updatesFunc, validateFields = true)
              .flip
          } yield assert(result)(isSubtype[OffersNotFound](anything))
        }
      },
      testM("Batch update not existing offers") {
        checkNM(1)(
          OfferGen.anyNonRemovedOffers(10).noShrink,
          SellerGen.anySellerId.noShrink,
          OfferUpdateGen.anyOfferUpdates(10).noShrink
        ) { case (offers1, sellerId1, update) =>
          val withUserId1 = offers1.map(_.copy(sellerId = sellerId1))
          val createOffer = withUserId1.take(5)
          val updates = offers1.map(_.offerId).zip(update).toMap
          val updatesFunc = updates.view.mapValues(update => (offer: Offer) => ZIO.succeed(update(offer))).toMap
          for {
            _ <- runTx(OfferDao.createOrUpdateOffers(createOffer))
            result <- OfferManager
              .updateOffers(sellerId1, Editors.seller(sellerId1), updatesFunc, validateFields = true)
              .flip
          } yield assert(result)(isSubtype[OffersNotFound](anything))
        }
      },
      testM("Batch hide offers") {
        checkNM(1)(OfferGen.anyActiveOffers(10).noShrink, SellerGen.anySellerId.noShrink) { case (offers, sellerId) =>
          val withUserId = offers.map(_.copy(sellerId = sellerId))
          for {
            _ <- runTx(OfferDao.createOrUpdateOffers(withUserId))
            _ <- OfferManager.hideOffers(
              sellerId = sellerId,
              editor = Editors.seller(sellerId),
              offerIds = withUserId.map(_.offerId),
              recallReason = Rethink
            )
            resultOffer <- OfferManager.getOffers(
              offers.map(_.offerId),
              Some(sellerId),
              includeRemoved = false,
              includeRestricted = true,
              failPolicy = FailPolicy.FAIL_FAST
            )
          } yield assert(resultOffer.map(o => OfferStatus.isInactive(o.status)))(forall(isTrue)) &&
            assert(
              resultOffer.map(o =>
                OfferStatus
                  .asInactive(o.status)
                  .flatMap(r => InactiveReason.asSellerRecall(r.reason).map(_.recallReason))
              )
            )(forall(isSome(equalTo(Rethink))))
        }
      },
      testM("Batch delete offers") {
        checkNM(1)(OfferGen.anyInactiveOffers(2).noShrink, SellerGen.anySellerId.noShrink) { case (offers, sellerId) =>
          val withUserId = offers.map(_.copy(sellerId = sellerId))
          val error =
            OffersNotFound(offersId = withUserId.map(_.offerId), sellerId = Some(sellerId), includeRemoved = false)
          for {
            _ <- runTx(OfferDao.createOrUpdateOffers(withUserId))
            _ <- OfferManager.deleteOffers(
              sellerId = sellerId,
              editor = Editors.seller(sellerId),
              offerIds = withUserId.map(_.offerId)
            )
            result <-
              OfferManager
                .getOffers(
                  offers.map(_.offerId),
                  Some(sellerId),
                  includeRemoved = false,
                  includeRestricted = true,
                  failPolicy = FailPolicy.FAIL_FAST
                )
                .flip
          } yield assert(result)(
            isSubtype[OffersNotFound](hasField("offersId", _.offersId.toSet, equalTo(error.offersId.toSet)))
          )
        }
      },
      testM("Delete offer") {
        checkNM(1)(OfferGen.anyInactiveOffer.noShrink, SellerGen.anySellerId.noShrink) { (offer, sellerId) =>
          zio.clock.instant.flatMap { now =>
            val fixedOffer = offer.copy(sellerId = sellerId, updatedAt = now)
            for {
              _ <- runTx(OfferDao.createOrUpdateOffer(fixedOffer))
              _ <- ZIO.accessM[Clock](_.get.sleep(Duration(1, TimeUnit.MILLISECONDS)))
              _ <- OfferManager.deleteOffer(offer.offerId, sellerId, Editors.seller(sellerId))
              deletedReuslt <- OfferManager.getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = true,
                includeRestricted = true
              )
              withoutUserResult <- OfferManager.getOffer(
                offer.offerId,
                None,
                includeRemoved = true,
                includeRestricted = true
              )
              result <- OfferManager
                .getOffer(offer.offerId, Some(sellerId), includeRemoved = false, includeRestricted = true)
                .flip
            } yield assert(result)(isSubtype[OfferNotFound](anything)) &&
              assert(withoutUserResult.offerId)(equalTo(fixedOffer.offerId)) &&
              assert(deletedReuslt.offerId)(equalTo(fixedOffer.offerId))
          }
        }
      },
      testM("Get offers by seller") {
        checkNM(1)(OfferGen.anyNonRemovedOffers(15).noShrink, SellerGen.anySellerId.noShrink) { (offers, sellerId) =>
          val enrichedOffers = offers.map(_.copy(sellerId = sellerId, status = OfferStatus.Active))
          val pagination1 = LimitOffset(limit = 10, offset = 0)
          val pagination2 = LimitOffset(limit = 10, offset = 10)
          for {
            _ <- ZIO.foreach_(enrichedOffers) { offer =>
              runTx(
                OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(o =>
                  ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
                )
              )
            }
            result1 <- OfferManager.getPresetOffers(sellerId, Preset.All, pagination1)
            result2 <- OfferManager.getPresetOffers(sellerId, Preset.All, pagination2)
          } yield assert(result1.size)(equalTo(10)) &&
            assert(result2.size)(equalTo(5))
        }
      },
      testM("Get offers batch") {
        checkNM(1)(OfferGen.anyNonRemovedOffers(15).noShrink, SellerGen.anySellerId.noShrink) { (offers, sellerId) =>
          val enrichedOffers = offers.map(_.copy(sellerId = sellerId, status = OfferStatus.Active))
          val offersIdWithAdditional = enrichedOffers.map(_.offerId) :+ OfferId("one-more")
          val error =
            OffersNotFound(offersId = List(OfferId("one-more")), sellerId = Some(sellerId), includeRemoved = false)
          for {
            _ <- ZIO.foreach_(enrichedOffers) { offer =>
              runTx(
                OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(o =>
                  ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
                )
              )
            }
            result1 <-
              OfferManager
                .getOffers(
                  offersId = offersIdWithAdditional,
                  sellerId = Some(sellerId),
                  includeRemoved = true,
                  includeRestricted = true,
                  failPolicy = FailPolicy.FAIL_FAST
                )
                .flip
            result2 <- OfferManager.getOffers(
              offersId = offersIdWithAdditional,
              sellerId = Some(sellerId),
              includeRemoved = true,
              includeRestricted = true,
              failPolicy = FailPolicy.FAIL_NEVER
            )
          } yield assert(result1)(
            isSubtype[OffersNotFound](hasField("offersId", _.offersId.toSet, equalTo(error.offersId.toSet)))
          ) &&
            assert(result2.size)(equalTo(enrichedOffers.size))
        }
      },
      testM("Get offers by seller (batch)") {
        checkNM(1)(OfferGen.anyNonRemovedOffers(15).noShrink, SellerGen.anySellerId.noShrink) { (offers, sellerId) =>
          val enrichedOffers = offers
            .map(_.copy(sellerId = sellerId, status = OfferStatus.Active))
            .sortBy(_.createdAt.getEpochSecond)
            .reverse
          val pagination1 = LimitOffset(limit = 10, offset = 0)
          val pagination2 = LimitOffset(limit = 10, offset = 10)
          for {
            _ <- runTx(
              OfferStore.updateOffers(offers.map(_.offerId), Editors.seller(sellerId))(o =>
                ZIO.succeed(enrichedOffers.map(UpdateResult(_, List.empty[ValidationError])))
              )
            )
            result1 <- OfferManager.getPresetOffers(sellerId, Preset.All, pagination1)
            result2 <- OfferManager.getPresetOffers(sellerId, Preset.All, pagination2)
          } yield assert(result1.map(_.offerId))(equalTo(enrichedOffers.take(10).map(_.offerId))) &&
            assert(result2.map(_.offerId))(equalTo(enrichedOffers.drop(10).map(_.offerId)))
        }
      },
      testM("Change sellerId") {
        checkNM(1)(
          OfferGen.anyNonRemovedOffers(10).noShrink,
          SellerGen.anySellerId.noShrink,
          SellerGen.anySellerId.noShrink
        ) { (offers, oldSellerId, newSellerId) =>
          val enrichedOld = offers.map(_.copy(sellerId = oldSellerId, status = OfferStatus.Active))
          val enrichedNew = offers.map(_.copy(sellerId = newSellerId, status = OfferStatus.Active))

          val pagination = LimitOffset(limit = 10, offset = 0)
          for {
            _ <- ZIO.foreach_(enrichedOld) { offer =>
              runTx(
                OfferStore.updateOffer(offer.offerId, Editors.seller(oldSellerId))(o =>
                  ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
                )
              )
            }
            result1 <- OfferManager.getPresetOffers(oldSellerId, Preset.All, pagination)
            _ <- ZIO.foreach_(enrichedNew) { offer =>
              runTx(
                OfferStore.updateOffer(offer.offerId, Editors.seller(oldSellerId))(o =>
                  ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
                )
              )
            }
            result2 <- OfferManager.getPresetOffers(newSellerId, Preset.All, pagination)
            result3 <- OfferManager.getPresetOffers(oldSellerId, Preset.All, pagination)
          } yield assert(result1.size)(equalTo(10)) &&
            assert(result2.size)(equalTo(10)) &&
            assert(result3.size)(equalTo(0))
        }
      },
      testM("Recall with Other reason and get single offer by seller") {
        checkNM(1)(OfferGen.anyNonRemovedOffer.noShrink, SellerGen.anySellerId.noShrink) { (offer, sellerId) =>
          val enrichedOffer1 = offer.copy(sellerId = sellerId, status = OfferStatus.Active)
          val pagination1 = LimitOffset(limit = 10, offset = 0)
          for {
            _ <- runTx(
              OfferStore.updateOffer(enrichedOffer1.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(enrichedOffer1, List.empty[ValidationError]))
              )
            )
            result1 <- OfferManager.getPresetOffers(sellerId, Preset.All, pagination1)
            resultActive1 <- OfferManager.getPresetOffers(sellerId, Preset.Active, pagination1)
            _ <- OfferManager.hideOffer(enrichedOffer1.offerId, sellerId, Editors.seller(sellerId), Other)

            resultAll2 <- OfferManager.getPresetOffers(sellerId, Preset.All, pagination1)
            resultActive2 <- OfferManager.getPresetOffers(sellerId, Preset.Active, pagination1)
            resultExpired2 <- OfferManager.getPresetOffers(sellerId, Preset.Expired, pagination1)
          } yield assert(result1.head.offerId)(equalTo(enrichedOffer1.offerId)) &&
            assert(resultActive1.head.offerId)(equalTo(enrichedOffer1.offerId)) &&
            assert(resultAll2.head.offerId)(equalTo(enrichedOffer1.offerId)) &&
            assert(resultActive2)(equalTo(Seq.empty)) &&
            assert(resultExpired2.head.offerId)(equalTo(enrichedOffer1.offerId))
        }
      },
      testM("Count offers by seller") {
        checkNM(1)(OfferGen.anyActiveOffers(2).noShrink, SellerGen.anySellerId.noShrink) { (offers, sellerId) =>
          val enrichedOffers = offers.map(_.copy(sellerId = sellerId, status = OfferStatus.Active))

          for {
            _ <- ZIO.foreach_(enrichedOffers) { offer =>
              runTx(
                OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                  ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
                )
              )
            }
            activeResult <- OfferManager.getPresetOffersCount(sellerId)
            _ <- OfferManager.hideOffers(
              offers.map(_.offerId),
              sellerId,
              Editors.seller(sellerId),
              SellerRecallReason.Rethink
            )
            hideOffers <- OfferManager.getPresetOffersCount(sellerId)
          } yield assert(activeResult(Preset.All))(equalTo(2)) &&
            assert(activeResult(Preset.Active))(equalTo(2)) &&
            assert(hideOffers.getOrElse(Preset.Active, 0))(equalTo(0))
        }
      },
      testM("Ban non removed offer by moderation opinion") {
        checkNM(1)(OfferGen.anyNonRemovedOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(sellerId = sellerId)

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.editableReason.code, Nil), false),
                  None,
                  None,
                  None,
                  Instant.MIN
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(updated.status)(isSubtype[OfferStatus.Banned](anything)) && assert(
            OfferStatus.asBanned(updated.status)
          )(
            isSome(
              equalTo(
                OfferStatus.Banned(
                  BanInfo(false, NonEmptyList.of(TestBansDictionaryService.editableReason.code), offer.status)
                )
              )
            )
          )
        }
      },
      testM("Update removed offer when handling moderation opinion with ban verdict") {
        checkNM(1)(OfferGen.anyRemovedOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(sellerId = sellerId, status = OfferStatus.Removed(OfferStatus.Active))

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.editableReason.code, Nil), false),
                  None,
                  None,
                  None,
                  Instant.MIN
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(OfferStatus.asRemoved(updated.status).map(_.previous))(
            isSome(
              equalTo(
                OfferStatus.Banned(
                  BanInfo(false, NonEmptyList.of(TestBansDictionaryService.editableReason.code), OfferStatus.Active)
                )
              )
            )
          )
        }
      },
      testM("Update banned removed offer when handling OK moderation opinion") {
        checkNM(1)(OfferGen.anyRemovedOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(
            sellerId = sellerId,
            status = OfferStatus.Removed(
              OfferStatus.Banned(
                BanInfo(false, NonEmptyList(TestBansDictionaryService.editableReason.code, Nil), OfferStatus.Active)
              )
            )
          )

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Ok,
                  None,
                  None,
                  None,
                  Instant.MIN
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(OfferStatus.asRemoved(updated.status).map(_.previous))(isSome(equalTo(OfferStatus.Active)))
        }
      },
      testM("Hide active offer by moderation opinion") {
        checkNM(1)(OfferGen.anyOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(sellerId = sellerId, status = OfferStatus.Active)

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.exclusionReason.code, Nil), false),
                  None,
                  None,
                  None,
                  Instant.MIN
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(updated.status)(isSubtype[OfferStatus.Inactive](anything))
        }
      },
      testM("Hide aggregator's offer if flag isn't specified") {
        checkNM(1)(OfferGen.anyOffer.noShrink, SellerGen.anyAggregatorId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(sellerId = sellerId, status = OfferStatus.Active)

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            updated <- OfferManager
              .getOffer(
                offer.offerId,
                Some(sellerId),
                includeRemoved = true,
                includeRestricted = false
              )
              .run
          } yield assert(updated.untraced)(fails(isSubtype[OfferNotFound](anything)))
        }
      },
      testM("Do nothing with nonactive offer when handling moderation opinion with hide verdict") {
        checkNM(1)(OfferGen.anyInactiveOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(sellerId = sellerId)

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.exclusionReason.code, Nil), false),
                  None,
                  None,
                  None,
                  Instant.MIN
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(updated.status)(equalTo(offer.status))
        }
      },
      testM("Set previous status when unbanning") {
        checkNM(1)(OfferGen.anyBannedOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(sellerId = sellerId)
          val previousStatus = OfferStatus.asBanned(offer.status).map(_.banInfo.previous)

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Ok,
                  None,
                  None,
                  None,
                  Instant.MIN
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(Some(updated.status))(equalTo(previousStatus))
        }
      },
      testM("Update mordaApproved status from moderation update") {
        checkNM(1)(OfferGen.anyOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(sellerId = sellerId, mordaApplicable = NotApproved)

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Ok,
                  Some(true),
                  Some(true),
                  Some(true),
                  Instant.MIN
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(updated.mordaApplicable)(equalTo(Approved)) &&
            assert(updated.yanApplicable)(equalTo(Approved)) &&
            assert(updated.potentiallyMordaApplicable)(equalTo(Approved))
        }
      },
      testM("Ignore unknown moderation codes") {
        checkNM(1)(OfferGen.anyOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(sellerId = sellerId, mordaApplicable = NotApproved)

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.unknownReason.code, Nil), false),
                  Some(true),
                  Some(true),
                  Some(true),
                  Instant.MIN
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(updated.status)(equalTo(offer.status))
        }
      },
      testM("Ignore old moderation updates") {
        checkNM(1)(OfferGen.anyOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer = offerGenerated.copy(
            sellerId = sellerId,
            mordaApplicable = NotApproved,
            status = OfferStatus.Active
          )

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Ok,
                  Some(true),
                  Some(true),
                  Some(true),
                  Instant.ofEpochSecond(3)
                )
              )
            )
            _ <- OfferManager.updateModerationOpinion(
              Seq(
                OfferManager.ModerationOpinionUpdate(
                  offer.offerId,
                  sellerId,
                  ModerationOpinion.Failed(NonEmptyList.one(TestBansDictionaryService.nonEditableReason.code), false),
                  None,
                  None,
                  None,
                  Instant.ofEpochSecond(1)
                )
              )
            )
            updated <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(updated.moderationInfo.lastOpinionTimestamp)(isSome(equalTo(Instant.ofEpochSecond(3)))) &&
            assert(updated.status)(equalTo(OfferStatus.Active))
        }
      },
      testM("Изменяем категорию и блокируем ее изменения, проверяем, разблокируем, проверяем") {
        checkNM(1)(OfferGen.anyOffer.noShrink, SellerGen.anySellerId.noShrink) { (offerGenerated, sellerId) =>
          val offer =
            offerGenerated.copy(
              sellerId = sellerId,
              mordaApplicable = NotApproved,
              category = CategoryInfo("category1", 0)
            )

          for {
            _ <- runTx(
              OfferStore.updateOffer(offer.offerId, Editors.seller(sellerId))(_ =>
                ZIO.succeed(UpdateResult(offer, List.empty[ValidationError]))
              )
            )
            _ <- OfferManager.changeOfferCategory(
              offer.offerId,
              sellerId,
              Editors.moderator("1234"),
              "category2",
              Some("market_sku_1"),
              lockCategory = true
            )
            updated1 <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
            _ <-
              OfferManager.updateOffer(
                offer.offerId,
                sellerId,
                Editors.seller(sellerId)
              )(_ => ZIO.succeed(updated1.copy(category = CategoryInfo("category3", 0))))
            updated2 <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
            _ <- OfferManager.changeOfferCategory(
              offer.offerId,
              sellerId,
              Editors.moderator("1235"),
              "category2",
              None,
              lockCategory = false
            )
            _ <-
              OfferManager.updateOffer(
                offer.offerId,
                sellerId,
                Editors.seller(sellerId)
              )(_ => ZIO.succeed(updated1.copy(category = CategoryInfo("category3", 0))))
            updated3 <- OfferManager.getOffer(
              offer.offerId,
              Some(sellerId),
              includeRemoved = true,
              includeRestricted = true
            )
          } yield assert(updated1.category.id)(equalTo("category2")) &&
            assert(updated1.yaMarketInfo)(
              isSome(hasField("sku", _.sku, isSome(equalTo("market_sku_1"))))
            ) &&
            assert(updated1.moderationInfo.lockedFields)(equalTo(Set[LockedField](LockedField.Category))) &&
            assert(updated2.category.id)(equalTo("category2")) &&
            assert(updated2.moderationInfo.lockedFields)(equalTo(Set[LockedField](LockedField.Category))) &&
            assert(updated3.category.id)(equalTo("category3")) &&
            assert(updated3.moderationInfo.lockedFields)(equalTo(Set.empty[LockedField]))
        }
      }
    ) @@ shrinks(1)
  }.provideCustomLayer {
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
    (deps >>> OfferManager.live) ++ (ydb >>> offerDao) ++ txRunner ++ Random.live ++ deps
  }
}
