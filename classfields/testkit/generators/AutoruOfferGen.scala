package ru.yandex.vertis.shark.model.generators

import ru.auto.api.api_offer_model._
import ru.auto.api.cars_model.CarInfo
import ru.auto.api.catalog_model._
import ru.auto.api.common_model.{Date, MileageInfo, PriceInfo}

trait AutoruOfferGen {

  def sampleOffer(): Offer = {
    val doc = Documents.defaultInstance
      .withLicensePlate("А111МР")
      .withVin("XTA210740J0390362")
      .withYear(2018)
      .withPurchaseDate(Date(2022, 1, 16))
    val category = Category.CARS
    val section = Section.NEW
    val price = PriceInfo.defaultInstance.withRurPrice(10000000f)
    val state = State.defaultInstance.withMileage(100500)
    Offer.defaultInstance
      .withId("1106637167-2861ebbe")
      .withColorHex("040001")
      .withDocuments(doc)
      .withCategory(category)
      .withCarInfo(carInfo)
      .withPriceInfo(price)
      .withState(state)
      .withUserRef("dealer:123123")
      .withMileageHistory(Seq(MileageInfo.defaultInstance.withMileage(100000)))
      .withSection(section)
  }

  private def carInfo: CarInfo = {
    val markCode = "MERCEDES"
    val markName = "Mercedes-Benz"
    val markInfo = Mark.defaultInstance
      .withCode(markCode)
      .withName(markName)

    val modelCode = "G_KLASSE_AMG"
    val modelName = "G-Класс AMG"
    val modelInfo = Model.defaultInstance
      .withCode(modelCode)
      .withName(modelName)

    val superGen = SuperGeneration.defaultInstance
      .withName("I (W463) Рестайлинг")

    val configuration = Configuration.defaultInstance
      .withHumanName("Внедорожник 5 дв.")

    val transmissionName = "AUTOMATIC"
    val engineTypeName = "GASOLINE"
    val techParam = TechParam.defaultInstance
      .withTransmission(transmissionName)
      .withEngineType(engineTypeName)
      .withDisplacement(5439)
      .withPower(507)

    CarInfo.defaultInstance
      .withMark(markCode)
      .withModel(modelCode)
      .withEngineType(engineTypeName)
      .withTransmission(transmissionName)
      .withMarkInfo(markInfo)
      .withModelInfo(modelInfo)
      .withSuperGen(superGen)
      .withConfiguration(configuration)
      .withTechParam(techParam)
  }

}
