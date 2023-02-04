package common.zio.clients.clickhouse.jdbc

import common.zio.clients.clickhouse.testkit.TestTransactor
import common.zio.ops.prometheus.Prometheus
import common.zio.ops.tracing.Tracing
import common.zio.ops.tracing.testkit.TestTracing
import doobie.implicits._
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.{DefaultRunnableSpec, _}

object ClickhouseJdbcSpec extends DefaultRunnableSpec {

  case class Column(name: String, `type`: String)

  override def spec = {
    suite("ClickhouseJdbcSpec")(
      testM("create table") {
        for {
          _ <- ClickhouseJdbcClient.executeQuery(initQuery)
          fields <- ClickhouseJdbcClient.executeQuery(
            sql"""DESCRIBE TABLE test_db.test_table""".stripMargin
              .query[Column]
              .to[Seq]
          )
        } yield {
          assert(fields)(equalTo(List(Column("field1", "String"), Column("field2", "UInt32"))))
        }
      }.provideCustomLayerShared {
        (Blocking.live ++ TestTracing.noOp >>> TestTransactor.live >>> ClickhouseJdbcClientLive.live).orDie
      }
    ) @@ sequential
  }

  private val initQuery =
    for {
      _ <- sql"""CREATE DATABASE IF NOT EXISTS test_db""".stripMargin.update.run
      _ <-
        sql"CREATE TABLE IF NOT EXISTS test_db.test_table ON CLUSTER '{cluster}' (field1 String, field2 UInt32) ENGINE = Memory;".update.run
    } yield ()
}
