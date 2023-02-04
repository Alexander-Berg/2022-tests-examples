package ru.yandex.vertis.punisher.builder

import java.time.Instant
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.{Category => ProtoCategory, Condition, SellerType}
import ru.yandex.vertis.moderation.proto.Model.Metadata.VinHistoryMetadata
import ru.yandex.vertis.moderation.proto.Model.{
  Context,
  Domain,
  Essentials,
  Instance,
  Metadata,
  Opinion,
  Reason,
  UpdateJournalRecord,
  User,
  Visibility
}
import ru.yandex.vertis.moderation.proto.RealtyLight.RealtyEssentials
import ru.yandex.vertis.punisher.builder.AutoruOfferStateBuilderSpec._
import ru.yandex.vertis.punisher.model.OfferState
import ru.yandex.vertis.punisher.{BaseSpec, ModerationProtoSpec}

@RunWith(classOf[JUnitRunner])
class AutoruOfferStateBuilderSpec extends BaseSpec with ModerationProtoSpec {

  val privateUser = User.newBuilder().setVersion(1).setAutoruUser(UserId)
  val dealerUser = User.newBuilder().setVersion(1).setDealerUser(DealerId)
  val realtyUser = User.newBuilder().setVersion(1).setYandexUser(YandexUid)

  val externalIdPrivate = buildExternalId(privateUser, AutoruObjectId)
  val externalIdDealer = buildExternalId(dealerUser, AutoruObjectId)
  val externalIdRealty = buildExternalId(realtyUser, RealtyObjectId)

  val autoruDomain = Domain.newBuilder().setVersion(1).setAutoru(Domain.Autoru.DEFAULT_AUTORU)
  val realtyDomain = Domain.newBuilder().setVersion(1).setRealty(Domain.Realty.DEFAULT_REALTY)

  val okOpinion = buildOpinion(Opinion.Type.OK, Seq.empty)
  val failedOpinionReseller = buildOpinion(Opinion.Type.FAILED, Seq(Reason.USER_RESELLER, Reason.WRONG_MARK))
  val failedOpinionWrongMark = buildOpinion(Opinion.Type.FAILED, Seq(Reason.WRONG_MARK))

  val autoruOkEntry = buildEntry(autoruDomain, okOpinion)
  val autoruFailedMarkEntry = buildEntry(autoruDomain, failedOpinionWrongMark)
  val autoruFailedResellerEntry = buildEntry(autoruDomain, failedOpinionReseller)
  val realtyFailedResellerEntry = buildEntry(realtyDomain, failedOpinionReseller)

  val autoruOkOpinions = buildOpinions(Seq(autoruOkEntry))
  val autoruWrongMarkOpinions = buildOpinions(Seq(autoruFailedMarkEntry))
  val autoruResellerOpinions = buildOpinions(Seq(autoruFailedMarkEntry, autoruFailedResellerEntry))
  val realtyResellerOpinions = buildOpinions(Seq(realtyFailedResellerEntry))

  val autoruPrivateEssentials = buildAutoruEssentials()
  val autoruPrivateEssentialsWithWasActiveTrue = buildAutoruEssentials(offerWasActive = true)
  val autoruDealerEssentials = buildAutoruEssentials(sellerType = SellerType.COMMERCIAL)

  val privateEssentials = Essentials.newBuilder().setVersion(1).setAutoru(autoruPrivateEssentials)

  val privateEssentialsWithWasActiveTrue =
    Essentials.newBuilder().setVersion(1).setAutoru(autoruPrivateEssentialsWithWasActiveTrue)
  val dealerEssentials = Essentials.newBuilder().setVersion(1).setAutoru(autoruDealerEssentials)
  val realtyEssentials = Essentials.newBuilder().setVersion(1).setRealty(RealtyEssentials.newBuilder().setVersion(1))

  val visibleContext = Context.newBuilder.setVersion(1).setVisibility(Visibility.VISIBLE)
  val inactiveContext = Context.newBuilder.setVersion(1).setVisibility(Visibility.INACTIVE)

  val metadata =
    List(
      Metadata.newBuilder().setVinHistoryMetadata(VinHistoryMetadata.newBuilder().setPreviousVin(PreviousVin)).build()
    )

  val instanceAutoruOk = buildInstance(externalIdPrivate, privateEssentials, autoruOkOpinions, visibleContext, metadata)

  val instanceAutoruDealer =
    buildInstance(externalIdDealer, privateEssentials, autoruOkOpinions, visibleContext, metadata)

  val instanceAutoruWrongMark =
    buildInstance(externalIdPrivate, privateEssentials, autoruWrongMarkOpinions, visibleContext, metadata)

  val instanceAutoruWrongMarkInactive =
    buildInstance(externalIdPrivate, privateEssentials, autoruWrongMarkOpinions, inactiveContext, metadata)

  val instanceAutoruInactive =
    buildInstance(externalIdPrivate, privateEssentials, autoruOkOpinions, inactiveContext, metadata)

  val instanceAutoruInactiveWithWasActiveTrue =
    buildInstance(externalIdPrivate, privateEssentialsWithWasActiveTrue, autoruOkOpinions, inactiveContext, metadata)

  val instanceAutoruReseller =
    buildInstance(externalIdPrivate, privateEssentials, autoruResellerOpinions, visibleContext, metadata)

  val instanceRealty = buildInstance(externalIdRealty, realtyEssentials, realtyResellerOpinions, visibleContext)

  val recordAutoruOk =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruOk),
      ts = Some(Timestamp)
    )

  val recordAutoruDealer =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruDealer),
      ts = Some(Timestamp)
    )

  val recordWithInactiveInstanceActivePrev =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruWrongMarkInactive),
      prev = Some(instanceAutoruOk),
      ts = Some(Timestamp)
    )

  val recordInactive =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruInactive),
      ts = Some(Timestamp)
    )

  val recordAllInactive =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruInactive),
      prev = Some(instanceAutoruInactive),
      ts = Some(Timestamp)
    )

  val recordActivePrevInactiveWasActiveTrue =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruOk),
      prev = Some(instanceAutoruInactiveWithWasActiveTrue),
      ts = Some(Timestamp)
    )

  val recordActivePrevInactiveWasActiveFalse =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruOk),
      prev = Some(instanceAutoruInactive),
      ts = Some(Timestamp)
    )

  val recordWithoutTs =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruWrongMarkInactive),
      prev = Some(instanceAutoruOk)
    )

  val recordWithInactiveInstance =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruWrongMarkInactive),
      ts = Some(Timestamp)
    )

  val recordWithActiveInstanceActivePrev =
    buildUpdateJournalRecord(
      instance = Some(instanceAutoruWrongMark),
      prev = Some(instanceAutoruOk),
      ts = Some(Timestamp)
    )

  val recordRealty =
    buildUpdateJournalRecord(
      instance = Some(instanceRealty),
      ts = Some(Timestamp)
    )

  val stateAutoruOk =
    OfferState.Autoru(
      userId = UserId,
      offerId = AutoruObjectId,
      isAuthorDealer = false,
      isAuthorReseller = false,
      isActive = true,
      vin = Some(Vin),
      previousVin = Some(PreviousVin),
      mark = Some(Mark),
      model = Some(Model),
      complectationId = Some(ComplectationId),
      year = Some(Year),
      regionId = GeobaseIds.headOption,
      placedForFree = Some(true),
      deactivated = None,
      triggerEventDatetime = Some(Instant.ofEpochMilli(Timestamp)),
      category = Some(ProtoCategory.CARS),
      firstActivated = Some(Instant.ofEpochMilli(Timestamp)),
      creationDate = None,
      condition = None,
      isCallCenter = None
    )

  val stateDealerOk = stateAutoruOk.copy(userId = DealerId)

  val autoruOfferStateBuilder = AutoruOfferStateBuilder

  val builderTestCases: Seq[BuilderTestCase] =
    Seq(
      BuilderTestCase(
        "correctly build OfferState for recordAutoruOk",
        recordAutoruOk,
        Some(stateAutoruOk)
      ),
      BuilderTestCase(
        "return None for realty instance",
        recordRealty,
        None
      ),
      BuilderTestCase(
        "correctlyBuild OfferState for userType=Dealer",
        recordAutoruDealer,
        Some(stateDealerOk)
      )
    )

  val tsTestCases: Seq[TimestampTestCase] =
    Seq(
      TimestampTestCase(
        "correctly get ts for record with inactive instance and active prev",
        recordWithInactiveInstanceActivePrev,
        Some(Instant.ofEpochMilli(Timestamp))
      ),
      TimestampTestCase(
        "return None for inactive instance without prev",
        recordWithInactiveInstance,
        None
      ),
      TimestampTestCase(
        "return None for active instance with active prev",
        recordWithActiveInstanceActivePrev,
        None
      ),
      TimestampTestCase(
        "return None for record without timestamp",
        recordWithoutTs,
        None
      )
    )

  val dealerTestCases: Seq[DealerTestCase] =
    Seq(
      DealerTestCase(
        "return true if user is dealer",
        autoruDealerEssentials,
        expected = true
      ),
      DealerTestCase(
        "return false if user is private person",
        autoruPrivateEssentials,
        expected = false
      )
    )

  val resellerTestCases: Seq[ResellerTestCase] =
    Seq(
      ResellerTestCase(
        "return false for instance with ok opinion",
        instanceAutoruOk,
        expected = false
      ),
      ResellerTestCase(
        "return false for instance with failed non reseller opinion",
        instanceAutoruWrongMark,
        expected = false
      ),
      ResellerTestCase(
        "return true for instance with failed reseller opinion",
        instanceAutoruReseller,
        expected = true
      )
    )

  val essentialsTestCases: Seq[EssentialsTestCase] =
    Seq(
      EssentialsTestCase(
        "return None for realty essentials",
        instanceRealty,
        None
      ),
      EssentialsTestCase(
        "return autoruEssentials correctly",
        instanceAutoruOk,
        Some(autoruPrivateEssentials)
      )
    )

  val firstActivatedTestCases: Seq[FirstActivatedTestCase] =
    Seq(
      FirstActivatedTestCase(
        "return first activated ts, if offer is new in moderation and active",
        recordAutoruOk,
        Some(Instant.ofEpochMilli(Timestamp))
      ),
      FirstActivatedTestCase(
        "return None, if offer is new in moderation and not acitve",
        recordInactive,
        None
      ),
      FirstActivatedTestCase(
        "return None, if offer is active and prev is not active, but wasActive is true",
        recordActivePrevInactiveWasActiveTrue,
        None
      ),
      FirstActivatedTestCase(
        "return first activated ts, if offer is active and prev is not active and wasActive is false",
        recordActivePrevInactiveWasActiveFalse,
        Some(Instant.ofEpochMilli(Timestamp))
      ),
      FirstActivatedTestCase(
        "return None, if offer is not active",
        recordAllInactive,
        None
      )
    )

  val allTestCases: Seq[TestCase] =
    builderTestCases ++
      tsTestCases ++
      dealerTestCases ++
      resellerTestCases ++
      essentialsTestCases

  "AutoruOfferStateBuilder" should {
    allTestCases.foreach {
      case BuilderTestCase(description, record, stateOpt) =>
        description in {
          autoruOfferStateBuilder.buildFrom(record) shouldBe stateOpt
        }

      case TimestampTestCase(description, record, tsOpt) =>
        description in {
          autoruOfferStateBuilder.getDeactivateTimestamp(record) shouldBe tsOpt
        }

      case DealerTestCase(description, essentials, expected) =>
        description in {
          autoruOfferStateBuilder.isUserDealer(essentials) shouldBe expected
        }

      case ResellerTestCase(description, instance, expected) =>
        description in {
          autoruOfferStateBuilder.isUserReseller(instance) shouldBe expected
        }

      case EssentialsTestCase(description, instance, expected) =>
        description in {
          autoruOfferStateBuilder.getAutoruEssentials(instance) shouldBe expected
        }

      case FirstActivatedTestCase(description, record, expected) =>
        description in (
          autoruOfferStateBuilder.getFirstActivationTime(record) shouldBe expected
        )
    }
  }
}

object AutoruOfferStateBuilderSpec {

  sealed trait TestCase

  case class BuilderTestCase(description: String, record: UpdateJournalRecord, stateOpt: Option[OfferState.Autoru])
      extends TestCase

  case class TimestampTestCase(description: String, record: UpdateJournalRecord, tsOpt: Option[Instant])
      extends TestCase

  case class DealerTestCase(description: String, essentials: AutoruEssentials, expected: Boolean) extends TestCase

  case class ResellerTestCase(description: String, instance: Instance, expected: Boolean) extends TestCase

  case class EssentialsTestCase(description: String, instance: Instance, expected: Option[AutoruEssentials])
      extends TestCase

  case class FirstActivatedTestCase(description: String,
                                    record: UpdateJournalRecord,
                                    firstActivatedOpt: Option[Instant]
                                   )
      extends TestCase
}
