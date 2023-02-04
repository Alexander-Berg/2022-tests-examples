package ru.yandex.vertis.telepony.util.ydb

import com.yandex.ydb.table.description.TableDescription
import com.yandex.ydb.table.query.Params
import com.yandex.ydb.table.values.{PrimitiveType, Value}
import org.joda.time.DateTime
import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.ydb.YdbResult
import vertis.zio.test.ZioSpecBase
import vertis.ydb.test.YdbTest
import zio.ZIO
import zio.clock.Clock

import scala.jdk.CollectionConverters._

class YdbResultSpec extends ZioSpecBase with YdbTest with Suite with SpecBase with YdbSupport {

  import ru.yandex.vertis.ydb.Ydb.ops._

  override def beforeAll(): Unit = {
    super.beforeAll()
    runSync {
      val description = TableDescription
        .newBuilder()
        .addNullableColumn("int_field", PrimitiveType.int32)
        .addNullableColumn("string_field", PrimitiveType.utf8)
        .addNullableColumn("time_field", PrimitiveType.timestamp)
        .addNullableColumn("boolean_field", PrimitiveType.bool)
        .setPrimaryKeys("int_field", "string_field")
        .build()

      ydbWrapper.createTable("test_ydb_result", description)
    }.get
  }

  "YdbResult" should {
    "return collection of rows" in ioTest {
      for {
        _ <- upsert(Some(1), Some("1"), Some(new DateTime(2020, 1, 1, 10, 0)), Some(true))
        _ <- upsert(None, None, None, None)
        _ <- upsert(Some(10), None, None, Some(false))
        res <- select
        _ <- check {
          val ls = res.toList.map { r =>
            (
              r.readOption[Int]("int_field"),
              r.readOption[String]("string_field"),
              r.readOption[DateTime]("time_field"),
              r.readOption[Boolean]("boolean_field")
            )
          }
          ls should contain((Some(1), Some("1"), Some(new DateTime(2020, 1, 1, 10, 0)), Some(true)))
          ls should contain((None, None, None, None))
          ls should contain((Some(10), None, None, Some(false)))
        }
      } yield ()
    }
  }

  def select: ZIO[Clock, Any, YdbResult] = {
    val tx = ydbWrapper
      .execute(selectQuery)
    ydbWrapper.runTx(tx).mapError(_.squash)
  }

  def upsert(
      intValue: Option[Int],
      stringValue: Option[String],
      timeValue: Option[DateTime],
      booleanValue: Option[Boolean]): ZIO[Clock, Any, Unit] = {

    val params: Map[String, Value[_]] = Map(
      "$int_value" -> intValue,
      "$string_value" -> stringValue,
      "$time_value" -> timeValue,
      "$boolean_value" -> booleanValue
    )

    val tx = ydbWrapper
      .execute(upsertQuery, Params.copyOf(params.asJava))
      .ignoreResult
      .withAutoCommit

    ydbWrapper.runTx(tx).mapError(_.squash)
  }

  private val upsertQuery =
    ydbWrapper.prepareStatement("""
      DECLARE $int_value AS Optional<Int32>;
      DECLARE $string_value AS Optional<Utf8>;
      DECLARE $time_value AS Optional<Timestamp>;
      DECLARE $boolean_value AS Optional<Bool>;

      UPSERT INTO test_ydb_result(int_field, string_field, time_field, boolean_field)
      VALUES ($int_value, $string_value, $time_value, $boolean_value);
    """)

  private val selectQuery = ydbWrapper.prepareStatement("""
      SELECT * FROM test_ydb_result;
    """)
}
