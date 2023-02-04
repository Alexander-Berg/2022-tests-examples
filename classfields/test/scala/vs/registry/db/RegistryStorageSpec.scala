package vs.registry.db

import bootstrap.config.Source
import bootstrap.metrics.Registry
import bootstrap.tracing.$
import bootstrap.ydb.*
import vertis.vasgen.document.RawDocument
import vs.registry.domain.RegistryRecord
import vs.registry.sample.{RawDocumentSamples, RegistrySamples}
import zio.*
import zio.test.*
import zio.test.Assertion.*

object RegistryStorageSpec extends ZIOSpecDefault {

  type ConfiguredRegistryStorage = RegistryStorage
  type ConfiguredYDB             = YDB[TEST]

  val suites =
    suite("RegistryStorage.Service")(
      test("Create empty table")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredRegistryStorage]
              _       <- storage.createTableIfAbsent
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Attempt to create database second time")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[ConfiguredRegistryStorage]
              _       <- storage.createTableIfAbsent
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select none") {
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredRegistryStorage]
              record <- ydb.withTx(tx =>
                storage
                  .retrieve(tx, RegistrySamples.one_0_1.pk)
                  .mapError(TxError.abort),
              )
            } yield record
          ).exit,
        )(succeeds(isNone))
      },
      test("Select empty") {
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredRegistryStorage]
              record <- ydb.withTx(tx =>
                storage
                  .retrieve(
                    tx,
                    List(
                      RegistrySamples.one_0_1.pk,
                      RegistrySamples.two_0_1.pk,
                      RegistrySamples.three_0_1.pk,
                    ),
                  )
                  .mapError(TxError.abort),
              )
            } yield record
          ).exit,
        )(succeeds(isEmpty))
      },
      test("Store one")(
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
      test("Select stored record")(
        for {
          ydb     <- ZIO.service[ConfiguredYDB]
          storage <- ZIO.service[ConfiguredRegistryStorage]
          maybe <- ydb.withTx(tx =>
            storage
              .retrieve(tx, RegistrySamples.one_0_1.pk)
              .mapError(TxError.abort),
          )
          record <- ZIO
            .fromOption(maybe)
            .orElseFail(new IllegalArgumentException(s"Record is empty"))
        } yield recordsAreEqual(record, RegistrySamples.one_0_1),
      ),
      test("Store three")(
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredRegistryStorage]
              _ <- ydb.withTx(tx =>
                storage
                  .store(tx, RegistrySamples.three_0_1)
                  .mapError(TxError.abort),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select one,three") {

        for {
          ydb     <- ZIO.service[ConfiguredYDB]
          storage <- ZIO.service[ConfiguredRegistryStorage]
          record <- ydb.withTx(tx =>
            storage
              .retrieve(
                tx,
                List(
                  RegistrySamples.one_0_1.pk,
                  RegistrySamples.two_0_1.pk,
                  RegistrySamples.three_0_1.pk,
                ),
              )
              .mapError(TxError.abort),
          )
        } yield sequencesAreEqual(
          record,
          List(RegistrySamples.one_0_1, RegistrySamples.three_0_1),
        )
      },
      test("Store two, three")(
        assertZIO(
          (
            for {
              ydb     <- ZIO.service[ConfiguredYDB]
              storage <- ZIO.service[ConfiguredRegistryStorage]
              _ <- ydb.withTx(tx =>
                storage
                  .store(
                    tx,
                    List(RegistrySamples.two_0_1, RegistrySamples.three_0_1),
                  )
                  .mapError(TxError.abort),
              )
            } yield ()
          ).exit,
        )(succeeds(isUnit)),
      ),
      test("Select one,two,three") {

        for {
          ydb     <- ZIO.service[ConfiguredYDB]
          storage <- ZIO.service[ConfiguredRegistryStorage]
          record <- ydb.withTx(tx =>
            storage
              .retrieve(
                tx,
                List(
                  RegistrySamples.one_0_1.pk,
                  RegistrySamples.two_0_1.pk,
                  RegistrySamples.three_0_1.pk,
                ),
              )
              .mapError(TxError.abort),
          )
        } yield sequencesAreEqual(
          record,
          List(
            RegistrySamples.one_0_1,
            RegistrySamples.two_0_1,
            RegistrySamples.three_0_1,
          ),
        )
      },
      test("Store and select four")(
        for {
          ydb     <- ZIO.service[ConfiguredYDB]
          storage <- ZIO.service[ConfiguredRegistryStorage]
          _ <- ydb.withTx(tx =>
            storage.store(tx, RegistrySamples.four_0_1).mapError(TxError.abort),
          )
          maybe <- ydb.withTx(tx =>
            storage
              .retrieve(tx, RegistrySamples.four_0_1.pk)
              .mapError(TxError.abort),
          )
          record <- ZIO
            .fromOption(maybe)
            .orElseFail(new IllegalArgumentException(s"Record is empty"))
          rawDocument <- ZIO.attempt(
            RawDocument.parseFrom(record.document.data.value.toByteArray),
          )
        } yield assertTrue(
          RegistrySamples.four_0_1 == record &&
            rawDocument == RawDocumentSamples.upsert1,
        ),
      ),
    )

  val domain = "test"

  val layer: ZLayer[
    Any,
    Throwable,
    $ &
      Registry &
      Clock &
      YDB[Source.Const[YDB.Config]] &
      ConfiguredRegistryStorage,
  ] = Scope.default >>> Context.ydbLayer >+> storageLayer

  private val storageLayer = ZLayer.fromZIO(
    for {
      ydb <- ZIO.service[YDB[TEST]]
    } yield RegistryStorageImpl[TEST](
      ydb,
      RegistryStorage.Config(domain = "test"),
    ),
  )

  override def spec =
    suites.provideLayerShared(layer) @@ TestAspect.sequential @@
      TestAspect.tag("testcontainers")

  final def sequencesAreEqual(
    actual: Seq[RegistryRecord],
    sample: Seq[RegistryRecord],
  ): TestResult =
    assertTrue(
      actual.zip(sample).forall(t => recordsAreEqual(t._1, t._2).isSuccess),
    )

  def recordsAreEqual(
    actual: RegistryRecord,
    sample: RegistryRecord,
  ): TestResult =
    assertTrue(actual.pk == sample.pk) &&
      assertTrue(actual.version == sample.version) &&
      assertTrue(actual.epoch == sample.epoch) &&
      assertTrue(actual.ttl == sample.ttl) &&
      assertTrue(actual.modifyTimestamp == sample.modifyTimestamp) &&
      assertTrue(actual.document.status == sample.document.status) &&
      assertTrue(actual.document.data == sample.document.data)

}
