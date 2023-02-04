package scandex.builder.db

import scandex.bitset.mutable.LongArrayBitSet
import scandex.builder.db.Schema.uuIdGen
import scandex.db.SimpleDatabase
import scandex.db.index.DocumentId
import scandex.model.Document
import scandex.model.gen.{DocumentGenSchema, StorageFieldGen}
import strict.{Int8, PrimitiveTypeTag, Utf8}
import tests.strict.types.{anyBool, anyInt8, anyUtf8}
import zio.*
import zio.stream.ZStream
import zio.test.*
import zio.test.Assertion.*

import scala.collection.mutable

object SimpleDatabaseBuilderDeserializerSpec extends ZIOSpecDefault {

  private[db] val storageSchema = DocumentGenSchema(
    uuIdGen,
    StorageFieldGen("int8", anyInt8),
    StorageFieldGen("singleString", anyUtf8),
    StorageFieldGen("isSomething", anyBool),
  )

  private[db] val isActiveGenerator = Gen
    .weighted((Gen.const(true), 8), (Gen.const(false), 2))

  private val keys         = mutable.Map.empty[Utf8, DocumentId]
  private val activeDocs   = mutable.Map.empty[Utf8, Document[Utf8]]
  private val inactiveDocs = mutable.Set.empty[Utf8]

  private val dbLayer = ZLayer
    .scoped {
      DatabaseBuilder.createImpl[Utf8]
    }
    .orDie
    .++(ZLayer.scoped(Ref.make(Chunk[Byte]())))

  override def spec =
    suite("SimpleDB")(
      test("fill database") {
        checkN(100)(storageSchema(), isActiveGenerator) {
          case (doc: Document[Utf8], isActive: Boolean) =>
            for {
              builder <- ZIO.service[DatabaseBuilderImpl[Utf8]]
              _       <- builder.upsert(doc)
              _       <- builder.remove(doc.primaryKey).when(!isActive)
            } yield {
              if (isActive) {
                activeDocs.put(doc.primaryKey, doc)
              } else {
                inactiveDocs.add(doc.primaryKey)
              }

              assertTrue(true)
            }
        }
      },
      test("serialize and write") {
        for {
          builder <- ZIO.service[DatabaseBuilderImpl[Utf8]]
          db      <- builder.build
          _ <-
            ZIO.foreachDiscard(activeDocs.keys ++ inactiveDocs)(key =>
              db.asInstanceOf[SimpleDatabase[Utf8]]
                .pkIndex
                .getDocumentId(key)
                .map(id => keys.put(key, id)),
            )
          serialized <- db.serializeToStream.runCollect
          ref        <- ZIO.service[Ref[Chunk[Byte]]]
          _          <- ref.set(serialized)
        } yield assertTrue(serialized.nonEmpty)
      },
      test("Deserialize database") {
        for {
          ref        <- ZIO.service[Ref[Chunk[Byte]]]
          serialized <- ref.get
          _          <- ZIO.debug(s"Really active = ${activeDocs.keys}")
          _          <- ZIO.debug(s"Really inactive = ${inactiveDocs}")
          db <- SimpleDatabase
            .deserializer
            .deserialize(ZStream.fromChunk(serialized))
          dbIsSame <- assertDatabase(db.asInstanceOf[SimpleDatabase[Utf8]])
        } yield dbIsSame
      },
      test("Deserialize builder") {
        for {
          ref        <- ZIO.service[Ref[Chunk[Byte]]]
          serialized <- ref.get
          db <- SimpleDatabase
            .deserializer
            .deserialize(ZStream.fromChunk(serialized))
          dbIsSame <- assertDatabase(db.asInstanceOf[SimpleDatabase[Utf8]])
        } yield assert(db)(isSubtype[SimpleDatabase[?]](anything)) && dbIsSame
      },
    ).provideCustomLayerShared(dbLayer) @@ TestAspect.sequential

  private def assertDatabase(
    database: SimpleDatabase[?],
  ): ZIO[Any, Throwable, TestResult] = {
    val dbUtf8 = database.asInstanceOf[SimpleDatabase[Utf8]]
    ZIO
      .foreach {
        activeDocs.keys ++ inactiveDocs
      } { key =>
        for {
          id <- dbUtf8.pkIndex.getDocumentId(key)
          isActive <- {
            val bitset = new LongArrayBitSet(keys.keys.size.toLong)
            bitset.update(id, value = true)
            dbUtf8.sieveIndex.siftActive(bitset)
          }
          int8values <-
            dbUtf8
              .getStorageIndex[Int8]("int8", PrimitiveTypeTag.Int8Type)
              .fold[Task[Set[Int8]]](ZIO.fail(new IllegalArgumentException))(
                index => index.values(id).map(_.toSet),
              )
          utf8values <-
            dbUtf8
              .getStorageIndex[Utf8]("singleString", PrimitiveTypeTag.Utf8Type)
              .fold[Task[Set[Utf8]]](ZIO.fail(new IllegalArgumentException))(
                index => index.values(id).map(_.toSet),
              )
        } yield {
          val expectedActualDoc = TestDoc(
            keys(key),
            true,
            activeDocs
              .get(key)
              .flatMap(
                _.fields.get("int8").map(_.values.asInstanceOf[Set[Int8]]),
              )
              .getOrElse(Set.empty[Int8]),
            activeDocs
              .get(key)
              .flatMap(
                _.fields
                  .get("singleString")
                  .map(_.values.asInstanceOf[Set[Utf8]]),
              )
              .getOrElse(Set.empty[Utf8]),
          )

          val actualDoc = TestDoc(id, isActive, int8values, utf8values)
          assertTrue(
            (inactiveDocs.contains(key) && !actualDoc.isActive) ||
              actualDoc == expectedActualDoc,
          )
        }
      }
      .map(asserts => TestResult.all(asserts.toSeq*))
  }

  private case class TestDoc(
      id: DocumentId,
      isActive: Boolean,
      int8: Set[Int8],
      singleString: Set[Utf8],
  )

}
