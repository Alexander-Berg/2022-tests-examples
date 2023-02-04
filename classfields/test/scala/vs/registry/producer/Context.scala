package vs.registry.producer

import bootstrap.config.Source
import bootstrap.metrics.Registry
import bootstrap.test.TestBase
import bootstrap.testcontainers.ydb.YdbContainer
import bootstrap.tracing.$
import bootstrap.ydb.{YDB, YdbTest}
import vs.core.distribution.InstanceId
import vs.registry.db.*
import vs.registry.domain.QueueId
import vs.registry.service.*
import zio.{Clock, Ref, Scope, ZIO, ZLayer}

import scala.concurrent.duration.DurationInt

//noinspection TypeAnnotation
object Context {

  type Type =
    $ &
      Registry &
      Clock &
      YDB[Source.Const[YDB.Config]] &
      FieldMetaStorageImpl[TEST] &
      QueueStorage[TEST] &
      RegistryStorageImpl[TEST] &
      RegistryProducerImpl[TEST]

  val bootstrap = TestBase.bootstrap

  val ydbLayer: ZLayer[
    Scope,
    Nothing,
    $ & Registry & Clock & YDB[Source.Const[YDB.Config]],
  ] =
    YdbContainer.stable.orDie >>>
      (bootstrap >+>
        YdbTest.fromContainer[TEST](
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

  val fieldMetaStorageLayer
    : ZLayer[$ & YDB[TEST], Throwable, FieldMetaStorageImpl[TEST]] = ZLayer
    .fromFunction(
      FieldMetaStorageImpl[TEST](
        _,
        FieldMetaStorage.Config(table = "test/index/meta"),
      ),
    )

  val domain = "test"

  val queueStorageLayer: ZLayer[$ & YDB[TEST], Throwable, QueueStorage[TEST]] =
    ZLayer.fromZIO(
      for {
        ydb  <- ZIO.service[YDB[TEST]]
        eRef <- Ref.make(Map.empty[QueueId, QueueEventStatements])
      } yield QueueStorage[TEST](
        ydb,
        QueueStorage.Config(domain = domain),
        eRef,
      ),
    )

  val registryStorageLayer
    : ZLayer[$ & YDB[TEST], Throwable, RegistryStorageImpl[TEST]] = ZLayer
    .fromFunction(
      RegistryStorageImpl[TEST](_, RegistryStorage.Config(domain = "test")),
    )

  val instanceIdLayer = ZLayer.succeed(InstanceId("test"))

  val shardLockerLayer: ZLayer[Any, Throwable, ShardLockerMock & ShardLocker] =
    ShardLockerMock.live

  val fieldMetaValidatorLayer = FieldMetaValidator.live

  val recordAssemblyLayer = ZLayer.succeed(
    RecordAssembly(RecordAssembly.Config(zio.Duration.fromScala(1.days))),
  )

  val registryProducerLayer =
    (shardLockerLayer ++ fieldMetaValidatorLayer ++ recordAssemblyLayer ++
      instanceIdLayer) >>>
      ZLayer.fromZIO(
        RegistryProducer.create[TEST](RegistryProducer.Config(5, 1000)),
      )

  val layer =
    Scope.default >>> ydbLayer >+>
      (bootstrap ++ fieldMetaStorageLayer ++ queueStorageLayer ++
        registryStorageLayer) >+> registryProducerLayer

}
