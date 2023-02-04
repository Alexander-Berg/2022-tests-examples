package vsmoney.auction.services.testkit

import vsmoney.auction.model.{AuctionKey, AuctionParams}
import vsmoney.auction.services.AuctionParamsService
import zio.test.mock._
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.Mock

object AuctionParamsServiceMock extends Mock[Has[AuctionParamsService]] {
  object Get extends Effect[AuctionKey, Throwable, AuctionParams]

  override val compose: URLayer[Has[Proxy], Has[AuctionParamsService]] = ZLayer.fromService { proxy =>
    new AuctionParamsService {
      override def get(key: AuctionKey): Task[AuctionParams] = proxy(Get, key)
    }
  }
}
