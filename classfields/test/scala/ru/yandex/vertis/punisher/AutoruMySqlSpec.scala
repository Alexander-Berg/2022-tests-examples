package ru.yandex.vertis.punisher

import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.punisher.config.MysqlConfig
import ru.yandex.vertis.punisher.database.{DatabaseFactory, DatabaseReadWrite}
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._

object AutoruMySqlSpec extends BaseSpec {

  private lazy val container: MySQLContainer[Nothing] = {
    val c =
      new MySQLContainer("mysql:5.7") {

        override def configure(): Unit = {
          optionallyMapResourceParameterAsVolume("TC_MY_CNF", "/etc/mysql/conf.d", "mysql-default-conf")
          addExposedPort(3306)
          addEnv("MYSQL_DATABASE", "test")
          addEnv("MYSQL_USER", "test")
          addEnv("MYSQL_PASSWORD", "test")
          addEnv("MYSQL_ROOT_PASSWORD", "test")
          setCommand(
            "mysqld",
            "--character-set-server=utf8mb4",
            "--sql-mode=NO_ENGINE_SUBSTITUTION",
            "--default-time-zone=+3:00"
          )
          setStartupAttempts(3)
        }
      }
    c.start()
    c
  }

  lazy val dbConfig: MysqlConfig =
    MysqlConfig(
      driver = "com.mysql.jdbc.Driver",
      readUrl = container.getJdbcUrl,
      writeUrl = Some(container.getJdbcUrl),
      username = container.getUsername,
      password = container.getPassword,
      idleTimeout = 1.minute,
      maxConnections = 2,
      executorNumThreads = 2,
      executorQueueSize = 1000,
      writeValidationQuery = None,
      readValidationQuery = None
    )

  val db: DatabaseReadWrite = DatabaseFactory.getDatabase(dbConfig)

  val dbRoot: DatabaseReadWrite = DatabaseFactory.getDatabase(dbConfig.copy(username = "root"))

  log.info("Initialization of the schema...")
  dbRoot.writeDb.run(DBIO.sequence(getSchemaActions).transactionally).futureValue
  log.info("Initialization of the schema complete")

  private def getSchemaActions = {
    val initScript =
      s"GRANT ALL PRIVILEGES ON *.* TO '${container.getUsername}'@'%';\n" +
        "FLUSH PRIVILEGES;\n"

    (initScript + resourceString("/sql/autoru.sql"))
      .split("\n")
      .map(_.trim)
      .filterNot(_.startsWith("--"))
      .filterNot(_.startsWith("#"))
      .filter(_.nonEmpty)
      .mkString
      .split(";")
      .toSeq
      .map(statement => sqlu"#$statement;")
  }
}
