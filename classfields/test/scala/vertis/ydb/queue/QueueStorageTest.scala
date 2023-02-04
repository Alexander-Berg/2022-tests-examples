package vertis.ydb.queue

import org.scalatest.Suite
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.ydb.Ydb
import ru.yandex.vertis.ydb.zio.{Tx, YdbZioWrapper}
import vertis.ydb.partitioning.manual.ManualUniformPartitioning
import vertis.ydb.queue.storage.{AbstractQueueStorage, QueueElement}
import vertis.ydb.test.YdbTest
import vertis.zio.test.ZioSpecBase
import common.zio.logging.Logging
import zio.ZIO
import zio.clock.Clock

trait QueueStorageTest extends YdbTest {
  this: Suite with ZioSpecBase =>

  lazy val partitioning: ManualUniformPartitioning = ManualUniformPartitioning.ofBits(3)

  lazy val storage: AbstractQueueStorage[Logging.Logging] = new AbstractQueueStorage(partitioning) {
    override def ydb: YdbZioWrapper = ydbWrapper
    override def tableName: String = "queue"
    override protected def prometheusRegistry: PrometheusRegistry = QueueStorageTest.this.prometheusRegistry
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    runSync(storage.init)
    ()
  }

  def runTx[R <: Clock, E, T](tx: Tx[R with Ydb, E, T]): ZIO[R, E, T] =
    Ydb.runTx(tx).provideSomeLayer[R](storage.ydbLayer)

  implicit protected val codec: QueueElement.PayloadCodec[Unit] = new QueueElement.PayloadCodec[Unit] {
    override def read(bytes: Array[Byte]): Unit = ()
    override def write(payload: Unit): Array[Byte] = Array.emptyByteArray
  }

}
