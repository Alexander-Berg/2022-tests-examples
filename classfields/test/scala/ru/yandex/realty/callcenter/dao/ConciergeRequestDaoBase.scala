package ru.yandex.realty.callcenter.dao

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatest.{Matchers, Suite}
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, SlickOperations, TestContainerDatasource}
import ru.yandex.realty.ops.DaoMetrics

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait ConciergeRequestDaoBase
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
    databaseName = "realty_callcenter",
    databaseUser = "realty_callcenter",
    databasePassword = "realty_callcenter_pass"
  )

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  lazy val daoMetrics: DaoMetrics = DaoMetrics.stub()
  lazy val database2: MasterSlaveJdbcDatabase2.DbWithProperties =
    MasterSlaveJdbcDatabase2.DbWithProperties(database, containerConfig.databaseName)
  lazy val masterSlaveDb2: MasterSlaveJdbcDatabase2 = MasterSlaveJdbcDatabase2(database2, database2)

  lazy val conciergeRequestDao = new MysqlConciergeRequestDao(masterSlaveDb2, daoMetrics)

}
