package vertis.logbroker.client.producer

import java.util.concurrent.atomic.AtomicReference
import vertis.logbroker.client.{BLbTask, LbTask}
import vertis.logbroker.client.model.LogbrokerError.UnexpectedLogbrokerError
import vertis.logbroker.client.producer.LbNativeProducerSession.ProducerCb
import vertis.logbroker.client.producer.model.{InitResult, LogbrokerWriteResult, Message}
import zio._

/**
  */
class TestNativeProducerSession(cb: ProducerCb, responder: Message => UIO[LogbrokerWriteResult])
  extends LbNativeProducerSession {

  private val shouldFailInit = new AtomicReference(false)

  def failOnInit(v: Boolean): UIO[Unit] = UIO(shouldFailInit.set(v))

  override def init: LbTask[InitResult] =
    if (!shouldFailInit.get()) ZIO.succeed(InitResult(0, "test", 0))
    else ZIO.fail(UnexpectedLogbrokerError(new RuntimeException("Init failing mode")))

  override def send(msg: Message): BLbTask[Unit] = responder(msg).flatMap(cb).unit

  override def close: URIO[Any, Unit] = UIO.unit
}

object TestNativeProducerSession {

  def make(responder: Message => UIO[LogbrokerWriteResult])(cb: ProducerCb): UManaged[TestNativeProducerSession] =
    ZManaged.succeed(new TestNativeProducerSession(cb, responder))
}
