package ru.auto.api.util

import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators
import ru.auto.api.util.form.FormOptionsMapper
import ru.auto.api.util.form.ReplacerMapper.OptionsMap

import scala.jdk.CollectionConverters._

class FormOptionsMapperSpec extends BaseSpec {
  "FromOptionsMapper" should {
    "map options from catalog to form format and back" in {
      val origin: OptionsMap = Map(
        "driver-seat-electric" -> true,
        "passenger-seat-electric" -> true,
        "wheel-configuration2" -> true,
        "wheel-configuration1" -> false,
        "some-test-option-1" -> true,
        "some-test-option-2" -> false
      )
      val testOffer = ModelGenerators.CarsOfferGen.next.toBuilder
      testOffer.getCarInfoBuilder.clearEquipment()
      testOffer.getCarInfoBuilder.putAllEquipment(origin.asJava)

      FormOptionsMapper.offerFromCatalogToForm(testOffer)

      val form = testOffer.getCarInfo.getEquipmentMap
      form.get("electro-seat") shouldBe true
      form.containsKey("wheel-configuration3") shouldBe false
      form.containsKey("climate-control-3") shouldBe false
      form.get("some-test-option-1") shouldBe true
      form.get("some-test-option-2") shouldBe false

      FormOptionsMapper.offerFromFormToCatalog(testOffer)

      testOffer.getCarInfo.getEquipmentMap.asScala shouldEqual origin
    }
  }
}
