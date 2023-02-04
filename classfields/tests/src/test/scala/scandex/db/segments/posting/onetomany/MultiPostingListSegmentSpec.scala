package scandex.db.segments.posting.onetomany

import alien.memory.*
import scandex.db.index.ValueIdx
import zio.test.*

/*
  index for field with 5 possible values for 8 documents
  |documentId  | 7 | 6 | 5 | 4 || 3 | 2 | 1 | 0 | LSB!
  ----------------------------------------------
  |valueIndex=0| 0 | 0 | 0 | 0 || 0 | 1 | 1 | 0 | 0x06
  |valueIndex=1| 1 | 1 | 0 | 1 || 0 | 0 | 1 | 0 | 0xd2
  |valueIndex=2| 0 | 0 | 0 | 1 || 0 | 1 | 0 | 0 | 0x14
  |valueIndex=3| 1 | 1 | 0 | 1 || 1 | 0 | 0 | 0 | 0xd8
  |valueIndex=4| 0 | 0 | 0 | 0 || 0 | 0 | 0 | 0 | 0x0
  |null        | 0 | 0 | 1 | 0 || 0 | 0 | 0 | 1 | 0x21
 */
object MultiPostingListSegmentSpec extends zio.test.ZIOSpecDefault {

  val a = "bitmap" := Values.Long * 1 * 5
  val b = "null"   := Values.Long * 1

  val memoryL: ("bitmap" := BoundedSequence[BoundedSequence[Value[Long]]]) >>:
    (>>["null" := BoundedSequence[Value[Long]]]) = a >>: b

  val matrix = memoryL / "bitmap" / % / % / $
  val nullVh = memoryL / "null" /   % / $

  val segmentMatrix = List[Long](0x06, 0xD2, 0x14, 0xD8, 0x00, 0x21)
  val fullMatrix    = List[Long](0x07, 0xD8, 0x23, 0x78, 0x56, 0)
  val maxMatrix     = List[Long](0x0E, 0xD2, 0x14, 0xD0, 0x80, 0x21)

  override def spec: Spec[TestEnvironment, Any] =
    suite("MultiPostingList")(
      suite("equalsTo")(
        test("should return false for filter -4") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          val found = buildSegment(segmentMatrix)
            .equalsTo(result, ValueIdx(-4L))
          assertTrue(result.array.head == 0L) && assertTrue(!found)
        },
        test("should return true for filter 0") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          val found = buildSegment(segmentMatrix).equalsTo(result, ValueIdx(0L))
          assertTrue(result.array.head == segmentMatrix.head) &&
          assertTrue(found)
        },
        test("should return false for filter 4(not set for all)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          val found = buildSegment(segmentMatrix).equalsTo(result, ValueIdx(4L))
          assertTrue(result.array.head == 0L) && assertTrue(!found)
        },
        test("should return false for filter 9(out bounds)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          val found = buildSegment(segmentMatrix).equalsTo(result, ValueIdx(9L))
          assertTrue(result.array.head == 0L) && assertTrue(!found)
        },
      ),
      suite("greaterThan")(
        test("should return all documents for filter 0 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(0L), orEquals = true)

          assertTrue(found) && assertTrue(result.array.head == 0xDEL)

        },
        test("should return all documents for filter 0 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(0L), orEquals = false)

          assertTrue(found) && assertTrue(result.array.head == 0xDEL)

        },
        test("should return all documents for filter 1 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(1L), orEquals = true)

          assertTrue(found) && assertTrue(result.array.head == 0xDEL)

        },
        test(
          "should return all documents except rows {0,1} for filter 1 (exclude)",
        ) {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(1L), orEquals = false)

          assertTrue(found) && assertTrue(result.array.head == 0xDCL)

        },
        test(
          "should return all documents except rows {0,1} for filter 2 (include)",
        ) {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(2L), orEquals = true)

          assertTrue(found) && assertTrue(result.array.head == 0xDCL)

        },
        test("should return two last rows for filter 2 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(2L), orEquals = false)

          assertTrue(found) && assertTrue(result.array.head == 0xD8L)

        },
        test("should return two last rows for filter 3 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(3L), orEquals = true)

          assertTrue(found) && assertTrue(result.array.head == 0xD8L)

        },
        test("should return nothing for filter 3 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(3L), orEquals = false)

          assertTrue(!found) && assertTrue(result.array.head == 0L)

        },
        test("should return nothing for filter 7 (illegal filter)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .greaterThan(result, ValueIdx(7L), orEquals = false)

          assertTrue(!found) && assertTrue(result.array.head == 0L)

        },
      ),
      suite("lessThan")(
        test("should return nothing documents for 0 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(0L), orEquals = false)

          assertTrue(!found) && assertTrue(result.array.head == 0L)

        },
        test("should return first row for filter 0 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(0L), orEquals = true)

          assertTrue(found) &&
          assertTrue(result.array.head == segmentMatrix.head)

        },
        test("should return first row for filter 1 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(1L), orEquals = false)

          assertTrue(found) &&
          assertTrue(result.array.head == segmentMatrix.head)

        },
        test("should return two first rows for filter 1 (include)") {

          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(1L), orEquals = true)

          assertTrue(found) && assertTrue(result.array.head == 0xD6L)

        },
        test("should return two first rows for filter 2 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(2L), orEquals = false)

          assertTrue(found) && assertTrue(result.array.head == 0xD6L)

        },
        test("should return three rows for filter 2 (include)") {

          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(2L), orEquals = true)

          assertTrue(found) && assertTrue(result.array.head == 0xD6L)

        },
        test("should return three rows for filter 3 (exclude)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(3L), orEquals = false)

          assertTrue(found) && assertTrue(result.array.head == 0xD6L)

        },
        test("should return all documents for filter 3 (include)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(3L), orEquals = true)

          assertTrue(found) && assertTrue(result.array.head == 0xDEL)

        },
        test("should return nothing for filter 7 (illegal filter)") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix)
            .lessThan(result, ValueIdx(7L), orEquals = false)

          assertTrue(!found) && assertTrue(result.array.head == 0L)

        },
      ),
      suite("isNull")(
        test("should return true") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)

          val found = buildSegment(segmentMatrix).isNull(result)

          assertTrue(found) && assertTrue(result.array.head == (0x21).toLong)

        },
        test("should return false for full buffer bitset") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          val found  = buildSegment(fullMatrix).isNull(result)

          assertTrue(!found) && assertTrue(result.array.head == 0L)
        },
      ),
      suite("min")(
        test("should return NOT_FOUND for unset subset") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.setWord(0, 0x21)
          val found = buildSegment(segmentMatrix).min(result)
          assertTrue(found == ValueIdx.NOT_FOUND)
        },
        test("should return 0") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.setWord(0, 0xCD)
          val found = buildSegment(segmentMatrix).min(result)
          assertTrue(found == ValueIdx(0L))
        },
        test("should return 1") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.setWord(0, 0xC8)
          val found = buildSegment(segmentMatrix).min(result)
          assertTrue(found == ValueIdx(1L))
        },
        test("should return 3") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.setWord(0, 0x29)
          val found = buildSegment(segmentMatrix).min(result)
          assertTrue(found == ValueIdx(3L))
        },
      ),
      /* maxMatrix
  index for field with 5 possible values for 8 documents
  |documentId  | 7 | 6 | 5 | 4 || 3 | 2 | 1 | 0 | LSB!
  ----------------------------------------------
  |valueIndex=0| 0 | 0 | 0 | 0 || 1 | 1 | 1 | 0 | 0xe
  |valueIndex=1| 1 | 1 | 0 | 1 || 0 | 0 | 1 | 0 | 0xd2
  |valueIndex=2| 0 | 0 | 0 | 1 || 0 | 1 | 0 | 0 | 0x14
  |valueIndex=3| 1 | 1 | 0 | 1 || 0 | 0 | 0 | 0 | 0xd0
  |valueIndex=4| 1 | 0 | 0 | 0 || 0 | 0 | 0 | 0 | 0x80
  |null        | 0 | 0 | 1 | 0 || 0 | 0 | 0 | 1 | 0x21
       */
      suite("max")(
        test("should return NOT_FOUND for unset subset") {
          val source = new scandex.bitset.mutable.LongArrayBitSet(8)
          source.setWord(0, 0x21)
          val found = buildSegment(maxMatrix).max(source)
          assertTrue(found == ValueIdx.NOT_FOUND)
        },
        test("should return 0") {
          val source = new scandex.bitset.mutable.LongArrayBitSet(8)
          source.setWord(0, 0x29)
          val found = buildSegment(maxMatrix).max(source)
          assertTrue(found == ValueIdx(0L))
        },
        test("should return 1") {
          val source = new scandex.bitset.mutable.LongArrayBitSet(8)
          source.setWord(0, 0x2B)
          val found = buildSegment(maxMatrix).max(source)
          assertTrue(found == ValueIdx(1L))
        },
        test("should return 2") {
          val source = new scandex.bitset.mutable.LongArrayBitSet(8)
          source.setWord(0, 0x2F)
          val found = buildSegment(maxMatrix).max(source)
          assertTrue(found == ValueIdx(2L))
        },
        test("should return 3") {
          val source = new scandex.bitset.mutable.LongArrayBitSet(8)
          source.setWord(0, 0x7F)
          val found = buildSegment(maxMatrix).max(source)
          assertTrue(found == ValueIdx(3L))
        },
        test("should return 4") {
          val source = new scandex.bitset.mutable.LongArrayBitSet(8)
          source.fill()
          val found = buildSegment(maxMatrix).max(source)
          assertTrue(found == ValueIdx(4L))
        },
      ),
    )

  def buildSegment(values: List[Long]): MultiPostingListSegment = {
    val memory = Memory.unsafeAllocateNative(memoryL)
    values
      .dropRight(1)
      .zipWithIndex
      .foreach { case (v, i) =>
        matrix.set(memory, v)(0, i.toLong)
      }
    nullVh.set(memory, values.last)(0)

    new MultiPostingListSegment(
      memory,
      matrix,
      nullVh,
      ValueIdx(values.size.toLong - 2L),
    )
  }

}
