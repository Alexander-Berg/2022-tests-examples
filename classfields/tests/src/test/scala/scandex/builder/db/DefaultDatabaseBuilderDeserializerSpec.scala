package scandex.builder.db

import scandex.builder.db.Schema.idGenerator
import scandex.builder.serde.DatabaseBuilderDeserializer
import scandex.db.DefaultDatabase
import scandex.db.index.DocumentId
import scandex.model.*
import scandex.model.gen.*
import scandex.model.meta.FieldDataTypeMeta.IndexType
import strict.*
import tests.strict.types.*
import zio.*
import zio.stream.ZStream
import zio.test.*
import zio.test.Assertion.*

import java.io.ByteArrayOutputStream
import java.nio.channels.Channels
import scala.collection.*

object DefaultDatabaseBuilderDeserializerSpec extends zio.test.ZIOSpecDefault {

  private[db] val schema: DocumentGenSchema[Utf8] = DocumentGenSchema(
    idGenerator,
    SingleValueFieldGen("int8", IndexType.FILTER, anyInt8),
    MultiValuedFieldGen("int32", IndexType.RANGE, anyInt32),
    StorageFieldGen("singleString", anyUtf8),
  )

  private val keys   = mutable.Map.empty[Utf8, DocumentId]
  private val sample = mutable.Map.empty[Utf8, Document[Utf8]]

  private var serialized = Array.empty[Byte]

  override def spec =
    suite("DefaultDatabase")(
      test("fill database") {
        checkN(1000)(schema()) { doc: Document[Utf8] =>
          for {
            builder <- ZIO.service[DatabaseBuilderImpl[Utf8]]
            _       <- builder.upsert(doc)
          } yield {
            sample.put(doc.primaryKey, doc)
            assert(())(isUnit)
          }
        }
      },
      test("serialize and write") {
        for {
          builder <- ZIO.service[DatabaseBuilderImpl[Utf8]]
          db      <- builder.build
          _ <-
            ZIO.foreachDiscard(sample.keys)(key =>
              db.asInstanceOf[DefaultDatabase[Utf8]]
                .pkIndex
                .getDocumentId(key)
                .map(id => keys.put(key, id)),
            )
          dbIsSame <- isDatabaseSameAsSample(
            db.asInstanceOf[DefaultDatabase[Utf8]],
          )
          baos <- ZIO.attempt(new ByteArrayOutputStream())
          _    <- db.writeTo(Channels.newChannel(baos))
          _ <- ZIO.attempt {
            baos.close()
            serialized = baos.toByteArray
          }
        } yield assertTrue(dbIsSame) && assertTrue(serialized.nonEmpty)
      },
      test("Deserialize database") {
        for {
          db <- DefaultDatabase
            .deserialize(ZStream.fromChunk(Chunk.fromArray(serialized)))
          dbIsSame <- isDatabaseSameAsSample(db)
        } yield assertTrue(dbIsSame)
      },
      test("Deserialize builder") {
        for {
          builder <- DatabaseBuilderDeserializer
            .deserialize(ZStream.fromChunk(Chunk.fromArray(serialized)))
          db <- builder.build
          dbIsSame <- isDatabaseSameAsSample(
            db.asInstanceOf[DefaultDatabase[Utf8]],
          )
        } yield assert(db)(isSubtype[DefaultDatabase[?]](anything)) &&
          assertTrue(dbIsSame)
      },
    ).provideCustomLayerShared(dbLayer) @@ TestAspect.sequential

  private def isDatabaseSameAsSample(db: DefaultDatabase[?]): Task[Boolean] = {
    val dbUtf8 = db.asInstanceOf[DefaultDatabase[Utf8]]
    ZIO.forall(sample.keys)(key =>
      for {
        id <- dbUtf8.pkIndex.getDocumentId(key)
        value <-
          dbUtf8
            .getStorageIndex[Int8]("int8")
            .fold[Task[Int8]](ZIO.fail(new IllegalArgumentException))(index =>
              index.values(id).map(_.head),
            )
      } yield {
        val isContainsKey = keys.get(key).contains(id)
        val isContainsValue = sample
          .get(key)
          .exists(
            _.fields
              .get("int8")
              .exists { t =>
                val v = t.values.asInstanceOf[SortedSet[Int8]].contains(value)
                v
              },
          )

        isContainsKey && isContainsValue
      },
    )
  }

  private val dbLayer = ZLayer(DatabaseBuilder.createImpl[Utf8]).orDie

}
