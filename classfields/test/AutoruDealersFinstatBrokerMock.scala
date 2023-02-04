package vsmoney.dealer_finstat_writer.autoru_monetization

import billing.finstat.autoru_dealers.AutoruDealersFinstat
import common.zio.events_broker.Broker
import common.zio.events_broker.Broker.BrokerError
import zio._
import zio.test.mock._

object AutoruDealersFinstatBrokerMock extends Mock[Broker.TypedBroker[AutoruDealersFinstat]] {
  object Send extends Effect[(AutoruDealersFinstat, Option[String], Option[String]), BrokerError, Unit]

  val compose: URLayer[Has[Proxy], Broker.TypedBroker[AutoruDealersFinstat]] =
    ZLayer.fromServiceM { proxy: Proxy =>
      withRuntime.map { _ => (event: AutoruDealersFinstat, id: Option[String], schemaVersion: Option[String]) =>
        proxy(Send, event, id, schemaVersion)
      }
    }
}
