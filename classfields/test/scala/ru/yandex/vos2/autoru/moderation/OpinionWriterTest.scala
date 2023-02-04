package ru.yandex.vos2.autoru.moderation

import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{eq => eeq}
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import ru.auto.api.ApiOfferModel.Section
import ru.auto.api.ModerationFieldsModel.ModerationFields
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.moderation.proto.Model.Metadata.ProvenOwnerMetadata
import ru.yandex.vertis.moderation.proto.Model.Opinion.Type._
import ru.yandex.vertis.moderation.proto.Model.Reason._
import ru.yandex.vertis.moderation.proto.Model.{DetailedReason, Diff, Metadata, Opinion, Reason}
import ru.yandex.vertis.moderation.proto.Model.Diff.Autoru.Value._
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.AutoruModel.AutoruOffer.{Notification, UserChangeAction}
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag, OfferStatusHistoryItem}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.{AutoruOfferID, AutoruSaleStatus, TestUtils}
import ru.yandex.vos2.autoru.services.moderation.OpinionWriter
import ru.yandex.vos2.autoru.services.moderation.OpinionWriter.OpinionWrapper
import ru.yandex.vos2.autoru.utils.testforms.CommonTestForms
import ru.yandex.vos2.dao.offers.OfferUpdate.OfferUpdate
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.ModelUtils.{RichOffer, RichOfferBuilder}
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.util.Dates._
import ru.yandex.vos2.util.{ExternalAutoruUserRef, RandomUtil}

import java.time.Instant
import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by sievmi on 31.08.18
  */
class OpinionWriterTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with InitTestDbs
  with BeforeAndAfterEach
  with BeforeAndAfter
  with TestOperationalSupport {

  implicit private val t: Traced = Traced.empty
  initDbs()

  override protected def beforeEach(): Unit = {
    components.autoruSalesDao.setStatus(
      id = AutoruOfferID.parse(dealerSale.getOfferID).id,
      expectedStatuses = Seq(),
      newStatus = AutoruSaleStatus.STATUS_SHOW
    )

    components.autoruSalesDao.setStatus(
      id = AutoruOfferID.parse(bannedSale.getOfferID).id,
      expectedStatuses = Seq(),
      newStatus = AutoruSaleStatus.STATUS_MODERATOR_DELETED
    )

    components.featureRegistry.updateFeature(components.featuresManager.DuplicateBanNotification.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.UserBatchNotifications.name, true)
    components.featureRegistry.updateFeature(components.featuresManager.ResellerProtectionEnabled.name, true)
  }

  after {
    components.featureRegistry.updateFeature(components.featuresManager.DuplicateBanNotification.name, false)
    components.featureRegistry.updateFeature(components.featuresManager.DuplicateBanNotification.name, false)
  }

  val opinionWriter =
    new OpinionWriter(components, prometheusRegistry, components.protectedResellerDecider, components.banStrategy)

  private case class TestCase(description: String,
                              offer: Offer,
                              entries: Seq[OpinionWrapper],
                              check: OfferUpdate => Boolean)

  private val nowTs = Instant.now

  abstract private class Fixture {
    val offer: Offer
    val opinions: Seq[OpinionWrapper]

    val withDefaultResellerDeactivationParams: Boolean = true
    if (withDefaultResellerDeactivationParams) {
      when(components.resellerDeactivationParams.startDate)
        .thenReturn(None)
      when(components.resellerDeactivationParams.endDate)
        .thenReturn(None)
      when(components.resellerDeactivationParams.regionIds)
        .thenReturn(Set.empty[Long])
      when(components.resellerDeactivationParams.ifExpRuns)
        .thenReturn(false)
    }

    lazy val result: OfferUpdate =
      opinionWriter.updateStateForOffer(offer, opinions, Map.empty)
  }

  "Offer" should {
    "be skipped" when {
      "offer is draft" in new Fixture {
        private val offerBuilder = TestUtils.createOffer(dealer = true)
        offerBuilder.putFlag(OfferFlag.OF_DRAFT)
        val offer: Offer = offerBuilder.build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(UNKNOWN), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.isEmpty)
      }

      "opinion unknown and offer is active" in new Fixture {
        val offer: Offer = TestUtils.createOffer(dealer = true).build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(UNKNOWN), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.isEmpty)
      }

      "opinion failed and offer already banned" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(dealer = true)
          .putFlag(OfferFlag.OF_BANNED)
          .build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.isEmpty)
      }

      "opinion failed but offer is from reseller and protected" in new Fixture {
        val offer: Offer = {
          val builder = TestUtils
            .createOffer(dealer = false)
          builder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(213L)
          builder.build()
        }

        override val withDefaultResellerDeactivationParams: Boolean = false
        when(components.resellerDeactivationParams.startDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.endDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.regionIds)
          .thenReturn(Set(3L))
        when(components.resellerDeactivationParams.ifExpRuns)
          .thenReturn(true)
        val opinion: Opinion =
          getOpinion(FAILED, isReseller = Some(true), isBanByInheritance = Some(false), Nil, reasons = USER_RESELLER)
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(opinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(!result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED))
        assert(!result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE))
      }

      "opinion failed but offer is from reseller and protected and inactive before" in new Fixture {
        val offer: Offer = {
          val builder = TestUtils
            .createOffer(dealer = false)
          builder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(213L)
          builder.clearFlag()
          builder.addFlag(OfferFlag.OF_INACTIVE)
          builder.build()
        }

        override val withDefaultResellerDeactivationParams: Boolean = false
        when(components.resellerDeactivationParams.startDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.endDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.regionIds)
          .thenReturn(Set(3L))
        when(components.resellerDeactivationParams.ifExpRuns)
          .thenReturn(true)
        val opinion: Opinion =
          getOpinion(FAILED, isReseller = Some(true), isBanByInheritance = Some(false), Nil, reasons = USER_RESELLER)
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(opinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE))
      }

      "opinion failed but offer is from reseller and protected before" in new Fixture {
        val offer: Offer = {
          val builder = TestUtils
            .createOffer(dealer = false)
          builder.getOfferAutoruBuilder.setIsProtectedReseller(true)
          builder.build()
        }

        override val withDefaultResellerDeactivationParams: Boolean = false
        when(components.resellerDeactivationParams.startDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.endDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.regionIds)
          .thenReturn(Set(3L))
        when(components.resellerDeactivationParams.ifExpRuns)
          .thenReturn(true)
        val opinion: Opinion =
          getOpinion(FAILED, isReseller = Some(true), isBanByInheritance = Some(false), Nil, reasons = USER_RESELLER)
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(opinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(
          !result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) && !result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE)
        )
      }

      "opinion failed but offer is from reseller and protected but feature false" in new Fixture {
        components.featureRegistry.updateFeature(components.featuresManager.ResellerProtectionEnabled.name, false)

        val offer: Offer = {
          val builder = TestUtils
            .createOffer(dealer = false)
          builder.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(213L)
          builder.build()
        }

        override val withDefaultResellerDeactivationParams: Boolean = false
        when(components.resellerDeactivationParams.startDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.endDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.regionIds)
          .thenReturn(Set(3L))
        when(components.resellerDeactivationParams.ifExpRuns)
          .thenReturn(true)
        val opinion: Opinion =
          getOpinion(FAILED, isReseller = Some(true), isBanByInheritance = Some(false), Nil, reasons = USER_RESELLER)
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(opinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) || result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE))
      }

      "opinion failed but offer is from reseller and protected before but inactive" in new Fixture {
        val offer: Offer = {
          val builder = TestUtils
            .createOffer(dealer = false)
          builder.getOfferAutoruBuilder.setIsProtectedReseller(true)
          builder.clearFlag()
          builder.addFlag(OfferFlag.OF_INACTIVE)
          builder.build()
        }

        override val withDefaultResellerDeactivationParams: Boolean = false
        when(components.resellerDeactivationParams.startDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.endDate)
          .thenReturn(None)
        when(components.resellerDeactivationParams.regionIds)
          .thenReturn(Set(3L))
        when(components.resellerDeactivationParams.ifExpRuns)
          .thenReturn(true)
        val opinion: Opinion =
          getOpinion(FAILED, isReseller = Some(true), isBanByInheritance = Some(false), Nil, reasons = USER_RESELLER)
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(opinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(
          result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE)
        )
      }

      "opinion failed and offer already deleted" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(dealer = true)
          .putFlag(OfferFlag.OF_DELETED)
          .build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.isEmpty)
      }

      "last moderation update ts > offset ts" in new Fixture {
        val offer: Offer = {
          val b = TestUtils.createOffer()
          b.getOfferAutoruBuilder.setLastModerationUpdate(getNow + 100000)
          b.build()
        }
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.isEmpty)
      }

      "opinion failed with reasons user_reseller and offer is from callcenter" in new Fixture {
        val offer: Offer = {
          val b = TestUtils.createOffer()
          b.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
          b.build()
        }
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, USER_RESELLER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === false)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === false)
        assert(result.getUpdate.get.getFlagCount === 0)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
        assert(result.getUpdate.get.getOfferAutoru.getIsProtectedReseller === true)
      }
    }

    "be banned" when {
      "dealer=true, opinion failed with reasons mos_ru_validation" in new Fixture {
        val offer: Offer = dealerSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, MOS_RU_VALIDATION), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === false)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }

      "opinion failed with reasons mos_ru_validation" in new Fixture {
        val offer: Offer = privateSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, MOS_RU_VALIDATION), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === false)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }

      "opinion failed with reason LOW_PRICE" in new Fixture {
        val offer: Offer = TestUtils.createOffer().addReasonsBan("xxx").build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, LOW_PRICE), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        // должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)
        assert(
          result.getUpdate.get.getOfferAutoru.getNotifications(0).getType ===
            Notification.NotificationType.MODERATION_BAN
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotifications(0)
            .getExtraArgs(0) === "low_price"
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotifications(0)
            .getExtraArgsCount === 1
        )

        // в причинах бана должна быть одна причина и больше никаких
        assert(result.getUpdate.get.getReasonsBanCount === 1)
        assert(result.getUpdate.get.getReasonsBan(0) === "low_price")

        // добавлена история статуса
        assert(result.getUpdate.get.getStatusHistoryList.asScala.last.getComment == "OpinionWriter: LOW_PRICE")
      }

      "dealer = true, section = new, reason = LOW_PRICE" in new Fixture {
        private val builder = TestUtils.createOffer(dealer = true)
        builder.addReasonsBan("xxx")
        builder.getOfferAutoruBuilder.setSection(Section.NEW)
        val offer: Offer = builder.build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, LOW_PRICE), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        // не должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
        // в причинах бана должна быть одна причина и больше никаких
        assert(result.getUpdate.get.getReasonsBanCount === 1)
        assert(result.getUpdate.get.getReasonsBan(0) === "low_price")

        // добавлена история статуса
        assert(result.getUpdate.get.getStatusHistoryList.asScala.last.getComment == "OpinionWriter: LOW_PRICE")

        // добавлена батчевая нотификация
        assert(
          components.notificationsDao
            .getNotSent(UserRef.from(offer.getUserRef), NotificationType.U_DEALER_NEW_LOW_PRICE_BAN)
            .nonEmpty
        )
      }

      "dealer is freezed (opinion failed with reason USER_BANNED)" in new Fixture {
        val offer: Offer = TestUtils.createOffer().addReasonsBan("xxx").build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, USER_BANNED), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        // не должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)

        // в причинах бана должна быть одна причина и больше никаких
        assert(result.getUpdate.get.getReasonsBanCount === 1)
        assert(result.getUpdate.get.getReasonsBan(0) === "user_banned")
      }

      "opinion failed with reasons sold and docs_sale" in new Fixture {
        val offer: Offer = privateSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, SOLD, DOCS_SALE), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)
      }

      "user is dealer and opinion failed with reason NO_ANSWER" in new Fixture {
        val offer: Offer = dealerSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, NO_ANSWER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }
    }

    "be unbanned" when {
      "opinion ok and offer already banned" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(dealer = true)
          .putFlag(OfferFlag.OF_BANNED)
          .build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(OK), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.getFlagCount === 1)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_NEED_ACTIVATION) === true)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === false)

        // добавлена история статуса
        assert(result.getUpdate.get.getStatusHistoryList.asScala.last.getComment == "OpinionWriter")
      }

      "opinion unknown and offer already banned" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(dealer = true)
          .putFlag(OfferFlag.OF_BANNED)
          .build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(UNKNOWN), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.getFlagCount === 1)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_NEED_ACTIVATION) === true)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === false)
      }

      "with prev status restore" in new Fixture {
        val offer: Offer = TestUtils
          .createOffer(dealer = true)
          .addStatusHistory(
            OfferStatusHistoryItem
              .newBuilder()
              .setOfferStatus(CompositeStatus.CS_REMOVED)
              .setTimestamp(getNow)
          )
          .putFlag(OfferFlag.OF_BANNED)
          .build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(UNKNOWN), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.getFlagCount === 1)
        assert(result.getUpdate.get.getFlag(0) === OfferFlag.OF_INACTIVE)
      }
    }

    "be revoked" when {
      "opinion failed with reason reseller" in new Fixture {
        val offer: Offer = privateSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, USER_RESELLER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }

      "opinion failed with reasons reseller and mos_ru_validation" in new Fixture {
        val offer: Offer = privateSale
        val opinions: Seq[OpinionWrapper] = Seq(
          OpinionWrapper(
            getOpinion(FAILED, USER_RESELLER, MOS_RU_VALIDATION),
            Seq.empty,
            Diff.Autoru.getDefaultInstance,
            getNow
          )
        )

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === false)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }

      "dealer=true, opinion failed with reasons no_answer and mos_ru_validation" in new Fixture {
        val offer: Offer = dealerSale
        val opinions: Seq[OpinionWrapper] = Seq(
          OpinionWrapper(
            getOpinion(FAILED, NO_ANSWER, MOS_RU_VALIDATION),
            Seq.empty,
            Diff.Autoru.getDefaultInstance,
            getNow
          )
        )

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === false)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }

      "opinion failed with reason sold" in new Fixture {
        val offer: Offer = privateSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, SOLD), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)
      }

      "opinion failed with reason reseller and offer already banned" in new Fixture {
        val offer: Offer = bannedSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, USER_RESELLER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }

      "user is private and opinion failed with reason NO_ANSWER" in new Fixture {
        val offer: Offer = privateSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, NO_ANSWER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)
      }

      "user is from call center, and opinion failed with reason NO_ANSWER" in new Fixture {
        val offer: Offer = {
          val b = privateSale.toBuilder()
          b.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
          b.build()
        }
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, NO_ANSWER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)
      }
    }

    "be unrevoked" when {
      "opinion failed and than unknown with reason reseller" in new Fixture {
        val offer: Offer = privateSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, USER_RESELLER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)

        val updatedOffer = result.getUpdate.get
        assert(updatedOffer.getOfferAutoru.getModerationRecallInfo.getIsRecalled)
        val newEntries: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(UNKNOWN, ANOTHER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        val updatedAndRestored = opinionWriter.updateStateForOffer(updatedOffer, newEntries, Map.empty).getUpdate.get

        assert(!updatedAndRestored.getOfferAutoru.getModerationRecallInfo.getIsRecalled)
        assert(updatedAndRestored.getOfferAutoru.getModerationRecallInfo.getOpinionHistoryCount == 2)
        assert(
          !updatedAndRestored.getOfferAutoru.getModerationRecallInfo.getOpinionHistoryList.asScala.last.getIsRecalledByOpinion
        )
        assert(updatedAndRestored.hasFlag(OfferFlag.OF_INACTIVE) === false)
      }

      "dealer=true, opinion failed with reasons no_answer and mos_ru_validation and then restore" in new Fixture {
        val offer: Offer = dealerSale
        val opinions: Seq[OpinionWrapper] = Seq(
          OpinionWrapper(
            getOpinion(FAILED, NO_ANSWER, MOS_RU_VALIDATION),
            Seq.empty,
            Diff.Autoru.getDefaultInstance,
            getNow
          )
        )

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === false)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)

        val updatedOffer = result.getUpdate.get
        assert(!updatedOffer.getOfferAutoru.getModerationRecallInfo.hasIsRecalled)
      }

      "opinion failed with reason reseller and offer already banned" in new Fixture {
        val offer: Offer = bannedSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, USER_RESELLER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)

        val updatedOffer = result.getUpdate.get
        assert(updatedOffer.getOfferAutoru.getModerationRecallInfo.getIsRecalled)
        val newEntries: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(UNKNOWN, ANOTHER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        val updatedAndRestored = opinionWriter.updateStateForOffer(updatedOffer, newEntries, Map.empty).getUpdate.get

        assert(!updatedAndRestored.getOfferAutoru.getModerationRecallInfo.getIsRecalled)
        assert(updatedAndRestored.getOfferAutoru.getModerationRecallInfo.getOpinionHistoryCount == 2)
        assert(
          !updatedAndRestored.getOfferAutoru.getModerationRecallInfo.getOpinionHistoryList.asScala.last.getIsRecalledByOpinion
        )
        assert(updatedAndRestored.hasFlag(OfferFlag.OF_INACTIVE) === false)
      }

      "do not unrecall if offer was hidden by api" in new Fixture {

        val privateBuilder = privateSale.toBuilder
        privateBuilder.getOfferAutoruBuilder.getModerationRecallInfoBuilder.setIsRecalled(true)
        privateBuilder.addFlag(OfferFlag.OF_INACTIVE)

        val userChangehistoryActivation = UserChangeAction
          .newBuilder()
          .setTimestamp(DateTime.now.minusMinutes(1).getMillis)
          .setActionType(UserChangeAction.UserChangeActionType.ACTIVATE)
        val userChangehistoryHidding = UserChangeAction
          .newBuilder()
          .setTimestamp(DateTime.now.getMillis)
          .setActionType(UserChangeAction.UserChangeActionType.HIDE)
        privateBuilder.getOfferAutoruBuilder.addUserChangeActionHistory(userChangehistoryActivation)
        privateBuilder.getOfferAutoruBuilder.addUserChangeActionHistory(userChangehistoryHidding)
        val offer: Offer = privateBuilder.build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(UNKNOWN, ANOTHER), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }
    }

    "be forced to chat_only" when {
      "opinion failed with reason reseller and detailed reason should_stay_active" in new Fixture {

        override val offer: Offer = privateSale
        private val detailed = DetailedReason
          .newBuilder()
          .setReason(USER_RESELLER)
          .setDetails {
            DetailedReason.Details
              .newBuilder()
              .setUserReseller {
                DetailedReason.Details.UserReseller
                  .newBuilder()
                  .setShouldStayActive(true)
              }
          }
          .build()
        val user: String = ExternalAutoruUserRef.fromUserRef(offer.getUserRef).get
        doNothing().when(components.chatClient).banUser(eeq(user))(?)
        override val opinions: Seq[OpinionWrapper] =
          Seq(
            OpinionWrapper(
              getDetailedOpinion(FAILED, List(USER_RESELLER), List(detailed)),
              Seq.empty,
              Diff.Autoru.getDefaultInstance,
              getNow
            )
          )

        val aro = result.getUpdate.get.getOfferAutoru
        assert(aro.getModerationProtectedFieldsList.contains(ModerationFields.Fields.CHAT_ONLY))
        assert(aro.getSeller.getChatOnly)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getShouldStayActiveInSearcher === true)
        Mockito.reset(components.chatClient)
      }
    }

    "be update description" when {
      "opinion VIN_IN_DESC" in new Fixture {
        val vin1 = CommonTestForms.randomVin()
        val vin2 = CommonTestForms.randomVin()

        def randDescPart: String = RandomUtil.nextSymbols(5)

        val originDesc = s"$randDescPart $vin1 $randDescPart $vin2 $randDescPart"

        override val offer: Offer = {
          val b = TestUtils.createOffer().toBuilder
          b.setDescription(originDesc)
          b.build()
        }

        override val opinions: Seq[OpinionWrapper] = {

          val vinInDesc = DetailedReason.Details.VinInDescription.newBuilder()
          vinInDesc.addVins(vin1).addVins(vin2)

          val details = DetailedReason.Details.newBuilder()
          details.setVinInDescription(vinInDesc)

          val detailReason = DetailedReason.newBuilder()

          detailReason.setReason(VIN_IN_DESCRIPTION)
          detailReason.setDetails(details)

          val opinion = getOpinion(UNKNOWN).toBuilder
          opinion.addWarnDetailedReasons(detailReason)

          Seq(OpinionWrapper(opinion.build, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))
        }

        private val newDesc = result.getUpdate.get.getDescription

        assert(newDesc == originDesc.replace(vin1, "*" * vin1.length).replace(vin2, "*" * vin2.length))
      }

      "opinion PHONE_IN_DESC" in new Fixture {

        val rawPhone1 = "8(831)2-500-500"
        val rawPhone2 = "+79267917752"

        val originDesc =
          s"xxx $rawPhone1 xxx, yyy $rawPhone2 yyy, zzz $rawPhone1, $rawPhone2 xyz"

        override val offer: Offer = {
          val b = TestUtils.createOffer().toBuilder
          b.setDescription(originDesc)
          b.build()
        }

        override val opinions: Seq[OpinionWrapper] = {

          val phone1 = DetailedReason.Details.PhoneInDesc.Phone.newBuilder()
          phone1.setRawPhone(rawPhone1)

          val phone2 = DetailedReason.Details.PhoneInDesc.Phone.newBuilder()
          phone2.setRawPhone(rawPhone2)

          val phoneDesc = DetailedReason.Details.PhoneInDesc.newBuilder()
          phoneDesc.addPhones(phone1)
          phoneDesc.addPhones(phone2)

          val details = DetailedReason.Details.newBuilder()
          details.setPhoneInDesc(phoneDesc)

          val detailReason = DetailedReason.newBuilder()

          detailReason.setReason(PHONE_IN_DESC)
          detailReason.setDetails(details)

          val option = getOpinion(UNKNOWN).toBuilder
          option.addWarnDetailedReasons(detailReason)

          Seq(OpinionWrapper(option.build, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))
        }

        private val newDesc = result.getUpdate.get.getDescription

        assert(newDesc == "xxx *************** xxx, yyy ************ yyy, zzz ***************, ************ xyz")

      }
    }
  }

  "notification" should {
    "be added" when {
      "banned private offer" in new Fixture {
        val offer: Offer = TestUtils.createOffer().build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, DOCS_SALE), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        // должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getType ===
            Notification.NotificationType.MODERATION_BAN
        )
      }

      "recall private offer" in new Fixture {
        val offer: Offer = privateSale
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, SOLD), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_INACTIVE) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        // должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getType ===
            Notification.NotificationType.MODERATION_BAN
        )
      }

      "private offer revalidation failed" in new Fixture {
        override val offer: Offer = privateSale
        val meta = Metadata
          .newBuilder()
          .setCompleteCheck(
            Metadata.CompleteCheckMetadata
              .newBuilder()
              .setValue(
                Metadata.CompleteCheckMetadata.Value.COMPLETE_CHECK_FAILED
              )
          )
          .build
        override val opinions: Seq[OpinionWrapper] =
          Seq(
            OpinionWrapper(
              getOpinion(FAILED),
              Seq(meta),
              Diff.Autoru.newBuilder().setVersion(1).addValues(COMPLETE_CHECK_METADATA).build(),
              getNow
            )
          )

        // должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getType ===
            Notification.NotificationType.BANNED_REVALIDATION
        )
      }
    }

    "be added twice" when {
      "banned with reasons duplicate and reseller" in new Fixture {
        val offer: Offer = TestUtils.createOffer().build()
        val opinions: Seq[OpinionWrapper] = Seq(
          OpinionWrapper(
            getOpinion(FAILED, DUPLICATE, USER_RESELLER),
            Seq.empty,
            Diff.Autoru.getDefaultInstance,
            getNow
          )
        )

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 2)

        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getExtraArgsCount === 1
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getExtraArgs(0) === "duplicate"
        )

        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(1)
            .getExtraArgsCount === 1
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(1)
            .getExtraArgs(0) === "user_reseller"
        )
      }

      "banned with reasons duplicate, commercial, do_not_exist" in new Fixture {
        val offer: Offer = TestUtils.createOffer().build()
        val opinions: Seq[OpinionWrapper] = Seq(
          OpinionWrapper(
            getOpinion(FAILED, DUPLICATE, COMMERCIAL, DO_NOT_EXIST),
            Seq.empty,
            Diff.Autoru.getDefaultInstance,
            getNow
          )
        )

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 2)

        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getExtraArgsCount === 1
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getExtraArgs(0) === "duplicate"
        )

        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(1)
            .getExtraArgsCount === 2
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(1)
            .getExtraArgs(0) === "commercial"
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(1)
            .getExtraArgs(1) === "do_not_exist"
        )
      }
    }

    "not be added twice" when {
      "banned with reasons duplicate and reseller and do_not_exist" in new Fixture {
        val offer: Offer = TestUtils.createOffer().build()
        val opinions: Seq[OpinionWrapper] = Seq(
          OpinionWrapper(
            getOpinion(FAILED, DUPLICATE, USER_RESELLER, DO_NOT_EXIST),
            Seq.empty,
            Diff.Autoru.getDefaultInstance,
            getNow
          )
        )

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 1)

        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getExtraArgsCount === 2
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getExtraArgs(0) === "user_reseller"
        )
        assert(
          result.getUpdate.get.getOfferAutoru
            .getNotificationsOrBuilder(0)
            .getExtraArgs(1) === "do_not_exist"
        )
      }
    }

    "not be added" when {
      "banned dealers offer" in new Fixture {
        val offer: Offer = TestUtils.createOffer(dealer = true).build()
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED, DOCS_SALE), Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getFlagCount === 1)

        // не должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }
      "private offer revalidation succeeded" in new Fixture {
        override val offer: Offer = privateSale
        val meta = Metadata
          .newBuilder()
          .setCompleteCheck(
            Metadata.CompleteCheckMetadata
              .newBuilder()
              .setValue(
                Metadata.CompleteCheckMetadata.Value.COMPLETE_CHECK_OK
              )
          )
          .build
        override val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(getOpinion(FAILED), Seq(meta), Diff.Autoru.getDefaultInstance, getNow))

        // не должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }

      "private offer revalidation failed but diff is empty" in new Fixture {
        override val offer: Offer = privateSale
        val meta = Metadata
          .newBuilder()
          .setCompleteCheck(
            Metadata.CompleteCheckMetadata
              .newBuilder()
              .setValue(
                Metadata.CompleteCheckMetadata.Value.COMPLETE_CHECK_FAILED
              )
          )
          .build
        override val opinions: Seq[OpinionWrapper] =
          Seq(
            OpinionWrapper(
              getOpinion(FAILED),
              Seq(meta),
              Diff.Autoru.getDefaultInstance,
              getNow
            )
          )

        // не должна быть добавлена нотификация
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
      }
    }
  }

  "inheritance flag & reseller flags" should {
    "be added" when {
      "offer banned with inheritance flag" in new Fixture {
        val offer: Offer = TestUtils.createOffer().build()
        val opinion: Opinion =
          getOpinion(FAILED, isReseller = Some(true), isBanByInheritance = Some(true), Nil, DO_NOT_EXIST)
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(opinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.getFlagCount === 1)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === true)
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsCount === 0)
        assert(result.getUpdate.get.getOfferAutoru.getReseller === true)
        assert(result.getUpdate.get.getOfferAutoru.getIsBanByInheritance === true)
      }
    }
    "be removed" when {
      "unban offer by inheritabce" in new Fixture {
        private val builder =
          TestUtils.createOffer().putFlag(OfferFlag.OF_BANNED)
        builder.getOfferAutoruBuilder.setReseller(true)
        builder.getOfferAutoruBuilder.setIsBanByInheritance(true)
        val offer: Offer = builder.build()
        val opinion: Opinion = getOpinion(OK, isReseller = Some(false), isBanByInheritance = Some(true), Nil)
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(opinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.getFlagCount === 1)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === false)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_NEED_ACTIVATION) === true)
        assert(result.getUpdate.get.getOfferAutoru.getNotificationsList.isEmpty)
        assert(result.getUpdate.get.getOfferAutoru.getReseller === false)
        assert(result.getUpdate.get.getOfferAutoru.getIsBanByInheritance === true)
      }
      "unban offer" in new Fixture {
        private val builder =
          TestUtils.createOffer().putFlag(OfferFlag.OF_BANNED)
        builder.getOfferAutoruBuilder.setReseller(true)
        builder.getOfferAutoruBuilder.setIsBanByInheritance(true)
        val offer: Offer = builder.build()
        val opinion: Opinion = getOpinion(OK, isReseller = Some(false), isBanByInheritance = Some(false), Nil)
        val opinions: Seq[OpinionWrapper] =
          Seq(OpinionWrapper(opinion, Seq.empty, Diff.Autoru.getDefaultInstance, getNow))

        assert(result.getUpdate.get.getFlagCount === 1)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === false)
        assert(result.getUpdate.get.hasFlag(OfferFlag.OF_NEED_ACTIVATION) === true)
        assert(!result.getUpdate.get.getOfferAutoru.getNotificationsList.isEmpty)
        assert(result.getUpdate.get.getOfferAutoru.getReseller === false)
        assert(result.getUpdate.get.getOfferAutoru.getIsBanByInheritance === false)
      }
    }
  }

  "multiple opinions write" should {
    "ban + unban" in new Fixture {
      val offer: Offer = TestUtils.createOffer().addReasonsBan("xxx").build()
      val opinions: Seq[OpinionWrapper] = Seq(
        OpinionWrapper(getOpinion(FAILED, LOW_PRICE), Seq.empty, Diff.Autoru.getDefaultInstance, getNow),
        OpinionWrapper(getOpinion(OK), Seq.empty, Diff.Autoru.getDefaultInstance, getNow + 1000L)
      )

      assert(result.getUpdate.get.getFlagCount === 1)
      assert(result.getUpdate.get.hasFlag(OfferFlag.OF_NEED_ACTIVATION) === true)
      assert(result.getUpdate.get.hasFlag(OfferFlag.OF_BANNED) === false)

      // добавлена история статуса
      private val history = result.getUpdate.get.getStatusHistoryList.asScala
      assert(history.last.getComment == "OpinionWriter")
      assert(history.last.getOfferStatus == CompositeStatus.CS_NEED_ACTIVATION)

      assert(history.takeRight(2).head.getComment == "OpinionWriter: LOW_PRICE")
      assert(history.takeRight(2).head.getOfferStatus == CompositeStatus.CS_BANNED)
    }
  }

  "ProvenOwnerMetadata" when {
    import ProvenOwnerMetadata.Verdict._

    def buildProvenOwnerMetadata(verdicts: Seq[ProvenOwnerMetadata.Verdict],
                                 verdict: Option[ProvenOwnerMetadata.Verdict],
                                 ts: Instant): Metadata = {
      val provenOwnerMetadata =
        Metadata.ProvenOwnerMetadata.newBuilder().addAllVerdicts(verdicts.asJava)
      verdict.foreach(provenOwnerMetadata.setVerdict)

      Metadata
        .newBuilder()
        .setProvenOwnerMetadata(provenOwnerMetadata)
        .setTimestamp(ts)
        .build()
    }

    def buildOffer(verdicts: Seq[ProvenOwnerMetadata.Verdict] = Seq.empty,
                   state: Option[ProvenOwnerMetadata.Verdict] = None,
                   stateTs: Option[Instant] = None,
                   tags: Seq[String] = Seq.empty): Offer = {
      val offer = TestUtils.createOffer()
      for {
        ts <- stateTs
      } {
        val b = offer.getOfferAutoruBuilder.getProvenOwnerModerationStateBuilder
          .setTimestamp(ts)
          .addAllVerdicts(verdicts.asJava)

        state.foreach(b.setState)
      }

      offer
        .addAllTag(tags.asJava)
        .build
    }

    val provenOwnerTestCases: Seq[TestCase] = Seq(
      {
        val meta = buildProvenOwnerMetadata(Seq.empty, Some(PROVEN_OWNER_OK), nowTs)
        val someOtherMeta = Metadata
          .newBuilder()
          .setGeobaseIp(Metadata.GeobaseIp.newBuilder)
          .setTimestamp(nowTs)
          .build
        TestCase(
          description = "None to PROVEN_OWNER_OK",
          offer = buildOffer(),
          entries =
            Seq(OpinionWrapper(getOpinion(OK), Seq(meta, someOtherMeta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 1 &&
            offerAutoru
              .getNotifications(0)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(0)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_OK.toString)
          }
        )
      }, {
        val meta = buildProvenOwnerMetadata(Seq.empty, Some(PROVEN_OWNER_OK), nowTs)
        val newerTs = nowTs.plusMillis(1000L)
        TestCase(
          description = "PROVEN_OWNER_FAILED to PROVEN_OWNER_OK with old timestamp",
          offer = buildOffer(Seq.empty, Some(PROVEN_OWNER_FAILED), Some(newerTs)),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq(meta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => result.getUpdate.isEmpty
        )
      }, {
        val meta = buildProvenOwnerMetadata(Seq.empty, Some(PROVEN_OWNER_OK), nowTs)
        val oldTs = nowTs.minusMillis(1000L)
        TestCase(
          description = "PROVEN_OWNER_FAILED to PROVEN_OWNER_OK",
          offer = buildOffer(Seq.empty, Some(PROVEN_OWNER_FAILED), Some(oldTs)),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq(meta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 1 &&
            offerAutoru
              .getNotifications(0)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(0)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_OK.toString)
          }
        )
      }, {
        val meta = buildProvenOwnerMetadata(Seq.empty, Some(PROVEN_OWNER_FAILED), nowTs)
        val oldTs = nowTs.minusMillis(1000L)
        TestCase(
          description = "PROVEN_OWNER_OK to PROVEN_OWNER_FAILED",
          offer = buildOffer(Seq.empty, Some(PROVEN_OWNER_OK), Some(oldTs), Seq(OpinionWriter.ProvenOwnerTag)),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq(meta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            !tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 1 &&
            offerAutoru
              .getNotifications(0)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(0)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_FAILED.toString)
          }
        )
      }, {
        val meta =
          buildProvenOwnerMetadata(Seq.empty, Some(PROVEN_OWNER_NOT_ENOUGH_PHOTOS), nowTs)
        val oldTs = nowTs.minusMillis(1000L)
        TestCase(
          description = "PROVEN_OWNER_OK to PROVEN_OWNER_NOT_ENOUGH_PHOTOS",
          offer = buildOffer(Seq.empty, Some(PROVEN_OWNER_OK), Some(oldTs), Seq(OpinionWriter.ProvenOwnerTag)),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq(meta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            !tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 1 &&
            offerAutoru
              .getNotifications(0)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(0)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_NOT_ENOUGH_PHOTOS.toString)
          }
        )
      }, {
        val meta = buildProvenOwnerMetadata(Seq.empty, Some(PROVEN_OWNER_BAD_PHOTOS), nowTs)
        val oldTs = nowTs.minusMillis(1000L)
        TestCase(
          description = "PROVEN_OWNER_OK to PROVEN_OWNER_BAD_PHOTOS",
          offer = buildOffer(Seq.empty, Some(PROVEN_OWNER_OK), Some(oldTs), Seq(OpinionWriter.ProvenOwnerTag)),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq(meta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            !tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 1 &&
            offerAutoru
              .getNotifications(0)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(0)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_BAD_PHOTOS.toString)
          }
        )
      }, {
        val oldTs = nowTs.minusMillis(1000L)
        TestCase(
          description = "PROVEN_OWNER_OK to None",
          offer = buildOffer(Seq.empty, Some(PROVEN_OWNER_OK), Some(oldTs), Seq(OpinionWriter.ProvenOwnerTag)),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq.empty, Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            !tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 0
          }
        )
      }, {
        val verdicts = Seq(PROVEN_OWNER_OK, PROVEN_OWNER_BAD_PHOTOS)
        val meta = buildProvenOwnerMetadata(verdicts, None, nowTs)
        TestCase(
          description = "None to PROVEN_OWNER_OK + PROVEN_OWNER_BAD_PHOTOS",
          offer = buildOffer(),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq(meta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            val state = offerAutoru.getProvenOwnerModerationState()
            tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 2 &&
            offerAutoru
              .getNotifications(0)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(0)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_OK.toString) &&
            offerAutoru
              .getNotifications(1)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(1)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_BAD_PHOTOS.toString) &&
            state.getVerdictsList().asScala == verdicts &&
            state.hasState && state.getState == PROVEN_OWNER_OK
          }
        )
      }, {
        val verdicts = Seq(PROVEN_OWNER_NOT_ENOUGH_PHOTOS, PROVEN_OWNER_BAD_PHOTOS)
        val meta = buildProvenOwnerMetadata(verdicts, None, nowTs)
        TestCase(
          description = "None to PROVEN_OWNER_NOT_ENOUGH_PHOTOS + PROVEN_OWNER_BAD_PHOTOS",
          offer = buildOffer(),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq(meta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            val state = offerAutoru.getProvenOwnerModerationState()
            !tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 2 &&
            offerAutoru
              .getNotifications(0)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(0)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_NOT_ENOUGH_PHOTOS.toString) &&
            offerAutoru
              .getNotifications(1)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(1)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_BAD_PHOTOS.toString) &&
            state.getVerdictsList().asScala == verdicts &&
            state.hasState && state.getState == PROVEN_OWNER_NOT_ENOUGH_PHOTOS
          }
        )
      }, {
        val verdicts = Seq(PROVEN_OWNER_FAILED)
        val meta = buildProvenOwnerMetadata(verdicts, None, nowTs)
        val oldTs = nowTs.minusMillis(1000L)
        TestCase(
          description = "PROVEN_OWNER_OK to PROVEN_OWNER_FAILED (new verdicts field)",
          offer = buildOffer(Seq(PROVEN_OWNER_OK), None, Some(oldTs), Seq(OpinionWriter.ProvenOwnerTag)),
          entries = Seq(OpinionWrapper(getOpinion(OK), Seq(meta), Diff.Autoru.getDefaultInstance, getNow)),
          check = (result: OfferUpdate) => {
            val tags = result.getUpdate.get.getTagList.asScala
            val offerAutoru = result.getUpdate.get.getOfferAutoru
            val state = offerAutoru.getProvenOwnerModerationState()
            !tags.contains(OpinionWriter.ProvenOwnerTag) &&
            offerAutoru.getNotificationsCount == 1 &&
            offerAutoru
              .getNotifications(0)
              .getType == NotificationType.PROVEN_OWNER_MODERATION &&
            offerAutoru
              .getNotifications(0)
              .getExtraArgsList
              .asScala
              .contains(PROVEN_OWNER_FAILED.toString) &&
            state.getVerdictsList().asScala == verdicts &&
            state.hasState && state.getState == PROVEN_OWNER_FAILED
          }
        )
      }
    )

    provenOwnerTestCases.foreach {
      case TestCase(description, offer, entries, check) =>
        description in {
          assert(check(opinionWriter.updateStateForOffer(offer, entries, Map.empty)))
        }
    }
  }

  private lazy val privateSale =
    getOfferById(1043045004).toBuilder.clearFlag().build()

  private lazy val dealerSale =
    getOfferById(1043026846).toBuilder.clearFlag().build()

  private lazy val bannedSale = getOfferById(1044216699).toBuilder
    .clearFlag()
    .putFlag(OfferFlag.OF_BANNED)
    .build()

  private def getOpinion(opinionType: Opinion.Type, reasons: Reason*): Opinion = {
    val isReseller = if (reasons.contains(Reason.USER_RESELLER)) Some(true) else None
    getOpinion(opinionType, isReseller = isReseller, isBanByInheritance = None, Nil, reasons: _*)
  }

  private def getDetailedOpinion(opinionType: Opinion.Type,
                                 reasons: Seq[Reason],
                                 detailed: Seq[DetailedReason]): Opinion = {
    getOpinion(opinionType, isReseller = None, isBanByInheritance = None, detailed = detailed, reasons: _*)
  }

  private def getOpinion(opinionType: Opinion.Type,
                         isReseller: Option[Boolean],
                         isBanByInheritance: Option[Boolean],
                         detailed: Seq[DetailedReason],
                         reasons: Reason*): Opinion = {

    val builder: Opinion.Builder = Opinion.newBuilder()
    builder
      .setVersion(1)
      .setType(opinionType)
    isReseller.foreach(v => builder.getDetailsBuilder.getAutoruBuilder.setIsFromReseller(v))
    isBanByInheritance.foreach(v => builder.getDetailsBuilder.getAutoruBuilder.setIsBanByInheritance(v))
    if (reasons.nonEmpty) builder.addAllReasons(reasons.asJava)
    if (detailed.nonEmpty) builder.addAllDetailedReasons(detailed.asJava)
    builder.build()
  }

}
