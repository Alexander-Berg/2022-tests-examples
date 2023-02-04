package scandex.serde

import org.testcontainers.shaded.org.apache.commons.io.output.ByteArrayOutputStream
import scandex.assertBitSet
import scandex.bitset.mutable.LongArrayBitSet
import scandex.db.SimpleDatabase
import scandex.db.index.DocumentId
import scandex.model.meta.FieldDataTypeMeta.IndexType.{
  PRIMARY_KEY,
  SIEVE,
  STORAGE,
}
import scandex.model.meta.{DatabaseMeta, FieldDataTypeMeta, IndexMeta}
import scandex.model.serde.{
  BitsetSegmentMeta,
  ForwardSegmentMeta,
  SegmentHeader,
  ValueEncoderMeta,
}
import strict.{Bytes, Int32, PrimitiveTypeTag, Utf8}
import zio.stream.ZStream
import zio.test.*
import zio.test.Assertion.*
import zio.{Chunk, Task, ZIO}

import java.io.*

object SimpleDatabaseDeserSpec extends ZIOSpecDefault {

  val baos = new ByteArrayOutputStream()

  override def spec =
    suite("DB deserializer")(
      test("serialize to file: only to generate new data") {
        val fileOutputStream: FileOutputStream =
          new FileOutputStream(
            new File(getClass.getResource("simpleDB").getPath),
          )
        for {
          _ <- doSerialize(fileOutputStream)
        } yield assertTrue(true)
      } @@ TestAspect.ignore,
      test("serialize") {
        doSerialize(baos).as(assertTrue(true))
      },
      test("deser test db file") {
        val target = new LongArrayBitSet(TestData.docCount)
        target.fill()
        val expectedSieve = new LongArrayBitSet(TestData.docCount)
        expectedSieve.setWord(0, 819)

        for {
          db <- SimpleDatabase
            .deserializer
            .deserialize(ZStream.fromChunk(Chunk.fromArray(baos.toByteArray)))
          pks <-
            ZIO.foreach((0L until TestData.docCount).toList)(doc =>
              db.getPKIndex[Int32].getPrimaryKey(DocumentId(doc)),
            )
          allAnimals <- ZIO
            .foreach((0L until TestData.docCount).toList)(doc =>
              ZIO
                .fromOption(
                  db.getStorageIndex[Utf8]("animals", PrimitiveTypeTag.Utf8Type),
                )
                .flatMap(_.values(DocumentId(doc))),
            )
            .map(_.flatten)
          bytes <- ZIO
            .foreach((0L until TestData.docCount).toList)(doc =>
              ZIO
                .fromOption(
                  db.getStorageIndex[Bytes](
                    "content",
                    PrimitiveTypeTag.BytesType,
                  ),
                )
                .flatMap(_.values(DocumentId(doc))),
            )
            .map(_.flatten)

          _ <- db.sieveIndex.siftActive(target)
        } yield assertTrue(db.documentCount == TestData.docCount) &&
          assert(pks)(hasSameElements(TestData.pk)) &&
          assert(allAnimals)(
            hasSameElements(TestData.strStorage.filterNot(_.isEmpty)),
          ) &&
          assertTrue(
            bytes.map(bs => new String(bs.toArray())).head == "lio7878n",
          ) && assertBitSet(target)(expectedSieve)
      },
    ) @@ TestAspect.sequential @@ TestAspect.ignore

  private def doSerialize(os: OutputStream) = {
    val write: OutputStream => Task[Unit] =
      os => {
        val (tasks, segmentMetas) =
          List(
            generateAndStorePKIndex(TestData.pk, os),
            generateAndStoreSieveIndex(TestData.sieve, os),
            generateAndStoreStorageIndex(
              TestData.intStorage,
              Some(Int32(-1)),
              "some numbers",
              os,
            ),
            generateAndStoreStorageIndex(
              TestData.strStorage,
              Some(Utf8("")),
              "animals",
              os,
            ),
            generateAndStoreStorageIndex(
              TestData.bytesStorage,
              Some(TestData.emptyBytes),
              "content",
              os,
            ),
            generateAndStoreStorageIndex(
              TestData.isSomething,
              None,
              "isSomething",
              os,
            ),
          ).unzip
        // пишем мету базы
        DatabaseMeta
          .of(documentsCount = TestData.docCount, indexes = segmentMetas)
          .writeDelimitedTo(os)
        ZIO.collectAllDiscard(tasks)
      }
    ZIO
      .acquireReleaseWith[Any, Throwable, OutputStream](acquire =
        ZIO.attempt(os),
      )(stream => ZIO.succeed(stream.close()))(write)
      .zipLeft(ZIO.attempt(println(s"Serialized")))
      .catchAll(_ => ZIO.succeed(()))
  }

  /** Сформировать мету индекса и создать(но пока не выполнять, т.к. сначала
    * пишутся меты базы и индексов) таску на запись индекса во внешний поток
    *
    * @param forwardWithRealValues
    *   человекочитаемый прямой сегмент с настоящими значениями (не ValueIdx)
    * @param absentValue
    *   то, что считается отсутствующим значением
    * @param outputStream
    *   внешний поток
    * @return
    *   мета индекса и таска на запись содержимого индекса
    */
  private def generateAndStoreStorageIndex[T](
    forwardWithRealValues: Seq[T],
    absentValue: Option[T],
    indexName: String,
    outputStream: OutputStream,
  )(
    implicit
    ord: Ordering[T],
    longWriter: DataWriter[Long],
    writer: DataWriter[T],
    tpe: scandex.model.FieldType[T],
  ): (Task[Unit], IndexMeta) = {
    val values =
      forwardWithRealValues
        .distinct
        .filter(absentValue.isEmpty || _ != absentValue.get)
        .sorted
    val forward = forwardWithRealValues
      .map(realV => values.indexOf(realV).toLong)
    //    println(s"forward = $forward")
    //    println(s"values = ${values.zipWithIndex.map(_.swap)}")
    (
      ZIO.attempt {
        longWriter.saveData(forward, outputStream)
        writer.saveData(values, outputStream)
      },
      prepareStorageMeta(
        indexName,
        tpe.tpe,
        forward.size.toLong,
        values.size.toLong,
        tpe.tpe match {
          case PrimitiveTypeTag.Utf8Type =>
            Some(values.map(_.asInstanceOf[Utf8].length).sum.toLong)
          case PrimitiveTypeTag.BytesType =>
            Some(values.map(_.asInstanceOf[Bytes].length).sum.toLong)
          case _ =>
            None
        },
      ),
    )

  }

  /** @param totalValuesSize
    *   только для Utf8 и Bytes
    * @return
    *   мета Storage индекса
    */
  private def prepareStorageMeta(
    indexName: String,
    tpe: PrimitiveTypeTag,
    docCount: Long,
    valuesCount: Long,
    totalValuesSize: Option[Long],
  ): IndexMeta = {

    val forwardSegmentHeader = SegmentHeader
      .defaultInstance
      .withAllocatedSize(docCount * java.lang.Long.BYTES)
      .withForward(ForwardSegmentMeta())

    val encoderSegmentHeader = SegmentHeader
      .defaultInstance
      .withAllocatedSize(
        totalValuesSize.getOrElse(1L) + valuesCount * java.lang.Long.BYTES,
      )
      .withEncoder(ValueEncoderMeta(valuesCount, totalValuesSize))

    IndexMeta(
      name = indexName,
      dataTypeMeta = Some(FieldDataTypeMeta(docCount, STORAGE, tpe)),
      segments = Seq(forwardSegmentHeader, encoderSegmentHeader),
    )
  }

  /** @see
    *   [[generateAndStoreStorageIndex]]
    */
  private def generateAndStorePKIndex[T](
    forwardWithRealValues: Seq[T],
    outputStream: OutputStream,
  )(
    implicit
    ord: Ordering[T],
    longWriter: DataWriter[Long],
    typedWriter: DataWriter[T],
    tpe: scandex.model.FieldType[T],
  ): (Task[Unit], IndexMeta) = {
    val values: Seq[T] = forwardWithRealValues.sorted
    val forward: Seq[Long] = forwardWithRealValues
      .map(realV => values.indexOf(realV).toLong)
    val inverted: Seq[Long] = forward.zipWithIndex.sortBy(_._1).map(_._2.toLong)
    //    println(s"forward = $forward")
    //    println(s"inverted = $inverted")
    //    println(s"values = ${values.zipWithIndex.map(_.swap)}")
    (
      ZIO.attempt {
        longWriter.saveData(forward, outputStream)
        longWriter.saveData(inverted, outputStream)
        typedWriter.saveData(values, outputStream)
      },
      preparePKMeta(tpe.tpe, forward.size.toLong, values.size.toLong),
    )
  }

  private def preparePKMeta(
    tpe: PrimitiveTypeTag,
    docCount: Long,
    valuesCount: Long,
  ): IndexMeta = {
    val forwardSegmentHeader = SegmentHeader
      .defaultInstance
      .withAllocatedSize(docCount * java.lang.Long.BYTES)
      .withForward(ForwardSegmentMeta())

    // TODO вычислять множитель размера одного элемента исходя из типа элемента
    val encoderSegmentHeader = SegmentHeader
      .defaultInstance
      .withAllocatedSize(valuesCount * 4)
      .withEncoder(ValueEncoderMeta(valuesCount))

    IndexMeta(
      name = "PK",
      dataTypeMeta = Some(FieldDataTypeMeta(docCount, PRIMARY_KEY, tpe)),
      segments = Seq(
        forwardSegmentHeader,
        encoderSegmentHeader,
        encoderSegmentHeader,
      ),
    )
  }

  private def generateAndStoreSieveIndex(
    bitset: Seq[Boolean],
    outputStream: OutputStream,
  )(
    implicit
    longWriter: DataWriter[Long],
  ): (Task[Unit], IndexMeta) = {
    //    println(s"bitset = $bitset")
    val u =
      bitset
        .grouped(64)
        .map(
          _.reverse
            .zipWithIndex
            .foldLeft(0L) { case (acc, (isActive, ind)) =>
              if (isActive)
                acc + Math.pow(2, ind.toDouble).toLong
              else
                acc
            },
        )
        .toList

    //    println(s"bitset = $u")
    (
      ZIO.attempt {
        longWriter.saveData(u, outputStream)
      },
      prepareSieveMeta(bitset.size.toLong),
    )
  }

  private def prepareSieveMeta(docCount: Long): IndexMeta = {

    val bitsetSegmentHeader = SegmentHeader
      .defaultInstance
      .withAllocatedSize(1 + docCount / 8)
      .withBitset(BitsetSegmentMeta(docCount))

    IndexMeta(
      name = "Sieve",
      dataTypeMeta = Some(FieldDataTypeMeta(docCount, SIEVE)),
      segments = Seq(bitsetSegmentHeader),
    )
  }

}
