package alien

import java.io.{ByteArrayInputStream, IOException}
import java.nio.ByteBuffer
import java.nio.channels.Channels
import alien.memory.*
import alien.memory.Memory.*
import zio.test.Assertion.*
import zio.test.*
import zio.test.ZIOSpecDefault

object MemorySpec extends ZIOSpecDefault {

  def spec =
    suite("Memory")(
      test("foldChunked") {
        val sample = "abcdefghij"

        doAndClose(Memory.ofArray(sample.getBytes)) { memory =>
          val result =
            memory.foldChunked(3)(Seq.empty[String]) { (seq, buffer) =>
              seq :+ bufferToString(buffer)
            }

          assert(result)(hasSameElements(Seq("abc", "def", "ghi", "j")))
        }
      },
      test("read enough to fit chunked") {

        doAndClose(Memory.allocateNative(10)) { memory =>
          val sample = "abcdefghijklmno"
          val bais   = new ByteArrayInputStream(sample.getBytes)
          val status = memory
            .readFrom(Channels.newChannel(bais), chunkLimit = 3)

          assertTrue(status == ReadSucceed) &&
          assertTrue(
            memory.foldChunked(4)("") { (string, buffer) =>
              string + bufferToString(buffer)
            } == "abcdefghij",
          )
        }
      } @@ TestAspect.ignore, // todo fix
      test("read enough to fit one slice") {
        doAndClose(Memory.allocateNative(10)) { memory =>
          val sample = "abcdefghij"
          val bais   = new ByteArrayInputStream(sample.getBytes)
          val status = memory.readFrom(Channels.newChannel(bais))

          assertTrue(status == ReadSucceed) &&
          assertTrue(
            memory.foldChunked(4)("") { (string, buffer) =>
              string + bufferToString(buffer)
            } == sample,
          )
        }
      } @@ TestAspect.ignore, // todo fix
      test("read small portion") {
        doAndClose(Memory.ofArray("0123456789".getBytes())) { memory =>
          val sample = "abcd"
          val bais   = new ByteArrayInputStream(sample.getBytes)
          val status = memory
            .readFrom(Channels.newChannel(bais), chunkLimit = 4)

          assertTrue(status == ReadEofReached(4L)) &&
          assertTrue(
            memory.foldChunked(4)("") { (string, buffer) =>
              string + bufferToString(buffer)
            } == "abcd456789",
          )
        }
      },
      test("read to desired position") {
        doAndClose(Memory.ofArray("0123456789".getBytes())) { memory =>
          val sample = "abcdefgh"
          val bais   = new ByteArrayInputStream(sample.getBytes)
          val status =
            memory.readToSliceFromChannel(offset = 3, limit = 5)(
              Channels.newChannel(bais),
              chunkLimit = 4,
            )

          assertTrue(status == ReadSucceed) &&
          assertTrue(
            memory.foldChunked(4)("") { (string, buffer) =>
              string + bufferToString(buffer)
            } == "012abcde89",
          )
        }
      },
      test("read from closed channel") {
        doAndClose(Memory.ofArray("0123456789".getBytes())) { memory =>
          val sample  = "abcdefgh"
          val bais    = new ByteArrayInputStream(sample.getBytes)
          val channel = Channels.newChannel(bais)
          channel.close()

          val status =
            memory.readToSliceFromChannel(offset = 3, limit = 5)(
              channel,
              chunkLimit = 4,
            )

          assert(status)(isSubtype[ReadFailed](Assertion.anything)) &&
          assert(status.asInstanceOf[ReadFailed].cause)(
            isSubtype[IOException](Assertion.anything),
          )
        }
      },
    )

  def doAndClose[L <: Layout, T](c: Memory[L])(f: Memory[L] => T): T = {
    try {
      f(c)
    } finally {
      c.close().getOrElse(())
    }
  }

  def bufferToString(buffer: ByteBuffer): String = {
    val bytes = new Array[Byte](buffer.remaining)
    buffer.get(bytes)
    new String(bytes)
  }

}
