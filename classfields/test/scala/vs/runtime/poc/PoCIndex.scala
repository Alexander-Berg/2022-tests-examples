package vs.runtime.poc

import io.circe.Printer
import io.circe.syntax.*
import scandex.db.index.*
import scandex.db.serde.DeserializationError
import scandex.db.serde.Reason.InternalStateError
import scandex.db.{Command, Database, ExecutionError}
import scandex.model.FieldType
import scandex.model.meta.FieldDataTypeMeta.{Cardinality, IndexType}
import scandex.model.meta.{DatabaseMeta, FieldDataTypeMeta, IndexMeta}
import strict.{OptionalValue, PrimitiveTypeTag, Utf8}
import zio.stream.ZStream
import zio.{Chunk, IO, Task, ZIO}

import java.io.InputStream
import scala.collection.SortedMap

/** Индекс-имитация. Сериализуется и десериализуется в виде json.
  * @param map
  *   ключ - DocumentId; значение - значение единственного строкового поля
  */
case class PoCIndex(map: SortedMap[Int, String]) extends Database {

  def getValue(docId: Int): Task[String] = {
    ZIO.fromOption(map.get(docId)).orElseFail(new IllegalArgumentException())
  }

  def serializeToStream(): ZStream[Any, Nothing, Byte] = {
    val json = map.asJson.printWith(Printer.noSpaces)
    ZStream.fromChunk(Chunk.fromArray(json.getBytes))
  }

  override def toString: String = {
    map.asJson.printWith(Printer.noSpaces)
  }

  override def pkType: PrimitiveTypeTag = PrimitiveTypeTag.Uint32Type

  override def pkIndex: PrimaryIndex[?] = ???

  override def documentCount: Long = map.size.toLong

  override def electricalTapeExecute(
    conditions: Seq[Command],
  ): IO[ExecutionError, Map[(String, PrimitiveTypeTag), OptionalValue]] = ???

  override def getDBSchema: DatabaseMeta =
    DatabaseMeta
      .defaultInstance
      .addIndexes(
        IndexMeta
          .defaultInstance
          .withDataTypeMeta(
            FieldDataTypeMeta(
              documentsCount = 1,
              indexType = IndexType.PRIMARY_KEY,
              plain = PrimitiveTypeTag.Uint32Type,
              cardinality = Cardinality.SINGLE,
            ),
          ), // todo тут есть sieve или нет?
        IndexMeta
          .defaultInstance
          .withDataTypeMeta(
            FieldDataTypeMeta(
              documentsCount = 1,
              indexType = IndexType.STORAGE,
              plain = PrimitiveTypeTag.Utf8Type,
              cardinality = Cardinality.SINGLE,
            ),
          ),
      )

  override def getStorageIndex[T : FieldType](
    indexName: String,
  ): Option[StorageIndex[T]] =
    Some(
      new StorageIndex[T] {

        override def values(documentId: DocumentId): Task[List[T]] =
          ZIO.succeed(map.values.map(Utf8(_).asInstanceOf[T]).toList)

        override def name: String = ???

        override def getIndexMeta: IndexMeta = ???

        override def getSerializedSize: Long = ???

        override def serializeToStream: ZStream[Any, Throwable, Byte] = ???
      },
    )

  override def getFilterIndex[T : FieldType](
    indexName: String,
  ): Option[FilterIndex[T]] = ???

  override def getRangeIndex[T : FieldType](
    indexName: String,
  ): Option[RangeIndex[T]] = ???

}

object PoCIndex {

  val Empty = PoCIndex(SortedMap[Int, String]())

  def deserialize(in: InputStream): Task[PoCIndex] = {
    import io.circe.parser.*
    val y = new String(in.readAllBytes())
    ZIO
      .fromEither(parse(y).flatMap(u => u.as[SortedMap[Int, String]]))
      .mapBoth(
        { e =>
          DeserializationError(
            InternalStateError(s"Can't parse ${e.getMessage}"),
          )
        },
        PoCIndex(_),
      )

  }

}
