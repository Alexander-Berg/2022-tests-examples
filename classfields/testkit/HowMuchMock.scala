package vsmoney.auction.clients.testkit

import vsmoney.auction.clients.HowMuch
import vsmoney.auction.model.howmuch.{ChangePriceBatchRequest, ChangePriceRequest, PriceRequest, PriceResponse}
import zio.test.mock._
import zio.{Has, Task, URLayer, ZLayer}

object HowMuchMock extends Mock[Has[HowMuch]] {

  object GetPrices extends Effect[PriceRequest, Throwable, PriceResponse]

  object ChangePrices extends Effect[ChangePriceRequest, Throwable, Unit]

  object ChangePriceBatch extends Effect[ChangePriceBatchRequest, Throwable, Unit]

  override val compose: URLayer[Has[Proxy], Has[HowMuch]] =
    ZLayer.fromService { proxy =>
      new HowMuch {
        override def getPrices(requests: PriceRequest): Task[PriceResponse] = proxy(GetPrices, requests)

        override def changePrice(request: ChangePriceRequest): Task[Unit] = proxy(ChangePrices, request)

        override def changePriceBatch(request: ChangePriceBatchRequest): Task[Unit] = proxy(ChangePriceBatch, request)
      }
    }

}
