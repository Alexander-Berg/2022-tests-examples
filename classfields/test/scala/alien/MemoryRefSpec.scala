package alien

import alien.memory.*
import alien.memory.ref.*
import zio.*
import zio.test.*
import zio.test.Assertion.*

object MemoryRefSpec extends ZIOSpecDefault {

  def spec =
    suite("MemoryRef")(
      test("deref") {
        val sample = "abcdefghij"

        val a = Values.Char * 10
        val b = Values.Char * 10

        val dynA = "a" := a
        val dynB = "b" := b

        val vha = dynA / % / $
        val vhb = dynB / % / $

        for {
          ref1 <- MemoryRef.allocate(dynA)
          ref2 <- MemoryRef.allocate(dynB)
          _ <- ref1.derefM { mem =>
            ZIO.foreach((0 until 10).toList) { i =>
              ZIO.from(vha.set(mem, sample.charAt(i))(i.toLong))
            }
          }
          _ <- ref1.derefM { mem1 =>
            ref2.derefM { mem2 =>
              ZIO.foreachDiscard(0 until 10) { i =>
                ZIO.succeed(vhb.set(mem2, vha.get(mem1)(i.toLong))(i.toLong))
              }
            }
          }
          chars <- ref2.derefM { mem =>
            ZIO.foreach((0 until 10).toList) { i =>
              ZIO.from(vhb.get(mem)(i.toLong))
            }
          }
          _ <- ref2.free()
          _ <- ref1.free()

        } yield assert(new String(chars.toArray))(equalTo(sample))
      },
    )

}
