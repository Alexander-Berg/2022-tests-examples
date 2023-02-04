package ru.yandex.vertis.broker.client.simple

import ru.yandex.vertis.broker.{WriteEventRequest, WriteEventResponse}
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller

import scala.concurrent.Future

case object TestBrokerClient extends BrokerClient {

  override protected def writeEvent(request: WriteEventRequest): Future[WriteEventResponse] =
    Future.successful(WriteEventResponse.of(WriteEventResponse.Result.Ack(WriteEventResponse.Ack())))

  override def send[T: ProtoMarshaller](
      id: Option[String],
      message: T,
      schemaVersionOpt: Option[String]): Future[Unit] = Future.successful(())

  override def close(): Unit = ()
}
