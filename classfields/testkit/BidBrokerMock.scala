package vsmoney.auction.services.testkit

import common.zio.events_broker.Broker
import common.zio.events_broker.Broker.BrokerError
import vsmoney.auction.auction_bids_delivery.AuctionBid
import zio.{Has, IO, URLayer, ZLayer}
import zio.test.mock.{Mock, Proxy}

object BidBrokerMock extends Mock[Broker.TypedBroker[AuctionBid]] {
  object Send extends Effect[(AuctionBid, Option[String], Option[String]), BrokerError, Unit]

  val compose: URLayer[Has[Proxy], Broker.TypedBroker[AuctionBid]] =
    ZLayer.fromServiceM { proxy: Proxy =>
      withRuntime.map { _ =>
        new Broker.Typed[AuctionBid] {
          override def send(
              event: AuctionBid,
              id: Option[String],
              schemaVersion: Option[String]): IO[BrokerError, Unit] = proxy(Send, event, id, schemaVersion)
        }
      }
    }
}
