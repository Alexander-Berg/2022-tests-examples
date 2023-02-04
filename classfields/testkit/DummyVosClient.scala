package auto.common.clients.vos.testkit

import zio._
import cats.data.NonEmptySet
import auto.common.clients.vos.Vos
import auto.common.clients.vos.Vos.{OfferId, OwnerId, Vin, Vos, VosError}
import ru.auto.api.api_offer_model.Multiposting.Classified
import ru.auto.api.api_offer_model.{Category, Offer}
import ru.auto.api.response_model.OfferIdsByVinsResponse

class DummyVosClient extends Vos.Service {

  override def getOffer(category: Category, id: String, readFromMaster: Boolean = false): IO[VosError, Option[Offer]] =
    ZIO.some(Offer.defaultInstance)

  override def getOffersCount(
      category: Option[Category],
      ownerId: Vos.OwnerId,
      filters: List[Vos.OffersCountFilter]): IO[VosError, Int] = {
    ZIO.succeed(0)
  }

  override def getDetailedOfferIds(
      clientId: OwnerId.DealerId,
      vins: NonEmptySet[Vin],
      includeRemoved: Boolean): IO[VosError, OfferIdsByVinsResponse] =
    ZIO.succeed(OfferIdsByVinsResponse.defaultInstance)

  override def addMultipostingServices(
      user: OwnerId.DealerId,
      offerID: OfferId,
      category: Option[Category],
      classified: Classified.ClassifiedName,
      services: Set[String]): IO[VosError, Unit] = Task.unit

  override def removeMultipostingServices(
      user: OwnerId.DealerId,
      offerID: OfferId,
      category: Option[Category],
      classified: Classified.ClassifiedName,
      services: Set[String]): IO[VosError, Unit] = Task.unit
}

object DummyVosClient {
  val dummy: ULayer[Vos] = ZLayer.succeed(new DummyVosClient)
}
