package common.zio.events_broker.testkit

import common.zio.events_broker.Broker
import common.zio.events_broker.Broker.{BrokerEvent, TypedBroker}
import izumi.reflect.Tag
import zio.{IO, ULayer, ZIO, ZLayer}

object TestBroker {

  class NoOpBroker extends Broker.Service {

    override def send[T: Broker.BrokerEvent](
        event: T,
        id: Option[String],
        schemaVersion: Option[String]): IO[Broker.BrokerError, Unit] =
      ZIO.unit
  }

  def noOpTyped[T: BrokerEvent: Tag]: ULayer[TypedBroker[T]] = ZLayer.succeed(new NoOpBroker().typed[T])
}
