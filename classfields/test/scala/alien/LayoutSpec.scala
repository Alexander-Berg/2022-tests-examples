package alien

import alien.memory.{Padding, Sequence, Value}

import java.nio.ByteOrder
import jdk.incubator.foreign.MemoryLayout.*
import zio.test.*
import zio.test.ZIOSpecDefault

object LayoutSpec extends ZIOSpecDefault {

  def spec =
    suite("Layout generation")(
      test("value") {
        val prototype = Value[Int](32)
        val equiv     = valueLayout(32, ByteOrder.nativeOrder())
        assertTrue(prototype.toLayout == equiv)
      },
      test("padding") {
        val prototype = Padding(32)
        val equiv     = paddingLayout(32)
        assertTrue(prototype.toLayout == equiv)
      },
      test("sequence") {
        val prototype = Sequence(10, Value[Long](32))
        val equiv = sequenceLayout(10, valueLayout(32, ByteOrder.nativeOrder()))
        assertTrue(prototype.toLayout == equiv)
      },
      test("unbounded sequence") {
        val prototype = Sequence(Value[Long](32))
        val equiv     = sequenceLayout(valueLayout(32, ByteOrder.nativeOrder()))
        assertTrue(prototype.toLayout == equiv)
      },
      //      test("structure") {
      //        case class Prototype(a: Value[Int], b: Value[Long]) extends Struct
      //        val prototype = Prototype(Value[Int](32), Value[Long](32))
      //        val equiv = ofStruct(
      //          ofValueBits(32, ByteOrder.nativeOrder()).withName("a"),
      //          ofValueBits(32, ByteOrder.nativeOrder()).withName("b"),
      //        )
      //        assert(prototype.toLayout)(equalTo(equiv))
      //      },
      //      test("union") {
      //        case class Prototype(a: Value[Int], b: Value[Long]) extends Union
      //        val prototype = Prototype(Value[Int](32), Value[Long](32))
      //        val equiv = ofUnion(
      //          ofValueBits(32, ByteOrder.nativeOrder()).withName("a"),
      //          ofValueBits(32, ByteOrder.nativeOrder()).withName("b"),
      //        )
      //        assert(prototype.toLayout)(equalTo(equiv))
      //      },
    )

}
