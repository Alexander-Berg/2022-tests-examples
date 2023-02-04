package ru.auto.api.util.offer

import org.scalacheck.Gen
import ru.auto.api.ApiOfferModel.{Availability, Multiposting, Offer, OfferStatus, PtsStatus, Section}
import ru.auto.api.CommonModel.SteeringWheel
import ru.auto.api.MotoModel.MotoCategory
import ru.auto.api.TrucksModel.{LightTruck, TruckCategory}
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.gen.NetGenerators
import ru.auto.api.services.CachingHttpClient

trait MinimalTestOffers { this: CachingHttpClient =>

  private val testDescription =
    "Продаётся замечательный полноприводный автомобиль! " +
      "Не битая, не крашенная, в отличном состоянии. На 74000 км поменяно масло и колодки. 2 ключа. " +
      "Резина лето и зима. Возможен торг"

  def minimalCarOffer: Offer = {
    val builder = Offer
      .newBuilder()
      .setSection(Section.USED)
      .setAvailability(Availability.IN_STOCK)
      .setColorHex("007F00")
      .setDescription(testDescription)

    builder.getPriceInfoBuilder.setPrice(randomPrice.toFloat).setCurrency("RUR")
    builder.getDocumentsBuilder
      .setYear(2010)
      .setPts(PtsStatus.ORIGINAL)
      .setVin(VinGenerator.next)
      .setOwnersNumber(2)

    builder.getStateBuilder.setMileage(50000)
    builder.getSellerBuilder
      .setName("Не банить в тестинге!")
      .setUnconfirmedEmail(NetGenerators.emailGen.next)
    builder.getSellerBuilder.getLocationBuilder.setGeobaseId(213L)
    builder.getSellerBuilder.addPhonesBuilder().setPhone("79213027807")

    builder.getCarInfoBuilder
      .setMark("MERCEDES")
      .setModel("GL_KLASSE")
      .setBodyType("ALLROAD_5_DOORS")
      .setEngineType("DIESEL")
      .setDrive("ALL_WHEEL_DRIVE")
      .setTransmission("AUTOMATIC")
      .setSuperGenId(4986814L)
      .setTechParamId(20494193L)
      .setSteeringWheel(SteeringWheel.LEFT)

    builder.build()
  }

  def minimalMultipostingCarOffer: Offer = {
    val builder = minimalCarOffer.toBuilder
    val multiposting = Multiposting.newBuilder().setStatus(OfferStatus.ACTIVE).build()
    builder.setMultiposting(multiposting)

    builder.build()
  }

  def minimalTruckOffer: Offer = {
    val builder = Offer
      .newBuilder()
      .setSection(Section.NEW)
      .setAvailability(Availability.IN_STOCK)
      .setColorHex("007F00")
      .setDescription(testDescription)

    builder.getPriceInfoBuilder.setPrice(randomPrice.toFloat).setCurrency("RUR")
    builder.getDocumentsBuilder
      .setYear(2010)
      .setPts(PtsStatus.ORIGINAL)
      .setVin(VinGenerator.next)
      .setOwnersNumber(2)

    builder.getTruckInfoBuilder
      .setMark("BAW")
      .setModel("TONIK")
      .setTruckCategory(TruckCategory.LCV)
      .setLightTruckType(LightTruck.BodyType.REFRIGERATOR)
      .setDisplacement(300)

    builder.build()
  }

  def minimalMotoOffer: Offer = {
    val builder = Offer
      .newBuilder()
      .setSection(Section.NEW)
      .setAvailability(Availability.IN_STOCK)
      .setColorHex("007F00")
      .setDescription(testDescription)

    builder.getPriceInfoBuilder.setPrice(randomPrice.toFloat).setCurrency("RUR")
    builder.getDocumentsBuilder
      .setYear(1999)
      .setPts(PtsStatus.ORIGINAL)
      .setVin(VinGenerator.next)
      .setOwnersNumber(2)

    builder.getMotoInfoBuilder
      .setMark("EUROTEX")
      .setModel("BULLET")
      .setMotoCategory(MotoCategory.MOTORCYCLE)
      .setDisplacement(2500)

    builder.build()
  }

  private def randomPrice = 150000 + Gen.posNum[Int].next
}
