package vs.registry.db

import bootstrap.config.Source
import bootstrap.metrics.Registry
import bootstrap.tracing.$
import bootstrap.ydb.*
import com.yandex.ydb.table.settings.DescribeTableSettings
import ru.yandex.vertis.ydb.YdbWrapperContainer
import strict.Uint64
import vs.registry.domain.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

object QueueOffsetStorageSpec extends ZIOSpecDefault {

  type ConfiguredStorage = QueueOffsetStorage
  type ConfiguredYDB     = YDB[TEST]
  val domain                         = "test"
  val container: YdbWrapperContainer = YdbWrapperContainer.stable
  val settings                       = new DescribeTableSettings()

  val layer: ZLayer[
    Any,
    Throwable,
    $ & Registry & Clock & YDB[Source.Const[YDB.Config]] & ConfiguredStorage,
  ] = Scope.default >>> Context.ydbLayer >+> storageLayer

  private val consumerId = ConsumerId("smell")

  private val suites =
    suite("QueueOffsetStorage.Service")(
      test("Create empty table")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredStorage]
              _       <- storage.createTableIfAbsent(QueueId.main)
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Attempt to create database second time")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredStorage]
              _       <- storage.createTableIfAbsent(QueueId.main)
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select default record") {
        for {
          ydb     <- ZIO.service[ConfiguredYDB]
          storage <- ZIO.service[ConfiguredStorage]
          record <- ydb.withTx(tx =>
            storage
              .retrieve(QueueId.main, tx)(ShardId(0), consumerId)
              .mapError(TxError.abort),
          )
        } yield assertTrue(
          record == QueueOffset(ShardId(0), consumerId, Uint64(0)),
        )
      },
      test("Store")(
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredStorage]
              _ <- ydb.withTx(tx =>
                storage
                  .store(QueueId.main, tx)(
                    QueueOffset(ShardId(0), consumerId, Uint64(1L)),
                  )
                  .mapError(TxError.abort),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Store next offset")(
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredStorage]
              _ <- ydb.withTx(tx =>
                storage
                  .store(QueueId.main, tx)(
                    QueueOffset(ShardId(0), consumerId, Uint64(2L)),
                  )
                  .mapError(TxError.abort),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select stored record") {
        for {
          ydb     <- ZIO.service[ConfiguredYDB]
          storage <- ZIO.service[ConfiguredStorage]
          record <- ydb.withTx(tx =>
            storage
              .retrieve(QueueId.main, tx)(ShardId(0), consumerId)
              .mapError(TxError.abort),
          )
        } yield assertTrue(
          record == QueueOffset(ShardId(0), consumerId, Uint64(2L)),
        )
      },
    ) @@ TestAspect.tag("testcontainers")

  private val storageLayer = ZLayer.fromZIO(
    for {
      ydb <- ZIO.service[ConfiguredYDB]
      ref <- Ref.make(Map.empty[QueueId, QueueOffsetStatements])
    } yield QueueOffsetStorageImpl(
      ydb,
      QueueOffsetStorage.Config(domain = domain),
      ref,
    ),
  )

  override def spec = suites.provideLayerShared(layer) @@ TestAspect.sequential

  settings.setIncludeTableStats(true)
  settings.setIncludeShardKeyBounds(true)

}
