package common.zio.clients.clickhouse.testkit

import common.db.config.DbConfig
import common.zio.clients.clickhouse.jdbc.ClickhouseTransactor
import common.zio.ops.tracing.Tracing
import common.zio.ops.tracing.Tracing.Tracing
import doobie.util.transactor.Transactor
import org.testcontainers.containers.ClickHouseContainer
import zio.blocking.Blocking
import zio.{Has, Task, ZLayer}

object TestTransactor {

  val live: ZLayer[Blocking with Tracing, Throwable, Has[Transactor[Task]]] = {
    val transactorLayer =
      ZLayer.fromServiceManaged[ClickHouseContainer, Blocking with Tracing, Throwable, Transactor[Task]] { container =>
        ClickhouseTransactor.make(
          "clickhouse",
          DbConfig(
            driver = "ru.yandex.clickhouse.ClickHouseDriver",
            url = container.getJdbcUrl,
            user = container.getUsername,
            password = container.getPassword,
            properties = Map("nullAsDefault" -> "2")
          )
        )
      }

    (Blocking.any ++ Tracing.any ++ TestClickhouse.live) >>> transactorLayer
  }

}
