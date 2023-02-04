package scandex.db.bitset.mutable

import zio.ZIO
import zio.test.*
import zio.test.Assertion.*

object LongArrayBitSetSpec extends zio.test.ZIOSpecDefault {

  override def spec: Spec[TestEnvironment, Any] =
    suite("on-heap bitset")(
      suite("immutable bitset methods")(
        suite("apply")(
          test("valid bit number") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            assertTrue((0 until 68).forall(i => result.apply(i.toLong)))
          },
          test("out-of-bound bit number: false without error") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(8)
            result.fill()
            assertTrue(!result.apply(68L)) && assertTrue(!result.apply(100L))
          },
        ),
        suite("word")(
          test("valid word number") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            assert(result.word(0L))(equalTo(-1L))
          },
          test("out-of-bound word number: false without error") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            assertZIO(ZIO.from(result.word(2L)).exit)(
              fails(
                isSubtype[IndexOutOfBoundsException](
                  hasMessage(containsString("out of bounds")),
                ),
              ),
            )
          },
        ),
        suite("size")(
          test("ceil to nearest power of 2") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            assert(result.size)(equalTo(128L))
          },
          test("minimal size") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(14)
            assert(result.size)(equalTo(64L))
          },
          test("full bitset") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(64)
            assert(result.size)(equalTo(64L))
          },
        ),
        suite("cardinality")(
          test("reset two eldest bit") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            result.reset(67)
            result.reset(66)
            assert(result.cardinality)(equalTo(66L))
          },
          test("7 78") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.setWord(0, 78L) // 0100 1110
            result.setWord(1, 7L)  // 111
            assert(result.cardinality)(equalTo(7L))
          },
          test("empty bitset") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            assert(result.cardinality)(equalTo(0L))
          },
          test("full bitset") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            assert(result.cardinality)(equalTo(68L))
          },
        ),
        suite("isEmpty vs nonEmpty")(
          test("reset two eldest bit") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            result.reset(67)
            result.reset(66)
            assertTrue(!result.isEmpty) && assertTrue(result.nonEmpty)
          },
          test("empty bitset") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            assertTrue(result.isEmpty) && assertTrue(!result.nonEmpty)
          },
          test("full bitset") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            assertTrue(!result.isEmpty) && assertTrue(result.nonEmpty)
          },
        ),
      ),
      suite("mutable bitset methods")(
        suite("bit updates")(
          test("update two bits") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            result.update(1L, value = false)
            result.update(65L, value = false)
            assertTrue(!result.apply(1L)) && assertTrue(!result.apply(65L))
          },
          test("set two bits") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.set(1L)
            result.set(65L)
            assertTrue(result.apply(1L)) && assertTrue(result.apply(65L))
          },
          test("reset two bits") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            result.reset(1L)
            result.reset(65L)
            assertTrue(!result.apply(1L)) && assertTrue(!result.apply(65L))
          },
          test("empty bitset") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            assertTrue(result.isEmpty) && assertTrue(!result.nonEmpty)
          },
          test("full bitset") {
            val result = new scandex.bitset.mutable.LongArrayBitSet(68)
            result.fill()
            assertTrue(!result.isEmpty) && assertTrue(result.nonEmpty)
          },
        ),
        suite("copy")(
          test("copy") {
            val source = new scandex.bitset.mutable.LongArrayBitSet(68)
            source.setWord(0, 0x56765788L)
            source.setWord(1, 0xDL)
            val output = new scandex.bitset.mutable.LongArrayBitSet(68)
            output.copy(source)
            assertTrue(output.word(0) == 0x56765788L) &&
            assertTrue(output.word(1) == 0xDL)
          },
          test("copy with different sizes: large into small") {
            val source = new scandex.bitset.mutable.LongArrayBitSet(68)
            source.setWord(0, 0x56765788L)
            source.setWord(1, 0xDL)
            val output = new scandex.bitset.mutable.LongArrayBitSet(65)
            output.setWord(0, 7L)
            output.copy(source)
            assertTrue(output.word(0) == 0x56765788L) &&
            assertTrue(output.word(1) == 0x1L)
          },
          test("copy with different sizes: small into large") {
            val source = new scandex.bitset.mutable.LongArrayBitSet(64)
            source.setWord(0, 0x56765788L)
            val output = new scandex.bitset.mutable.LongArrayBitSet(68)
            output.setWord(0, 7L)
            output.setWord(1, 0xDL)
            assertZIO(ZIO.from(output.copy(source)).exit)(
              fails(
                isSubtype[IndexOutOfBoundsException](
                  hasMessage(containsString("out of bounds")),
                ),
              ),
            )
          },
        ),
        suite("logic operations")(
          suite("or")(
            test("or: DA | 6 = DE") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(68)
              a.setWord(0, 0xAL)
              a.setWord(1, 0xDL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(68)
              b.setWord(0, 0x6L)
              a.or(b)
              assertTrue(a.word(0) == 0xEL) && assertTrue(a.word(1) == 0xDL)
            },
            test("or: 0 | 0 = 0") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(68)
              val b = new scandex.bitset.mutable.LongArrayBitSet(68)
              a.or(b)
              assertTrue(a.word(0) == 0L) && assertTrue(a.word(1) == 0L)
            },
            test("or for bitsets with different sizes: error") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(68)
              a.setWord(0, 0xAL)
              a.setWord(1, 0xDL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(8)
              b.setWord(0, 0x6L)
              assertZIO(ZIO.from(a.or(b)).exit)(
                fails(
                  isSubtype[IndexOutOfBoundsException](
                    hasMessage(containsString("out of bounds")),
                  ),
                ),
              )
            },
          ),
          suite("and")(
            test("and: BC8 & E4E = A48") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.setWord(0, 0x8L)
              a.setWord(1, 0xCL)
              a.setWord(2, 0xBL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(150)
              b.setWord(0, 0xEL)
              b.setWord(1, 0x4L)
              b.setWord(2, 0xEL)
              a.and(b)
              assertTrue(a.word(0) == 0x8L) && assertTrue(a.word(1) == 0x4L) &&
              assertTrue(a.word(2) == 0xAL)
            },
            test("and: BC8 & 0 = 0") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.setWord(0, 0x8L)
              a.setWord(1, 0xCL)
              a.setWord(2, 0xBL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(149)
              a.and(b)
              assertTrue(a.word(0) == 0L) && assertTrue(a.word(1) == 0L)
            },
            test("and for bitsets with different sizes: error") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(68)
              a.setWord(0, 0xAL)
              a.setWord(1, 0xDL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(7)
              b.setWord(0, 0x6L)
              assertZIO(ZIO.from(a.and(b)).exit)(
                fails(
                  isSubtype[IndexOutOfBoundsException](
                    hasMessage(containsString("out of bounds")),
                  ),
                ),
              )
            },
            test("and for bitsets with different count of bits: ok") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(68)
              a.setWord(0, 0xAL)
              a.setWord(1, 0xDL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(67)
              b.setWord(0, 0x6L)
              a.and(b)
              assertTrue(a.word(0) == 2L) && assertTrue(a.word(1) == 0L)
            },
          ),
          suite("andNot")(
            test("andNot: ABC & ¬ ABC = 0") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.setWord(0, 0xCL)
              a.setWord(1, 0xBL)
              a.setWord(2, 0xAL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(150)
              b.copy(a)
              a.andNot(b)
              assertTrue(a.word(0) == 0L) && assertTrue(a.word(1) == 0L) &&
              assertTrue(a.word(2) == 0L)
            },
            test("andNot: ABC & ¬ 0 = ABC") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.setWord(0, 0xCL)
              a.setWord(1, 0xBL)
              a.setWord(2, 0xAL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.andNot(b)
              assertTrue(a.word(0) == 0xCL) && assertTrue(a.word(1) == 0xBL) &&
              assertTrue(a.word(2) == 0xAL)
            },
            test("andNot: ABC & ¬ A48 = B4") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.setWord(0, 0xCL)
              a.setWord(1, 0xBL)
              a.setWord(2, 0xAL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(150)
              b.setWord(0, 0x8L)
              b.setWord(1, 0x4L)
              b.setWord(2, 0xAL)
              a.andNot(b)
              assertTrue(a.word(0) == 0x4L) && assertTrue(a.word(1) == 0xBL) &&
              assertTrue(a.word(2) == 0L)
            },
          ),
          suite("xor")(
            test("xor: ABC XOR ABC = 0") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.setWord(0, 0xCL)
              a.setWord(1, 0xBL)
              a.setWord(2, 0xAL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(150)
              b.setWord(0, 0xCL)
              b.setWord(1, 0xBL)
              b.setWord(2, 0xAL)
              a.xor(b)
              assertTrue(a.word(0) == 0L) && assertTrue(a.word(1) == 0L) &&
              assertTrue(a.word(2) == 0L)
            },
            test("xor: ABC XOR 543 = FFF") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.setWord(0, 0xCL)
              a.setWord(1, 0xBL)
              a.setWord(2, 0xAL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(150)
              b.setWord(0, 0x3L)
              b.setWord(1, 0x4L)
              b.setWord(2, 0x5L)
              a.xor(b)
              assertTrue(a.word(0) == 0xFL) && assertTrue(a.word(1) == 0xFL) &&
              assertTrue(a.word(2) == 0xFL)
            },
            test("xor: ABC XOR 103 = BBF") {
              val a = new scandex.bitset.mutable.LongArrayBitSet(150)
              a.setWord(0, 0xCL)
              a.setWord(1, 0xBL)
              a.setWord(2, 0xAL)
              val b = new scandex.bitset.mutable.LongArrayBitSet(150)
              b.setWord(0, 0x3L)
              b.setWord(1, 0L)
              b.setWord(2, 0x1L)
              a.xor(b)
              assertTrue(a.word(0) == 0xFL) && assertTrue(a.word(1) == 0xBL) &&
              assertTrue(a.word(2) == 0xBL)
            },
          ),
        ),
      ),
      suite("internal iterative update")(
        test("should return false for not updated bitset") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()

          assert(result.updateWordWithIndex((word, _) => word & 0L))(
            equalTo(false),
          )
        },
        test("should return true for non-zero multiply") {
          val result = new scandex.bitset.mutable.LongArrayBitSet(8)
          result.fill()

          assertTrue(result.updateWordWithIndex((word, _) => word & 0xFEL))
        },
      ),
    )

}
