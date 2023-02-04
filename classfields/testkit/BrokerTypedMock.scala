package auto.dealers.multiposting.scheduler.testkit

import auto.events.model.ClassifiedStatisticUpdateEvent
import common.zio.events_broker.Broker
import common.zio.events_broker.Broker.BrokerError
import zio._
import zio.test.mock._

object BrokerTypedMock extends Mock[Broker.TypedBroker[ClassifiedStatisticUpdateEvent]] {
  object Send extends Effect[(ClassifiedStatisticUpdateEvent, Option[String], Option[String]), BrokerError, Unit]

  val compose: URLayer[Has[Proxy], Broker.TypedBroker[ClassifiedStatisticUpdateEvent]] =
    ZLayer.fromServiceM { proxy: Proxy =>
      withRuntime[Any].map { _ =>
        new Broker.Typed[ClassifiedStatisticUpdateEvent] {
          override def send(
              event: ClassifiedStatisticUpdateEvent,
              id: Option[String],
              schemaVersion: Option[String]): IO[BrokerError, Unit] = proxy(Send, event, id, schemaVersion)

        }
      }
    }
}
