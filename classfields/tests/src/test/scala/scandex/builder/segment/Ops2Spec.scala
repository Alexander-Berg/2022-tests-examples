package scandex.builder.segment

import alien.memory.*
import zio.test.Assertion.*
import zio.test.*

object Ops2Spec extends ZIOSpecDefault {

  type L = "bitmap" := BoundedSequence[BoundedSequence[Value[Long]]]

  override def spec: Spec[TestEnvironment, Any] =
    suite("Ops2")(
      test("Fill Two-dimension memory without overlaps") {
        val width = 2L

        val height = 128L

        val memL = "bitmap" := (Values.Long * height) * width

        val vh = memL / % / % / $

        val memory: Memory[L] = Memory.unsafeAllocateNative(memL)

        var overlapped = false

        (0L until height).map { y =>
          (0L until width).map { x =>
            val mark = ((y + 1L) << 32L) + x + 1L

            val previous = vh.get(memory)(x, y)
            if (previous != 0) {
              val px = (previous & 0xFFFFFFFFL) - 1L
              val py = (previous >> 32L) - 1L

              println(s"BAD: x=$x y=$y px=$px py=$py")
              overlapped = true
            }

            vh.set(memory, mark)(x, y)
          }
        }
        assert(overlapped)(isFalse)
      },
    )

}
