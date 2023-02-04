package scandex.db.segments.posting.onetoone

import scandex.bitset.mutable.LongArrayBitSet
import scandex.db.index.ValueIdx
import scandex.db.segments.SegmentTool
import scandex.db.segments.constructors.posting.RRBMPostingListSegmentBuilder
import scandex.db.segments.posting.PostingList
import scandex.db.serde.SegmentDeserializer
import scandex.model.meta.FieldDataTypeMeta.Cardinality
import zio.test.*
import zio.test.Gen.*

object RRBMPostingListSegmentBuilderSpec extends ZIOSpecDefault {

  val values: Seq[ValueIdx] = ValueIdx.cast(
    List(0L, 2L, -1L, 3L, 0L, 3L, -1L, 2L, 3L, 4L, 5L, 6L, 7L, 0L, -1L, 8L),
  )

  def samples(values: Seq[ValueIdx]): Map[ValueIdx, Seq[Long]] =
    values
      .zipWithIndex
      .filterNot(_._1 == ValueIdx.NOT_FOUND)
      .groupMap(_._1)(t => t._2.toLong)
      .map { case (idx, ids) =>
        idx -> ids.sorted
      }

  def nullSamples(values: Seq[ValueIdx]): Seq[Long] =
    values.zipWithIndex.filter(_._1 == ValueIdx.NOT_FOUND).map(_._2.toLong)

  def buildSegment(values: Iterable[ValueIdx]) = {
    val maxValueIdx = values.max
    val assembler = RRBMPostingListSegmentBuilder
      .assembler(values.size.toLong, maxValueIdx)

    assembler.assemble(
      values.map(idx =>
        if (idx == ValueIdx.NOT_FOUND)
          List.empty
        else
          List(idx),
      ),
    )
  }

  override def spec =
    suite("RRBMPostingListSegment")(
      test("Builder")(
        check(listOfBounded(63, 96)(long(-1L, 8L))) { docValues =>
          val values  = ValueIdx.cast(docValues)
          val segment = buildSegment(values)

          assertTrue(
            samples(values)
              .keys
              .map { idx =>
                val target = new LongArrayBitSet(values.length.toLong)
                segment.equalsTo(target, idx)
                idx -> target.iterator.toSeq
              }
              .toMap == samples(values),
          ) &&
          assertTrue({
              val target = new LongArrayBitSet(values.length.toLong)
              segment.isNull(target)
              target.iterator.toSeq
            } == nullSamples(values),
          )
        },
      ),
      test("SerDe")(
        check(listOfBounded(63, 96)(long(-1L, 8L))) { docValues =>
          val values  = ValueIdx.cast(docValues)
          val segment = buildSegment(values)
          for {
            deser <- SegmentDeserializer.createEmptySegment(
              SegmentDeserializer.SegmentMeta(
                segment.getHeader,
                docValues.length.toLong,
                Cardinality.SINGLE,
                None,
              ),
            )
            _ <- SegmentTool.serializeDeserialize(segment, deser)
          } yield {
            assertTrue(
              samples(values)
                .keys
                .map { idx =>
                  val target = new LongArrayBitSet(values.length.toLong)
                  deser.asInstanceOf[PostingList].equalsTo(target, idx)
                  idx -> target.iterator.toSeq
                }
                .toMap == samples(values),
            ) &&
            assertTrue({
                val target = new LongArrayBitSet(values.length.toLong)
                deser.asInstanceOf[PostingList].isNull(target)
                target.iterator.toSeq
              } == nullSamples(values),
            )
          }
        },
      ),
    )

}
