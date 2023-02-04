package ru.yandex.realty.rent.chat

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2
import ru.yandex.realty.application.ng.ExecutionContextProvider
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, SlickOperations, TestContainerDatasource}
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig
import ru.yandex.realty.tracing.Traced

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait BaseDbSpec
  extends MySQLTestContainer.V8_0
  with TestContainerDatasource
  with SlickOperations
  with ScalaFutures
  with ExecutionContextProvider {

  override lazy val containerConfig: ContainerConfig = ContainerConfig(
    databasePort = 3306,
    databaseName = "rent_chat",
    databaseUser = "rent_chat",
    databasePassword = "rent_chat"
  )

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(30, Seconds), interval = Span(20, Millis))

  implicit override def ec: ExecutionContextExecutor = ExecutionContext.global

  lazy val masterSlaveDatabase: MasterSlaveJdbcDatabase2 =
    MasterSlaveJdbcDatabase2(databaseWithProperties, databaseWithProperties)

  implicit def traced: Traced = Traced.empty

}

trait CleanSchemaBeforeAll extends BeforeAndAfterAll {
  this: BaseDbSpec with Suite =>

  override protected def beforeAll(): Unit = {
    database.run(script"sql/schema.sql").futureValue
  }
}
