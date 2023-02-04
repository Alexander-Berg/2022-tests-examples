package ru.yandex.vertis.general.gost.logic.test

import cats.data.NonEmptyList
import common.id.IdGenerator
import common.zio.ydb.Ydb
import common.zio.ydb.Ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.gost.storage.ydb.feed.YdbFeedIdsMappingDao
import ru.yandex.vertis.general.common.dictionaries.testkit.TestBansDictionaryService
import ru.yandex.vertis.general.common.model.editor.testkit.Editors
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import ru.yandex.vertis.general.common.model.user.testkit.SellerGen
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.gost.logic._
import ru.yandex.vertis.general.gost.logic.testkit.TestValidationManager
import ru.yandex.vertis.general.gost.model.Offer.{CategoryInfo, Condition, FeedInfo, OfferId}
import ru.yandex.vertis.general.gost.model.Photo._
import ru.yandex.vertis.general.gost.model.attributes.{Attribute, AttributeValue, Attributes}
import ru.yandex.vertis.general.gost.model.bans.BanInfo
import ru.yandex.vertis.general.gost.model.feed._
import ru.yandex.vertis.general.gost.model.moderation.ModerationInfo
import ru.yandex.vertis.general.gost.model.testkit.{Data, FeedInfoGen, FeedOfferGen, OfferGen}
import ru.yandex.vertis.general.gost.model.validation.attributes.AttributeValueRequired
import ru.yandex.vertis.general.gost.model.{Offer, _}
import ru.yandex.vertis.general.gost.storage.ActiveFeedOfferDao.ActiveFeedOfferDao
import ru.yandex.vertis.general.gost.storage.FeedIdsMappingDao
import ru.yandex.vertis.general.gost.storage.FeedIdsMappingDao.FeedIdsMappingDao
import ru.yandex.vertis.general.gost.storage.ydb.YdbQueueDao
import ru.yandex.vertis.general.gost.storage.ydb.counters.YdbTotalCountersDao
import ru.yandex.vertis.general.gost.storage.ydb.feed.{YdbActiveFeedOfferDao, YdbFeedOfferDao}
import ru.yandex.vertis.general.gost.storage.ydb.offer.YdbOfferDao
import ru.yandex.vertis.general.gost.storage.ydb.preset.{YdbOfferPresetDao, YdbOfferPresetsCountDao}
import common.zio.logging.Logging
import zio.clock.Clock
import zio.random.Random
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.{assert, _}
import zio.{random, Ref, ZIO, ZLayer}

import java.time.Instant
import general.gost.feed_api.NamespaceId
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import ru.yandex.vertis.general.gost.logic.OfferManager.OfferManager

object DefaultFeedOfferManagerTest extends DefaultRunnableSpec {

  import TestYdb._

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    def hasFeedInfo(taskId: Long) = {
      hasField[Offer, Option[FeedInfo]](
        "feedInfo",
        _.feedInfo,
        isSome[FeedInfo](hasField("taskId", _.taskId, equalTo(taskId)))
      )
    }

    def hasStatus(status: OfferStatus) = {
      hasField[Offer, OfferStatus](
        "status",
        _.status,
        equalTo(status)
      )
    }

    def hasTitle(title: String) = {
      hasField[Offer, String]("title", _.title, equalTo(title))
    }

    def hasTotalCounter(value: Int) = {
      hasField[FeedOffersProcessingResult, Int]("totalOfferCount", _.totalOfferCount, equalTo(value))
    }

    def hasActiveCounter(value: Int) = {
      hasField[FeedOffersProcessingResult, Int]("activeOfferCount", _.activeOfferCount, equalTo(value))
    }

    def hasCriticalErrorCounter(value: Int) = {
      hasField[FeedOffersProcessingResult, Int]("criricalErrorOfferCount", _.criticalErrorCount, equalTo(value))
    }

    def hasCounters(total: Int, active: Int, criticalError: Int) = {
      hasTotalCounter(total) && hasActiveCounter(active) && hasCriticalErrorCounter(criticalError)
    }

    def hasOfferId(offerId: OfferId) = {
      hasField[Offer, OfferId]("offerId", _.offerId, equalTo(offerId))
    }

    def hasCategory(category: CategoryInfo) = {
      hasField[Offer, CategoryInfo]("category", _.category, equalTo(category))
    }

    def hasCondition(condition: Option[Condition]) = {
      hasField[Offer, Option[Condition]]("condition", _.condition, equalTo(condition))
    }

    def updateModerationOpinion(
        offerId: OfferId,
        sellerId: ru.yandex.vertis.general.common.model.user.SellerId,
        moderationOpinion: ModerationOpinion,
        mordaApproved: Option[Boolean],
        potentiallyMordaApproved: Option[Boolean],
        yanApproved: Option[Boolean],
        opinionTimestamp: Instant): ZIO[OfferManager, GostError, Unit] =
      OfferManager.updateModerationOpinion(
        Seq(
          OfferManager.ModerationOpinionUpdate(
            offerId,
            sellerId,
            moderationOpinion,
            mordaApproved,
            potentiallyMordaApproved,
            yanApproved,
            opinionTimestamp
          )
        )
      )

    suite("DefaultFeedOfferManager")(
      testM("Создает новые объявления с фото и фильтрует невалидные атрибуты") {
        (checkNM(1): CheckVariants.CheckNM)(
          Gen.listOfN(10)(FeedOfferGen.normalFeedOffer.noShrink).noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (normalOffers, sellerId, namespaceId) =>
          val offers = normalOffers.map(feed =>
            feed.copy(
              offer = feed.offer.map(offer =>
                offer.copy(
                  attributes = offer.attributes + Attributes(
                    Seq(
                      Attribute(
                        id = TestValidationManager.invalidAttribute,
                        version = 0,
                        AttributeValue.BooleanValue(true)
                      ),
                      Attribute(id = "valid_attr", version = 0, AttributeValue.Empty)
                    )
                  ),
                  photos = Seq(IncompletePhoto(url = "http://www.strangeurl.ru", error = false))
                )
              ),
              errors = Seq()
            )
          )
          for {
            result <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, offers)
            activeOffers <- OfferManager.getPresetOffers(sellerId, Preset.Active, LimitOffset(100, 0))
            allOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(result)(
            equalTo(
              FeedOffersProcessingResult(
                failedOffers = Seq.empty,
                totalOfferCount = 10,
                activeOfferCount = 10,
                errorCount = 0,
                criticalErrorCount = 0
              )
            )
          ) && assert(activeOffers)(hasSize(equalTo(10))) &&
            assert(allOffers)(hasSize(equalTo(10))) &&
            assert(activeOffers.map(offer => (offer.attributes.map(_.id))))(
              forall(not(contains(TestValidationManager.invalidAttribute)))
            ) &&
            assert(activeOffers.map(offer => (offer.attributes.map(_.id))))(forall(contains("valid_attr")))
        }
      },
      testM("Создает новые объявления без фото для avito-parsed и фильтрует невалидные атрибуты") {
        (checkNM(1): CheckVariants.CheckNM)(
          Gen.listOfN(10)(FeedOfferGen.normalFeedOffer.noShrink).noShrink,
          SellerGen.anySellerId.noShrink
        ) { (normalOffers, sellerId) =>
          val offers = normalOffers.map(feed =>
            feed.copy(
              offer = feed.offer.map(offer =>
                offer.copy(
                  attributes = offer.attributes + Attributes(
                    Seq(
                      Attribute(
                        id = TestValidationManager.invalidAttribute,
                        version = 0,
                        AttributeValue.BooleanValue(true)
                      ),
                      Attribute(id = "valid_attr", version = 0, AttributeValue.Empty)
                    )
                  ),
                  photos = Seq.empty
                )
              ),
              errors = Seq()
            )
          )
          for {
            result <- FeedOfferManager.processOffers(sellerId, NamespaceId("avito-parsed"), 1, offers)
            withoutPhotos <- OfferManager.getPresetOffers(sellerId, Preset.WithoutPhotos, LimitOffset(100, 0))
            allOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(result)(
            equalTo(
              FeedOffersProcessingResult(
                failedOffers = Seq.empty,
                totalOfferCount = 10,
                activeOfferCount = 10,
                errorCount = 0,
                criticalErrorCount = 0
              )
            )
          ) && assert(withoutPhotos)(hasSize(equalTo(10))) &&
            assert(allOffers)(hasSize(equalTo(10))) &&
            assert(withoutPhotos.map(offer => (offer.attributes.map(_.id))))(
              forall(not(contains(TestValidationManager.invalidAttribute)))
            ) &&
            assert(withoutPhotos.map(offer => (offer.attributes.map(_.id))))(forall(contains("valid_attr")))
        }
      },
      testM("Не создает объявления, в которых отсутствует обязательные атрибуты") {
        (checkNM(1): CheckVariants.CheckNM)(
          Gen.listOfN(10)(FeedOfferGen.normalFeedOffer.noShrink).noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (normalOffers, sellerId, namespaceId) =>
          val offers = normalOffers.map(feed =>
            feed.copy(
              offer = feed.offer.map(offer =>
                offer.copy(
                  category = offer.category.copy(id = TestValidationManager.categoryWithRequiredAttribute),
                  attributes = offer.attributes + Attributes(
                    Seq(
                      Attribute(
                        id = TestValidationManager.invalidAttribute,
                        version = 0,
                        AttributeValue.BooleanValue(true)
                      ),
                      Attribute(id = "valid_attr", version = 0, AttributeValue.Empty)
                    )
                  ),
                  photos = Seq(IncompletePhoto(url = "http://www.strangeurl.ru", error = false))
                )
              ),
              errors = Seq()
            )
          )
          for {
            result <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, offers)
            activeOffers <- OfferManager.getPresetOffers(sellerId, Preset.Active, LimitOffset(100, 0))
            failedOffers = offers.map(offer =>
              FailedFeedOffer(
                offer.externalId,
                NonEmptyList.of(
                  FeedOfferError(
                    offer.offer.get.title,
                    ErrorLevel.Error,
                    ValidationErrorInfo(
                      AttributeValueRequired(
                        TestValidationManager.missingAttribute,
                        TestValidationManager.missingAttribute
                      )
                    )
                  )
                )
              )
            )
          } yield assert(result)(
            equalTo(
              FeedOffersProcessingResult(
                failedOffers = failedOffers,
                totalOfferCount = 10,
                activeOfferCount = 0,
                errorCount = 10,
                criticalErrorCount = 10
              )
            )
          ) && assert(activeOffers)(isEmpty)
        }
      },
      testM("Создает новые валидные объявления и пропускает невалидные") {
        (checkNM(1): CheckVariants.CheckNM)(
          Gen.listOfN(10)(FeedOfferGen.feedOfferWithPhoto).noShrink,
          Gen.listOfN(10)(FeedOfferGen.failedFeedOffer).noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (normalOffers, failedOffers, sellerId, namespaceId) =>
          for {
            offers <- random.shuffle(normalOffers ++ failedOffers)
            result <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, offers)
            allErrorCount = offers.flatMap(_.errors).size
            criticalErrorCount = offers.flatMap(_.errors).count(_.errorLevel == ErrorLevel.Error)
            failedOffers = offers.flatMap(offer =>
              NonEmptyList.fromList(offer.errors.toList).map(errors => FailedFeedOffer(offer.externalId, errors))
            )
            activeOffers <- OfferManager.getPresetOffers(sellerId, Preset.Active, LimitOffset(100, 0))
            allOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(result)(
            equalTo(
              FeedOffersProcessingResult(
                failedOffers = failedOffers,
                totalOfferCount = 20,
                activeOfferCount = 10,
                errorCount = allErrorCount,
                criticalErrorCount = criticalErrorCount
              )
            )
          ) && assert(activeOffers)(hasSize(equalTo(10))) &&
            assert(allOffers)(hasSize(equalTo(10))) &&
            assert(activeOffers.map(offer => (offer.title, offer.description)))(
              hasSameElements(
                normalOffers.map(feedOffer => (feedOffer.offer.get.title, feedOffer.offer.get.description))
              )
            )
        }
      },
      testM("Не обновляет метаданные, если унификация была неуспешна") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, List(offer))
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 2, List(offer.copy(offer = None)))
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasFeedInfo(1L))
        }
      },
      testM("Создает метаданные и объявление, если его не было и оно прошло валидацию") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("INITIAL") && hasFeedInfo(1L)) &&
            assert(processResult)(hasCounters(1, 1, 0))
        }
      },
      testM("Делает ретраи при параллельном создании объявления") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- ZIO.foreachPar_(0 until 3)(i =>
              FeedOfferManager.processOffers(
                sellerId,
                namespaceId,
                i,
                List(offer.copy(offer = Some(offer.offer.get.copy(title = i.toString))))
              )
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("2") && hasFeedInfo(2L))
        }
      },
      testM("Делает ретраи при параллельном обновлении объявления") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              0,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "-1"))))
            )
            _ <- ZIO.foreachPar_(0 until 3)(i =>
              FeedOfferManager.processOffers(
                sellerId,
                namespaceId,
                i + 1,
                List(offer.copy(offer = Some(offer.offer.get.copy(title = i.toString))))
              )
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("2") && hasFeedInfo(3L))
        }
      },
      testM("Не создает метаданные и объявление, если его не было и оно не прошло валидацию") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = TestValidationManager.badTitle))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(isEmpty) && assert(processResult)(hasCounters(1, 0, 1))
        }
      },
      testM("Не обновляет метаданные и объявление, если оно не забанено и не прошло валидацию") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              2,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = TestValidationManager.badTitle))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("INITIAL") && hasFeedInfo(1L)) &&
            assert(processResult)(hasCounters(1, 0, 1))
        }
      },
      testM("Обновляет метаданные и объявление, если оно не забанено и прошло валидацию") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              2,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "UPDATED"))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("UPDATED") && hasFeedInfo(2L)) &&
            assert(processResult)(hasCounters(1, 1, 0))
        }
      },
      testM("Не обновляет метаданные и объявление, если оно не забанено и не прошло валидацию") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              2,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = TestValidationManager.badTitle))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("INITIAL") && hasFeedInfo(1L)) &&
            assert(processResult)(hasCounters(1, 0, 1))
        }
      },
      testM("Обновляет забаненные объявления и метаданные, если причина редактируема и обновление прошло валидацию") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, List(offer))
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- updateModerationOpinion(
              initialOffer.offerId,
              sellerId,
              ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.editableReason.code, Nil), false),
              None,
              None,
              None,
              Instant.MIN
            )
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              2,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "UPDATED"))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("UPDATED") && hasFeedInfo(2L)) &&
            assert(processResult)(hasCounters(1, 1, 0))
        }
      },
      testM(
        "Не обновляет забаненные объявления и обновляет метаданные, если причина редактируема и обновление не прошло валидацию"
      ) {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- updateModerationOpinion(
              initialOffer.offerId,
              sellerId,
              ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.editableReason.code, Nil), false),
              None,
              None,
              None,
              Instant.MIN
            )
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              2,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = TestValidationManager.badTitle))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("INITIAL") && hasFeedInfo(2L)) &&
            assert(processResult)(hasCounters(1, 0, 1))
        }
      },
      testM("Не обновляет забаненные объявления и обновляет метаданные, если причина не редактируема") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- updateModerationOpinion(
              initialOffer.offerId,
              sellerId,
              ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.nonEditableReason.code, Nil), false),
              None,
              None,
              None,
              Instant.MIN
            )
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              2,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "UPDATED"))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("INITIAL") && hasFeedInfo(2L)) &&
            assert(processResult)(hasCounters(1, 0, 1))
        }
      },
      testM("Воскрешает забаненные объявления, если причина редактируема") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          val validOffer = offer.copy(offer = offer.offer.map(_.copy(title = "INITIAL")))
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(validOffer)
            )
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 1)
            banReasons = NonEmptyList.one(TestBansDictionaryService.editableReason.code)
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- updateModerationOpinion(
              initialOffer.offerId,
              sellerId,
              ModerationOpinion.Failed(banReasons, false),
              None,
              None,
              None,
              Instant.MIN
            )

            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 2) // удаляем объявление

            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              3,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "UPDATED"))))
            )
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 3)
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(
              hasTitle("UPDATED") && hasFeedInfo(3L) &&
                hasStatus(OfferStatus.Banned(BanInfo(false, banReasons, OfferStatus.Active)))
            ) &&
            assert(processResult)(hasCounters(1, 1, 0))
        }
      },
      testM("Воскрешает забаненные объявления, если причина не редактируема") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          val validOffer = offer.copy(offer = offer.offer.map(_.copy(title = "INITIAL")))
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(validOffer)
            )
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 1)
            banReasons = NonEmptyList.one(TestBansDictionaryService.nonEditableReason.code)
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- updateModerationOpinion(
              initialOffer.offerId,
              sellerId,
              ModerationOpinion.Failed(banReasons, false),
              None,
              None,
              None,
              Instant.MIN
            )

            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 2) // удаляем объявление

            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              3,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "UPDATED"))))
            )
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 3)
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(
              hasTitle("INITIAL") && hasFeedInfo(3L) &&
                hasStatus(OfferStatus.Banned(BanInfo(false, banReasons, OfferStatus.Active)))
            ) &&
            assert(processResult)(hasCounters(1, 0, 1))
        }
      },
      testM(
        "Удаляет объявления, если они есть в загрузке, но в них присутствуют критические ошибки"
      ) {
        (checkNM(1): CheckVariants.CheckNM)(
          Gen.listOfN(10)(FeedOfferGen.feedOfferWithPhoto).noShrink,
          Gen.listOfN(10)(FeedOfferGen.failedFeedOffer).noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (normalOffers, failedOffers, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, normalOffers)
            newOffers = normalOffers.zip(failedOffers).zipWithIndex.map {
              case ((normalOffer, failedOffer), i) if i % 2 == 0 =>
                failedOffer.copy(externalId = normalOffer.externalId)
              case ((normalOffer, _), _) =>
                normalOffer.copy(offer = Some(normalOffer.offer.get.copy(title = "UPDATED")))
            }
            result <- FeedOfferManager.processOffers(sellerId, namespaceId, 2, newOffers)
            allErrorCount = newOffers.flatMap(_.errors).size
            criticalErrorCount = newOffers.flatMap(_.errors).count(_.errorLevel == ErrorLevel.Error)
            failedOffers = newOffers.flatMap(offer =>
              NonEmptyList.fromList(offer.errors.toList).map(errors => FailedFeedOffer(offer.externalId, errors))
            )
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 2)
            activeOffers <- OfferManager.getPresetOffers(sellerId, Preset.Active, LimitOffset(100, 0))
            allOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(result)(
            equalTo(
              FeedOffersProcessingResult(
                failedOffers = failedOffers,
                totalOfferCount = 10,
                activeOfferCount = 5,
                errorCount = allErrorCount,
                criticalErrorCount = criticalErrorCount
              )
            )
          ) && assert(activeOffers)(hasSize(equalTo(5))) &&
            assert(allOffers)(hasSameElements(activeOffers)) &&
            assert(allOffers)(forall(hasFeedInfo(2L))) &&
            assert(allOffers)(forall(hasField("title", _.title, equalTo("UPDATED"))))
        }
      },
      testM("Удаляет объявления, которых не было в последней загрузке") {
        (checkNM(1): CheckVariants.CheckNM)(
          Gen.listOfN(10)(FeedOfferGen.feedOfferWithPhoto).noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (normalOffers, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, normalOffers)
            updates = normalOffers
              .take(5)
              .map(feedOffer => feedOffer.copy(offer = Some(feedOffer.offer.get.copy(title = "UPDATED"))))
            result <- FeedOfferManager.processOffers(sellerId, namespaceId, 2, updates)
            allErrorCount = updates.flatMap(_.errors).size
            failedOffers = updates.flatMap(offer =>
              NonEmptyList.fromList(offer.errors.toList).map(errors => FailedFeedOffer(offer.externalId, errors))
            )
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 2)
            allOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
            offersInFeedDao <- FeedIdsMappingDao.listLessThanTaskId(sellerId, namespaceId, 10).runCollect
          } yield assert(result)(
            equalTo(
              FeedOffersProcessingResult(
                failedOffers = failedOffers,
                totalOfferCount = 5,
                activeOfferCount = 5,
                errorCount = allErrorCount,
                criticalErrorCount = 0
              )
            )
          ) && assert(allOffers)(hasSize(equalTo(5))) &&
            assert(allOffers)(
              forall(hasField("feedInfo", _.feedInfo, isSome[FeedInfo](hasField("taskId", _.taskId, equalTo(2L)))))
            ) &&
            assert(allOffers)(forall(hasField("title", _.title, equalTo("UPDATED")))) &&
            assert(offersInFeedDao)(hasSize(equalTo(5))) &&
            assert(offersInFeedDao.map(_.offerId))(hasSameElements(allOffers.map(_.offerId)))
        }
      },
      testM("Объявление, удаленное через ЛК, создается c прежним id") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, List(offer))
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- OfferManager.updateOffer(
              initialOffer.offerId,
              sellerId,
              Editors.seller(sellerId)
            )((offer: Offer) => ZIO.succeed(offer.copy(status = OfferStatus.Removed(OfferStatus.Active))))
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 2, List(offer))
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers)(hasAt(0)(hasField("offerId", _.offerId, equalTo(initialOffer.offerId))))
        }
      },
      testM("Объявление, удаленное через фиды, создается c прежним id") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, List(offer))
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 1)
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 2, List())
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 2)
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 3, List(offer))
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 3)
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers)(hasAt(0)(hasField("offerId", _.offerId, equalTo(initialOffer.offerId))))
        }
      },
      testM("Объявление, удаленное через фиды, подтягивает прошлые опинионы модерации") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, List(offer))
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 1)
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- updateModerationOpinion(
              initialOffer.offerId,
              sellerId,
              ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.editableReason.code, Nil), false),
              None,
              None,
              None,
              Instant.MIN
            )
            bannedOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 2, List())
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 2)
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 3, List(offer))
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 3)
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers)(
              hasAt(0)(
                hasField[Offer, OfferId]("offerId", _.offerId, equalTo(initialOffer.offerId)) &&
                  hasField[Offer, OfferStatus]("status", _.status, equalTo(bannedOffer.status)) &&
                  hasField[Offer, ModerationInfo]("moderation", _.moderationInfo, equalTo(bannedOffer.moderationInfo))
              )
            )
        }
      },
      testM("Объявление, удаленное через фиды, подтягивает залоченную категорию и обновляет состояние") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink,
          OfferGen.predefCategory.noShrink
        ) { (offer, sellerId, namespaceId, lockedCategory) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, List(offer))
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 1)
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- OfferManager.changeOfferCategory(
              initialOffer.offerId,
              sellerId,
              Editors.moderator("Чебурашка"),
              lockedCategory.id,
              None,
              lockCategory = true
            )
            offerWithLockedCategory <- OfferManager
              .getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
              .map(_.head)
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 2, List(offer))
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 2)
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers)(
              hasAt(0)(
                hasOfferId(offerWithLockedCategory.offerId) &&
                  hasCategory(offerWithLockedCategory.category) &&
                  hasCondition(offerWithLockedCategory.condition)
              )
            )
        }
      },
      testM("Пропускает объявление, если оно никак не изменилось") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              1,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 1)

            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              2,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            initial <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 2)
            processResult <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              3,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "INITIAL"))))
            )
            _ <- FeedOfferManager.finishTask(sellerId, namespaceId, 3)
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))

          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(equalTo(initial.head.copy(feedInfo = resultOffers.head.feedInfo))) &&
            assert(processResult)(hasCounters(1, 1, 0)) &&
            assert(resultOffers.head)(hasFeedInfo(3L))
        }
      },
      testM("Сохраняем объявления скрытые модерацией скрытыми при загрузке фида") {
        (checkNM(1): CheckVariants.CheckNM)(
          FeedOfferGen.feedOfferWithPhoto.noShrink,
          SellerGen.anySellerId.noShrink,
          FeedInfoGen.anyNamespaceIdGen.noShrink
        ) { (offer, sellerId, namespaceId) =>
          for {
            _ <- FeedOfferManager.processOffers(sellerId, namespaceId, 1, List(offer))
            initialOffer <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0)).map(_.head)
            _ <- updateModerationOpinion(
              initialOffer.offerId,
              sellerId,
              ModerationOpinion.Failed(NonEmptyList(TestBansDictionaryService.exclusionReason.code, Nil), false),
              None,
              None,
              None,
              Instant.MIN
            )
            _ <- FeedOfferManager.processOffers(
              sellerId,
              namespaceId,
              2,
              List(offer.copy(offer = Some(offer.offer.get.copy(title = "UPDATED"))))
            )
            resultOffers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
          } yield assert(resultOffers)(hasSize(equalTo(1))) &&
            assert(resultOffers.head)(hasTitle("UPDATED") && hasFeedInfo(2L)) &&
            assert(resultOffers.head.status)(isSubtype[OfferStatus.Inactive](anything))
        }
      }
    ) @@ sequential
  }.provideCustomLayer {
    val dict = TestBansDictionaryService.layer
    val logging = Logging.live
    val ydb: ZLayer[Any, Nothing, Ydb] = TestYdb.ydb
    val txRunner = ydb >>> Ydb.txRunner
    val clock = Clock.live
    val random = Random.live
    val cacher = Cache.noop ++ logging >>> RequestCacher.live
    val offerDao = YdbOfferDao.live
    val ydbFeedIdsMappingDao: ZLayer[Ydb, Nothing, FeedIdsMappingDao] = YdbFeedIdsMappingDao.live
    val presetsDao = YdbOfferPresetDao.live
    val presetsCountDao = YdbOfferPresetsCountDao.live
    val totalCountersDao = YdbTotalCountersDao.live
    val totalCountersStore = totalCountersDao >>> TotalCountersStore.live
    val presetManager = (presetsDao ++ presetsCountDao) >+> OfferPresetsStore.live
    val offerStore =
      (offerDao ++ YdbQueueDao.live ++ ydbFeedIdsMappingDao ++ presetManager ++ totalCountersStore ++ clock) >>> OfferStore.live
    val offerIdGenerator = IdGenerator.snowflake >>> OfferIdGenerator.live
    val sellingAddressEnricher = TestGeoService.layer ++ cacher >>> SellingAddressEnricher.live
    val bonsaiSnapshot = Ref.make(Data.bonsaiSnapshotMock).toLayer
    val deps =
      ydb >>> (offerDao ++ ydbFeedIdsMappingDao ++ totalCountersDao ++ presetManager ++ offerStore ++ txRunner) ++
        clock ++ logging ++ TestValidationManager.layer ++ (random ++ clock >>> offerIdGenerator) ++
        sellingAddressEnricher ++ dict ++ bonsaiSnapshot ++ ChangeCategoryEventSender.noop
    (deps >>> OfferManager.live) ++ (deps >>> FeedOfferManager.live) ++
      (ydb >>> offerDao) ++ (ydb >>> ydbFeedIdsMappingDao) ++ txRunner ++ random ++ deps
  }
}
