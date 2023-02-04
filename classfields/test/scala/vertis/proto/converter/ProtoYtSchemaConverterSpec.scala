package vertis.proto.converter

import org.scalatest.matchers.should.Matchers
import vertis.yt.model.YtColumn
import com.google.protobuf.Descriptors.Descriptor
import ProtoYtSchemaConverterSpec._
import common.yt.schema.YtTypes
import vertis.proto.converter.test._
import vertis.zio.BTask
import vertis.zio.test.ZioSpecBase
import zio.interop.catz._

/** @author kusaeva
  */
class ProtoYtSchemaConverterSpec extends ZioSpecBase with Matchers {

  val converter = new ProtoYsonConverterImpl[BTask]()

  "ProtoYtSchemaConverterSpec" should {
    testCases.foreach {
      case TestPrimitiveCase(message, descriptor, expected) =>
        s"get schema from $message" in ioTest {
          val columns = converter.toSchema(descriptor)
          check("check columns:")(columns should contain theSameElementsAs expected)
        }
      case TestProtoCase(message, descriptor, protoColumns) =>
        s"get schema from $message" in ioTest {
          val columns = converter.toSchema(descriptor)
          check("check proto columns:") {
            protoColumns.forall { case col @ ProtoColumn(index, _) => col.check(columns(index)) } shouldBe true
          }
        }
    }
  }
}

object ProtoYtSchemaConverterSpec {
  sealed trait TestCase

  case class TestPrimitiveCase(message: String, descriptor: Descriptor, expected: Seq[YtColumn]) extends TestCase

  case class TestProtoCase(message: String, descriptor: Descriptor, protoColumns: Seq[ProtoColumn]) extends TestCase

  case class ProtoColumn(index: Int, name: String) {

    def check(column: YtColumn): Boolean = {
      column.`type` == YtTypes.string &&
      column.name == name &&
      column.protoDescriptor.isDefined
    }
  }

  private val int32column = YtColumn(
    name = "num",
    `type` = YtTypes.int32
  )

  private val uint32column = YtColumn(
    name = "num",
    `type` = YtTypes.uint32
  )

  private val strColumn = YtColumn(
    name = "str",
    `type` = YtTypes.string
  )

  private val boolColumn = YtColumn(
    name = "bool",
    `type` = YtTypes.boolean
  )

  private val messageColumn = YtColumn(
    name = "foo",
    `type` = YtTypes.string,
    protoDescriptor = Some(SimpleMessage.javaDescriptor)
  )

  private val timestampColumn = YtColumn(
    name = "ts",
    `type` = YtTypes.timestamp
  )

  private val enumColumn = YtColumn(
    name = "enum",
    `type` = YtTypes.string
  )

  private val testCases = Seq(
    TestPrimitiveCase(
      "primitive fields",
      SimpleMessage.javaDescriptor,
      Seq(int32column, strColumn, boolColumn)
    ),
    TestPrimitiveCase(
      "message field",
      WithMessage.javaDescriptor,
      Seq(int32column, strColumn, boolColumn, messageColumn)
    ),
    TestPrimitiveCase(
      "enum field",
      WithEnum.javaDescriptor,
      Seq(int32column, enumColumn)
    ),
    TestPrimitiveCase(
      "timestamp field",
      WithTimestamp.javaDescriptor,
      Seq(strColumn, timestampColumn)
    ),
    TestPrimitiveCase(
      "wrapped primitive field",
      WithWrapped.javaDescriptor,
      Seq(strColumn, uint32column)
    ),
    TestPrimitiveCase(
      "one of field",
      WithOneOf.javaDescriptor,
      Seq(
        int32column,
        YtColumn(name = "web_referrer", `type` = YtTypes.string),
        YtColumn(name = "mobile_referrer", `type` = YtTypes.string)
      )
    ),
    TestPrimitiveCase(
      "one of field of different types",
      WithOneOfOtherType.javaDescriptor,
      Seq(
        int32column,
        YtColumn(name = "web_referrer", `type` = YtTypes.string),
        YtColumn(name = "mobile_referrer", `type` = YtTypes.int64)
      )
    ),
    TestProtoCase(
      "one of field of message type",
      WithOneOfMessage.javaDescriptor,
      Seq(ProtoColumn(index = 1, name = "simple"), ProtoColumn(index = 2, name = "with_timestamp"))
    ),
    TestProtoCase(
      "repeated primitive field",
      WithRepeatedPrimitive.javaDescriptor,
      Seq(ProtoColumn(1, "list_of_int"))
    ),
    TestProtoCase(
      "repeated wrapped primitive field",
      WithRepeatedWrappedPrimitive.javaDescriptor,
      Seq(ProtoColumn(1, "list_of_wrapped_int"))
    ),
    TestProtoCase(
      "repeated message field",
      WithRepeatedMessage.javaDescriptor,
      Seq(ProtoColumn(1, "list_of_msg"))
    ),
    TestProtoCase(
      "map field",
      WithMap.javaDescriptor,
      Seq(ProtoColumn(1, "map_of_string"))
    ),
    TestProtoCase(
      "map of enum field",
      WithMapOfEnum.javaDescriptor,
      Seq(ProtoColumn(1, "map_of_enum"))
    ),
    TestProtoCase(
      "map of wrapped primitive field",
      WithMapOfWrapped.javaDescriptor,
      Seq(ProtoColumn(1, "map_of_wrapped_int"))
    ),
    TestProtoCase(
      "map of message field",
      WithMapOfMessage.javaDescriptor,
      Seq(ProtoColumn(1, "map_of_foo"))
    )
  )
}
