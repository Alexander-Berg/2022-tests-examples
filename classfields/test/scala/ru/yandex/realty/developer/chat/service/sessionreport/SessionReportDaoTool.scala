package ru.yandex.realty.developer.chat.service.sessionreport

import ru.yandex.vertis.util.config.RichConfig
import ru.yandex.realty.application.ng.db.{
  DefaultSlickAsyncExecutorProvider,
  DefaultSlickDatabaseProvider,
  HikariDataSourceProvider,
  MasterSlaveDatabaseConfig,
  SlickMasterSlaveDatabaseProvider
}
import ru.yandex.realty.application.ng.DefaultConfigProvider
import ru.yandex.realty.ops.DaoOperationalComponents
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.tracing.Traced

import java.time.{Duration, Instant}
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt

object SessionReportDaoTool
  extends App
  with SlickMasterSlaveDatabaseProvider
  with DefaultSlickDatabaseProvider
  with HikariDataSourceProvider
  with DefaultSlickAsyncExecutorProvider
  with TestOperationalComponents
  with DaoOperationalComponents {

  implicit val ec = ExecutionContext.global

  val database = createMasterSlave2(
    MasterSlaveDatabaseConfig(
      DefaultConfigProvider
        .provideForName("developer-chat-core")
        .config(
          "mysql"
        )
    )
  )

  val dao = new MysqlSessionReportDao(database, daoMetrics)

  println(
    Await.result(
      dao.insert(
        SessionReportRecord(
          id = None,
          "my-room",
          sessionStartMessageId = "",
          sessionResetTime = None,
          developerId = 1,
          billingAgencyId = 333,
          billingClientId = 1000,
          siteId = 2,
          Instant.now(),
          "+7",
          None,
          None,
          None,
          0,
          None,
          None
        )
      )(Traced.empty),
      5.seconds
    )
  )
  println(
    Await.result(
      dao.insert(
        SessionReportRecord(
          id = None,
          "my-room",
          sessionStartMessageId = "",
          sessionResetTime = None,
          developerId = 1,
          billingAgencyId = 333,
          billingClientId = 1000,
          siteId = 2,
          Instant.now(),
          "+7",
          None,
          None,
          None,
          0,
          None,
          None
        )
      )(Traced.empty),
      5.seconds
    )
  )

  println(
    Await.result(
      dao.count(
        billingAgencyId = None,
        billingClientId = 1000,
        siteIdOpt = Some(2),
        from = Instant.now().minus(Duration.ofHours(1)),
        to = Instant.now()
      )(Traced.empty),
      5.seconds
    )
  )

  println(
    Await.result(
      dao.count(
        billingAgencyId = Some(333),
        billingClientId = 1000,
        siteIdOpt = None,
        from = Instant.now().minus(Duration.ofHours(1)),
        to = Instant.now()
      )(Traced.empty),
      5.seconds
    )
  )

  Await.result(
    dao.setEndTimestamp("my-room", None, Instant.now())(Traced.empty),
    5.seconds
  )

}
