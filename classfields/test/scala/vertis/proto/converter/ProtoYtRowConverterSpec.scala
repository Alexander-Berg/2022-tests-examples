package vertis.proto.converter

import com.google.protobuf.DescriptorProtos.FieldDescriptorProto
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.timestamp.Timestamp
import com.google.protobuf.util.Timestamps
import com.google.protobuf.{ByteString, BytesValue, DynamicMessage, Message, UInt32Value}
import org.scalatest.matchers.should.Matchers
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.{YTreeNode, YTreeStringNode}
import ru.yandex.vertis.generators.ProducerProvider._
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import vertis.proto.converter.ProtoYtRowConverterSpec._
import vertis.proto.converter.test._
import vertis.yt.proto.testkit.DynamicProtoTestAccessors
import vertis.yt.util.proto.{DescriptorWrapper, FieldType}
import vertis.yt.util.support.YsonSupport
import vertis.zio.BTask
import vertis.zio.test.ZioSpecBase

import scala.jdk.CollectionConverters._
import zio.interop.catz._

/** @author kusaeva
  */
class ProtoYtRowConverterSpec extends ZioSpecBase with Matchers with DynamicProtoTestAccessors with YsonSupport {

  val converter = new ProtoYsonConverterImpl[BTask]()

  "ProtoYtRowConverterSpec" should {
    testCases.foreach { case TestCase(description, msg, expected) =>
      description in ioTest {
        for {
          row <- converter.toRow(msg)
          _ <- check(description)(
            row.values() should contain theSameElementsAs expected
          )
        } yield ()
      }
    }

    "convert repeated field correctly" in {
      val descriptor = WithRepeatedPrimitive.javaDescriptor
      val field = descriptor.findFieldByName("list_of_int")
      val ints = Seq(15, 16, 23, 42)

      val builder = WithRepeatedPrimitive.of(0, ints)
      val msg = getMessage(
        descriptor,
        builder.toByteString
      )
      val ytColumn = converter.toColumn(field)
      val ytNode = converter.toNode(field, msg).value
      ytColumn.protoDescriptor shouldBe defined

      val protoResult = DynamicMessage.parseFrom(ytColumn.protoDescriptor.get, ytNode.bytesValue())
      val values = getTheOnlyFieldRepeatedValues(protoResult)

      values.size shouldBe 4
      values should contain theSameElementsAs ints
    }

    "convert map field correctly" in {
      val descriptor = WithMap.javaDescriptor
      val field = descriptor.findFieldByName("map_of_string")
      val t1 = "1" -> "2"
      val t2 = "3" -> "4"
      val map = Map(t1, t2)
      val msg = getMessage(
        descriptor,
        WithMap.of(0, map).toByteString
      )
      val ytColumn = converter.toColumn(field)
      val ytNode = converter.toNode(field, msg).value
      ytColumn.protoDescriptor shouldBe defined

      val protoResult = DynamicMessage.parseFrom(ytColumn.protoDescriptor.get, ytNode.bytesValue())
      val mapValues = getTheOnlyFieldRepeatedValues(protoResult)

      mapValues.size shouldBe 2

      val values = mapValues.map(_.getAllFields.asScala.values.toSeq)
      values should contain theSameElementsAs Seq(t1, t2).map(_.productIterator.toList)
    }
  }
}

object ProtoYtRowConverterSpec {

  object YsonSupporter extends YsonSupport {
    override def stringNode(msg: Message): YTreeStringNode = super.stringNode(msg)
  }

  import YsonSupporter._

  case class TestCase(description: String, msg: DynamicMessage, expected: Seq[YTreeNode])

  private def getMessage(descriptor: Descriptor, bytes: ByteString): DynamicMessage =
    DynamicMessage.parseFrom(descriptor, bytes)

  val UIntMaxValue: Long = Int.MaxValue.toLong * 2 + 1

  val WrappedUIntMaxValue: UInt32Value = {
    UInt32Value.newBuilder.setValue(-1).build()
  }

  val ListOfUIntMaxValue: DynamicMessage = {
    val dsc = DescriptorWrapper.listify(FieldType.Primitive(FieldDescriptorProto.Type.TYPE_UINT32))
    val fieldDescriptor = dsc.getFields.get(0)
    DynamicMessage.newBuilder(dsc).addRepeatedField(fieldDescriptor, -1).build
  }

  val ListOfWrappedUIntMaxValue: DynamicMessage = {
    val dsc = DescriptorWrapper.listify(FieldType.Message(UInt32Value.getDescriptor))
    val fieldDescriptor = dsc.getFields.get(0)
    DynamicMessage.newBuilder(dsc).addRepeatedField(fieldDescriptor, WrappedUIntMaxValue).build
  }

  def listOfBytes(bytes: Seq[Array[Byte]]): DynamicMessage = {
    val dsc = DescriptorWrapper.listify(FieldType.Primitive(FieldDescriptorProto.Type.TYPE_BYTES))
    val fieldDescriptor = dsc.getFields.get(0)
    val b = DynamicMessage.newBuilder(dsc)
    bytes.foreach(b.addRepeatedField(fieldDescriptor, _))
    b.build()
  }

  def listOfWBytes(bytes: Seq[Array[Byte]]): DynamicMessage = {
    val dsc = DescriptorWrapper.listify(FieldType.Message(BytesValue.getDescriptor))
    val fieldDescriptor = dsc.getFields.get(0)
    val b = DynamicMessage.newBuilder(dsc)
    bytes.foreach(x =>
      b.addRepeatedField(fieldDescriptor, BytesValue.newBuilder.setValue(ByteString.copyFrom(x)).build())
    )
    b.build()
  }

  private lazy val testCases = Seq(
    {
      val msg = RandomProtobufGenerator.genForScala[PrimitiveTypesMessage].next(1).head
      TestCase(
        "convert primitive fields",
        getMessage(
          PrimitiveTypesMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.stringNode(msg.string),
          YTree.integerNode(msg.int32),
          YTree.integerNode(msg.int64),
          YTree.integerNode(msg.sint32),
          YTree.integerNode(msg.sint64),
          YTree.integerNode(msg.sfixed32),
          YTree.integerNode(msg.sfixed64),
          YTree.unsignedIntegerNode(msg.uint32),
          YTree.unsignedIntegerNode(msg.uint64),
          YTree.unsignedIntegerNode(msg.fixed32),
          YTree.unsignedIntegerNode(msg.fixed64),
          YTree.doubleNode(msg.float),
          YTree.doubleNode(msg.double),
          YTree.booleanNode(msg.bool)
        )
      )
    }, {
      val msg = PrimitiveTypesMessage.defaultInstance
      TestCase(
        "convert empty primitive fields",
        getMessage(
          PrimitiveTypesMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.stringNode(msg.string),
          YTree.integerNode(msg.int32),
          YTree.integerNode(msg.int64),
          YTree.integerNode(msg.sint32),
          YTree.integerNode(msg.sint64),
          YTree.integerNode(msg.sfixed32),
          YTree.integerNode(msg.sfixed64),
          YTree.unsignedIntegerNode(msg.uint32),
          YTree.unsignedIntegerNode(msg.uint64),
          YTree.unsignedIntegerNode(msg.fixed32),
          YTree.unsignedIntegerNode(msg.fixed64),
          YTree.doubleNode(msg.float),
          YTree.doubleNode(msg.double),
          YTree.booleanNode(msg.bool)
        )
      )
    }, {
      val msg = RandomProtobufGenerator.genForScala[WithAllWrapped].next(1).head
      TestCase(
        "convert wrapped primitive fields",
        getMessage(
          WithAllWrapped.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.unsignedIntegerNode(msg.getUint32),
          YTree.unsignedIntegerNode(msg.getUint64),
          YTree.integerNode(msg.getInt32),
          YTree.integerNode(msg.getInt64),
          YTree.stringNode(msg.getString),
          YTree.booleanNode(msg.getBool),
          YTree.doubleNode(msg.getDouble),
          YTree.doubleNode(msg.getFloat),
          YTree.bytesNode(msg.getBytes.toByteArray)
        )
      )
    }, {
      val msg = WithAllWrapped.defaultInstance
      TestCase(
        "convert empty wrapped primitive fields",
        getMessage(
          WithAllWrapped.javaDescriptor,
          msg.toByteString
        ),
        Seq()
      )
    }, {
      val msg = RandomProtobufGenerator.genForScala[WithMessage].next(1).head
      TestCase(
        "convert proto message field",
        getMessage(
          WithMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.integerNode(msg.num),
          YTree.stringNode(msg.str),
          YTree.booleanNode(msg.bool),
          stringNode(getMessage(SimpleMessage.javaDescriptor, msg.getFoo.toByteString))
        )
      )
    }, {
      val fullMsg = RandomProtobufGenerator.genForScala[WithMessage].next(1).head
      val msg = fullMsg.clearFoo
      TestCase(
        "convert empty proto message field",
        getMessage(
          WithMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.integerNode(msg.num),
          YTree.stringNode(msg.str),
          YTree.booleanNode(msg.bool)
        )
      )
    }, {
      val fullMsg = RandomProtobufGenerator.genForScala[WithEnum].next(1).head
      val msg = fullMsg.withEnum(Enum.BAR)

      val `enum` = msg.enum.javaValueDescriptor.getName
      TestCase(
        "convert enum field",
        getMessage(
          WithEnum.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.integerNode(msg.num),
          YTree.stringNode(enum)
        )
      )
    }, {
      val fullMsg = RandomProtobufGenerator.genForScala[WithEnum].next(1).head
      val msg = fullMsg.withEnum(Enum.UNKNOWN)
      TestCase(
        "convert empty enum field",
        getMessage(
          WithEnum.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.integerNode(msg.num),
          YTree.stringNode("UNKNOWN")
        )
      )
    }, {
      val msg = RandomProtobufGenerator.genForScala[WithTimestamp].next(1).head
      TestCase(
        "convert timestamp field",
        getMessage(
          WithTimestamp.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.stringNode(msg.str),
          YTree.unsignedIntegerNode(Timestamps.toMicros(Timestamp.toJavaProto(msg.getTs)))
        )
      )
    }, {
      val fullMsg = RandomProtobufGenerator.genForScala[WithTimestamp].next(1).head
      val msg = fullMsg.clearTs
      TestCase(
        "convert empty timestamp field",
        getMessage(
          WithTimestamp.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.stringNode(msg.str)
        )
      )
    }, {
      val msg = RandomProtobufGenerator.genForScala[WithOneOfOtherType].next(1).head
      TestCase(
        "convert missing oneOf fields as defaults",
        getMessage(
          WithOneOfOtherType.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.integerNode(msg.num),
          YTree.integerNode(msg.getMobileReferrer),
          YTree.stringNode(msg.getWebReferrer)
        )
      )
    }, {
      val msg = RandomProtobufGenerator.genForScala[WithOneOfMessage].next(1).head
      val hasTimestamp = msg.message.withTimestamp.isDefined
      TestCase(
        "convert missing oneOf proto fields as nulls",
        getMessage(
          WithOneOfMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.integerNode(msg.num),
          stringNode(
            if (hasTimestamp) {
              getMessage(WithTimestamp.javaDescriptor, msg.getWithTimestamp.toByteString)
            } else {
              getMessage(SimpleMessage.javaDescriptor, msg.getSimple.toByteString)
            }
          )
        )
      )
    }, {
      val longValue = Int.MaxValue + 100L
      val msg = TypesMessage.defaultInstance.withInt64(longValue)
      TestCase(
        "convert int64 as signed",
        getMessage(
          TypesMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.unsignedIntegerNode(0),
          YTree.unsignedIntegerNode(0),
          YTree.integerNode(0),
          YTree.integerNode(0),
          YTree.stringNode(""),
          YTree.booleanNode(false),
          YTree.integerNode(0),
          YTree.doubleNode(0),
          YTree.doubleNode(0),
          YTree.integerNode(0),
          YTree.integerNode(longValue),
          YTree.unsignedIntegerNode(0),
          YTree.integerNode(0),
          YTree.unsignedIntegerNode(0)
        )
      )
    }, {
      val msg = TypesMessage.defaultInstance.withUint32(-1)
      TestCase(
        "convert uint32 max value without an error",
        getMessage(
          TypesMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.unsignedIntegerNode(0),
          YTree.unsignedIntegerNode(0),
          YTree.integerNode(0),
          YTree.integerNode(0),
          YTree.stringNode(""),
          YTree.booleanNode(false),
          YTree.integerNode(0),
          YTree.doubleNode(0),
          YTree.doubleNode(0),
          YTree.integerNode(0),
          YTree.integerNode(0),
          YTree.unsignedIntegerNode(0),
          YTree.integerNode(0),
          YTree.unsignedIntegerNode(UIntMaxValue)
        )
      )
    }, {
      val msg = TypesMessage.defaultInstance.withFixed32(-1)
      TestCase(
        "convert fixed32 max value without an error",
        getMessage(
          TypesMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.unsignedIntegerNode(0),
          YTree.unsignedIntegerNode(UIntMaxValue),
          YTree.integerNode(0),
          YTree.integerNode(0),
          YTree.stringNode(""),
          YTree.booleanNode(false),
          YTree.integerNode(0),
          YTree.doubleNode(0),
          YTree.doubleNode(0),
          YTree.integerNode(0),
          YTree.integerNode(0),
          YTree.unsignedIntegerNode(0),
          YTree.integerNode(0),
          YTree.unsignedIntegerNode(0)
        )
      )
    }, {
      val msg = TypesMessage.defaultInstance.withUint64(-1)
      TestCase(
        "convert uint64 max value without an error",
        getMessage(
          TypesMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.unsignedIntegerNode(0),
          YTree.unsignedIntegerNode(0),
          YTree.integerNode(0),
          YTree.integerNode(0),
          YTree.stringNode(""),
          YTree.booleanNode(false),
          YTree.integerNode(0),
          YTree.doubleNode(0),
          YTree.doubleNode(0),
          YTree.integerNode(0),
          YTree.integerNode(0),
          YTree.unsignedIntegerNode(0),
          YTree.integerNode(0),
          YTree.unsignedIntegerNode(-1)
        )
      )
    }, {
      val msg = WithWrapped.defaultInstance.withNum(-1) // todo
      TestCase(
        "convert wrapped uint32 max value without an error",
        getMessage(
          WithWrapped.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.stringNode(""),
          YTree.unsignedIntegerNode(UIntMaxValue)
        )
      )
    }, {
      val msg = WithRepeatedPrimitive.defaultInstance.withListOfInt(Seq(-1))
      TestCase(
        "convert a list of uint32 max value without an error",
        getMessage(
          WithRepeatedPrimitive.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.integerNode(0),
          YTree.bytesNode(ListOfUIntMaxValue.toByteArray)
        )
      )
    }, {
      val msg = WithRepeatedWrappedPrimitive.defaultInstance.withListOfWrappedInt(Seq(-1))
      TestCase(
        "convert a list of  wrapped uint32 max value without an error",
        getMessage(
          WithRepeatedWrappedPrimitive.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.integerNode(0),
          YTree.bytesNode(ListOfWrappedUIntMaxValue.toByteArray)
        )
      )
    }, {
      val msg = RandomProtobufGenerator
        .genForScala[BytesMessage]
        .next(1)
        .head
      TestCase(
        "convert bytes fields",
        getMessage(
          BytesMessage.javaDescriptor,
          msg.toByteString
        ),
        Seq(
          YTree.bytesNode(msg.b.toByteArray),
          YTree.bytesNode(msg.wb.map(_.toByteArray).getOrElse(Array[Byte]())),
          stringNode(listOfBytes(msg.rb.map(_.toByteArray))),
          stringNode(listOfWBytes(msg.rwb.map(_.toByteArray)))
        )
      )
    }
  )
}
