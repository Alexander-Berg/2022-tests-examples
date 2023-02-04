package ru.yandex.realty.notifier.db

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, Suite}
import ru.yandex.realty.application.ng.ExecutionContextProvider
import ru.yandex.realty.application.ng.db.{
  MasterSlaveDatabaseConfig,
  MasterSlaveDatabaseProvider,
  MasterSlaveJdbcDatabase,
  MasterSlaveJdbcDatabase2
}
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, SlickOperations, TestContainerDatasource}
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.notifier.application.{DefaultNotifierDaoProvider, DefaultNotifierDbActionsProvider}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait BaseDbSpec
  extends MySQLTestContainer
  with TestContainerDatasource
  with SlickOperations
  with ScalaFutures
  with Matchers
  with Checkers
  with PropertyChecks
  with Logging
  with MasterSlaveDatabaseProvider
  with DefaultNotifierDbActionsProvider
  with DefaultNotifierDaoProvider
  with ExecutionContextProvider {
  this: Suite =>

  override lazy val containerConfig: ContainerConfig = ContainerConfig(
    databasePort = 3306,
    databaseName = "realty_rent",
    databaseUser = "realty_rent",
    databasePassword = "realty_rent"
  )

  override def createMasterSlave(c: MasterSlaveDatabaseConfig): MasterSlaveJdbcDatabase =
    MasterSlaveJdbcDatabase(database, database)

  override def createMasterSlave2(c: MasterSlaveDatabaseConfig): MasterSlaveJdbcDatabase2 = ???

  override def dbConfig: MasterSlaveDatabaseConfig = null

  implicit val ec: ExecutionContextExecutor = ExecutionContext.global

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))
}

trait CleanSchemaBeforeAll extends BeforeAndAfterAll {
  this: BaseDbSpec with Suite =>

  override protected def beforeAll(): Unit = {
    database.run(script"sql/schema.sql").futureValue
  }
}
