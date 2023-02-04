package scandex.db.segments.posting.onetomany

import scandex.bitset.mutable.LongArrayBitSet
import scandex.db.index.ValueIdx
import scandex.db.segments.SegmentTool
import scandex.db.segments.constructors.posting.MultiValuesPostingListSegmentBuilder
import scandex.db.segments.posting.PostingList
import scandex.db.segments.serde.SerializableSegment
import scandex.db.serde.SegmentDeserializer
import scandex.model.meta.FieldDataTypeMeta.Cardinality
import zio.ZIO
import zio.test.*
import zio.test.Gen.*

object MultiPostingListSegmentSerDeSpec extends ZIOSpecDefault {

  private def buildSegment(
    data: Seq[Set[Long]],
  ): PostingList & SerializableSegment = {
    val assembler = MultiValuesPostingListSegmentBuilder.assembler(
      numberOfDocuments = data.length.toLong,
      maxValueIdx = ValueIdx(data.flatten.max),
    )

    assembler.assemble(data.map(_.map(l => ValueIdx(l))))
  }

  override def spec =
    suite("forward segment 1-many")(
      test("SerDe") {
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

          val segment = buildSegment(docValues)

          for {
            deser <- SegmentDeserializer.createEmptySegment(
              SegmentDeserializer.SegmentMeta(
                segment.getHeader,
                docValues.length.toLong,
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
    )

}
