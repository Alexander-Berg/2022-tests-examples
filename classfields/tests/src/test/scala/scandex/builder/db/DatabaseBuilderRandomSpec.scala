package scandex.builder.db

import scandex.builder.db.Schema.idGenerator
import scandex.model.Document
import scandex.model.gen.*
import scandex.model.meta.FieldDataTypeMeta.IndexType
import strict.*
import tests.strict.types.*
import zio.test.*
import zio.test.Assertion.*
import zio.{ZIO, ZLayer}

object DatabaseBuilderRandomSpec extends ZIOSpecDefault {

  val schema = DocumentGenSchema(
    idGenerator,
    StorageFieldGen("int8", anyInt8),
    MultiValuedFieldGen("multiString", IndexType.RANGE, anyUtf8),
    StorageFieldGen("singleString", anyUtf8),
  )

  val dbLayer: ZLayer[Any, Nothing, DatabaseBuilderImpl[Utf8]] =
    ZLayer.fromZIO(DatabaseBuilder.createImpl[Utf8]).orDie

  override def spec: Spec[TestEnvironment, Any] =
    suite("DatabaseBuilder")(
      test("db.upsert") {
        checkN(1000)(schema()) { doc: Document[Utf8] =>
          for {
            db <- ZIO.service[DatabaseBuilderImpl[Utf8]]
            _  <- db.upsert(doc)
          } yield assert(())(isUnit)
        }
      },
    ).provideCustomLayerShared(dbLayer)

}
