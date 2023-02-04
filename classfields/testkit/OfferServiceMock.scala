package ru.auto.comeback.consumer.testkit

import ru.auto.api.api_offer_model.Offer
import ru.auto.comeback.OfferService
import ru.auto.comeback.OfferService.OfferService
import ru.auto.comeback.model.Comeback
import zio.test.mock
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.Mock

object OfferServiceMock extends Mock[OfferService] {

  object GetOffer extends Effect[(Comeback.OfferRef, Boolean), Throwable, Option[Offer]]

  override val compose: URLayer[Has[mock.Proxy], OfferService] = ZLayer.fromService { proxy =>
    new OfferService.Service {
      override def getOffer(ref: Comeback.OfferRef, readFromMaster: Boolean): Task[Option[Offer]] =
        proxy(GetOffer, ref, readFromMaster)
    }
  }
}
