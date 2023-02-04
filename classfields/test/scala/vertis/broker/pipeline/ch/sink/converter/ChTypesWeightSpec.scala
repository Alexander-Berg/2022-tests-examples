package vertis.broker.pipeline.ch.sink.converter

import com.google.protobuf.{Message, Timestamp}
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.generators.ProducerProvider._
import vertis.broker.pipeline.ch.sink.converter.ChTypesWeight.weight
import vertis.broker.pipeline.ch.sink.converter.ChTypesWeightSpec._
import vertis.broker.pipeline.ch.testkit.TestProtoClickhouseConverter
import vertis.clickhouse.model.ChType
import vertis.stream.model.TopicPartition
import vertis.stream.sink.StoredOffsetConverters

import scala.jdk.CollectionConverters._

/** @author kusaeva
  */
class ChTypesWeightSpec extends AnyWordSpec with Matchers {

  private val converter = TestProtoClickhouseConverter

  "ChTypesWeight" should {
    testCases.foreach { t =>
      s"get row from ${t.proto.getDescriptorForType.getName} with correct weight" in {
        val prepared = PreparedMessage(msg = t.proto, id = id, topicPartition = tp, offset = 0L)
        val schema = converter.toSchema(t.proto.getDescriptorForType)
        val row = converter.toRow(prepared, schema)
        row.weight shouldBe (t.weight + metaWeight)
      }
    }
  }
}

object ChTypesWeightSpec {
  sealed trait TestCase

  case class TestPrimitiveCase(proto: Message, weight: Long) extends TestCase

  private val id = "id"
  private val tp = TopicPartition("topic", 0)

  private val metaWeight =
    weight(ChType.String, id) +
      weight(ChType.String, StoredOffsetConverters.offsetName(tp)) +
      weight(ChType.UInt64, ()) // offset

  private val simpleMessageCase = {
    val x = Gen.chooseNum(Int.MinValue, Int.MaxValue).next
    val y = Gen.chooseNum(0, Int.MaxValue).next
    val str = Gen.alphaNumStr.next
    TestPrimitiveCase(
      SimpleMessage
        .newBuilder()
        .setInt32(x)
        .setUint32(y)
        .setBool(true)
        .setStr(str)
        .build(),
      weight(ChType.Int32, ()) +
        weight(ChType.UInt32, ()) +
        weight(ChType.UInt8, ()) +
        weight(ChType.String, str)
    )
  }

  private val withMessageCase = {
    val x = Gen.chooseNum(Int.MinValue, Int.MaxValue).next
    val str = Gen.alphaNumStr.next
    val foo = SimpleMessage.parseFrom(simpleMessageCase.proto.toByteString)
    val proto = WithMessage
      .newBuilder()
      .setNum(x)
      .setBool(false)
      .setStr(str)
      .setFoo(foo)
      .build()

    TestPrimitiveCase(
      proto,
      weight(ChType.Int32, ()) +
        weight(ChType.UInt8, ()) +
        weight(ChType.String, str) +
        weight(ChType.String, ProtoClickhouseConverterImpl.jsonPrinter.print(foo))
    )
  }

  private val withRepeatedCase = {
    val n = Gen.chooseNum(2, 10).next
    val proto = WithRepeated
      .newBuilder()
      .setNum(n)
      .setTimestamp(Timestamp.getDefaultInstance)
      .addAllListOfInt(Iterable.fill[Integer](n)(0).asJava)
      .build()
    TestPrimitiveCase(
      proto,
      // num
      weight(ChType.UInt32, ()) +
        weight(ChType.UInt64, ()) + // timestamp
        n * weight(ChType.UInt32, ()) // list of int
    )
  }

  private val withBytesCase = {
    val bs = simpleMessageCase.proto.toByteString
    val proto = WithBytes
      .newBuilder()
      .setB(bs)
      .build()
    TestPrimitiveCase(
      proto,
      bs.size().toLong
    )
  }

  private val withRepeatedEnumCase = {
    val proto = WithRepeatedEnum
      .newBuilder()
      .addAllListOfEnum(List(Enum.BAR, Enum.BAZ).asJava)
      .build()
    TestPrimitiveCase(
      proto,
      6
    )
  }

  private val testCases =
    Seq(
      simpleMessageCase,
      withMessageCase,
      withRepeatedCase,
      withBytesCase,
      withRepeatedEnumCase
    )
}
