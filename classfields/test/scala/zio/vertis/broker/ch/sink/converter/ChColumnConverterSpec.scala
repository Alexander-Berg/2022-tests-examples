package zio.vertis.broker.ch.sink.converter

import com.google.protobuf.{BoolValue, Message}
import com.google.protobuf.util.Timestamps
import common.zio.testkit.protogen.JavaProtoGen
import common.zio.testkit.protogen.ProtoGen.GenSettings
import org.joda.time.DateTime
import vertis.broker.pipeline.ch.sink.converter.ChColumnProtoDescriptors.TimestampColumn
import vertis.broker.pipeline.ch.sink.converter.{PreparedMessage, ProtoClickhouseConverterImpl, WithAllWrapped}
import vertis.clickhouse.model.ChColumn
import vertis.stream.model.TopicPartition
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import scala.jdk.CollectionConverters._

/**
 * @author reimai
  */
object ChColumnConverterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("WrapperColumn")(
      test("write nulls") {
        val emptyWrapped = WithAllWrapped.newBuilder().build()
        val columns = toColumns(emptyWrapped)
        assert(columns.values)(forall(equalTo(None)))
      },
      testM("write non-empty values") {
        check(
          JavaProtoGen
            .gen[WithAllWrapped](GenSettings(nullProbability = 0))
            .map(
              _.toBuilder
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .build()
            )
        ) { msg =>
          val columns = toColumns(msg)
          assert(columns)(isNonEmpty) &&
          assert(columns.values)(forall(not(equalTo(None))))
        }
      }
    )

  private def toColumns(m: Message, ts: String = "timestamp"): Map[ChColumn, Any] = {
    val desc = m.getDescriptorForType
    val schema = ProtoClickhouseConverterImpl.toSchema(desc, ts, desc.getFields.asScala.map(_.getName).toSeq)
    ProtoClickhouseConverterImpl
      .toRow(PreparedMessage(m, "", TopicPartition("", 0), 0), schema)
      .data
      .view
      .filterKeys(!_.name.startsWith("_"))
      .mapValues(_.value)
      .toMap
  }
}
