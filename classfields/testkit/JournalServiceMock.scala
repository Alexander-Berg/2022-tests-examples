package vsmoney.auction.services.testkit

import vsmoney.auction.model.AuctionChangeSource
import vsmoney.auction.model.request.{LeaveAuctionEntityRequest, PlaceBidBEntityRequest}
import vsmoney.auction.services.JournalService
import zio.test.mock.{Mock, Proxy}
import zio.{Has, Task, URLayer, ZLayer}

object JournalServiceMock extends Mock[Has[JournalService]] {
  object BidPlaced extends Effect[(List[PlaceBidBEntityRequest], Option[AuctionChangeSource]), Throwable, Unit]
  object AuctionStopped extends Effect[(List[LeaveAuctionEntityRequest], Option[AuctionChangeSource]), Throwable, Unit]

  override val compose: URLayer[Has[Proxy], Has[JournalService]] = ZLayer.fromService { proxy =>
    new JournalService {
      override def placeBidBatch(
          userAuctionBid: List[PlaceBidBEntityRequest],
          source: Option[AuctionChangeSource]): Task[Unit] = proxy(BidPlaced, userAuctionBid, source)

      override def stopBatchAuction(
          userAuctionBid: List[LeaveAuctionEntityRequest],
          source: Option[AuctionChangeSource]): Task[Unit] = proxy(AuctionStopped, userAuctionBid, source)
    }
  }

}
