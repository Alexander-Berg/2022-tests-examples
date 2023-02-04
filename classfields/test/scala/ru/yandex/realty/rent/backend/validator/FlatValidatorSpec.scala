package ru.yandex.realty.rent.backend.validator

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.rent.proto.api.common.FlatTypeNamespace.FlatType
import ru.yandex.realty.rent.proto.api.flats.PatchFlatRequest
import ru.yandex.realty.rent.proto.api.moderation.FlatQuestionnaire.Flat.RoomsNamespace.Rooms
import ru.yandex.realty.util.Mappings._

import scala.jdk.CollectionConverters.asScalaBufferConverter

@RunWith(classOf[JUnitRunner])
class FlatValidatorSpec extends AsyncSpecBase {
  "FlatValidatorImplSpec" when {
    "patchFlatValidation" should {
      "return None for empty request" in {
        val request = PatchFlatRequest.getDefaultInstance
        FlatValidatorImpl.patchFlatValidation(request).futureValue shouldBe None
      }
      "return None for valid full filled FlatInfo" in {
        val request = PatchFlatRequest
          .newBuilder()
          .applySideEffect {
            _.getFlatInfoBuilder
              .applySideEffect(_.getAreaBuilder.setValue(30))
              .applySideEffect(_.getEntranceBuilder.setValue(3))
              .applySideEffect(_.getFlatTypeBuilder.setValue(FlatType.FLAT))
              .applySideEffect(_.getFloorBuilder.setValue(8))
              .applySideEffect(_.getRoomsBuilder.setValue(Rooms.ONE))
              .applySideEffect(_.getIntercomBuilder.setCode("13B"))
              .applySideEffect(_.getDesiredRentPriceBuilder.setValue(22000 * 100))
          }
          .build
        FlatValidatorImpl.patchFlatValidation(request).futureValue shouldBe None
      }
      "return None for valid partial FlatInfo" in {
        val request = PatchFlatRequest
          .newBuilder()
          .applySideEffect {
            _.getFlatInfoBuilder
              .applySideEffect(_.getAreaBuilder.setValue(30))
              .applySideEffect(_.getFlatTypeBuilder.setValue(FlatType.FLAT))
              .applySideEffect(_.getIntercomBuilder.setCode("13B"))
          }
          .build
        FlatValidatorImpl.patchFlatValidation(request).futureValue shouldBe None
      }
      "return error for invalid FlatInfo" in {
        val request = PatchFlatRequest
          .newBuilder()
          .applySideEffect {
            _.getFlatInfoBuilder
              .applySideEffect(_.getAreaBuilder.setValue(-3))
              .applySideEffect(_.getEntranceBuilder.setValue(-3))
              .applySideEffect(_.getFlatTypeBuilder.setValueValue(-1))
              .applySideEffect(_.getFloorBuilder.setValue(-8))
              .applySideEffect(_.getRoomsBuilder.setValueValue(-1))
              .applySideEffect(_.getIntercomBuilder.setCode("13B"))
              .applySideEffect(_.getDesiredRentPriceBuilder.setValue(999 * 100))
          }
          .build
        val result = FlatValidatorImpl.patchFlatValidation(request).futureValue
        result.nonEmpty shouldBe true
        val errList = result.get.getValidationErrorsList.asScala
        errList.length shouldBe 6
        errList.exists(_.getParameter == "/flatInfo/rooms") shouldBe true
        errList.exists(_.getParameter == "/flatInfo/flatType") shouldBe true
        errList.exists(_.getParameter == "/flatInfo/entrance") shouldBe true
        errList.exists(_.getParameter == "/flatInfo/area") shouldBe true
        errList.exists(_.getParameter == "/flatInfo/floor") shouldBe true
        errList.exists(_.getParameter == "/flatInfo/desiredRentPrice") shouldBe true
      }
    }
  }
}
