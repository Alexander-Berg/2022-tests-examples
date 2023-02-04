package ru.yandex.vertis.vsquality.hobo.analyticsupdater

import ru.yandex.vertis.broker.api.requests.WriteEventRequest
import ru.yandex.vertis.broker.api.responses.WriteEventResponse
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.{BrokerClient, BrokerClientConfig}

import scala.concurrent.Future

class StubBrokerClient extends BrokerClient {

  override def send[T: ProtoMarshaller](
      id: Option[String],
      message: T): Future[Unit] =
    Future.unit

  override protected def writeEvent(request: WriteEventRequest): Future[WriteEventResponse] =
    Future.successful(WriteEventResponse())

  override def close(): Unit = ()

  override protected def config: BrokerClientConfig = BrokerClientConfig("hobo", "localhost")
}
