package vertis.pipeline.convert

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.util.Timestamps
import com.google.protobuf.{ByteString, DynamicMessage, Message, UInt32Value}
import common.zio.clients.clickhouse.jdbc.ClickhouseJdbcClient.ClickhouseJdbcClient
import common.zio.logging.Logging
import io.circe.Encoder
import ru.yandex.clickhouse.util.ClickHouseArrayUtil
import vertis.broker.pipeline.ch.batching.TestBatching
import vertis.broker.pipeline.ch.sink.converter.ChColumnProtoDescriptors.TimestampColumn
import vertis.broker.pipeline.ch.sink.converter.ProtoClickhouseConverterImpl.jsonPrinter
import vertis.broker.pipeline.ch.sink.converter.{Enum, SimpleMessage, TestMessage}
import vertis.broker.pipeline.ch.sink.queries.{ClickhouseQueries, QueriesSupport}
import vertis.core.time.DateTimeUtils.DefaultTimeZone
import vertis.core.utils.NoWarnFilters
import vertis.stream.model.TopicPartition
import vertis.zio.BaseEnv
import zio.UIO
import zio.test.Assertion.equalTo
import zio.test._

import java.lang.{Boolean => JavaBoolean, Long => JavaLong}
import java.time.format.DateTimeFormatter
import java.util
import scala.annotation.nowarn
import scala.jdk.CollectionConverters._

/** @author kusaeva
  */
object ChWriterReadingIntSpec
  extends ClickhouseTestSpec
  with TestBatching
  with QueriesSupport
  with ChProtoCompareSupport {

  override protected def chSpec: ZSpec[BaseEnv with ClickhouseJdbcClient, Any] =
    suite("ChWriterRead")(createAndWrite)

  private val tp = TopicPartition(topic = "myt//test/topic", partition = 0)

  private def getMessage(descriptor: Descriptor, bytes: ByteString) = DynamicMessage.parseFrom(descriptor, bytes)

  private def listToString[T](list: List[T]): String =
    ClickHouseArrayUtil.toString(list.asJavaCollection, DefaultTimeZone, DefaultTimeZone)

  private val createAndWrite = testM("create table and write test message correctly") {
    chTestM(TestMessage.getDescriptor).use { test =>
      import test._
      @nowarn(NoWarnFilters.UnusedLocals)
      implicit val encoder: Encoder[SimpleMessage] = Encoder[String].contramap(jsonPrinter.print)

      for {
        table <- UIO(getTable).tap(schemaEvolution.update)
        tableCreated <- assertM(client.executeQuery(ClickhouseQueries.checkTableExists(sinkConf.id)))(equalTo(true))

        ints = List(1, 2, 3)
        strings = List("a", "b", "c")
        enum = Enum.BAR
        enums = List(Enum.BAR, Enum.BAZ)
        simpleMsg = SimpleMessage.newBuilder().setBool(true).setInt32(42).build()
        ts = Timestamps.fromMillis(1603367778745L)
        timestamps = List(ts)
        foos = List(SimpleMessage.getDefaultInstance, simpleMsg)
        mapOfString = Map("a" -> "b", "c" -> "d")
        mapOfWInt = Map(JavaLong.valueOf(1) -> UInt32Value.of(1), JavaLong.valueOf(10) -> UInt32Value.of(2))
        mapOfFoo = Map("a" -> SimpleMessage.getDefaultInstance)
        mapOfEnum = Map(1 -> Enum.BAZ)
          .map { case (k, v) => java.lang.Long.valueOf(k.toLong) -> v }
        mapOfTimestamp = Map(JavaBoolean.valueOf(true) -> ts)

        bs = TestMessage
          .newBuilder()
          .setEnum(enum)
          .addAllListOfEnum(enums.asJava)
          .addAllListOfWrappedInt(ints.map(UInt32Value.of).asJava)
          .addAllListOfFoo(foos.asJava)
          .addAllListOfString(strings.asJava)
          .addAllListOfTimestamp(timestamps.asJava)
          .addAllListOfInt(ints.map(Integer.valueOf).asJava)
          .putAllMapOfString(mapOfString.asJava)
          .putAllMapOfFoo(mapOfFoo.asJava)
          .putAllMapOfWrappedInt(mapOfWInt.asJava)
          .putAllMapOfEnum(mapOfEnum.asJava)
          .putAllMapOfTimestamp(mapOfTimestamp.asJava)
          .build()
          .toByteString

        message = getMessage(descriptor, bs)
        pmessage = toPMessage(message, 0L, tp)
        batch <- createBatchFromPMessages(table.schema, Seq(pmessage))
        _ <- writer.write(batch)
        _ <- Logging.info("Batch written")

        id = pmessage.envelope.id
        tableName = table.id

        gotEnum <- testExpectedField[String](
          client,
          tableName,
          "enum",
          id,
          enum.name()
        )
        gotListOfInt <- testExpectedField[String](
          client,
          tableName,
          "list_of_int",
          id,
          listToString(ints)
        )
        gotListOfWrappedInt <- testExpectedField[String](
          client,
          tableName,
          "list_of_wrapped_int",
          id,
          listToString(ints)
        )
        gotListOfMessages <- testExpectedField[String](
          client,
          tableName,
          "list_of_foo",
          id,
          s"""['{"int32":0,"uint32":0,"str":"","bool":false}','{"int32":42,"uint32":0,"str":"","bool":true}']"""
        )
        gotListOfString <- testExpectedField[String](
          client,
          tableName,
          "list_of_string",
          id,
          listToString(strings)
        )
        gotListOfEnum <- testExpectedField[String](
          client,
          tableName,
          "list_of_enum",
          id,
          listToString(enums.map(_.name()))
        )
        gotListOfTs <- testExpectedField[String](
          client,
          tableName,
          "list_of_timestamp",
          id,
          listToString(timestamps.map(ts => TimestampColumn.convertTimestamp(ts.asInstanceOf[Message])))
        )
        gotMapOfString <- testExpectedField[String](
          client,
          tableName,
          "map_of_string",
          id,
          """{"a":"b","c":"d"}"""
        )
        gotMapOfMessages <- testExpectedField[String](
          client,
          tableName,
          "map_of_foo",
          id,
          """{"a":{"int32":0,"uint32":0,"str":"","bool":false}}"""
        )
        gotMapOfWrappedInt <- testExpectedField[String](
          client,
          tableName,
          "map_of_wrapped_int",
          id,
          """{"1":1,"10":2}"""
        )
        gotMapOfEnum <- testExpectedField[String](
          client,
          tableName,
          "map_of_enum",
          id,
          """{"1":"BAZ"}"""
        )
        gotMapOfTs <- testExpectedField[String](
          client,
          tableName,
          "map_of_timestamp",
          id,
          // timestamp in json is written in default proto's json printer
          """{"true":"2020-10-22T11:56:18.745Z"}"""
        )
      } yield BoolAlgebra.all(
        tableCreated,
        gotEnum,
        gotListOfInt,
        gotListOfWrappedInt,
        gotListOfMessages,
        gotListOfString,
        gotListOfEnum,
        gotListOfTs,
        gotMapOfString,
        gotMapOfWrappedInt,
        gotMapOfMessages,
        gotMapOfEnum,
        gotMapOfTs
      )
    }
  }
}
