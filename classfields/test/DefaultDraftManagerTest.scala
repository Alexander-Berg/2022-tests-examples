package ru.yandex.vertis.general.gost.logic.test

import common.id.IdGenerator
import common.zio.ydb.Ydb
import common.zio.ydb.testkit.TestYdb
import general.bonsai.category_model.Category
import general.bonsai.export_model.ExportedEntity
import general.bonsai.export_model.ExportedEntity.CatalogEntity.{Category => ExportCategory}
import general.common.editor_model.SellerEditor
import general.gost.storage.ydb.feed.YdbFeedIdsMappingDao
import general.users.model.{ModerationInfo, ModerationStatus, OffersPublishModeration, User, UserView}
import ru.yandex.vertis.general.bonsai.public.BonsaiSnapshot
import ru.yandex.vertis.general.common.dictionaries.testkit.TestBansDictionaryService
import ru.yandex.vertis.general.common.model.editor.testkit.Editors
import ru.yandex.vertis.general.common.model.pagination.LimitOffset
import ru.yandex.vertis.general.common.model.user.{OwnerId, SellerId}
import ru.yandex.vertis.general.globe.testkit.TestGeoService
import ru.yandex.vertis.general.gost.logic._
import ru.yandex.vertis.general.gost.logic.testkit.TestValidationManager
import ru.yandex.vertis.general.gost.model.{DraftUpdate, Preset, Price, WayToContact}
import ru.yandex.vertis.general.gost.model.PublishResult.{Banned, Successful}
import ru.yandex.vertis.general.gost.model.attributes.Attributes
import ru.yandex.vertis.general.gost.model.testkit.Data
import ru.yandex.vertis.general.gost.storage.ydb.counters.YdbTotalCountersDao
import ru.yandex.vertis.general.gost.storage.ydb.feed.{YdbActiveFeedOfferDao, YdbFeedOfferDao}
import ru.yandex.vertis.general.gost.storage.ydb.offer.YdbOfferDao
import ru.yandex.vertis.general.gost.storage.ydb.preset.{YdbOfferPresetDao, YdbOfferPresetsCountDao}
import ru.yandex.vertis.general.gost.storage.ydb.{YdbDraftDao, YdbQueueDao}
import ru.yandex.vertis.general.users.testkit.TestUserService
import common.zio.logging.Logging
import ru.yandex.vertis.general.common.cache.{Cache, RequestCacher}
import zio.{Ref, ZIO}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test._

object DefaultDraftManagerTest extends DefaultRunnableSpec {

  private val publishDraftUserId: Long = 678
  private val publishDraftConcurrentlyUserId: Long = 777
  private val bannedUserId: Long = 666
  private val userIdWithPhoneRedirectOn: Long = 19348267
  private val userIdWithPhoneRedirectOff: Long = 59276

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {

    (suite("DefaultDraftManager")(
      testM("create new draft with set isPhoneRedirectEnabled") {
        for {
          redirectOnDraft <- DraftManager.currentDraft(OwnerId.UserId(userIdWithPhoneRedirectOn), categoryId = None)
          redirectOffDraft <- DraftManager.currentDraft(OwnerId.UserId(userIdWithPhoneRedirectOff), categoryId = None)
        } yield assert(redirectOnDraft.draft.isPhoneRedirectEnabled)(equalTo(Some(true))) &&
          assert(redirectOffDraft.draft.isPhoneRedirectEnabled)(equalTo(Some(false)))
      },
      testM("Update draft") {
        val update = DraftUpdate(
          title = Some("update-new"),
          description = "update new description",
          categoryId = None,
          marketSkuId = None,
          attributes = Attributes.empty,
          photos = Seq.empty,
          video = None,
          price = Price.Unset,
          addresses = Seq.empty,
          contacts = Seq.empty,
          preferredWayToContact = WayToContact.Any,
          isPhoneRedirectEnabled = Some(true),
          condition = None,
          currentControlNum = 0,
          categoryPreset = None,
          deliveryInfo = None
        )
        val ownerId = OwnerId.UserId(123)
        for {
          draftId <- DraftManager.currentDraft(ownerId, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(ownerId, draftId, update)
          savedOffer <- DraftManager.currentDraft(ownerId, categoryId = None).map(_.draft)
        } yield assert(savedOffer.title)(equalTo(update.title)) &&
          assert(savedOffer.description)(equalTo(update.description))
      },
      testM("Delete draft") {
        val ownerId = OwnerId.UserId(345)
        for {
          draftId <- DraftManager.currentDraft(ownerId, categoryId = None).map(_.draft.id)
          _ <- DraftManager.deleteDraft(ownerId, draftId)
          deleted <- DraftManager.currentDraft(ownerId, categoryId = None).map(_.draft)
        } yield assert(deleted.id)(not(equalTo(draftId)))
      },
      testM("publish offer") {
        val ownerId = OwnerId.UserId(publishDraftUserId)
        val sellerId = SellerId.UserId(publishDraftUserId)
        for {
          draftId <- DraftManager.currentDraft(ownerId, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(ownerId, draftId, Data.validDraftUpdate)
          response <- DraftManager.publishDraft(ownerId, draftId, sellerId, Editors.seller(sellerId))
        } yield assert(response)(isSubtype[Successful](anything))
      },
      testM("concurrent offer publishing") {
        val ownerId = OwnerId.UserId(publishDraftConcurrentlyUserId)
        val sellerId = SellerId.UserId(publishDraftConcurrentlyUserId)
        for {
          draftId <- DraftManager.currentDraft(ownerId, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(ownerId, draftId, Data.validDraftUpdate)
          responses <- ZIO.foreachPar((0 until 3).toList)(_ =>
            DraftManager.publishDraft(ownerId, draftId, sellerId, Editors.seller(sellerId))
          )
          offers <- OfferManager.getPresetOffers(sellerId, Preset.All, LimitOffset(100, 0))
        } yield assert(offers)(hasSize(equalTo(1))) &&
          assert(responses)(forall(equalTo(Successful(offers.head.offerId))))
      },
      testM("Can't publish offer when banned") {
        val ownerId = OwnerId.UserId(bannedUserId)
        val sellerId = SellerId.UserId(bannedUserId)
        for {
          draftId <- DraftManager.currentDraft(ownerId, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(ownerId, draftId, Data.validDraftUpdate)
          result <- DraftManager.publishDraft(ownerId, draftId, sellerId, Editors.seller(sellerId))
        } yield assert(result)(isSubtype[Banned](anything))
      },
      testM("pass draft from anonymous to user") {
        val anon = OwnerId.Anonymous("au1_p")
        val userId = OwnerId.UserId(90, Some("au1_p"))

        for {
          aDraft <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          passed <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)
          aDraft2 <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(userId, passed, Data.validDraftUpdate)
        } yield assert(aDraft)(equalTo(passed)) && assert(aDraft)(not(equalTo(aDraft2)))
      },
      testM("Choose latest draft when anonymous created after user") {
        val anon = OwnerId.Anonymous("au1_a")
        val userId = OwnerId.UserId(999, Some("au1_a"))
        for {
          uDraft <- DraftManager.currentDraft(userId.copy(anonymous = None), categoryId = None).map(_.draft.id)
          aDraft <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          passed <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)
        } yield assert(passed)(equalTo(aDraft)) && assert(passed)(not(equalTo(uDraft)))
      },
      testM("choose latest draft when passing from anonymous") {
        val anon = OwnerId.Anonymous("au1_2")
        val userId = OwnerId.UserId(876, Some("au1_2"))
        for {
          uDraft <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)
          aDraft <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          passed <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)

          _ <- DraftManager.deleteDraft(userId, passed)
          aDraft2 <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          uDraft2 <- DraftManager.currentDraft(userId.copy(anonymous = None), categoryId = None).map(_.draft.id)
          passed2 <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)

        } yield assert(passed)(equalTo(aDraft)) && assert(passed)(not(equalTo(uDraft))) &&
          assert(passed2)(equalTo(uDraft2)) && assert(passed2)(not(equalTo(aDraft2)))
      },
      testM("take anon draft if it suit category") {
        val anon = OwnerId.Anonymous("au1337")
        val userId = OwnerId.UserId(1337, Some("au1337"))
        for {
          uDraft <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)
          aDraft <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(anon, aDraft, Data.validDraftUpdate)
          passed <- DraftManager.currentDraft(userId, categoryId = Data.validDraftUpdate.categoryId).map(_.draft.id)

        } yield assert(passed)(equalTo(aDraft)) && assert(passed)(not(equalTo(uDraft)))
      },
      testM("take user draft if it suit category") {
        val anon = OwnerId.Anonymous("au1338")
        val userId = OwnerId.UserId(1338, Some("au1338"))
        for {
          uDraft <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(userId, uDraft, Data.validDraftUpdate)
          aDraft <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          passed <- DraftManager.currentDraft(userId, categoryId = Data.validDraftUpdate.categoryId).map(_.draft.id)

        } yield assert(passed)(equalTo(uDraft)) && assert(passed)(not(equalTo(aDraft)))
      },
      testM("take anon draft if both drafts suit but anon is newer") {
        val anon = OwnerId.Anonymous("au1339")
        val userId = OwnerId.UserId(1339, Some("au1339"))
        for {
          uDraft <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(userId, uDraft, Data.validDraftUpdate)
          aDraft <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(anon, aDraft, Data.validDraftUpdate)
          passed <- DraftManager.currentDraft(userId, categoryId = Data.validDraftUpdate.categoryId).map(_.draft.id)

        } yield assert(passed)(equalTo(aDraft)) && assert(passed)(not(equalTo(uDraft)))
      },
      testM("keeps user draft if no other suit the requested category") {
        val anon = OwnerId.Anonymous("au1340")
        val userId = OwnerId.UserId(1340, Some("au1340"))
        for {
          uDraft <- DraftManager.currentDraft(userId, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(userId, uDraft, Data.validDraftUpdate)
          aDraft <- DraftManager.currentDraft(anon, categoryId = None).map(_.draft.id)
          _ <- DraftManager.updateDraft(anon, aDraft, Data.validDraftUpdate)
          passed <- DraftManager.currentDraft(userId, categoryId = Some("other_category")).map(_.draft.id)

        } yield assert(passed)(not(equalTo(aDraft))) && assert(passed)(equalTo(uDraft))
      }
    ) @@ sequential).provideCustomLayerShared {
      val dict = TestBansDictionaryService.emptyLayer
      val clock = Clock.live
      val logging = Logging.live
      val offerDao = YdbOfferDao.live
      val ydbFeedIdsMappingDao = YdbFeedIdsMappingDao.live
      val users = TestUserService.withUsers(
        Seq(
          UserView(publishDraftUserId),
          UserView(publishDraftConcurrentlyUserId),
          UserView(
            bannedUserId,
            moderationInfo =
              Some(ModerationInfo(Some(OffersPublishModeration(ModerationStatus.BANNED, Seq.empty)), None))
          ),
          UserView(userIdWithPhoneRedirectOn, user = Some(User(isPhoneRedirectEnabled = Some(true)))),
          UserView(userIdWithPhoneRedirectOff, user = Some(User(isPhoneRedirectEnabled = Some(false))))
        )
      )
      val presetsDao = YdbOfferPresetDao.live
      val presetsCountDao = YdbOfferPresetsCountDao.live
      val presetManager = (presetsDao ++ presetsCountDao) >+> OfferPresetsStore.live
      val totalCountersDao = YdbTotalCountersDao.live
      val totalCountersStore = totalCountersDao >>> TotalCountersStore.live
      val offerStore =
        (offerDao ++ YdbQueueDao.live ++ ydbFeedIdsMappingDao ++ presetManager ++ totalCountersStore ++ clock) >>> OfferStore.live
      val draftIdGenerator = IdGenerator.snowflake >>> DraftIdGenerator.live
      val offerIdGenerator = IdGenerator.snowflake >>> OfferIdGenerator.live
      val categoryData =
        ExportedEntity(catalogEntity = ExportCategory(Category(id = Data.validDraftUpdate.categoryId.get)))
      val bonsaiSnapshot = Ref.make(BonsaiSnapshot(Seq(categoryData))).toLayer
      val cacher = Cache.noop ++ logging >>> RequestCacher.live

      val deps =
        TestYdb.ydb >>> (YdbDraftDao.live ++ offerDao ++ ydbFeedIdsMappingDao ++ presetManager ++ offerStore ++ totalCountersDao ++ Ydb.txRunner) ++
          clock ++ logging ++ TestValidationManager.layer ++ (TestGeoService.layer ++ cacher >>> SellingAddressEnricher.live) ++
          dict ++ users ++ bonsaiSnapshot ++ ChangeCategoryEventSender.noop
      ((deps ++ draftIdGenerator ++ offerIdGenerator) >>> (DraftManager.live ++ OfferManager.live)) ++ deps
    }
  }

}
