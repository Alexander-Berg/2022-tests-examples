package scandex.builder.segment.keyinv

import scandex.bitset.mutable.LongArrayBitSet
import scandex.builder.segment.value.ValueIdxRef
import scandex.db.index.{DocumentId, ValueIdx}
import scandex.db.segments.constructors.posting.MultiValuesPostingListSegmentBuilder
import zio.test.*

object KeyInvBuilderSpec extends ZIOSpecDefault {
  val maxDocumentId: DocumentId = DocumentId(95L)

  val insertions: Map[DocumentId, List[ValueIdxRef]] = (0L to maxDocumentId)
    .map(id => DocumentId(id) -> ref(id))
    .toMap

  insertions.values.foreach(_.foreach(ref => ref.idx = ValueIdx(ref.i.toLong)))
  val numberOfValues: Long = insertions.values.map(_.size).sum.toLong

  val maxValueIdx: ValueIdx =
    insertions.values.filter(_.nonEmpty).map(_.map(_.idx).max).max

  val invertedValues: Map[ValueIdx, Set[DocumentId]] = insertions
    .toList
    .flatMap { case (id, values) =>
      values.map(_.idx -> id)
    }
    .groupMap(_._1)(_._2)
    .map { case (idx, values) =>
      idx -> values.toSet
    }

  val emptyDocuments: Set[DocumentId] = insertions.filter(_._2.isEmpty).keySet

  val builder: KeyInvBuilderImpl = KeyInvBuilder.empty
  insertions.foreach { case (id, idxs) =>
    builder.set(id, idxs)
  }

  val bitSet = new LongArrayBitSet(maxDocumentId + 1)
  bitSet.fill()

  override def spec =
    suite("KeyInvBuilder")(
      test("KeyInvBuilder.buildForward") {
        val meta    = builder.markReferencesAndGetMeta(bitSet)
        val forward = builder.buildForward(meta.forwardAssembler)
        assertTrue(
          meta ==
            KeyInvBuilder
              .MultiForwardBuildData(maxDocumentId + 1L, numberOfValues),
        ) &&
        assertTrue(
          (0L to maxDocumentId)
            .map(id =>
              DocumentId(id) ->
                forward.valueIndexesIterator(DocumentId(id)).toList,
            )
            .toMap ==
            insertions.map { case (id, refs) =>
              id -> refs.map(_.idx)
            },
        )
      },
      test("KeyInvBuilder.buildInverted") {
        val inverted = builder.buildInverted(
          MultiValuesPostingListSegmentBuilder
            .assembler(maxDocumentId + 1L, maxValueIdx),
        )

        assertTrue(
          (0L to maxValueIdx)
            .map { idx =>
              val target = new LongArrayBitSet(maxDocumentId + 1L)
              inverted.equalsTo(target, ValueIdx(idx))
              ValueIdx(idx) -> target.iterator.map(id => DocumentId(id)).toSet
            }
            .toMap == invertedValues,
        ) &&
        assertTrue({
            val target = new LongArrayBitSet(maxDocumentId + 1L)
            inverted.isNull(target)
            target.iterator.map(DocumentId(_)).toSet: Set[DocumentId]
          } == emptyDocuments,
        )
      },
    )

  def ref(id: Long): List[ValueIdxRef] = {
    if (id < 16L)
      List(ValueIdxRef(id.toInt))
    else if (id > 16L && id <= 64L) {
      List(ValueIdxRef((80L - id).toInt), ValueIdxRef(16))
    } else if (id > 65L)
      List(ValueIdxRef((id - 5L).toInt), ValueIdxRef((96L - id).toInt))
    else
      List.empty
  }

}
