package ru.auto.api.managers.chat

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.model.CategorySelector
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.ModelUtils._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.util.Success

class OfferRoomSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  "OfferRoom" should {
    "provide correct title_v2" in {
      val offerBuilder = CarsOfferGen.next.toBuilder
      offerBuilder.getCarInfoBuilder.getMarkInfoBuilder.setName("test-mark")
      offerBuilder.getCarInfoBuilder.getModelInfoBuilder.setName("test-model")
      offerBuilder.getCarInfoBuilder.getSuperGenBuilder.setName("test-gen")
      offerBuilder.getDocumentsBuilder.setYear(2017)
      val offer = offerBuilder.build()
      val chatUsers = CleanChatUsers(Set(), None)
      val pointer = OfferPointer(CategorySelector.Cars, offer.id)
      val offerRoom = OfferRoom("test-id", chatUsers, pointer, Success(offer))
      val result = offerRoom.toRoom
      val titleV2 = result.getSubject.getTitleV2
      titleV2 shouldBe "test-mark test-model test-gen, 2017"
    }
  }
}
