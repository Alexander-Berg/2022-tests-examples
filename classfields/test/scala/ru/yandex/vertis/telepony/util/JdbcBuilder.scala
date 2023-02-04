package ru.yandex.vertis.telepony.util

import ru.yandex.vertis.telepony.dao.jdbc.JdbcSpecUtils
import ru.yandex.vertis.telepony.settings.{DualMysqlConfig, MySqlConfig}
import ru.yandex.vertis.telepony.util.db.PlainDualDatabase

import scala.concurrent.duration._

/**
  * @author evans
  */
trait JdbcBuilder {

  private lazy val defaultConfig = MySqlConfig(
    url = MySqlContainer.getJdbcUrl,
    username = MySqlContainer.User,
    password = MySqlContainer.Password,
    minConnections = JdbcBuilder.DefaultTestMaxConnections,
    maxConnections = JdbcBuilder.DefaultTestMaxConnections,
    idleTimeout = 90.seconds,
    executorThreads = JdbcBuilder.DefaultTestMaxConnections,
    executorQueue = 100
  )

  private lazy val defaultDualConfig = DualMysqlConfig(defaultConfig, defaultConfig)

  def schemaScript: String

  def dropDatabaseAfterExecution: Boolean = true

  def databaseName: String = s"telepony_unit_test_${getClass.getSimpleName}"

  def createSimpleDualDatabase(): PlainDualDatabase = JdbcSpecUtils.createDualDb(
    databaseName,
    defaultDualConfig,
    schemaScript,
    dropDatabaseAfterExecution
  )
}

object JdbcBuilder {
  val DefaultTestMaxConnections = 2
}
