package vertis.ydb.test

import com.yandex.ydb.table.description.TableDescription
import com.yandex.ydb.table.query.Params
import com.yandex.ydb.table.values.Type
import ru.yandex.vertis.ydb.Ydb
import vertis.ydb.convert.YdbParam
import vertis.ydb.convert.instances._
import vertis.ydb.convert.YdbRsReads.YdbRsReader
import vertis.zio.test.ZioSpecBase
import zio.RIO
import zio.clock.Clock

/** Replaces a value into a prepared table and reads it back
  * @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait YdbConsistencyTest extends YdbTest {
  this: org.scalatest.Suite with ZioSpecBase =>

  protected def oneColumnTable(tableName: String, columnName: String, columnType: Type) =
    ydbWrapper.createTable(
      tableName,
      TableDescription
        .newBuilder()
        .addNullableColumn(columnName, columnType)
        .setPrimaryKeys(columnName)
        .build()
    )

  protected def checkConsistent[T: YdbParam](tableName: String, column: String)(value: T): RIO[Ydb with Clock, Unit] = {
    implicit val reader: YdbRsReader[T] = columnReader[T](column)
    for {
      _ <- Ydb.runTx(
        ydbWrapper.execute(
          s"""
             |${"x".declare[T]}
             |REPLACE INTO $tableName ($column) VALUES($$x)""".stripMargin,
          Params.of("x".param, value.asValue)
        )
      )
      parsed <- Ydb
        .runTx(
          ydbWrapper
            .execute(
              s"""
                 |${"x".declare[T]}
                 |SELECT $column FROM $tableName WHERE $column = $$x LIMIT 1""".stripMargin,
              Params.of("x".param, value.asValue)
            )
        )
        .map(rs => consumeResultSet[T](rs.resultSet))
        .map(_.headOption)
      _ <- check("value is the same")(parsed should contain(value))
    } yield ()
  }
}
