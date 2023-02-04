package vs.registry.db

import bootstrap.config.Source
import bootstrap.metrics.Registry
import bootstrap.tracing.$
import bootstrap.ydb.*
import strict.{Timestamp, Uint64}
import vs.registry.domain.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

object QueueStorageSpec extends ZIOSpecDefault {

  type ConfiguredQueueStorage = QueueStorage[TEST]
  type ConfiguredYDB          = YDB[TEST]

  val suites =
    suite("QueueStorage.Service")(
      test("Create empty table")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredQueueStorage]
              _       <- storage.createTableIfAbsent(QueueId.main)
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Attempt to create database second time")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredQueueStorage]
              _       <- storage.createTableIfAbsent(QueueId.main)
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select none") {
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredQueueStorage]
              record <- ydb.withTx(tx =>
                storage
                  .retrieve(QueueId.main, tx)(ShardId(0), Uint64(0), 1000)
                  .mapError(TxError.abort),
              )
            } yield record
          ).exit,
        )(succeeds(isEmpty))
      },
      test("Store")(
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredQueueStorage]
              _ <- ydb.withTx(tx =>
                storage
                  .store(QueueId.main, tx)(
                    QueueEvent(
                      ShardId(0),
                      Uint64(1),
                      "some",
                      0,
                      0,
                      Status.Deleted,
                      Timestamp.of(1000),
                    ),
                  )
                  .mapError(TxError.abort),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select stored record") {
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredQueueStorage]
              records <- ydb.withTx(tx =>
                storage
                  .retrieve(QueueId.main, tx)(ShardId(0), Uint64(0), 1000)
                  .mapError(TxError.abort),
              )
              events <- ZIO.foreach(records)(record => record.asEvent)
            } yield events
          ).exit,
        )(
          succeeds(
            equalTo(
              Chunk(
                QueueEvent(
                  ShardId(0),
                  Uint64(1),
                  "some",
                  0,
                  0,
                  Status.Deleted,
                  Timestamp.of(1000),
                ),
              ),
            ),
          ),
        )
      },
      test("Select stored record TxLess") {
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredQueueStorage]
              records <-
                storage.retrieve(QueueId.main)(ShardId(0), Uint64(0), 1000)

              events <- ZIO.foreach(records)(record => record.asEvent)
            } yield events
          ).exit,
        )(
          succeeds(
            equalTo(
              Chunk(
                QueueEvent(
                  ShardId(0),
                  Uint64(1),
                  "some",
                  0,
                  0,
                  Status.Deleted,
                  Timestamp.of(1000),
                ),
              ),
            ),
          ),
        )
      },
      test("Store another shard")(
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredQueueStorage]
              _ <- ydb.withTx(tx =>
                storage
                  .store(QueueId.main, tx)(
                    QueueEvent(
                      ShardId(1),
                      Uint64(3),
                      "other",
                      0,
                      0,
                      Status.Exists,
                      Timestamp.of(1000),
                    ),
                  )
                  .mapError(TxError.abort),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select max idx") {
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredQueueStorage]
              max <- ydb.withTx(tx =>
                storage.maxIds(QueueId.main)(tx).mapError(TxError.abort),
              )
            } yield max
          ).exit,
        )(
          succeeds(
            equalTo(Map(ShardId(0) -> Uint64(1), ShardId(1) -> Uint64(3))),
          ),
        )
      },
    )

  val domain = "test"

  val layer: ZLayer[
    Any,
    Throwable,
    $ &
      Registry &
      Clock &
      YDB[Source.Const[YDB.Config]] &
      ConfiguredQueueStorage,
  ] = Scope.default >>> Context.ydbLayer >+> storageLayer

  private val storageLayer = ZLayer.fromZIO(
    for {
      ydb  <- ZIO.service[YDB[TEST]]
      eRef <- Ref.make(Map.empty[QueueId, QueueEventStatements])
    } yield QueueStorage[TEST](ydb, QueueStorage.Config(domain = domain), eRef),
  )

  override def spec =
    suites.provideLayerShared(layer) @@ TestAspect.sequential @@
      TestAspect.tag("testcontainers")

}
