package vsmoney.auction.services.testkit

import vsmoney.auction.model.{AuctionKey, UserBid}
import vsmoney.auction.services.UsersBidsService
import zio.test.mock.{Mock, Proxy, _}
import zio.{Has, URLayer, ZLayer}

object UsersBidsServiceMock extends Mock[Has[UsersBidsService]] {

  object Get extends Effect[AuctionKey, Throwable, Seq[UserBid]]

  override val compose: URLayer[Has[Proxy], Has[UsersBidsService]] = ZLayer.fromService { proxy => (key: AuctionKey) =>
    proxy(Get, key)
  }
}
