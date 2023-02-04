package scandex.db.index

import scandex.db.index.MockedSegments.{
  LookupStorageSegmentMock,
  SingleForwardSegmentMock,
  UniquePostingListMock,
}
import strict.{Int32, Utf8}
import zio.test.Assertion.*
import zio.test.{Assertion, TestAspect, assertTrue, assertZIO}

object PrimaryIndexImplSpec extends zio.test.ZIOSpecDefault {

  private val emptyIndex = PrimaryIndexImpl[Utf8](
    valueSegment = LookupStorageSegmentMock[Utf8](Array()),
    invertedSegment = UniquePostingListMock(Array()),
    forwardSegment = SingleForwardSegmentMock(Array[Long]()),
  )

  /** | docId | 0   | 1   | 2   | 3   | 4   |
    * |:------|:----|:----|:----|:----|:----|
    * | value | 222 | 111 | 000 | 666 | 555 |
    */
  private val consistentIndex = PrimaryIndexImpl[Int32](
    valueSegment = LookupStorageSegmentMock[Int32](
      Array(0, 111, 222, 333, 444, 555, 666),
    ),
    invertedSegment = UniquePostingListMock(
      Array(
        DocumentId(2L),
        DocumentId(1L),
        DocumentId(0L),
        DocumentId.NOT_FOUND,
        DocumentId.NOT_FOUND,
        DocumentId(4L),
        DocumentId(3L),
      ),
    ),
    forwardSegment = SingleForwardSegmentMock(Array(2L, 1L, 0L, 6L, 5L)),
  )

  private val inconsistentIndex = PrimaryIndexImpl[Utf8](
    valueSegment = LookupStorageSegmentMock[Utf8](Array("000", "111", "222")),
    invertedSegment = UniquePostingListMock(Array()),
    forwardSegment = SingleForwardSegmentMock(Array(7L)),
  )

  override def spec = {

    suite("PrimaryIndexImpl")(
      suite("getDocumentId")(
        test("empty index") {
          assertZIO(emptyIndex.getDocumentId(pk = "my_doc_PK").exit)(
            succeeds(equalTo(DocumentId.NOT_FOUND)),
          )
        },
        test("document found") {
          assertZIO(consistentIndex.getDocumentId(pk = 111).exit)(
            succeeds(equalTo(1L)),
          )
        },
        test("document not found") {
          assertZIO(consistentIndex.getDocumentId(pk = 333).exit)(
            succeeds(equalTo(DocumentId.NOT_FOUND)),
          )
        },
        test("inconsistent index") {
          assertZIO(inconsistentIndex.getDocumentId(pk = "111").exit)(
            fails(
              isSubtype[IndexOutOfBoundsException](
                hasMessage(containsString("out of bounds")),
              ),
            ),
          )
        },
      ),
      suite("getPrimaryKey")(
        test("empty index - now fixed") {
          assertZIO(emptyIndex.getPrimaryKey(DocumentId(0L)).exit)(
            fails(hasMessage(containsString("out of bounds"))),
          )
        },
        test("document found") {
          assertZIO(consistentIndex.getPrimaryKey(DocumentId(0L)).exit)(
            succeeds(equalTo(Int32(222))),
          )
        },
        test("inconsistent index - may fall") {
          assertZIO(inconsistentIndex.getPrimaryKey(DocumentId(0L)).exit)(
            fails(isSubtype[IndexOutOfBoundsException](Assertion.anything)),
          )
        } @@ TestAspect.debug @@ TestAspect.flaky(20),
      ),
      suite("equalsTo")(
        test("should return true for filter 222") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          for {
            isFound <- consistentIndex.equalsTo(result, 222)
          } yield assertTrue(isFound) && assertTrue(result.array.head == 1L)
        },
        test("should return false for non-existent 321") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          for {
            isFound <- consistentIndex.equalsTo(result, 321)
          } yield assertTrue(!isFound) && assertTrue(result.array.head == 0L)
        },
        test("should return false for nobody's 444") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          for {
            isFound <- consistentIndex.equalsTo(result, 444)
          } yield assertTrue(!isFound) && assertTrue(result.array.head == 0L)
        },
        test("should return false and nothing for non-existent 777") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          for {
            isFound <- consistentIndex.equalsTo(result, 777)
          } yield assertTrue(!isFound) && assertTrue(result.array.head == 0L)
        },
      ),
      suite("not set")(
        test("should return false in any case") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()
          for {
            isFound <- consistentIndex.notSet(result)
          } yield assertTrue(!isFound) && assertTrue(result.array.head == 0L)
        },
      ),
    )
  }

}
