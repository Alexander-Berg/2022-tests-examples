package vertis.pushnoy.dao.postgres

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.testcontainers.containers.PostgreSQLContainer
import scalikejdbc.{ConnectionPool, ConnectionPoolSettings}
import vertis.core.utils.NoWarnFilters

import scala.annotation.nowarn

/** @author kusaeva
  */
trait PostgreTest extends BeforeAndAfterAll {
  this: Suite =>

  @nowarn(NoWarnFilters.WFlagDeadCode)
  private val pgContainer: PostgreSQLContainer[_] =
    new PostgreSQLContainer("postgres:12.1")
      .withInitScript("pg_init.sql")

  private val settings = ConnectionPoolSettings(
    initialSize = 10,
    maxSize = 10,
    connectionTimeoutMillis = 3000L,
    driverName = "org.postgresql.Driver"
  )

  private def initPg(): Unit = {
    pgContainer.start()
    ConnectionPool.singleton(pgContainer.getJdbcUrl, pgContainer.getUsername, pgContainer.getPassword, settings)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    initPg()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    pgContainer.close()
  }
}
