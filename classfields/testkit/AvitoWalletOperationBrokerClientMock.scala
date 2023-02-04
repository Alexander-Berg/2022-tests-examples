package auto.dealers.multiposting.scheduler.testkit

import common.zio.events_broker.Broker
import common.zio.events_broker.Broker.BrokerError
import ru.auto.multiposting.operation_model.AvitoWalletOperation
import zio._
import zio.test.mock._

object AvitoWalletOperationBrokerClientMock extends Mock[Broker.TypedBroker[AvitoWalletOperation]] {
  object Send extends Effect[(AvitoWalletOperation, Option[String], Option[String]), BrokerError, Unit]

  val compose: URLayer[Has[Proxy], Broker.TypedBroker[AvitoWalletOperation]] =
    ZLayer.fromServiceM { proxy: Proxy =>
      withRuntime.map { _ =>
        new Broker.Typed[AvitoWalletOperation] {
          override def send(
              event: AvitoWalletOperation,
              id: Option[String],
              schemaVersion: Option[String]): IO[BrokerError, Unit] = proxy(Send, event, id, schemaVersion)
        }
      }
    }
}
