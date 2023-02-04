package vs.registry.db

import bootstrap.metrics.Registry
import bootstrap.tracing.$
import bootstrap.ydb.*
import ru.yandex.vertis.ydb.YdbWrapperContainer
import strict.*
import vs.registry.domain.*
import vs.registry.sample.RegistrySamples
import zio.*
import zio.test.*
import zio.test.Assertion.*

object QueueDocumentSpec extends ZIOSpecDefault {

  type ConfiguredRegistryStorage = RegistryStorage
  type ConfiguredQueueStorage    = QueueStorage[TEST]
  type ConfiguredYDB             = YDB[TEST]

  val suites =
    suite("QueueDocument")(
      test("Create necessary tables")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredRegistryStorage]
              _       <- storage.createTableIfAbsent
              storage <- ZIO.service[ConfiguredQueueStorage]
              _       <- storage.createTableIfAbsent(QueueId.main)
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Store registry")(
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredRegistryStorage]
              _ <- ydb.withTx(tx =>
                storage
                  .store(tx, RegistrySamples.one_0_1)
                  .mapError(TxError.abort),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Store queue event")(
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredQueueStorage]
              _ <-
                ydb.withTx(tx =>
                  storage
                    .store(QueueId.main, tx)(
                      QueueEvent(
                        ShardId(0),
                        Uint64(1),
                        "one",
                        0,
                        0,
                        Status.Exists,
                        Timestamp(1000),
                      ),
                    )
                    .mapError(TxError.abort),
                ) *>
                  ydb.withTx(tx =>
                    storage
                      .store(QueueId.main, tx)(
                        QueueEvent(
                          ShardId(0),
                          Uint64(2),
                          "one",
                          0,
                          1,
                          Status.Exists,
                          Timestamp(1000),
                        ),
                      )
                      .mapError(TxError.abort),
                  ) *>
                  ydb.withTx(tx =>
                    storage
                      .store(QueueId.main, tx)(
                        QueueEvent(
                          ShardId(0),
                          Uint64(3),
                          "one",
                          0,
                          2,
                          Status.Exists,
                          Timestamp(1000),
                        ),
                      )
                      .mapError(TxError.abort),
                  )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select documents") {
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredQueueStorage]

              (record, idx) <-
                storage.consumePortion(QueueId.main)(
                  Map(ShardId(0) -> Uint64(0)),
                  65535,
                )

            } yield record -> idx
          ).exit,
        )(
          succeeds(
            hasField[(Seq[QueueDocument], Map[ShardId, Uint64]), Seq[
              QueueDocument,
            ]](
              "records",
              _._1,
              equalTo(
                Chunk(
                  QueueDocument(
                    ShardId(0),
                    Uint64(2),
                    "one",
                    0,
                    1,
                    DocumentData(status = Status.Exists, Bytes("one".getBytes)),
                  ),
                ),
              ),
            ),
          ) &&
            succeeds(
              hasField[(Seq[QueueDocument], IdxMap), IdxMap](
                "idx",
                _._2,
                equalTo(Map(ShardId(0) -> Uint64(3))),
              ),
            ),
        )
      },
      test("Generate many documents") {
        assertZIO(
          (
            for {

              storage <- ZIO.service[ConfiguredQueueStorage]

              (records, idx) <-
                storage.consumePortion(QueueId.main)(
                  Map(ShardId(0) -> Uint64(0)),
                  65536,
                )
            } yield records -> idx
          ).exit,
        )(
          succeeds(
            hasField[(Seq[QueueDocument], Map[ShardId, Uint64]), Seq[
              QueueDocument,
            ]](
              "records",
              _._1,
              equalTo(
                List(
                  QueueDocument(
                    ShardId(0),
                    Uint64(2),
                    "one",
                    0,
                    1,
                    DocumentData(status = Status.Exists, Bytes("one".getBytes)),
                  ),
                ),
              ),
            ),
          ) &&
            succeeds(
              hasField[(Seq[QueueDocument], IdxMap), IdxMap](
                "idx",
                _._2,
                equalTo(Map(ShardId(0) -> Uint64(3))),
              ),
            ),
        )
      },
    ) @@ TestAspect.tag("testcontainers")

  val domain                         = "test"
  val container: YdbWrapperContainer = YdbWrapperContainer.stable

  val layer: ZLayer[
    Any,
    Throwable,
    $ & Registry & YDB[TEST] & QueueStorage[TEST] & RegistryStorageImpl[TEST],
  ] =
    Scope.default >>> Context.ydbLayer >+>
      (queueStorageLayer ++ registryStorageLayer)

  private val registryStorageLayer = ZLayer.fromZIO(
    for {
      ydb <- ZIO.service[YDB[TEST]]
    } yield RegistryStorageImpl[TEST](
      ydb,
      RegistryStorage.Config(domain = "test"),
    ),
  )

  private val queueStorageLayer = ZLayer.fromZIO(
    for {
      ydb  <- ZIO.service[YDB[TEST]]
      eRef <- Ref.make(Map.empty[QueueId, QueueEventStatements])
    } yield QueueStorage[TEST](ydb, QueueStorage.Config(domain = domain), eRef),
  )

  override def spec = suites.provideLayerShared(layer) @@ TestAspect.sequential

}
