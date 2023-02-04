package ru.yandex.vertis.chat.service.impl.jdbc

import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.chat.Domain
import ru.yandex.vertis.chat.dao.jdbc._
import ru.yandex.vertis.chat.util.logging.Logging
import slick.jdbc.JdbcBackend.DatabaseDef

import scala.concurrent.duration._

/**
  * Created by andrey on 11/8/17.
  */
object TestDockerConfigBuilder extends Logging {
  val dockerImageName = "percona:5.7.15"

  class TestMySQLContainer extends MySQLContainer[TestMySQLContainer](dockerImageName)

  val container = new TestMySQLContainer()

  private def containerUrl = container.getJdbcUrl + "?useSSL=false"

  private def createAndStartContainer(containerName: String, schemaFilename: String): Unit = {
    container
      .withCreateContainerCmdModifier { cmdModifier =>
        cmdModifier
          .withCmd("--datadir=/tmpfs")
          .withCmd("--character-set-server=utf8mb4")
          .withCmd("--sql-mode=NO_ENGINE_SUBSTITUTION")
      }
      .withDatabaseName(containerName)
      .withUsername("vos")
      .withPassword("sqlsql")
      .withReuse(true)
      .withInitScript(schemaFilename)
      .start()
  }

  def createDatabase(dbName: String, schemaFileName: String, domain: Domain): DatabaseDef = {
    createAndStartContainer(dbName, schemaFileName)

    val jdbcUrl = containerUrl

    val config = DatabaseConfig(
      masterUrl = jdbcUrl,
      slaveUrl = jdbcUrl,
      username = "root",
      password = "sqlsql",
      detailed = DetailedConfig("com.mysql.jdbc.Driver", PoolConfig(2, 100, 0, Some(10.seconds)))
    )

    DatabaseFactory.create(config, domain, DbType.Master)
  }
}
