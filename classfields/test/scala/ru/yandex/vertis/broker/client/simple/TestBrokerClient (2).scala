package ru.yandex.vertis.broker.client.simple

import ru.yandex.vertis.broker.api.requests.WriteEventRequest
import ru.yandex.vertis.broker.api.responses.WriteEventResponse
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller

import scala.concurrent.Future

/** @author kusaeva
  */
class TestBrokerClient extends BrokerClient {
  override protected def config: BrokerClientConfig = ???

  override protected def writeEvent(request: WriteEventRequest): Future[WriteEventResponse] = ???

  override def send[T: ProtoMarshaller](
      id: Option[String],
      message: T): Future[Unit] = Future.unit

  override def close(): Unit = ???
}
