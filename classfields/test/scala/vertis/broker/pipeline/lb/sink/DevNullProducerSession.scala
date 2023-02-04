package vertis.broker.pipeline.lb.sink

import vertis.logbroker.client.LbTask
import vertis.logbroker.client.producer.LbNativeProducerSession
import vertis.logbroker.client.producer.model.{InitResult, Message}
import zio.{UIO, URIO}

/** Accepts requests, never replies anything
  *
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
object DevNullProducerSession extends LbNativeProducerSession() {

  override def init: LbTask[InitResult] =
    UIO.succeed(InitResult(0L, "dev-null", 0))

  override def send(msg: Message): LbTask[Unit] = UIO.unit

  override def close: URIO[Any, Unit] = UIO.unit
}
