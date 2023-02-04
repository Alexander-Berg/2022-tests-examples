package scandex.db.search

import alien.memory.*
import scandex.*
import scandex.db.search.SpecializedBinarySearch.{
  Float64BinarySearch,
  Int8BinarySearch,
}
import zio.test.Gen.*
import zio.test.*
import zio.test.ZIOSpecDefault
import zio.test.Gen.{byte, float}

object BinarySearchSpec extends ZIOSpecDefault {

  def spec =
    suite("Binary search")(
      suite("Byte")(
        test("should find match") {
          check(ByteSequence) { bytes =>
            check(fromIterable(bytes)) { exact =>
              val byteBuffer = Memory.ofArray(bytes.toArray)
              val mh = BoundedSequence(bytes.length.toLong, Values.Byte) / % / $
              assertTrue(
                Int8BinarySearch.search(
                  mh.get(byteBuffer)(_),
                  exact,
                  0,
                  bytes.length.toLong,
                ) == bytes.indexOf(exact).toLong,
              )
            }
          }
        },
        test("should not find match") {
          check(ByteSequence) { bytes =>
            check(byte.withFilter(!bytes.contains(_))) { exact =>
              val byteBuffer = Memory.ofArray(bytes.toArray)
              val mh = BoundedSequence(bytes.length.toLong, Values.Byte) / % / $
              assertTrue(
                Int8BinarySearch.search(
                  mh.get(byteBuffer)(_),
                  exact,
                  0,
                  bytes.length.toLong,
                ) < 0L,
              )
            }
          }
        },
      ),
      suite("Double")(
        test("should find match") {
          check(DoubleSequence) { doubles =>
            check(fromIterable(doubles)) { exact =>
              val mh =
                BoundedSequence(doubles.length.toLong, Values.Double) / % / $
              val buf = Memory.ofArray(doubles.toArray)
              assertTrue(
                Float64BinarySearch
                  .search(mh.get(buf)(_), exact, 0, doubles.length.toLong) ==
                  doubles.indexOf(exact).toLong,
              )
            }
          }
        },
        test("should not find match") {
          check(DoubleSequence) { doubles =>
            check(float.map(_.toDouble).withFilter(!doubles.contains(_))) {
              exact =>
                val buf = Memory.ofArray(doubles.toArray)
                val mh  = Sequence(doubles.length.toLong, Values.Double) / % / $
                assertTrue(
                  Float64BinarySearch
                    .search(mh.get(buf)(_), exact, 0, doubles.length.toLong) <
                    0L,
                )
            }
          }
        },
      ),
    )

}
