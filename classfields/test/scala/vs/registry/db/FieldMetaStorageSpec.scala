package vs.registry.db

import bootstrap.config.Source
import bootstrap.metrics.Registry
import bootstrap.tracing.$
import bootstrap.ydb.YDB
import zio.test.*
import zio.test.Assertion.*
import zio.{durationInt as _, *}

object FieldMetaStorageSpec extends ZIOSpecDefault {

  import FieldMetaSamples.*

  val suites =
    suite("FieldMetaStorage.Service")(
      test("Create empty table twice ")(
        for {
          storage <- ZIO.service[FieldMetaStorage]
          _       <- storage.createTableIfAbsent
          _       <- storage.createTableIfAbsent
        } yield assert(())(isUnit),
      ),
      test("Store batch of new fields")(
        for {
          storage <- ZIO.service[FieldMetaStorage]
          _       <- storage.store(Chunk(a1str, b1i32, c1str, d1i64))
        } yield assert(())(isUnit),
      ),
      test("Epoch 1: Select stored fields") {
        for {
          storage <- ZIO.service[FieldMetaStorage]

          fields <- storage.retrieve(
            Chunk(
              1L -> a1str.name,
              1L -> b1i32.name,
              1L -> "nonexistent",
              1L -> d1i64.name,
              1L -> c1str.name,
            ),
          )

        } yield assert(fields)(hasSameElements(Seq(a1str, b1i32, d1i64, c1str)))
      },
      test("Store batch of existing fields with same epoch")(
        assertZIO(
          (
            for {
              storage <- ZIO.service[FieldMetaStorage]
              _       <- storage.store(Chunk(a1str, b1i32))
            } yield ()
          ).exit,
        )(fails(anything)),
      ),
      test("Store batch of existing fields with another epoch")(
        for {
          storage <- ZIO.service[FieldMetaStorage]
          _       <- storage.store(Chunk(a2i32, b2str, e1str))
        } yield assert(())(isUnit),
      ),
      test("Epoch 2: Select stored fields") {
        for {
          storage <- ZIO.service[FieldMetaStorage]
          fields <- storage.retrieve(
            Chunk(
              2L -> "nonexistent",
              2L -> a2i32.name,
              2L -> b2str.name,
              2L -> d1i64.name,
              2L -> c1str.name,
              2L -> e1str.name,
            ),
          )
        } yield assert(fields)(hasSameElements(Seq(a2i32, b2str)))
      },
    ) @@ TestAspect.sequential @@ TestAspect.tag("testcontainers")

  val table = "test/index/meta"

  private val storageLayer = ZLayer.fromZIO(
    for {
      ydb <- ZIO.service[YDB[TEST]]
    } yield FieldMetaStorageImpl(ydb, FieldMetaStorage.Config(table = table)),
  )

  override def spec: Spec[TestEnvironment & Scope, Throwable] =
    suites.provideSomeShared(layer)

  val layer: ZLayer[
    Any,
    Throwable,
    $ &
      Registry &
      Clock &
      YDB[Source.Const[YDB.Config]] &
      FieldMetaStorageImpl[TEST],
  ] = Scope.default >>> Context.ydbLayer >+> storageLayer

}
