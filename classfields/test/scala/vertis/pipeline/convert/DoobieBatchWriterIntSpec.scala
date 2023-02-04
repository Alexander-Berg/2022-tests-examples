package vertis.pipeline.convert

import com.google.protobuf.DynamicMessage
import com.google.protobuf.timestamp.Timestamp
import common.zio.clients.clickhouse.jdbc.ClickhouseJdbcClient.ClickhouseJdbcClient
import common.zio.doobie.QueryHelper._
import common.zio.doobie.logging.LogDoobieQueries.slf4jLogHandler
import common.zio.logging.Logging
import doobie.implicits._
import doobie.{ConnectionIO, Read}
import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.proto.util.RandomProtobufGenerator
import ru.yandex.vertis.proto.util.convert.ProtoConversions.RichInstant
import scalapb.UnknownFieldSet
import vertis.broker.pipeline.ch.sink.converter.PreparedMessage
import vertis.broker.pipeline.ch.sink.converter.test.SimpleMessage
import vertis.broker.pipeline.ch.sink.queries.ClickhouseQueries
import vertis.clickhouse.model.ChTableId
import vertis.clickhouse.model.ChTableId._
import vertis.stream.model.TopicPartition
import vertis.zio.BaseEnv
import zio.UIO
import zio.test.Assertion._
import zio.test.{ZSpec, _}

import java.util.UUID

/** @author kusaeva
  */
object DoobieBatchWriterIntSpec extends ClickhouseTestSpec with ProducerProvider {
  private val tp = TopicPartition(topic = "myt//test/topic", partition = 0)

  implicit val TimestampOptRead: Read[Option[Timestamp]] =
    Read[Option[String]]
      .map(_.map(java.sql.Timestamp.valueOf))
      .map(_.map(_.toInstant.toProtoTimestamp))

  implicit val UnknownFieldSetRead: Read[UnknownFieldSet] =
    new Read[UnknownFieldSet](Nil, (_, _) => UnknownFieldSet.empty)

  override protected def chSpec: ZSpec[BaseEnv with ClickhouseJdbcClient, Any] =
    suite("DoobieBatchWriter")(writeSimpleBatch)

  private val writeSimpleBatch = testM("write batch") {
    chTestM(SimpleMessage.javaDescriptor).use { t =>
      import t._
      (UIO(getTable).tap(schemaEvolution.update) *> writeBatch(t)).as(assertTrue(true))
    }
  }

  private def select[T: Read](table: ChTableId, columns: Seq[String]): ConnectionIO[List[T]] = {
    val filtered = columns.filterNot(_.startsWith("_"))
    sql"""|SELECT ${join(",")(filtered.map(backticks))} FROM ${table.fr0}
          |""".stripMargin.query[T].to[List]
  }

  private def writeBatch(test: ChTest) = {
    val table = test.getTable
    val schema = test.getSchema
    val columns = converter.toColumns(schema)
    val gen = RandomProtobufGenerator
      .genFor(DynamicMessage.getDefaultInstance(test.descriptor), maxRep = 1)
      .next(1)
      .zipWithIndex
    val messages = gen
      .map(dm => SimpleMessage.parseFrom(dm._1.toByteArray))
    val inputs = gen.map { case (m, o) =>
      PreparedMessage(msg = m, id = UUID.randomUUID().toString, topicPartition = tp, offset = o.toLong)
    }
    val rows = inputs.map(converter.toRow(_, schema)).toSeq
    val fixed = converter.fixEmptyColumns(rows, schema)
    val insertQuery = ClickhouseQueries.insertBatch(table.id, columns, fixed)

    for {
      _ <- Logging.info(s"messages: $messages")
      _ <- test.client
        .executeQuery(insertQuery)
        .unit
      messagesWritten <- assertM(test.client.executeQuery(select[SimpleMessage](table.id, columns)))(equalTo(messages))
    } yield messagesWritten
  }

}
