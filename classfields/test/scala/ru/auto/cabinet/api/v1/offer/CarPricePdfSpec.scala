package ru.auto.cabinet.api.v1.offer

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.{AnyFlatSpecLike => FlatSpecLike}
import ru.auto.api.ApiOfferModel.{Documents, Offer, Phone, Seller}
import ru.auto.api.CatalogModel.{Mark, Model, SuperGeneration, TechParam}
import ru.auto.api.CommonModel.PriceInfo
import ru.auto.api.ResponseModel.OfferResponse
import ru.auto.api.cert.CertModel.{CertInfo, CertStatus}
import ru.auto.api.{ApiOfferModel, CarsModel}

import scala.jdk.CollectionConverters._

class CarPricePdfSpec extends FlatSpecLike with Matchers {

  it should "generate pdf payload" in {

    val payload = CarPricePdf.generatePayload(createOfferResponse)

    val expectedPayload = PricePdfInfo(
      user = Some("Александр, с 13:00 до 17:00"),
      phone = Some("+7 985 123-45-00"),
      url =
        "https://test.avto.ru/cars/used/sale/volkswagen/passat/1068812020-4cf41692/",
      title = "VAZ KALINA Restyling",
      info = CarInfo(
        year = "2008 / 134 000 км",
        engine = "2.0 л / 161 л.с. / дизель",
        drive = "Передний",
        transmission = "Автомат"),
      price = 500000,
      certified = true,
      pluses = List(
        "high_reviews_mark",
        "airbag-5",
        "front-seats-heat",
        "remote-engine-start",
        "drowsy-driver-alert-system",
        "audiosystem-cd",
        "audiosystem"
      )
    )

    payload shouldBe expectedPayload
  }

  private def createOfferResponse = {
    val sellerName = "Александр"
    val originalPhone = "+7 985 123 45 67"
    val redirectPhone = "7 985 123 4500"
    val callHourStart = 13
    val callHourEnd = 17
    val mark = "VAZ"
    val model = "KALINA"
    val supergen = "Restyling"

    val offer = Offer
      .newBuilder()
      .addTags("high_reviews_mark")
      .setPriceInfo(PriceInfo.newBuilder().setPrice(500000))
      .setState(ApiOfferModel.State.newBuilder().setMileage(134000))
      .setUserRef("user:1234")
      .setDocuments(Documents.newBuilder().setYear(2008))
      .setUrl("https://test.avto.ru/cars/used/sale/volkswagen/passat/1068812020-4cf41692/")
      .setSeller(
        Seller
          .newBuilder()
          .setName(sellerName)
          .addPhones(
            Phone
              .newBuilder()
              .setOriginal(originalPhone)
              .setRedirect(redirectPhone)
              .setCallHourStart(callHourStart)
              .setCallHourEnd(callHourEnd)
          )
      )
      .setCarInfo(
        CarsModel.CarInfo
          .newBuilder()
          .setHorsePower(161)
          .setTechParam(TechParam.newBuilder().setDisplacement(1984))
          .setEngineType("DIESEL")
          .setTransmission("AUTOMATIC")
          .setDrive("FORWARD_CONTROL")
          .setMarkInfo(Mark.newBuilder().setName(mark))
          .setModelInfo(Model.newBuilder().setName(model))
          .setSuperGen(SuperGeneration.newBuilder().setName(supergen))
          .putAllEquipment(Map(
            "front-seats-heat" -> true, // 20
            "airbag-5" -> true, // 20
            "remote-engine-start" -> true, // 13
            "drowsy-driver-alert-system" -> true, // 11
            "audiosystem-cd" -> true, // 10
            "audiosystem" -> true, // 10
            "automatic-lighting-control" -> true, // 9
            "tyre-pressure" -> true // 9
          ).view.mapValues(Boolean.box).toMap.asJava))
      .setCertInfo(
        CertInfo
          .newBuilder()
          .setService("Auto.ru")
          .setStatus(CertStatus.ACTIVE)
      )
      .build()

    OfferResponse
      .newBuilder()
      .setOffer(offer)
      .build()
  }
}
