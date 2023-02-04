package common.zio.testkit.protogen

import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FieldDescriptor.Type
import com.google.protobuf.{CodedOutputStream, Descriptors}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}
import zio.random.Random
import zio.test.{Gen, Sized}

import java.io.ByteArrayOutputStream
import scala.jdk.CollectionConverters._

object ProtoGen {
  case class GenSettings(nullProbability: Double = 0.5)

  def apply[T <: GeneratedMessage: GeneratedMessageCompanion]: Gen[Random with Sized, T] = gen[T]()

  def gen[T <: GeneratedMessage: GeneratedMessageCompanion](
      genSettings: GenSettings = GenSettings()): Gen[Random with Sized, T] = {
    val companion = implicitly[GeneratedMessageCompanion[T]]
    generate(companion.javaDescriptor, genSettings)
      .map(companion.parseFrom)
  }

  private[protogen] def generate(
      desc: Descriptors.Descriptor,
      genSettings: GenSettings): Gen[Random with Sized, Array[Byte]] = {
    Gen
      .zipAll(desc.getFields.asScala.map(genField(_, genSettings)))
      .map { l =>
        val baos = new ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        l.foreach(pw => pw(cos).write())
        cos.flush()
        baos.toByteArray
      }
  }

  private def genPositiveInt: Gen[Random, Int] = Gen.int(0, Int.MaxValue)

  private def genPositiveLong: Gen[Random, Long] = Gen.long(0L, Long.MaxValue)

  // scalastyle:off cyclomatic.complexity method.length
  private def genField(field: FieldDescriptor, genSettings: GenSettings): Gen[Random with Sized, PartialWriter] = {
    val id = field.getNumber

    def genV: Gen[Random with Sized, PartialWriter] = field.getType match {
      case Type.DOUBLE => Gen.anyDouble.map(v => DoubleWriter(id, v))
      case Type.FLOAT => Gen.anyFloat.map(v => FloatWriter(id, v))
      case Type.INT32 => Gen.anyInt.map(v => Int32Writer(id, v))
      case Type.INT64 => Gen.anyLong.map(v => Int64Writer(id, v))
      case Type.UINT32 => genPositiveInt.map(v => UInt32Writer(id, v))
      case Type.UINT64 => genPositiveLong.map(v => UInt64Writer(id, v))
      case Type.SINT32 => Gen.anyInt.map(v => SInt32Writer(id, v))
      case Type.SINT64 => Gen.anyLong.map(v => SInt64Writer(id, v))

      case Type.FIXED32 => Gen.anyInt.map(v => Fixed32Writer(id, v))
      case Type.FIXED64 => Gen.anyLong.map(v => Fixed64Writer(id, v))
      case Type.SFIXED32 => Gen.anyInt.map(v => SFixed32Writer(id, v))
      case Type.SFIXED64 => Gen.anyLong.map(v => SFixed64Writer(id, v))
      case Type.BOOL => Gen.boolean.map(v => BoolWriter(id, v))
      case Type.STRING => Gen.anyString.map(v => StringWriter(id, v))

      case Type.ENUM =>
        Gen
          .oneOf(field.getEnumType.getValues.asScala.map(Gen.const(_)).toSeq: _*)
          .map(e => EnumWriter(id, e))

      case Type.BYTES =>
        Gen
          .int(1, 40)
          .flatMap(s => Gen.listOfN(s)(Gen.anyByte))
          .map(l => BytesWriter(id, l.toArray))

      case Type.MESSAGE =>
        generate(field.getMessageType, genSettings)
          .map(b => BytesWriter(id, b))

      case t => throw new RuntimeException(s"Unsupported field type $t")
    }

    (field.isOptional, field.isRepeated) match {
      case (_, true) => Gen.listOf(genV).map(l => RepeatedWriter(l))
      case (true, _) =>
        genV.flatMap { writer =>
          Gen.double(0, 1).map { e =>
            if (e < genSettings.nullProbability) NullWriter()
            else writer
          }
        }
      case _ => genV
    }
  }

  type PartialWriter = CodedOutputStream => Writer

  abstract class Writer {
    def write(): Unit
  }

  case class DoubleWriter(id: Int, v: Double)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeDouble(id, v)
  }

  case class FloatWriter(id: Int, v: Float)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeFloat(id, v)
  }

  case class Int32Writer(id: Int, v: Int)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeInt32(id, v)
  }

  case class UInt32Writer(id: Int, v: Int)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeUInt32(id, v)
  }

  case class SInt32Writer(id: Int, v: Int)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeSInt32(id, v)
  }

  case class Int64Writer(id: Int, v: Long)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeInt64(id, v)
  }

  case class UInt64Writer(id: Int, v: Long)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeUInt64(id, v)
  }

  case class SInt64Writer(id: Int, v: Long)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeSInt64(id, v)
  }

  case class Fixed32Writer(id: Int, v: Int)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeFixed32(id, v)
  }

  case class SFixed32Writer(id: Int, v: Int)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeSFixed32(id, v)
  }

  case class Fixed64Writer(id: Int, v: Long)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeFixed64(id, v)
  }

  case class SFixed64Writer(id: Int, v: Long)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeSFixed64(id, v)
  }

  case class BoolWriter(id: Int, v: Boolean)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeBool(id, v)
  }

  case class StringWriter(id: Int, v: String)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeString(id, v)
  }

  case class EnumWriter(id: Int, v: Descriptors.EnumValueDescriptor)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeEnum(id, v.getNumber)
  }

  case class BytesWriter(id: Int, v: Array[Byte])(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeByteArray(id, v)
  }

  case class MessageWriter(id: Int, v: Boolean)(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = cos.writeBool(id, v)
  }

  case class NullWriter()(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = ()
  }

  case class RepeatedWriter(v: List[PartialWriter])(cos: CodedOutputStream) extends Writer {
    override def write(): Unit = v.foreach(pw => pw(cos).write())
  }
}
