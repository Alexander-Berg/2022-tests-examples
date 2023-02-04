package vs.registry.consumer

import bootstrap.config.Source
import bootstrap.metrics.Registry
import bootstrap.test.TestBase
import bootstrap.testcontainers.ydb.YdbContainer
import bootstrap.tracing.$
import bootstrap.ydb.{YDB, YdbTest}
import ru.yandex.vertis.ydb.YdbWrapperContainer
import vs.core.distribution.InstanceId
import vs.registry.db.*
import vs.registry.document.RegistryDocument
import vs.registry.domain.*
import vs.registry.producer.*
import vs.registry.service.*
import zio.{Clock, durationInt as _, *}

import scala.concurrent.duration.DurationInt

//noinspection TypeAnnotation
object Context {

  val domain = "test"

  val container: YdbWrapperContainer = YdbWrapperContainer.stable

  type ConfiguredRegistryProducer   = RegistryProducerImpl[TEST]
  type ConfiguredRegistryConsumer   = RegistryConsumerImpl[TEST]
  type ConfiguredQueueStorage       = QueueStorage[TEST]
  type ConfiguredQueueOffsetStorage = QueueOffsetStorage
  type ConfiguredYDB                = YDB[TEST]

  private val instanceIdLayer = ZLayer.succeed(InstanceId("test"))

  val bootstrap = TestBase.bootstrap

  val ydbLayer: ZLayer[Scope & $ & Clock & Registry, Nothing, YDB[TEST]] =
    YdbContainer.stable.orDie >>>
      (YdbTest.fromContainer[TEST](
        Source.Const(
          YDB.Config(
            endpoint = "",
            database = "",
            tablePrefix = "/local",
            denyCrossDc = false,
            session = YDB.SessionConfig(
              minPoolSize = 10,
              maxPoolSize = 50,
              acquireTimeout = zio.Duration.fromScala(5.seconds),
              maxAcquireTries = 3,
              maxIdleTime = zio.Duration.fromScala(1.minute),
              keepAliveTimeout = zio.Duration.fromScala(5.minutes),
            ),
            keepQueryText = true,
            queryCacheSize = 16,
          ),
        ),
      ))

  val registryStorageLayer = ZLayer.fromZIO(
    for {
      ydb <- ZIO.service[YDB[TEST]]
    } yield RegistryStorageImpl[TEST](
      ydb,
      RegistryStorage.Config(domain = "test"),
    ),
  )

  val queueStorageLayer = ZLayer.fromZIO(
    for {
      ydb  <- ZIO.service[YDB[TEST]]
      eRef <- Ref.make(Map.empty[QueueId, QueueEventStatements])
    } yield QueueStorage[TEST](ydb, QueueStorage.Config(domain = domain), eRef),
  )

  val queueElementStorageLayer = ZLayer.fromZIO(
    for {
      ydb <- ZIO.service[YDB[TEST]]
      ref <- Ref.make(Map.empty[QueueId, QueueOffsetStatements])
    } yield QueueOffsetStorageImpl[TEST](
      ydb,
      QueueOffsetStorage.Config(domain = domain),
      ref,
    ),
  )

  private val disassemblerLayer = ZLayer
    .succeed(new RegistryDocumentDisassembler())

  val fieldMetaStorageLayer = ZLayer.fromZIO(
    for {
      ydb <- ZIO.service[YDB[TEST]]
    } yield FieldMetaStorageImpl(
      ydb,
      FieldMetaStorage.Config(table = "test/index/meta"),
    ),
  )

  val fieldMetaValidatorLayer =
    (bootstrap ++ fieldMetaStorageLayer) >>> FieldMetaValidator.live

  val recordAssemblyLayer = ZLayer.succeed(
    RecordAssembly(RecordAssembly.Config(zio.Duration.fromScala(1.days))),
  )

  val shardLockerLayer = ShardLockerMock.live

  val handlerZio =
    for {
      log <- ZIO.service[$]
    } yield new FailureHandler {

      override def report(documents: Chunk[RegistryDocument]): Task[Unit] =
        ZIO
          .when(documents.nonEmpty)(
            log.error(s"Documents failed: ${documents.size -> "count"}"),
          )
          .unit

      override def attachShards(shards: Set[ShardId]): Task[Unit] = ZIO.unit

    }

  val failureHandlerLayer = ZLayer
    .fromZIO[$, Throwable, FailureHandler](handlerZio)

  val consumer: ZLayer[Scope, Throwable, RegistryConsumerImpl[TEST]] = {
    ZLayer.makeSome[Scope, RegistryConsumerImpl[TEST]](
      bootstrap,
      ydbLayer,
      queueStorageLayer,
      queueElementStorageLayer,
      instanceIdLayer,
      shardLockerLayer,
      disassemblerLayer,
      failureHandlerLayer,
      ZLayer.fromZIO(
        RegistryConsumer.create[TEST](
          RegistryConsumer.Config(128, 1000.milliseconds, "test", ""),
          QueueId.main,
        ),
      ),
    )
  }

  val producer: ZLayer[Scope, Throwable, RegistryProducerImpl[TEST]] = ZLayer
    .makeSome[Scope, RegistryProducerImpl[TEST]](
      bootstrap,
      ydbLayer,
      registryStorageLayer,
      queueStorageLayer,
      instanceIdLayer,
      fieldMetaValidatorLayer,
      recordAssemblyLayer,
      shardLockerLayer,
      ZLayer.fromZIO(
        RegistryProducer.create[TEST](RegistryProducer.Config(16, 128)),
      ),
    )

  val live: ZLayer[
    Any,
    Throwable,
    Clock &
      QueueStorage[TEST] &
      RegistryStorageImpl[TEST] &
      $ &
      Registry &
      YDB[TEST] &
      QueueOffsetStorageImpl[TEST] &
      RegistryProducerImpl[TEST] &
      ShardLockerMock &
      ShardLocker &
      RegistryConsumerImpl[TEST],
  ] =
    Scope.default >>>
      ZLayer.makeSome[
        Scope,
        Clock &
          QueueStorage[TEST] &
          RegistryStorageImpl[TEST] &
          $ &
          Registry &
          YDB[TEST] &
          QueueOffsetStorageImpl[TEST] &
          RegistryProducerImpl[TEST] &
          ShardLockerMock &
          ShardLocker &
          RegistryConsumerImpl[TEST],
      ](
        bootstrap,
        queueStorageLayer,
        registryStorageLayer,
        ydbLayer,
        queueElementStorageLayer,
        producer,
        shardLockerLayer,
        consumer,
      )

}
