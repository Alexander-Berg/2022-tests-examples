package ru.yandex.realty.cadastr.dao

import org.scalatest.{BeforeAndAfter, Matchers, Suite}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.time.{Millis, Minutes, Span}
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2
import ru.yandex.realty.cadastr.dao.actions.impl.{ExcerptDbActionsImpl, ReportDbActionsImpl, RequestDbActionsImpl}
import ru.yandex.realty.cadastr.dao.impl.{ExcerptDaoImpl, ReportDaoImpl, RequestDaoImpl}
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, SlickOperations, TestContainerDatasource}
import ru.yandex.realty.ops.DaoMetrics

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait CadastrSpecBase
  extends MySQLTestContainer.V8_0
  with TestContainerDatasource
  with SlickOperations
  with ScalaFutures
  with Matchers
  with Checkers
  with PropertyChecks {
  this: Suite =>

  override lazy val containerConfig: ContainerConfig = ContainerConfig(
    databasePort = 3306,
    databaseName = "realty_cadastr",
    databaseUser = "realty_cadastr",
    databasePassword = "realty_cadastr"
  )

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  lazy val daoMetrics: DaoMetrics = DaoMetrics.stub()
  lazy val database2: MasterSlaveJdbcDatabase2.DbWithProperties =
    MasterSlaveJdbcDatabase2.DbWithProperties(database, containerConfig.databaseName)
  lazy val masterSlaveDb2: MasterSlaveJdbcDatabase2 = MasterSlaveJdbcDatabase2(database2, database2)
  lazy val reportDbActions = new ReportDbActionsImpl()
  lazy val requestDbActions = new RequestDbActionsImpl()
  lazy val excerptDbActions = new ExcerptDbActionsImpl()
  lazy val excerptDao = new ExcerptDaoImpl(excerptDbActions, masterSlaveDb2.v1)
  lazy val requestDao = new RequestDaoImpl(requestDbActions, masterSlaveDb2.v1)
  lazy val reportDao = new ReportDaoImpl(reportDbActions, requestDbActions, excerptDbActions, masterSlaveDb2.v1)
}

trait CleanSchemaBeforeEach extends BeforeAndAfter {
  this: CadastrSpecBase with Suite =>

  before {
    database.run(script"sql/schema.sql").futureValue
  }

  after {
    database.run(script"sql/deleteTables.sql").futureValue
  }
}
