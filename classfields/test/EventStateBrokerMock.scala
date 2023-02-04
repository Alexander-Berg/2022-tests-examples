package ru.yandex.vertis.billing.emon.scheduler

import billing.emon.model.EventState
import common.zio.events_broker.Broker
import common.zio.events_broker.Broker.BrokerError
import zio._
import zio.test.mock._

object EventStateBrokerMock extends Mock[Broker.TypedBroker[EventState]] {
  object Send extends Effect[(EventState, Option[String], Option[String]), BrokerError, Unit]

  val compose: URLayer[Has[Proxy], Broker.TypedBroker[EventState]] =
    ZLayer.fromServiceM { proxy: Proxy =>
      withRuntime.map { _ =>
        new Broker.Typed[EventState] {
          override def send(
              event: EventState,
              id: Option[String],
              schemaVersion: Option[String]): IO[BrokerError, Unit] = proxy(Send, event, id, schemaVersion)
        }
      }
    }
}
