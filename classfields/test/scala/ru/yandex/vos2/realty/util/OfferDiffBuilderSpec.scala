package ru.yandex.vos2.realty.util

import java.lang

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.Checkers
import org.scalatest.{Matchers, WordSpecLike}
import ru.yandex.realty.proto.offer.ModerationStatus
import ru.yandex.realty.vos.model.diff.FieldEnumNamespace.FieldEnum
import ru.yandex.vertis.vos2.model.realty.{Area, AreaUnit, OfferType, PricePaymentPeriod}
import ru.yandex.vertis.vos2.model.realty.RealtyOffer.{
  RealtyBalconyType,
  RealtyBathroomType,
  RealtyBuildingType,
  RealtyCategory,
  RealtyDealStatus,
  RealtyRenovation
}
import ru.yandex.vos2.OfferModel.{FieldModerationOpinion, Offer}
import ru.yandex.vos2.realty.model.TestUtils

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class OfferDiffBuilderSpec extends WordSpecLike with Matchers with Checkers {

  "Offer diff" should {
    "shouldn't have different in same offer" in {
      val offerBuilder: Offer.Builder = TestUtils.createOffer()
      val offer = offerBuilder.build()
      val sameOffer = offerBuilder.build()
      OfferDiffBuilder.makeDiff(offer, sameOffer).getChangedFieldsCount shouldBe 0
    }

    "should have different description" in {
      val offerBuilder: Offer.Builder = TestUtils.createOffer()
      val offer = offerBuilder.build()
      val differentDescrOffer = offerBuilder.setDescription(offerBuilder.getDescription + "another Description").build()
      val diff = OfferDiffBuilder.makeDiff(offer, differentDescrOffer)
      diff.getChangedFieldsCount shouldBe 1
      val diffField = diff.getChangedFields(0)
      diffField.getFieldName shouldBe FieldEnum.DESCRIPTION
      diffField.getOldValues(0) shouldBe offer.getDescription
      diffField.getNewValues(0) shouldBe differentDescrOffer.getDescription
    }
    "should have to much difference" in {
      val offerBuilder: Offer.Builder = TestUtils.createOffer()
      val (offer, differentOffer) = makeVeryDifferentOffer(offerBuilder)
      val diff = OfferDiffBuilder.makeDiff(offer, differentOffer)
      diff.getChangedFieldsCount shouldBe 20
      val diffFields = diff.getChangedFieldsList.asScala.map(_.getFieldName)
      diffFields.contains(FieldEnum.DESCRIPTION) shouldBe false
      diffFields.contains(FieldEnum.DEAL_STATUS) shouldBe true
      diffFields.contains(FieldEnum.RENT_PERIOD) shouldBe true
      diffFields.contains(FieldEnum.CATEGORY) shouldBe true
      diffFields.contains(FieldEnum.BUILDING_TYPE) shouldBe true
      diffFields.contains(FieldEnum.BUILT_YEAR) shouldBe true
      diffFields.contains(FieldEnum.FLOOR_COUNT) shouldBe true
      diffFields.contains(FieldEnum.IS_STUDIO) shouldBe true
      diffFields.contains(FieldEnum.RENOVATION) shouldBe true
      diffFields.contains(FieldEnum.ROOMS) shouldBe true
      diffFields.contains(FieldEnum.TOTAL_AREA) shouldBe true
      diffFields.contains(FieldEnum.LIVING_AREA) shouldBe true
      diffFields.contains(FieldEnum.KITCHEN_AREA) shouldBe true
      diffFields.contains(FieldEnum.BALCONY_TYPE) shouldBe true
      diffFields.contains(FieldEnum.BATHROOM_TYPE) shouldBe true
      diffFields.contains(FieldEnum.ROOMS_TOTAL) shouldBe true
      diffFields.contains(FieldEnum.ROOMS_OFFERED) shouldBe true
      diffFields.contains(FieldEnum.FLOOR) shouldBe true
      diffFields.contains(FieldEnum.VIDEO_STATUS) shouldBe true
      diffFields.contains(FieldEnum.TOUR_STATUS) shouldBe true
    }

  }

  //scalastyle:off
  def makeVeryDifferentOffer(offerBuilder: Offer.Builder): (Offer, Offer) = {
    val first = offerBuilder.build()
    val second = offerBuilder.build()
    val firstRealtyBuilder = first.getOfferRealty.toBuilder
    val secondRealtyBuilder = second.getOfferRealty.toBuilder
    firstRealtyBuilder.setDealStatus(RealtyDealStatus.SALE)
    secondRealtyBuilder.setDealStatus(RealtyDealStatus.DIRECT_RENT)

    firstRealtyBuilder.setPricePaymentPeriod(PricePaymentPeriod.PER_DAY)
    secondRealtyBuilder.setPricePaymentPeriod(PricePaymentPeriod.PER_MONTH)

    firstRealtyBuilder.setOfferType(OfferType.SELL)
    secondRealtyBuilder.setOfferType(OfferType.RENT)

    firstRealtyBuilder.setCategory(RealtyCategory.CAT_APARTMENT)
    secondRealtyBuilder.setCategory(RealtyCategory.CAT_ROOMS)

    firstRealtyBuilder.setBuildingType(RealtyBuildingType.BLOCK)
    secondRealtyBuilder.setBuildingType(RealtyBuildingType.MONOLIT)

    firstRealtyBuilder.setBuiltYear(1981)
    secondRealtyBuilder.setBuiltYear(2011)

    firstRealtyBuilder.setFloorsTotal(22)
    secondRealtyBuilder.setFloorsTotal(11)

    firstRealtyBuilder.setFacilities(firstRealtyBuilder.getFacilities.toBuilder.setFlagIsStudio(true).build())
    secondRealtyBuilder.setFacilities(firstRealtyBuilder.getFacilities.toBuilder.setFlagIsStudio(false).build())

    firstRealtyBuilder.setTypeRenovation(RealtyRenovation.DESIGN)
    secondRealtyBuilder.setTypeRenovation(RealtyRenovation.NEEDS_RENOVATION)

    firstRealtyBuilder.clearRoom()
    secondRealtyBuilder.clearRoom.addAllRoom(
      Seq(Area.newBuilder().setAreaUnit(AreaUnit.SQ_M).setAreaValue(30).build()).asJava
    )

    firstRealtyBuilder.setAreaFull(Area.newBuilder().setAreaUnit(AreaUnit.SQ_M).setAreaValue(28).build())
    secondRealtyBuilder.setAreaFull(Area.newBuilder().setAreaUnit(AreaUnit.SQ_M).setAreaValue(77).build())

    firstRealtyBuilder.setAreaKitchen(Area.newBuilder().setAreaUnit(AreaUnit.SQ_M).setAreaValue(7).build())
    secondRealtyBuilder.setAreaKitchen(Area.newBuilder().setAreaUnit(AreaUnit.SQ_M).setAreaValue(11).build())

    firstRealtyBuilder.setAreaLivingSpace(Area.newBuilder().setAreaUnit(AreaUnit.SQ_M).setAreaValue(11).build())
    secondRealtyBuilder.setAreaLivingSpace(Area.newBuilder().setAreaUnit(AreaUnit.SQ_M).setAreaValue(60).build())

    firstRealtyBuilder.setTypeBalcony(RealtyBalconyType.BALCONY)
    secondRealtyBuilder.setTypeBalcony(RealtyBalconyType.LOGGIA)

    firstRealtyBuilder.setTypeBathroomUnit(RealtyBathroomType.MATCHED)
    secondRealtyBuilder.setTypeBathroomUnit(RealtyBathroomType.SEPARATED)

    firstRealtyBuilder.setRoomsTotal(0)
    secondRealtyBuilder.setRoomsTotal(4)

    firstRealtyBuilder.setRoomsOffered(0)
    secondRealtyBuilder.setRoomsOffered(1)

    firstRealtyBuilder.clearFloor().addAllFloor(Seq(java.lang.Integer.valueOf(3)).asJava)
    secondRealtyBuilder.clearFloor().addAllFloor(Seq(java.lang.Integer.valueOf(5)).asJava)

    (
      first.toBuilder
        .setOfferRealty(firstRealtyBuilder)
        .setBlockedFields(
          FieldModerationOpinion
            .newBuilder()
            .setVideoReviewModerationStatus(ModerationStatus.MODERATION_STATUS_BLOCKED)
            .setVirtualTourModerationStatus(ModerationStatus.MODERATION_STATUS_ENABLED)
        )
        .build(),
      second.toBuilder
        .setOfferRealty(secondRealtyBuilder)
        .setBlockedFields(
          FieldModerationOpinion
            .newBuilder()
            .setVideoReviewModerationStatus(ModerationStatus.MODERATION_STATUS_ENABLED)
            .setVirtualTourModerationStatus(ModerationStatus.MODERATION_STATUS_BLOCKED)
        )
        .build()
    )
  }

}
