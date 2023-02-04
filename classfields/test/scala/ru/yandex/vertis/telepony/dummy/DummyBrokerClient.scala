package ru.yandex.vertis.telepony.dummy

import ru.yandex.vertis.broker.{WriteEventRequest, WriteEventResponse}
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.BrokerClient

import scala.concurrent.Future

class DummyBrokerClient extends BrokerClient {

  override def send[T: ProtoMarshaller](
      id: Option[String],
      message: T,
      schemaVersionOpt: Option[String] = None): Future[Unit] =
    Future.unit

  override protected def writeEvent(request: WriteEventRequest): Future[WriteEventResponse] =
    Future.successful(WriteEventResponse())

  override def close(): Unit = ()
}
