package ru.yandex.vertis.billing.banker.service.log

import ru.yandex.vertis.broker.api.requests.WriteEventRequest
import ru.yandex.vertis.broker.api.responses.WriteEventResponse
import ru.yandex.vertis.broker.client.marshallers.ProtoMarshaller
import ru.yandex.vertis.broker.client.simple.{BrokerClient, BrokerClientConfig}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

/**
  * @author brs-lphv
  */
case class BrokerClientMock[M: ProtoMarshaller]() extends BrokerClient {

  private val history = ArrayBuffer.empty[Any]

  def getMessages: Seq[M] = synchronized {
    history.toList.asInstanceOf[List[M]]
  }

  override def send[T: ProtoMarshaller](
      id: Option[String],
      message: T): Future[Unit] = synchronized {
    history += message
    Future.unit
  }

  override protected def writeEvent(request: WriteEventRequest): Future[WriteEventResponse] =
    Future.successful(WriteEventResponse())

  override def close(): Unit = ()

  override protected def config: BrokerClientConfig = ???
}
