package scandex.serde

import org.testcontainers.shaded.org.apache.commons.io.output.ByteArrayOutputStream
import scandex.bitset.mutable.LongArrayBitSet
import scandex.db.index.{DocumentId, ValueIdx}
import scandex.db.segments.SegmentTool
import scandex.db.segments.constructors.impl.v1.fixed.OffHeapBitsetSegmentBuilder
import scandex.db.segments.constructors.posting.{
  MultiValuesPostingListSegmentBuilder,
  RRBMPostingListSegmentBuilder,
}
import scandex.db.segments.forward.ForwardSegment
import scandex.db.segments.posting.PostingList
import scandex.db.segments.values.ValueEncoder
import scandex.db.segments.values.lookup.v1.fixed.OffHeapBitsetSegment
import scandex.db.serde.SegmentDeserializer
import scandex.model.meta.FieldDataTypeMeta.Cardinality
import scandex.model.meta.FieldDataTypeMeta.IndexType.STORAGE
import scandex.model.meta.{FieldDataTypeMeta, IndexMeta}
import scandex.model.serde.*
import scandex.serde.DataWriter.*
import scandex.{buildMultiForwardByPosting, computeRealValues}
import strict.{Int64, Int8, PrimitiveTypeTag, Utf8}
import zio.stream.ZStream
import zio.test.*
import zio.test.Assertion.*
import zio.test.Gen.*
import zio.{Chunk, ZIO}

object SegmentDeserializerSpec extends ZIOSpecDefault {

  val DocCount   = 84L
  val ValueCount = 10
  val BitsCount  = 246

  val index = IndexMeta.of(
    name = "",
    dataTypeMeta = Some(
      FieldDataTypeMeta.of(
        DocCount,
        STORAGE,
        PrimitiveTypeTag.Int64Type,
        Cardinality.SINGLE,
        scandex.model.meta.FieldDataTypeMeta.ByteOrdering.LITTLE_ENDIAN,
        scandex.model.meta.FieldDataTypeMeta.Compression.NONE,
      ),
    ),
    segments = Seq(),
  )

  override def spec =
    suite("Segment deserializer")(
      suite("Forward")(
        test("single") {
          val segmentHeader = SegmentHeader
            .defaultInstance
            .withForward(ForwardSegmentMeta())

          val outStream = new ByteArrayOutputStream()

          index.addSegments(segmentHeader).writeDelimitedTo(outStream)
          outStream.saveData(Seq[Int64](23, 24, 25, 26, 27))

          val inputStream = outStream.toInputStream

          val indexMeta = IndexMeta.parseDelimitedFrom(inputStream)

          val segment =
            for {
              segment <- SegmentDeserializer.createEmptySegment(
                SegmentDeserializer.SegmentMeta(
                  segmentHeader,
                  indexMeta
                    .get
                    .dataTypeMeta
                    .map(_.documentsCount)
                    .getOrElse(0L),
                  Cardinality.SINGLE,
                  None,
                ),
              )
              _ <- SegmentTool
                .deserialize(ZStream.fromInputStream(inputStream), segment)
            } yield segment.asInstanceOf[ForwardSegment]

          assertZIO(segment.map(_.valueIndexesIterator(DocumentId(3L)).toSeq))(
            hasSameElements(Seq(ValueIdx(26L))),
          )
        },
        test("multi") {
          check(listOfN(DocCount.toInt)(long(min = 0, max = 5))) { deltas =>
            // генерим данные
            val offsets = deltas
              .foldLeft(List(0L))((acc, cur) =>
                if (cur == 0)
                  acc :+ (acc.last + 1)
                else
                  acc :+ (acc.last + cur),
              )
              .map(Int64(_))

            val values = deltas.map(valuesCount =>
              if (valuesCount == 0)
                List(ValueIdx.NOT_FOUND.toLong)
              else
                (0L until valuesCount).toList,
            )
            val flatValues = values.flatten.map(Int64(_))
            // заполняем мету сектора и индекса
            val segmentHeader: SegmentHeader = SegmentHeader
              .defaultInstance
              .withForward(ForwardSegmentMeta(Some(flatValues.size.toLong)))

            // записываем в поток все данные
            val outStream = new ByteArrayOutputStream()

            outStream.saveData(flatValues)
            outStream.saveData(offsets)

            val segment =
              for {
                segment <- SegmentDeserializer.createEmptySegment(
                  SegmentDeserializer.SegmentMeta(
                    segmentHeader,
                    DocCount,
                    Cardinality.MULTI,
                    None,
                  ),
                )
                _ <- SegmentTool.deserialize(
                  ZStream.fromChunk(Chunk.fromArray(outStream.toByteArray)),
                  segment,
                )
              } yield segment.asInstanceOf[ForwardSegment]

            assertZIO(
              segment.map(_.valueIndexesIterator(DocumentId(1L)).toSeq),
            )(hasSameElements(values(1).map(ValueIdx(_))))
          }
        },
      ),
      suite("Test util")(
        //          0011 1000
        //          0101 1010
        //          0001 1110
        test("single: construct postings from words") {
          val res =
            computeRealValues(
              Seq(Seq[Long](56, 90, 30), Seq[Long](56, 90, 31)),
            )(84L).toList
          assert(res.take(8))(
            hasSameElements(Seq[Long](0, 6, 4, 7, 7, 1, 2, 0)),
          ) &&
          assert(res.slice(64, 72))(
            hasSameElements(Seq[Long](0, 6, 4, 7, 7, 1, 2, 4)),
          )
        },
        test("multi: construct postings from words") {
          val eR = Seq[Set[Long]](
            Set(),
            Set(1, 2),
            Set(2),
            Set(0, 1, 2),
            Set(0, 1, 2),
            Set(0),
            Set(1),
            Set(),
          )
          val res =
            buildMultiForwardByPosting(
              Seq(Seq[Long](56, 90, 30), Seq[Long](56, 90, 30)),
            )(84L).toList
          assert(res.take(8))(hasSameElements(eR)) &&
          assert(res.slice(64, 72))(hasSameElements(eR))
        },
      ),
      suite("Posting")(
        test("single postings") {
          check(listOfN(DocCount.toInt)(long(0, 64))) { values =>
            val sample =
              values
                .zipWithIndex
                .map { case (idx, i) =>
                  ValueIdx(idx) -> DocumentId(i.toLong)
                }
                .groupMap(_._1)(_._2)
            val maxValueIdx = sample.keys.max

            val assembler = RRBMPostingListSegmentBuilder
              .assembler(values.length.toLong, maxValueIdx)

            val segment = assembler
              .assemble(values.map(value => List(ValueIdx(value))))

            for {
              deser <- SegmentDeserializer.createEmptySegment(
                SegmentDeserializer.SegmentMeta(
                  segment.getHeader,
                  values.length.toLong,
                  Cardinality.SINGLE,
                  None,
                ),
              )
              _ <- SegmentTool.serializeDeserialize(segment, deser)

            } yield assertTrue(
              sample.forall { case (idx, documents) =>
                val target = OffHeapBitsetSegmentBuilder
                  .apply(values.length.toLong)
                  .build()

                deser.asInstanceOf[PostingList].equalsTo(target, idx)

                documents == target.iterator.toList
              },
            )
          }

        },
        test("multi") {
          check(
            listOf(
              oneOf(
                setOfBounded(min = 1, max = 3)(
                  fromIterable(Seq[Long](0, 1, 2, 3)),
                ),
                const(Set.empty[Long]),
              ),
            ),
          ) { docValues =>
            val eR: Map[ValueIdx, List[Long]] = docValues
              .zipWithIndex
              .flatMap { case (values, id) =>
                values.map(idx => ValueIdx(idx) -> id.toLong)
              }
              .groupMap(_._1)(_._2)
              .map { case (k, v) =>
                k -> v.sorted
              }

            val assembler = MultiValuesPostingListSegmentBuilder.assembler(
              numberOfDocuments = docValues.length.toLong,
              maxValueIdx = ValueIdx(docValues.flatten.max),
            )

            val segment = assembler
              .assemble(docValues.map(_.map(l => ValueIdx(l))))

            // заполняем мету сектора и индекса
            val segmentHeader: SegmentHeader = segment.getHeader

            val indexMeta = index
              .addSegments(segmentHeader)
              .copy(dataTypeMeta =
                index.dataTypeMeta.map(_.withCardinality(Cardinality.MULTI)),
              )

            for {
              deser <- SegmentDeserializer.createEmptySegment(
                SegmentDeserializer.SegmentMeta(
                  segmentHeader,
                  indexMeta.dataTypeMeta.map(_.documentsCount).getOrElse(0L),
                  Cardinality.MULTI,
                  None,
                ),
              )
              _ <- SegmentTool.serializeDeserialize(segment, deser)
              result <- ZIO.attempt {
                eR.keys
                  .map { idx =>
                    val target = new LongArrayBitSet(docValues.length.toLong)
                    deser.asInstanceOf[PostingList].equalsTo(target, idx)
                    idx -> target.iterator.toList
                  }
                  .toMap
              }
            } yield assertTrue(result == eR)
          }
        },
      ),
      suite("Value Encoders")(
        test("int8") {
          check(
            listOfN(ValueCount * 2)(
              byte,
            ) // сразу создаем в 2 раза больше, т.к. из-за уникальности сократится
              .map(
                _.sorted(Ordering.Byte).distinct.take(ValueCount).map(Int8(_)),
              ),
          ) { words =>
            // заполняем мету сектора и индекса
            val segmentHeader: SegmentHeader = SegmentHeader
              .defaultInstance
              .withEncoder(ValueEncoderMeta(ValueCount.toLong))

            // записываем в поток все данные
            val outStream = new ByteArrayOutputStream()
            val indexMeta = index
              .addSegments(segmentHeader)
              .copy(dataTypeMeta =
                index
                  .dataTypeMeta
                  .map(
                    _.withCardinality(Cardinality.SINGLE)
                      .withPlain(PrimitiveTypeTag.Int8Type),
                  ),
              )

            outStream.saveData(words.map(Int8(_)))

            for {
              deser <- SegmentDeserializer.createEmptySegment(
                SegmentDeserializer.SegmentMeta(
                  segmentHeader,
                  indexMeta.dataTypeMeta.map(_.documentsCount).getOrElse(0L),
                  Cardinality.SINGLE,
                  Some(PrimitiveTypeTag.Int8Type),
                ),
              )
              _ <- SegmentTool.deserialize(
                ZStream.fromChunk(Chunk.fromArray(outStream.toByteArray)),
                deser,
              )
            } yield assert(
              words.map(deser.asInstanceOf[ValueEncoder[Int8]].lookup),
            )(hasSameElements(words.indices.map(y => ValueIdx(y.toLong))))
          }
        },
        test("string") {
          check(
            listOfN(ValueCount)(alphaNumericStringBounded(3, 15)).map(
              _.sorted(Ordering.String).distinct.take(ValueCount).map(Utf8(_)),
            ),
          ) { words =>
            // заполняем мету сектора и индекса
            val segmentHeader: SegmentHeader = SegmentHeader
              .defaultInstance
              .withEncoder(
                ValueEncoderMeta(
                  words.size.toLong,
                  Some(words.map(_.length).sum.toLong),
                ),
              )

            // записываем в поток все данные
            val outStream = new ByteArrayOutputStream()
            outStream.saveData(words)

            val indexMeta = index
              .addSegments(segmentHeader)
              .copy(dataTypeMeta =
                index
                  .dataTypeMeta
                  .map(
                    _.withCardinality(Cardinality.SINGLE)
                      .withPlain(PrimitiveTypeTag.Utf8Type),
                  ),
              )

            for {
              deser <- SegmentDeserializer.createEmptySegment(
                SegmentDeserializer.SegmentMeta(
                  segmentHeader,
                  indexMeta.dataTypeMeta.map(_.documentsCount).getOrElse(0L),
                  Cardinality.SINGLE,
                  Some(PrimitiveTypeTag.Utf8Type),
                ),
              )
              _ <- SegmentTool.deserialize(
                ZStream.fromChunk(Chunk.fromArray(outStream.toByteArray)),
                deser,
              )
            } yield assert(
              words.map(deser.asInstanceOf[ValueEncoder[Utf8]].lookup),
            )(hasSameElements(words.indices.map(y => ValueIdx(y.toLong))))
          }
        },
      ),
      suite("Bitset")(
        test("bitset") {
          check(listOfN(Math.ceil(BitsCount / 64.0).toInt)(long)) { words =>
            // заполняем мету сектора и индекса
            val segmentHeader: SegmentHeader = SegmentHeader
              .defaultInstance
              .withBitset(BitsetSegmentMeta(BitsCount.toLong))

            // записываем в поток все данные
            val outStream = new ByteArrayOutputStream()
            outStream.saveData(words.map(Int64(_)))

            val indexMeta = index.addSegments(segmentHeader)

            for {
              deser <- SegmentDeserializer.createEmptySegment(
                SegmentDeserializer.SegmentMeta(
                  segmentHeader,
                  indexMeta.dataTypeMeta.map(_.documentsCount).getOrElse(0L),
                  Cardinality.SINGLE,
                  None,
                ),
              )
              _ <- SegmentTool.deserialize(
                ZStream.fromChunk(Chunk.fromArray(outStream.toByteArray)),
                deser,
              )
            } yield assert(
              words
                .indices
                .map(i =>
                  deser.asInstanceOf[OffHeapBitsetSegment].word(i.toLong),
                ),
            )(hasSameElements(words))
          }
        },
      ),
    )

}
