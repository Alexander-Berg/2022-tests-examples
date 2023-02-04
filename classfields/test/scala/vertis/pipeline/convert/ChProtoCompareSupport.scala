package vertis.pipeline.convert

import com.google.protobuf.Message
import common.zio.clients.clickhouse.jdbc.{ClickhouseError, ClickhouseJdbcClient}
import common.zio.logging.Logging
import doobie.implicits._
import doobie.util.fragment.Fragment.const0
import doobie.{ConnectionIO, Read}
import ru.yandex.vertis.proto.util.ProtoTypesUtil.anyTimestampToInstantNanos
import ru.yandex.vertis.proto.util.convert.ProtoSupport
import vertis.broker.pipeline.ch.sink.queries.QueriesSupport
import vertis.clickhouse.model.ChTableId
import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.TestResult

import java.util.TimeZone

/** @author kusaeva
 */
trait ChProtoCompareSupport extends ProtoSupport with QueriesSupport {

  protected def testExpectedField[T: Read](
      client: ClickhouseJdbcClient.Service,
      tableId: ChTableId,
      columnName: String,
      id: String,
      expected: T): ZIO[Logging.Logging, ClickhouseError, TestResult] = {
    client
      .executeQuery(
        selectQuery[T](tableId, columnName, id)
      )
      .tap { res =>
        Logging.info(s"Have $expected, read $res")
      }
      .map { res =>
        zio.test.assert(res)(equalTo(expected))
      }
  }

  protected def testProtoField[T: Read](
      client: ClickhouseJdbcClient.Service,
      tableId: ChTableId,
      columnName: String,
      id: String,
      msg: Message,
      f: AnyRef => AnyRef = identity): ZIO[Logging.Logging, ClickhouseError, TestResult] = {
    val expected = f(msg.getField(msg.getDescriptorForType.findFieldByName(columnName)))
    testExpectedField(client, tableId, columnName, id, expected.asInstanceOf[T])
  }

  protected def testTsField(
      client: ClickhouseJdbcClient.Service,
      tableId: ChTableId,
      columnName: String,
      id: String,
      msg: Message): ZIO[Logging.Logging, ClickhouseError, TestResult] =
    for {
      res <- client.executeQuery(
        selectQuery[String](tableId, columnName, id)
      )
      ts = java.sql.Timestamp.valueOf(res)
      v = msg.getField(msg.getDescriptorForType.findFieldByName(columnName)).asInstanceOf[Message]
      n = anyTimestampToInstantNanos(v)
      _ <- Logging.info(s"Have $n, read $res, parse $ts, ${TimeZone.getDefault.getDisplayName}")
    } yield zio.test.assert(ts.toInstant)(equalTo(n))

  private def selectQuery[T: Read](tableId: ChTableId, column: String, id: String): ConnectionIO[T] =
    sql"""|SELECT ${const0(column)}
          |FROM ${tableId.fr0}
          |WHERE _id = $id
          |""".stripMargin
      .query[T]
      .unique
}
