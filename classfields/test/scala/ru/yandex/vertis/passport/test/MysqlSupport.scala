package ru.yandex.vertis.passport.test

import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.passport.test.MySqlSupport.TestDatabasesContainer
import ru.yandex.vertis.passport.util.mysql.{DefaultDatabaseFactory, InstrumentedDatabase, MySqlConfig}
import ru.yandex.vertis.passport.util.tracing.TracingContext
import ru.yandex.vertis.tracing.Traced
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble
import scala.io.Source

/**
  * Provides test mysql db via TestContainers
  *
  * @author zvez
  */
trait MySqlSupport {

  def dbs: TestDatabasesContainer = MySqlSupport.container

  implicit val tracingContext: TracingContext = TracingContext("test", "test", Traced.empty)
}

object MySqlSupport {

  class TestDatabasesContainer extends DbInContainer {
    lazy val passport = buildDb("passport", "/sql/passport.sql")
    lazy val legacyUsers = buildDb("users", "/sql/legacy.users.sql")
    lazy val legacyOffice = buildDb("office7", "/sql/legacy.office7.sql")
    lazy val legacyAcl = buildDb("acl", "/sql/legacy.acl.sql")
    lazy val legacyLogs = buildDb("logs", "/sql/legacy.logs.sql")
    lazy val legacySalesAll = buildDb("all7", "/sql/legacy.sales.all7.sql")
    lazy val legacyOfficeLogs = buildDb("logs7", "/sql/legacy.logs7.sql")
  }

  lazy val container = new TestDatabasesContainer

}

//scalastyle:off multiple.string.literals
class DbInContainer {

  lazy val container = {
    val c = new MySQLContainer("mysql:5.7") {
      //override to remove strict sql-mode
      override def configure() = {
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
    sys.addShutdownHook {
      c.close()
    }
    c
  }

  val baseConfig = MySqlConfig(
    url = container.getJdbcUrl,
    username = container.getUsername,
    password = container.getPassword,
    minConnections = 1,
    maxConnections = 5,
    idleTimeout = 1.minute,
    executorThreads = 5,
    executorQueue = 1000,
    properties = Map(
      "zeroDateTimeBehavior" -> "convertToNull"
    )
  )

  lazy val rootConnect =
    DefaultDatabaseFactory.buildDatabase("test", baseConfig.copy(username = "root"), readonly = false)

  def buildDb(name: String, scriptFile: String): InstrumentedDatabase = {
    initSchema(name, scriptFile)
    DefaultDatabaseFactory.buildDatabase(
      name,
      baseConfig.copy(url = baseConfig.url.replaceAllLiterally("/test", "/" + name)),
      readonly = false
    )
  }

  private def initSchema(name: String, scriptFile: String): Unit = {
    val initScript =
      Source.fromInputStream(getClass.getResourceAsStream(scriptFile), "utf8").mkString

    val future = rootConnect.underlying.run {
      DBIO
        .seq(
          sqlu"CREATE DATABASE #$name",
          sqlu"GRANT ALL PRIVILEGES ON #$name.* TO 'test'@'%'",
          sqlu"FLUSH PRIVILEGES",
          sqlu"USE #$name",
          getSchemaAction(initScript)
        )
        .withPinnedSession
    }

    Await.ready(future, 30.seconds)
  }

  private def getSchemaAction(schemaScript: String): DBIO[Unit] = {
    val cleaned = schemaScript
      .split("\n")
      .filterNot(_.startsWith("--"))
      .filterNot(_.startsWith("#"))
      .filter(_.nonEmpty)
      .mkString
    val statements = cleaned.split(";")
    val effectiveStatements =
      statements
        .map(_.trim)
        .filterNot(_.startsWith("--"))
        .filterNot(_.startsWith("#"))
        .filter(_.nonEmpty)
        .map(statement => sqlu"#$statement;")
        .toSeq

    import scala.concurrent.ExecutionContext.Implicits.global
    DBIO.sequence(effectiveStatements).map(_ => ()).transactionally
  }
}
