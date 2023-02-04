package vertis.broker.api.produce.storage

import vertis.broker.api.model.{ProducerMessage, StorageError}
import vertis.zio.BaseEnv
import zio.{IO, UIO, URIO}

import java.util.concurrent.atomic.AtomicInteger

/** @author zvez
  */
class AlwaysOkStorageProducer extends StorageProducer {
  private val counter = new AtomicInteger(0)

  override def name: String = "always-ok"

  override def statistics: UIO[StorageProducerStatistics] = UIO(StorageProducerStatistics.Empty)

  override def write(msg: ProducerMessage): IO[StorageError, Unit] =
    UIO(counter.incrementAndGet()).unit

  override def close: UIO[Unit] = UIO.unit

  def messageProcessed: Int = counter.get()
}

object AlwaysOkStorageProducer {
  def make: UIO[AlwaysOkStorageProducer] = UIO.succeed(new AlwaysOkStorageProducer)
}
