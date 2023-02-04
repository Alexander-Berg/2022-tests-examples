package scandex.db.segments.posting.onetoone

import alien.memory.*
import scandex.bitset.pool.ThreadLocalBitSetPool
import scandex.db.index.ValueIdx
import zio.test.*
import zio.test.TestAspect.ignore

/*
  index for field with 4 possible values(00,01,10,11) for 8 documents
  |documentId | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 | LSB!
  --------------------------------------------
  |valueId = 0| 0 | 1 | 0 | 1 | 0 | 1 | 0 | 0 | 0x54   /\  младший(самый правый) бит
  |valueId = 1| 0 | 1 | 0 | 0 | 1 | 0 | 0 | 0 | 0x48   |   старший(самый левый) бит
  --------------------------------------------
  |!null      | 0 | 1 | 0 | 1 | 1 | 1 | 0 | 0 | 0x5c
  ----------------non-stored part-------------
  |null       | 1 | 0 | 1 | 0 | 0 | 0 | 1 | 1 | 0xa3
  -------------------real value----------------
              | - | 3 | - | 1 | 2 | 1 | - | - |
 */

object RRBMPostingListSegmentSpec extends zio.test.ZIOSpecDefault {

  val a = "bitmap"  := Values.Long * 1 * 2
  val b = "nonnull" := Values.Long * 1

  val memoryL: ("bitmap" := BoundedSequence[BoundedSequence[Value[Long]]]) >>:
    (>>["nonnull" := BoundedSequence[Value[Long]]]) = a >>: b

  val memory  = Memory.unsafeAllocateNative(memoryL)
  val matrix  = memoryL / "bitmap" /  % / % / $
  val nonnull = memoryL / "nonnull" / % / $

  val segmentMatrix = List[Long](0x54, 0x48, 0x5C)
  val fullMatrix    = List[Long](0x8, 0x8, 0xFF)

  override def spec: Spec[TestEnvironment, Any] =
    suite("PostingList")(
      suite("equalsTo")(
        test("should return true for filter 11") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .equalsTo(result, ValueIdx(3L))
          assertTrue(result.array.head == 0x40L) && assertTrue(isFound)
        },
        test("should return false for filter 00") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .equalsTo(result, ValueIdx(0L))
          assertTrue(result.array.head == 0L) && assertTrue(!isFound)
        },
        test("should return true for filter 10") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .equalsTo(result, ValueIdx(1L))
          assertTrue(result.array.head == 0x14L) && assertTrue(isFound)
        },
      ),
      suite("greaterThan")(
        test("should return true for filter 00 (include)") {

          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(0L), orEquals = true)
          assertTrue(result.array.head == segmentMatrix.last) &&
          assertTrue(isFound)
        },
        test("should return true for filter 00 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(0L), orEquals = false)
          assertTrue(result.array.head == segmentMatrix.last) &&
          assertTrue(isFound)
        },
        test("should return true for filter 01 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(1L), orEquals = true)
          assertTrue(result.array.head == segmentMatrix.last) &&
          assertTrue(isFound)
        },
        test("should return true for filter 01 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(1L), orEquals = false)
          assertTrue(result.array.head == 0x48L) && assertTrue(isFound)
        },
        test("should return true for filter 10 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(2L), orEquals = true)
          assertTrue(result.array.head == 0x48L) && assertTrue(isFound)
        },
        test("should return true for filter 10 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(2L), orEquals = false)
          assertTrue(result.array.head == 0x40L) && assertTrue(isFound)
        },
        test("should return true for filter 11 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(3L), orEquals = true)
          assertTrue(result.array.head == 0x40L) && assertTrue(isFound)
        },
        test("should return false for filter 11 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(3L), orEquals = false)
          assertTrue(result.array.head == 0L) && assertTrue(!isFound)
        },
        test("should return false for filter 100 (illegal filter)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(4L), orEquals = false)
          assertTrue(result.array.head == 0L) && assertTrue(!isFound)
        },
      ),
      suite("lessThan")(
        test("should return true for filter 11 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(3L), orEquals = true)
          assertTrue(result.array.head == segmentMatrix.last) &&
          assertTrue(isFound)
        },
        test("should return true for filter 11 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(3L), orEquals = false)
          assertTrue(result.array.head == 0x1CL) && assertTrue(isFound)
        },
        test("should return true for filter 10 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(2L), orEquals = true)
          assertTrue(result.array.head == 0x1CL) && assertTrue(isFound)
        },
        test("should return true for filter 10 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(2L), orEquals = false)
          assertTrue(result.array.head == 0x14L) && assertTrue(isFound)
        },
        test("should return true for filter 01 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(1L), orEquals = true)
          assertTrue(result.array.head == 0x14L) && assertTrue(isFound)
        },
        test("should return false for filter 01 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(1L), orEquals = false)
          assertTrue(result.array.head == 0L) && assertTrue(!isFound)
        },
        test("should return false for filter 00 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(0L), orEquals = true)
          assertTrue(result.array.head == 0L) && assertTrue(!isFound)
        },
        test("should return false for filter 00 (exclude)") {

          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(0L), orEquals = false)
          assertTrue(result.array.head == 0L) && assertTrue(!isFound)
        },
        test("should return all for filter 100 (illegal filter)") {

          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(5L), orEquals = true)
          assertTrue(result.array.head == segmentMatrix.last) &&
          assertTrue(isFound)
        },
      ),
      suite("isNull")(
        test("should return true") {

          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(segmentMatrix).isNull(result)
          assertTrue(result.array.head == 0xA3L) && assertTrue(isFound)

        },
        test("should return false for full buffer bitset") {

          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          val isFound = buildSegment(fullMatrix).isNull(result)
          assertTrue(result.array.head == 0L) && assertTrue(!isFound)

        },
      ) @@ ignore,
    )

  def buildSegment(values: List[Long]) = {
    values
      .take(values.size - 1)
      .zipWithIndex
      .foreach { case (v, i) =>
        matrix.set(memory, v)(0, i.toLong)
      }
    nonnull.set(memory, values.last)(0)

    new RRBMPostingListSegment(
      memory,
      matrix,
      nonnull,
      2,
      ValueIdx(3L),
      new ThreadLocalBitSetPool(8),
    )
  }

}
