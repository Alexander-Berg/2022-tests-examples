package ru.auto.api.managers.garage

import com.google.protobuf.BoolValue
import ru.auto.api.{BaseSpec, CommonModel}
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.CommonModel.SmallPhotoPreview
import ru.auto.api.auth.Application
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.features.AppsFeaturesManager
import ru.auto.api.managers.garage.ViewMode.{Full, Sharing}
import ru.auto.api.model.{AutoruUser, RequestParams, UserRef}
import ru.auto.api.model.ModelGenerators.{ModeratorUserGrantsGen, OfferIDGen, PrivateUserRefGen}
import ru.auto.api.recalls.RecallsApiModel
import ru.auto.api.util.{Request, RequestImpl}
import ru.auto.api.vin.Common.IdentifierType
import ru.auto.api.vin.VinReportModel.{ContentBlock, RawVinReport}
import ru.auto.api.vin.VinReportModel.ContentBlock.{ContentItem, ContentItemType}
import ru.auto.api.vin.VinResolutionEnums.Status
import ru.auto.api.vin.garage.GarageApiModel._
import ru.auto.api.vin.garage.GarageApiModel.ProvenOwnerState.ProvenOwnerStatus
import ru.auto.panoramas.PanoramasModel.Panorama
import ru.yandex.passport.model.api.ApiModel.SessionResult
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

@nowarn
class GarageDecayManagerSpec extends BaseSpec with MockitoSupport {

  private val featureManager: FeatureManager = mock[FeatureManager]
  private val feature: Feature[Boolean] = mock[Feature[Boolean]]
  when(featureManager.truncateRemovedOfferInfoInGarageCards).thenReturn(feature)

  private val garageDecayManager = new GarageDecayManager(
    mock[AppsFeaturesManager],
    featureManager
  )

  private def moderatorSession: SessionResult = {
    SessionResult.newBuilder().setGrants(ModeratorUserGrantsGen.next).build()
  }
  private val user = PrivateUserRefGen.next
  private val trace: Traced = Traced.empty

  private def generatePrivateReq(isModerator: Boolean = true, user: UserRef = user): RequestImpl = {
    val r = new RequestImpl
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.iosApp)
    r.setUser(user)
    r.setTrace(trace)
    if (isModerator) {
      r.setSession(moderatorSession)
    }
    r
  }

  implicit private val req: Request = generatePrivateReq(isModerator = false)

  private val VIN = "WBAFG81000LJ37566"
  private val LICENSE_PLATE = "B188CT716"

  before {
    when(feature.value).thenReturn(true)
  }

  "GarageDecayManager" should {

    def card(identifierType: IdentifierType, approvedOwner: Boolean = false): Card = {
      val anotherUser = AutoruUser(user.uid + 1)
      val builder = Card
        .newBuilder()
        .setSourceInfo(SourceInfo.newBuilder().setAddedByIdentifier(identifierType))
        .setUserId(anotherUser.toPlain)

      if (approvedOwner) {
        builder.getProvenOwnerStateBuilder.setStatus(ProvenOwnerStatus.OK)
      }

      builder.getReviewsBuilder.setHasOwnReviewsValue(BoolValue.of(true))
      builder.getRecallsBuilder.setCard(
        RecallsApiModel.Card
          .newBuilder()
          .setVinCode(VIN)
          .setIsSubscribed(true)
          .setCardId(123)
          .addRecalls(
            RecallsApiModel.Recall
              .newBuilder()
              .setTitle("Отзывная кампания")
              .setDescription("Дефект в коробке передач")
          )
      )
      builder.getVehicleInfoBuilder.getDocumentsBuilder
        .setVin(VIN)
        .setLicensePlate(LICENSE_PLATE)
        .setPurchaseDate(CommonModel.Date.newBuilder().setYear(2020).setDay(12).setMonth(12).build())
      builder.build()
    }

    def cardWithOfferInfo(identifierType: IdentifierType, offerStatus: OfferStatus): Card = {
      val builder = card(identifierType).toBuilder
      builder.getOfferInfoBuilder
        .setOfferId(OfferIDGen.next.toPlain)
        .setStatus(offerStatus)
        .setPrice(123456)
      builder.build()
    }

    "hide license plate" in {
      val decayed = garageDecayManager
        .decayCard(card(IdentifierType.VIN), DecayParams.NoDecayNeeded, viewMode = Full)

      decayed.getVehicleInfo.getDocuments.getVin shouldBe VIN
      decayed.getVehicleInfo.getDocuments.getLicensePlate shouldBe empty
    }

    "hide vin" in {
      val decayed = garageDecayManager
        .decayCard(card(IdentifierType.LICENSE_PLATE), DecayParams.NoDecayNeeded, viewMode = Full)

      decayed.getVehicleInfo.getDocuments.getVin shouldBe empty
      decayed.getVehicleInfo.getDocuments.getLicensePlate shouldBe LICENSE_PLATE
    }

    "not hide vin/lp for approved owner" in {
      val testCard = card(IdentifierType.LICENSE_PLATE, approvedOwner = true)
      val decayed = garageDecayManager.decayCard(testCard, DecayParams.NoDecayNeeded, viewMode = Full)(
        generatePrivateReq(isModerator = false, user = UserRef.parse(testCard.getUserId))
      )

      decayed.getVehicleInfo.getDocuments.getVin shouldBe VIN
      decayed.getVehicleInfo.getDocuments.getLicensePlate shouldBe LICENSE_PLATE
    }

    "hide vin when requested not by approved owner" in {
      val testCard = card(IdentifierType.LICENSE_PLATE, approvedOwner = true)
      val decayed = garageDecayManager.decayCard(testCard, DecayParams.NoDecayNeeded, viewMode = Full)

      decayed.getVehicleInfo.getDocuments.getVin shouldBe empty
      decayed.getVehicleInfo.getDocuments.getLicensePlate shouldBe LICENSE_PLATE
    }

    "hide lp when requested not by approved owner" in {
      val testCard = card(IdentifierType.VIN, approvedOwner = true)
      val decayed = garageDecayManager.decayCard(testCard, DecayParams.NoDecayNeeded, viewMode = Full)

      decayed.getVehicleInfo.getDocuments.getVin shouldBe VIN
      decayed.getVehicleInfo.getDocuments.getLicensePlate shouldBe empty
    }

    "do not hide vin/lp and purchaseDate for shared view when is moderation" in {
      val decayed = garageDecayManager
        .decayCard(card(IdentifierType.LICENSE_PLATE), DecayParams.NoDecayNeeded, viewMode = Sharing)(
          generatePrivateReq()
        )

      decayed.getVehicleInfo.getDocuments.getVin shouldBe VIN
      decayed.getVehicleInfo.getDocuments.getLicensePlate shouldBe LICENSE_PLATE
      decayed.getVehicleInfo.getDocuments.getPurchaseDate shouldBe CommonModel.Date
        .newBuilder()
        .setYear(2020)
        .setDay(12)
        .setMonth(12)
        .build()
    }

    "hide some fields for shared view" in {
      val decayed =
        garageDecayManager.decayCard(card(IdentifierType.VIN), DecayParams.NoDecayNeeded, viewMode = Sharing)

      decayed.getVehicleInfo.getDocuments.getVin shouldBe empty
      decayed.getVehicleInfo.getDocuments.getLicensePlate shouldBe empty
      decayed.getVehicleInfo.getDocuments.getPurchaseDate.getYear shouldBe 0
      decayed.getId shouldBe empty
      decayed.hasSourceInfo shouldBe false
      decayed.getRecalls.getCard.getVinCode shouldBe empty
      decayed.getRecalls.getCard.getIsSubscribed shouldBe false
      decayed.getRecalls.getCard.getCardId shouldBe 0
      decayed.getRecalls.getCard.getRecalls(0).getTitle shouldBe "Отзывная кампания"
      decayed.getRecalls.getCard.getRecalls(0).getDescription shouldBe "Дефект в коробке передач"
    }

    "not hide fields for full view" in {
      val testCard = card(IdentifierType.VIN)

      val decayed = garageDecayManager
        .decayCard(testCard, DecayParams.NoDecayNeeded, viewMode = Full)

      val expected = testCard.toBuilder
      expected.getVehicleInfoBuilder.getDocumentsBuilder.clearLicensePlate()
      decayed shouldBe expected.build()
    }

    "not hide offer_info when offer is not removed" in {
      val testCard = cardWithOfferInfo(IdentifierType.VIN, OfferStatus.NEED_ACTIVATION)

      val decayed = garageDecayManager
        .decayCard(testCard, DecayParams.NoDecayNeeded, viewMode = Full)

      decayed.hasOfferInfo shouldBe true
    }

    "hide offer_info when offer is removed" in {
      val testCard = cardWithOfferInfo(IdentifierType.VIN, OfferStatus.REMOVED)

      val decayed = garageDecayManager
        .decayCard(testCard, DecayParams.NoDecayNeeded, viewMode = Full)

      decayed.hasOfferInfo shouldBe false
    }

    "hide offer_info when offer is banned" in {
      val testCard = cardWithOfferInfo(IdentifierType.VIN, OfferStatus.BANNED)

      val decayed = garageDecayManager
        .decayCard(testCard, DecayParams.NoDecayNeeded, viewMode = Full)

      decayed.hasOfferInfo shouldBe false
    }

    "not hide offer_info for moderator" in {
      val testCard = cardWithOfferInfo(IdentifierType.VIN, OfferStatus.REMOVED)

      val decayed = garageDecayManager
        .decayCard(testCard, DecayParams.NoDecayNeeded, viewMode = Full)(
          generatePrivateReq()
        )

      decayed.hasOfferInfo shouldBe true
    }
  }

  "GarageDecayManager behave when device who make request is Android" should {
    val card = {
      val builder = Card
        .newBuilder()
        .setSourceInfo(SourceInfo.newBuilder().setAddedByIdentifier(IdentifierType.LICENSE_PLATE))

      val otherContentBlocks =
        GarageDecayManager.ContentItemTypesForAndroid.map { itemType =>
          ContentItem
            .newBuilder()
            .setValue("")
            .setKey("")
            .setStatus(Status.OK)
            .setAvailableForFree(false)
            .setType(itemType)
            .setRecordCount(1)
            .build()
        }.asJava

      val techInspectionBlock = ContentItem
        .newBuilder()
        .setValue("")
        .setKey("")
        .setStatus(Status.OK)
        .setAvailableForFree(false)
        .setType(ContentItemType.TECH_INSPECTION)
        .setRecordCount(1)

      val equipmentBlock = ContentItem
        .newBuilder()
        .setValue("")
        .setKey("")
        .setStatus(Status.OK)
        .setAvailableForFree(false)
        .setType(ContentItemType.EQUIPMENT)
        .setRecordCount(1)

      val contentBlock = ContentBlock
        .newBuilder()
        .addItems(techInspectionBlock)
        .addItems(equipmentBlock)
        .addAllItems(otherContentBlocks)

      val rawVinReport = RawVinReport.newBuilder().setContent(contentBlock)
      val report = Report.newBuilder().setReport(rawVinReport)

      builder.setReport(report)

      val panorama = ExteriorPanorama
        .newBuilder()
        .setPanorama(Panorama.newBuilder().setPreview(SmallPhotoPreview.newBuilder()))

      val vehicleInfo = Vehicle.newBuilder().setExteriorPanorama(panorama)

      builder.setVehicleInfo(vehicleInfo)
      builder.build()
    }

    "show some ContentItems when decayForAndroid is false" in {
      val decayed = garageDecayManager.decayCard(card, DecayParams.NoDecayNeeded, viewMode = Full)

      val types = decayed.getReport.getReport.getContent.getItemsList.asScala.map(_.getType)

      types.contains(ContentItemType.EQUIPMENT) shouldBe true
      types.contains(ContentItemType.TECH_INSPECTION) shouldBe true
      decayed.getReport.getReport.getContent.getItemsCount shouldBe
        GarageDecayManager.ContentItemTypesForAndroid.size + 2
    }

    "hide some ContentItems when decayForAndroid is true" in {
      val decayed =
        garageDecayManager
          .decayCard(
            card,
            DecayParams(decayContent = true, decayPreview = false),
            viewMode = Full
          )

      val types = decayed.getReport.getReport.getContent.getItemsList.asScala.map(_.getType)

      types.contains(ContentItemType.EQUIPMENT) shouldBe false
      types.contains(ContentItemType.TECH_INSPECTION) shouldBe false
      decayed.getReport.getReport.getContent.getItemsCount shouldBe
        GarageDecayManager.ContentItemTypesForAndroid.size
    }

    "show some ExteriorPanorama Preview when decayPreviewForAndroid is false" in {
      val decayed =
        garageDecayManager.decayCard(card, DecayParams.NoDecayNeeded, viewMode = Full)

      val panorama = decayed.getVehicleInfo.getExteriorPanorama.getPanorama

      panorama.hasPreview shouldBe true
    }

    "hide ExteriorPanorama Preview when decayPreviewForAndroid is true" in {
      val decayed =
        garageDecayManager
          .decayCard(
            card,
            DecayParams(decayContent = false, decayPreview = true),
            viewMode = Full
          )

      val panorama = decayed.getVehicleInfo.getExteriorPanorama.getPanorama

      panorama.hasPreview shouldBe false
    }
  }

}
