package ru.yandex.auto.garage.consumers.kafka.vos

import ru.auto.api.ApiOfferModel.{Category, Offer, OfferStatus, RecallInfo, Section}
import ru.auto.api.vin.garage.GarageApiModel.CardTypeInfo.CardType
import ru.yandex.auto.garage.dao.cards.CardsTableRow
import ru.yandex.auto.garage.managers.OfferInfo
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard
import ru.yandex.auto.vin.decoder.model.VinCode

import java.time.Instant

object VosProcessorTestUtils {

  def buildOffer(
      category: Category = Category.CARS,
      section: Section = Section.USED,
      optVin: Option[VinCode] = None,
      status: OfferStatus = OfferStatus.ACTIVE,
      optRecallInfo: Option[RecallInfo] = None): Offer = {
    val builder = Offer.newBuilder().setCategory(category).setSection(section)
    optVin.foreach(vin => builder.getDocumentsBuilder.setVin(vin.toString))
    builder.setStatus(status)
    optRecallInfo.foreach(builder.setRecallInfo)
    builder.build()
  }

  def buildOfferInfo(
      category: Category = Category.CARS,
      section: Section = Section.USED,
      optVin: Option[VinCode] = Some(VinCode("Z94CU51CBBR016064")),
      status: OfferStatus = OfferStatus.ACTIVE,
      optRecallInfo: Option[RecallInfo] = None): OfferInfo = {
    OfferInfo(buildOffer(category, section, optVin, status, optRecallInfo))
  }

  def buildRow(
      id: Long = 1,
      vin: Option[VinCode] = None,
      offerId: Option[String] = None,
      status: GarageCard.Status = GarageCard.Status.ACTIVE,
      card: GarageCard = GarageCard.newBuilder().build(),
      cardType: CardType = CardType.CURRENT_CAR): CardsTableRow = {
    CardsTableRow(
      id = id,
      userId = "user-123",
      vin = vin.map(_.toString),
      licensePlate = None,
      created = Instant.now(),
      status = status,
      cardType = cardType,
      card = card,
      exteriorPanoramaId = None,
      interiorPanoramaId = None,
      offerId = offerId,
      timestampVisit = Instant.now()
    )
  }
}
