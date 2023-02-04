package scandex.db.serde

import org.testcontainers.shaded.org.apache.commons.io.output.ByteArrayOutputStream
import scandex.builder.db.DatabaseBuilder
import scandex.builder.serde.DatabaseBuilderDeserializer
import scandex.db.segments.serde.SerializableDatabase
import scandex.db.serde.DatabaseStreamDeserializer.{
  Deserialized,
  WaitingForSegments,
}
import scandex.db.{DefaultDatabase, SimpleDatabase}
import scandex.model.*
import scandex.model.meta.FieldDataTypeMeta.{Compression, IndexType}
import strict.*
import zio.*
import zio.stream.ZStream
import zio.test.*

import scala.collection.SortedSet

object DatabaseStreamDeserializerSpec extends ZIOSpecDefault {

  val sample: Chunk[Document[Int64]] = Chunk(
    Document[Int64](
      12,
      Map("int32" -> Field[Int32](SortedSet(Int32(32)), IndexType.STORAGE)),
    ),
    Document[Int64](
      6,
      Map(
        "int8" -> Field[Int8](SortedSet(Int8(11)), IndexType.RANGE),
        "bytes" ->
          Field[Bytes](
            SortedSet(Bytes("01234567890".toByteArray)),
            IndexType.STORAGE,
            Compression.ZSTD,
          ),
      ),
    ),
    Document[Int64](
      8,
      Map(
        "int8"  -> Field[Int8](SortedSet(Int8(10)), IndexType.RANGE),
        "int32" -> Field[Int32](SortedSet(Int32(32)), IndexType.STORAGE),
      ),
    ),
  )

  override def spec =
    suite("DB deserializer")(
      test("Create sample database") {
        for {
          builder <- DatabaseBuilder.create[Int64]
          _       <- ZIO.foreachDiscard(sample)(builder.upsert)
          db      <- builder.build
          ref     <- ZIO.service[Ref[Option[SerializableDatabase]]]
          _       <- ref.set(Some(db))
        } yield assertTrue(
          db.getMeta
            .indexes
            .forall(field =>
              if (field.name == "bytes")
                field.dataTypeMeta.exists(_.compression == Compression.ZSTD)
              else
                field.dataTypeMeta.exists(_.compression == Compression.NONE),
            ),
        )
      },
      test("Ser/De header") {
        for {
          ref <- ZIO.service[Ref[Option[SerializableDatabase]]]

          meta <- ref.get.map(_.get.getMeta)
          serializedHeader <- ZIO.attempt {
            val baos = new ByteArrayOutputStream()
            meta.writeDelimitedTo(baos)
            baos.toByteArray
          }
          state <- DatabaseStreamDeserializer
            .deserializeImpl(
              ZStream.fromChunk(Chunk.fromArray(serializedHeader)),
            )
            .map(_.asInstanceOf[WaitingForSegments])

          stateRef <- ZIO.service[Ref[DatabaseStreamDeserializer.State]]
          _        <- stateRef.set(state)
        } yield assertTrue(
          state.meta == meta && state.currentSegment == 0 &&
            state.currentPosition == 0L,
        )
      },
      test("Ser/De PK") {
        for {
          ref      <- ZIO.service[Ref[Option[SerializableDatabase]]]
          db       <- ref.get.map(_.get.asInstanceOf[DefaultDatabase[Int64]])
          stateRef <- ZIO.service[Ref[DatabaseStreamDeserializer.State]]
          state1   <- stateRef.get
          state2 <- DatabaseStreamDeserializer
            .deserializeState(state1)(db.pkIndex.serializeToStream)
            .map(_.asInstanceOf[WaitingForSegments])

          _ <- stateRef.set(state2)
        } yield assertTrue(
          state2.currentSegment == 3 && state2.currentPosition == 0L,
        )
      },
      test("Ser/De Sieve") {
        for {
          ref      <- ZIO.service[Ref[Option[SerializableDatabase]]]
          db       <- ref.get.map(_.get.asInstanceOf[DefaultDatabase[Int64]])
          stateRef <- ZIO.service[Ref[DatabaseStreamDeserializer.State]]
          state1   <- stateRef.get
          state2 <- DatabaseStreamDeserializer
            .deserializeState(state1)(db.sieveIndex.serializeToStream)
            .map(_.asInstanceOf[WaitingForSegments])
          _ <- stateRef.set(state2)
        } yield assertTrue(
          state2.currentSegment == 4 && state2.currentPosition == 0L,
        )
      },
      test("Ser/De Indices") {
        for {
          ref      <- ZIO.service[Ref[Option[SerializableDatabase]]]
          db       <- ref.get.map(_.get.asInstanceOf[DefaultDatabase[Int64]])
          stateRef <- ZIO.service[Ref[DatabaseStreamDeserializer.State]]
          state1   <- stateRef.get
          state2 <- DatabaseStreamDeserializer
            .deserializeState(state1)(
              ZStream.concatAll(db.indices.drop(2).map(_.serializeToStream)),
            )
            .map(_.asInstanceOf[Deserialized])
          _ <- stateRef.set(state2)
          _ <-
            ZIO.foreachDiscard(
              state2.segments.zip(state2.segmentMeta).zipWithIndex,
            ) { case ((segment, meta), i) =>
              ZIO
                .fromTry(segment.validate(meta.header))
                .tapError(_ =>
                  ZIO.attempt(println(s"Mismatched checksum at index $i")),
                )
            }
        } yield assertTrue(true)
      },
      test("Ser/De Complex") {
        for {
          ref <- ZIO.service[Ref[Option[SerializableDatabase]]]
          db  <- ref.get.map(_.get.asInstanceOf[DefaultDatabase[Int64]])
          state <- DatabaseStreamDeserializer
            .deserializeState(DatabaseStreamDeserializer.initialState)(
              db.serializeToStream,
            )
            .map(_.asInstanceOf[Deserialized])
          _ <-
            ZIO.foreachDiscard(
              state.segments.zip(state.segmentMeta).zipWithIndex,
            ) { case ((segment, meta), i) =>
              ZIO
                .fromTry(segment.validate(meta.header))
                .tapError(_ =>
                  ZIO.attempt(println(s"Mismatched checksum at index $i")),
                )
            }
        } yield assertTrue(true)
      },
      test("Ser/De Simple Database") {
        for {
          ref <- ZIO.service[Ref[Option[SerializableDatabase]]]
          db  <- ref.get.map(_.get.asInstanceOf[DefaultDatabase[Int64]])
          simple <- SimpleDatabase
            .deserializer
            .deserialize(db.serializeToStream)
        } yield assertTrue(
          simple
            .getMeta
            .indexes
            .forall(field =>
              if (field.name == "bytes")
                field.dataTypeMeta.exists(_.compression == Compression.ZSTD)
              else
                field.dataTypeMeta.exists(_.compression == Compression.NONE),
            ),
        )
      },
      test("Ser/De Default Database") {
        for {
          ref     <- ZIO.service[Ref[Option[SerializableDatabase]]]
          db      <- ref.get.map(_.get.asInstanceOf[DefaultDatabase[Int64]])
          default <- DefaultDatabase.deserialize(db.serializeToStream)
        } yield assertTrue(
          default
            .getMeta
            .indexes
            .forall(field =>
              if (field.name == "bytes")
                field.dataTypeMeta.exists(_.compression == Compression.ZSTD)
              else
                field.dataTypeMeta.exists(_.compression == Compression.NONE),
            ),
        )
      },
      test("Ser/De Builder") {
        for {
          ref <- ZIO.service[Ref[Option[SerializableDatabase]]]
          db  <- ref.get.map(_.get.asInstanceOf[DefaultDatabase[Int64]])
          builder <- DatabaseBuilderDeserializer
            .deserialize(db.serializeToStream)
          db <- builder.build
        } yield assertTrue(
          db.getMeta
            .indexes
            .forall(field =>
              if (field.name == "bytes")
                field.dataTypeMeta.exists(_.compression == Compression.ZSTD)
              else
                field.dataTypeMeta.exists(_.compression == Compression.NONE),
            ),
        )
      },
    ).provideCustomLayerShared(
      ZLayer.fromZIO(Ref.make[Option[SerializableDatabase]](None)) ++
        ZLayer.fromZIO {
          Ref.make[DatabaseStreamDeserializer.State](
            DatabaseStreamDeserializer.initialState,
          )
        },
    ) @@ TestAspect.sequential

}
