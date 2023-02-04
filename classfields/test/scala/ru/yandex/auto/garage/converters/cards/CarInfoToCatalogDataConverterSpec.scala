package ru.yandex.auto.garage.converters.cards

import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.CarsModel.CarInfo
import ru.yandex.auto.garage.converters.cards.CarInfoToCatalogDataConverterSpec.CatalogCard
import ru.yandex.auto.message.CatalogSchema._
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.model.CarCard
import ru.yandex.auto.vin.decoder.extdata.catalog.cars.CarsCatalog
import ru.yandex.vertis.mockito.MockitoSupport

class CarInfoToCatalogDataConverterSpec extends AnyWordSpecLike with MockitoSupport {

  private val carsCatalog = mock[CarsCatalog]
  private val converter = new CarInfoToCatalogDataConverter(carsCatalog)

  "CarInfoToCatalogDataConverter" should {
    "fill supergen, configuration and techparam and complectation" in {
      when(carsCatalog.findFirst(?)).thenReturn(Some(CatalogCard))
      val carInfo = CarInfo
        .newBuilder()
        .setTechParamId(3)
        .setComplectationId(4)
        .build()
      val catalogInfo = converter.convert(carInfo).get
      assert(catalogInfo.getSuperGenId == 1)
      assert(catalogInfo.getConfigurationId == 2)
      assert(catalogInfo.getTechParamId == 3)
      assert(catalogInfo.getComplectationId == 4) // Должно проставиться
    }

    "fill supergen, configuration and complectation" in {
      when(carsCatalog.findFirst(?)).thenReturn(Some(CatalogCard))
      val carInfo = CarInfo
        .newBuilder()
        .setConfigurationId(2)
        .build()
      val catalogInfo = converter.convert(carInfo).get
      assert(catalogInfo.getSuperGenId == 1)
      assert(catalogInfo.getConfigurationId == 2)
      assert(catalogInfo.getTechParamId == 0) // Не должно проставиться
      assert(catalogInfo.getComplectationId == 0) // Не должно проставиться тк не передано в запросе
    }

    "fill supergen and complectation" in {
      when(carsCatalog.findFirst(?)).thenReturn(Some(CatalogCard))
      val carInfo = CarInfo
        .newBuilder()
        .setSuperGenId(1)
        .build()
      val catalogInfo = converter.convert(carInfo).get
      assert(catalogInfo.getSuperGenId == 1)
      assert(catalogInfo.getConfigurationId == 0) // Не должно проставиться
      assert(catalogInfo.getTechParamId == 0) // Не должно проставиться
      assert(catalogInfo.getComplectationId == 0) // Не Должно проставиться
    }
  }
}

object CarInfoToCatalogDataConverterSpec {

  private lazy val CatalogCard = CarCard(
    CatalogCardMessage
      .newBuilder()
      .setVersion(1)
      .setMark(MarkMessage.newBuilder().setCode("BMW").setVersion(1))
      .setModel(ModelMessage.newBuilder().setCode("X5").setVersion(1))
      .setSuperGeneration(SuperGenerationMessage.newBuilder().setId(1).setVersion(1))
      .setConfiguration(ConfigurationMessage.newBuilder().setId(2).setVersion(1))
      .setTechparameter(TechparameterMessage.newBuilder().setId(3).setVersion(1))
      .setComplectation(ComplectationMessage.newBuilder().setId(4).setVersion(1))
      .build()
  )
}
