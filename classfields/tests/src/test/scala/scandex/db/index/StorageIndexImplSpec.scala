package scandex.db.index

import scandex.db.index.MockedSegments.{
  MultiForwardSegmentMock,
  SingleForwardSegmentMock,
  ValueStorageSegmentMock,
}
import scandex.model.meta.FieldDataTypeMeta.Compression
import strict.*
import zio.test.*
import zio.test.Assertion.*

object StorageIndexImplSpec extends zio.test.ZIOSpecDefault {

  val valuesMock = ValueStorageSegmentMock[Int32](
    Array(1994, 1995, 1996, 1997, 1998, 1999, 2000),
  )

  val singleForwardMock = SingleForwardSegmentMock(
    Array[Long](0, 1, 2, 0, 0, -1, 7, 3, 1, 3, 0, 2, 3),
  )

  val multiForwardMock = MultiForwardSegmentMock(
    Seq(
      Set[Long](0, 1, 2),
      Set[Long](-1),
      Set[Long](0),
      Set[Long](0),
      Set[Long](-1),
      Set[Long](2, 3),
      Set[Long](1, 7),
      Set[Long](0, 2, 3),
    ),
  )

  val singleIndex =
    new StorageIndexImpl[Int32](
      "single",
      Compression.NONE,
      singleForwardMock,
      valuesMock,
    )

  val multiIndex =
    new StorageIndexImpl[Int32](
      "multi",
      Compression.NONE,
      multiForwardMock,
      valuesMock,
    )

  override def spec: Spec[TestEnvironment, Any] = {

    suite("StorageIndexImpl")(
      suite("get values")(
        suite("one value")(
          test("one") {
            assertZIO(singleIndex.values(documentId = DocumentId(0L)).exit)(
              succeeds(hasSameElements(List[Int32](1994))),
            )
          },
          test("no values") {
            assertZIO(singleIndex.values(documentId = DocumentId(5L)).exit)(
              succeeds(hasSameElements(List())),
            )
          },
          test("absent values - inconsistent") {
            assertZIO(multiIndex.values(documentId = DocumentId(6L)).exit)(
              fails(isSubtype[IndexOutOfBoundsException](anything)),
            )
          },
          test("absent document") {
            assertZIO(singleIndex.values(documentId = DocumentId(42L)).exit)(
              fails(isSubtype[IndexOutOfBoundsException](anything)),
            )
          },
        ),
        suite("many values")(
          test("three values") {
            assertZIO(multiIndex.values(documentId = DocumentId(0L)).exit)(
              succeeds(hasSameElements(List[Int32](1994, 1995, 1996))),
            )
          },
          test("no values") {
            assertZIO(multiIndex.values(documentId = DocumentId(1L)).exit)(
              succeeds(hasSameElements(List())),
            )
          },
          test("absent document") {
            assertZIO(multiIndex.values(documentId = DocumentId(42L)).exit)(
              fails(isSubtype[IndexOutOfBoundsException](anything)),
            )
          },
          test("absent values - inconsistent") {
            assertZIO(multiIndex.values(documentId = DocumentId(6L)).exit)(
              fails(isSubtype[IndexOutOfBoundsException](anything)),
            )
          },
        ),
      ),
    )
  }

}
