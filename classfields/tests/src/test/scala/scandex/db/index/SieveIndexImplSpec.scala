package scandex.db.index

import scandex.db.segments.constructors.impl.v1.fixed.OffHeapBitsetSegmentBuilder
import zio.test.*
import zio.test.Assertion.equalTo

object SieveIndexImplSpec extends ZIOSpecDefault {

  private val active = Array[Long](0xB6) // 10110110

  override def spec: Spec[TestEnvironment, Any] = {
    suite("SiftIndex")(
      suite("sift inactive")(
        test("among all documents") {
          val documents = new scandex.bitset.mutable.LongArrayBitSet(8)
          documents.fill()
          for {
            isFound <- buildIndex(active).siftActive(documents)
          } yield assertTrue(isFound) &&
            assertTrue(documents.array.head == active(0))
        },
        test("one active") {
          val documents = new scandex.bitset.mutable.LongArrayBitSet(8)
          documents.setWord(0, 0x69)
          for {
            isFound <- buildIndex(active).siftActive(documents)
          } yield assertTrue(isFound) &&
            assertTrue(documents.array.head == 0x20L)
        },
        test("no active") {
          val documents = new scandex.bitset.mutable.LongArrayBitSet(8)
          documents.setWord(0, 0x49)
          for {
            isFound <- buildIndex(active).siftActive(documents)
          } yield assert(isFound)(equalTo(false)) &&
            assertTrue(documents.array.head == 0L)
        },
      ),
    )
  }

  def buildIndex(active: Array[Long]): SieveIndex = {
    val activeBitSetBuilder = OffHeapBitsetSegmentBuilder(active.length * 64L)
    active.indices.foreach(i => activeBitSetBuilder.set(i.toLong, active(i)))

    SieveIndexImpl(activeBitSetBuilder.build())
  }

}
