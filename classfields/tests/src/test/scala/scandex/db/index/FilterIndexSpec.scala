//package scandex.db.index
//
//import zio.test.Assertion.equalTo
//import zio.test.TestAspect.ignore
//import zio.test.*
//
//object FilterIndexSpec extends ZIOSpecDefault {
//  /*
//  index for field with 5 possible values for 8 documents
//  Размер шин
//  |Размер шин| 7 | 6 | 5 | 4 || 3 | 2 | 1 | 0 | LSB!
//  ----------------------------------------------
//  |   175    | 0 | 0 | 0 | 0 || 0 | 1 | 1 | 0 | 0x06
//  |   185    | 1 | 1 | 0 | 1 || 0 | 0 | 1 | 0 | 0xd2
//  |   195    | 0 | 0 | 0 | 1 || 0 | 1 | 0 | 0 | 0x14
//  |   205    | 1 | 1 | 0 | 1 || 1 | 0 | 0 | 0 | 0xd8
//  |   225    | 0 | 0 | 0 | 0 || 0 | 0 | 0 | 0 | 0x0
//  (non-stored row)------------------------------------
//  |!null     | 1 | 1 | 0 | 1 || 1 | 1 | 1 | 0 | 0xde
//   */
//
//  val index: FilterIndex[Int] = ???
//  val matrix                  = Array[Long](0x06, 0xD2, 0x14, 0xD8, 0x0)
//
//  override def spec: Spec[TestEnvironment, Any] =
//    suite("multi values")(
//      suite("equalsTo")(
//        test("should return true for filter 175") {
//          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
//          result.fill()
//          for {
//            isFound <- index.equalsTo(result, 175)
//          } yield assert(isFound)(equalTo(true)) &&
//            assert(result.array.head)(equalTo(matrix(0)))
//        },
//        test("should return true for filter 185") {
//          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
//          result.fill()
//          for {
//            isFound <- index.equalsTo(result, 185)
//          } yield assert(isFound)(equalTo(true)) &&
//            assert(result.array.head)(equalTo(matrix(1)))
//        },
//        test("should return true for filter 195") {
//          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
//          result.fill()
//          for {
//            isFound <- index.equalsTo(result, 195)
//          } yield assert(isFound)(equalTo(true)) &&
//            assert(result.array.head)(equalTo(matrix(2)))
//        },
//        test("should return true for filter 205") {
//          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
//          result.fill()
//          for {
//            isFound <- index.equalsTo(result, 205)
//          } yield assert(isFound)(equalTo(true)) &&
//            assert(result.array.head)(equalTo(matrix(3)))
//        },
//        test("should return false for filter 225") {
//          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
//          result.fill()
//          for {
//            isFound <- index.equalsTo(result, 225)
//          } yield assert(isFound)(equalTo(false)) &&
//            assert(result.array.head)(equalTo(0L))
//        },
//      ),
//      suite("not set documents")(
//        test("should return 5th and 0th doc") {
//          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
//          result.fill()
//          for {
//            isFound <- index.notSet(result)
//          } yield assert(isFound)(equalTo(true)) &&
//            assert(result.array.head)(equalTo(0x21L))
//        },
//      ),
//    ) @@ ignore
//
//}
