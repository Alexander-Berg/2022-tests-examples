package vertis.broker.api.produce.storage

import common.zio.logging.Logging
import vertis.broker.api.model.{ProducerMessage, StorageError}
import vertis.zio.BaseEnv
import zio._

/** @author zvez
  */
class DummyStorageProducer(val q: Queue[(ProducerMessage, Promise[StorageError, Unit])], val closedRef: Ref[Boolean])
  extends StorageProducer {

  override def name: String = "dummy"

  override def statistics: UIO[StorageProducerStatistics] = UIO(StorageProducerStatistics.Empty)

  override def write(msg: ProducerMessage): ZIO[BaseEnv, StorageError, Unit] =
    for {
      _ <- Logging.info(s"Got message ${msg.seqNo}")
      promise <- Promise.make[StorageError, Unit]
      _ <- q.offer(msg -> promise)
      _ <- promise.await
      _ <- Logging.info(s"Acknowledged message ${msg.seqNo}")
    } yield ()

  override def close: UIO[Unit] = closedRef.set(true)
}

object DummyStorageProducer {

  def make: UIO[DummyStorageProducer] =
    for {
      q <- Queue.unbounded[(ProducerMessage, Promise[StorageError, Unit])]
      closedRef <- Ref.make(false)
    } yield new DummyStorageProducer(q, closedRef)
}
