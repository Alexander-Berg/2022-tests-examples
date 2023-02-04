package vertis.logbroker.client.test.unit

import vertis.logbroker.client.BLbTask
import vertis.logbroker.client.consumer.model.out.BatchRequestMessage
import vertis.logbroker.client.consumer.model.{Cookie, LogbrokerReadResult}
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession
import vertis.logbroker.client.consumer.session.lb_native.LbNativeConsumerSession.ConsumerCb
import vertis.zio.BaseEnv
import zio._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class TestNativeConsumer private[unit] (
    cb: ConsumerCb,
    q: Queue[LogbrokerReadResult],
    onInit: Option[Map[Int, Long]] => BLbTask[Unit],
    onCommit: Seq[Cookie] => URIO[BaseEnv, Unit],
    onClose: URIO[BaseEnv, Unit])
  extends LbNativeConsumerSession {

  /** @param initialOffsets offsets to start reading from,
    *                       None to rely on server's info (commits must be enabled)
    */
  override def init(initialOffsets: Option[Map[Int, Long]], lastWrittenOffsets: Option[Map[Int, Long]]): BLbTask[Unit] =
    onInit(initialOffsets)

  override def commit(cookies: Seq[Cookie]): BLbTask[Unit] = onCommit(cookies)

  override def request(batch: BatchRequestMessage): BLbTask[Unit] =
    (q.take >>= cb).fork.unit

  override def close: URIO[BaseEnv, Unit] = onClose
}
