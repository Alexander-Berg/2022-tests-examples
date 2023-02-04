package vertis.pica.service

import com.yandex.ydb.table.Session
import com.yandex.ydb.table.description.TableDescription
import com.yandex.ydb.table.query.{ExplainDataQueryResult, Params}
import com.yandex.ydb.table.result.ResultSetReader
import com.yandex.ydb.table.settings.{
  CreateTableSettings,
  DescribeTableSettings,
  DropTableSettings,
  ExecuteScanQuerySettings,
  ReadTableSettings
}
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.ydb.{QueryOptions, RetryOptions, YdbPreparedStatement, YdbResult}
import ru.yandex.vertis.ydb.zio.{Tx, TxEnv, TxError, TxRIO, TxTask, TxUIO, YdbZioWrapper}
import vertis.pica.Namespace
import vertis.pica.model.{ImageId, ImageRecord}
import vertis.ydb.partitioning.manual.{ManualPartition, ManualUniformPartitioning}
import vertis.ydb.queue.storage.{QueueElement, QueueStorage}
import vertis.zio.BaseEnv
import zio.{Exit, Task, UIO, ZIO, ZManaged}
import zio.stream.ZStream

import java.time.Instant

/** @author kusaeva
  */
object TestBrokerQueueService extends TestOperationalSupport {

  private val storage = new QueueStorage[BaseEnv] {
    override def partitioning: ManualUniformPartitioning = ???

    override def addElements[T: QueueElement.PayloadCodec](
        partition: ManualPartition,
        elements: Seq[QueueElement[T]]): TxRIO[BaseEnv, Unit] = ???

    override def addElement[T: QueueElement.PayloadCodec](element: QueueElement[T]): TxRIO[BaseEnv, Unit] = ???

    override def peekElements[T: QueueElement.PayloadCodec](
        until: Instant,
        partition: ManualPartition,
        elementType: String,
        limit: Int): TxRIO[BaseEnv, Seq[QueueElement[T]]] = ???

    override def countElements(
        now: Instant,
        partition: ManualPartition,
        elementType: String,
        limit: Int): TxRIO[BaseEnv, Int] = ???

    override def dropElementsInPartition[T](
        partition: ManualPartition,
        elements: Seq[QueueElement[T]]): TxRIO[BaseEnv, Unit] = ???

    override def pollElements[T: QueueElement.PayloadCodec](
        until: Instant,
        partition: ManualPartition,
        elementType: String,
        limit: Int): TxRIO[BaseEnv, Seq[QueueElement[T]]] = ???

    override def ydb: YdbZioWrapper = new YdbZioWrapper {
      override val defaultRetryOptions: RetryOptions = RetryOptions.default

      override def prepareStatement(query: String): YdbPreparedStatement = ???

      override def prepareFromResource(caller: Class[_], name: String): YdbPreparedStatement = ???

      override def createTable(path: String, description: TableDescription, settings: CreateTableSettings): Task[Unit] =
        ???

      override def dropTable(path: String, settings: DropTableSettings): Task[Unit] = ???

      override def describeTable(path: String, settings: DescribeTableSettings): Task[TableDescription] = ???

      override def executeSchema(query: String): Task[Unit] = ???

      override def executeSchema(query: String, options: QueryOptions): Task[Unit] = ???

      override def execute(query: String): TxUIO[YdbResult] = ???

      override def execute(query: String, options: QueryOptions): TxUIO[YdbResult] = ???

      override def execute(query: String, params: Params): TxUIO[YdbResult] = ???

      override def execute(query: String, params: Params, options: QueryOptions): TxUIO[YdbResult] = ???

      override def execute(ps: YdbPreparedStatement): TxUIO[YdbResult] = ???

      override def execute(ps: YdbPreparedStatement, options: QueryOptions): TxUIO[YdbResult] = ???

      override def execute(ps: YdbPreparedStatement, params: Params): TxUIO[YdbResult] = ???

      override def execute(ps: YdbPreparedStatement, params: Params, options: QueryOptions): TxUIO[YdbResult] = ???

      override def scanQuery[T](
          query: String,
          params: Params,
          mapper: ResultSetReader => T,
          settings: ExecuteScanQuerySettings,
          queryOptions: QueryOptions): ZStream[Any, Throwable, T] = ???

      override def explain(query: String): TxUIO[ExplainDataQueryResult] = ???

      override def explain(query: String, options: QueryOptions): TxUIO[ExplainDataQueryResult] = ???

      override def readTable[T](
          table: String,
          mapper: ResultSetReader => T,
          settings: ReadTableSettings): ZStream[Any, Throwable, T] = ???

      override def commit: TxTask[Unit] = ???

      override def abort: TxTask[Unit] = ???

      override protected def managedSession: ZManaged[Any, Throwable, Session] = ???

      override protected def runTransaction[R, E, A](
          tx: TxEnv[R],
          action: Tx[R, E, A],
          tryNumber: Int): UIO[Exit[TxError[E], A]] = ???
    }

    override def init: Task[Unit] = ???

    override def tableName: String = ???

    override protected def prometheusRegistry: PrometheusRegistry = TestBrokerQueueService.prometheusRegistry
  }

  def apply(namespace: Namespace) = new BrokerQueueService(storage, namespace) {

    override def putImageEvent(imageRecord: ImageRecord, now: Instant): TxRIO[BaseEnv, Unit] =
      ZIO.unit

    override def putErasureEvent(imageRecord: ImageRecord, now: Instant): TxRIO[BaseEnv, Unit] =
      ZIO.unit

    override def putErasureEvent(imageId: ImageId, now: Instant): TxRIO[BaseEnv, Unit] =
      ZIO.unit
  }
}
