package vsmoney.auction.services.testkit

import vsmoney.auction.services.BidEventJournal
import zio.{Has, Task, URLayer, ZLayer}
import zio.test.mock.{Mock, Proxy}

object BidEventJournalMock extends Mock[Has[BidEventJournal]] {
  object BidPlaced extends Effect[Set[BidEventJournal.BidEvent], Throwable, Unit]
  object AuctionStopped extends Effect[Set[BidEventJournal.AuctionStopEvent], Throwable, Unit]

  override val compose: URLayer[Has[Proxy], Has[BidEventJournal]] = ZLayer.fromService { proxy =>
    new BidEventJournal {
      override def bidPlaced(batch: Set[BidEventJournal.BidEvent]): Task[Unit] =
        proxy(BidPlaced, batch)

      override def auctionStopped(batch: Set[BidEventJournal.AuctionStopEvent]): Task[Unit] =
        proxy(AuctionStopped, batch)
    }
  }

}
