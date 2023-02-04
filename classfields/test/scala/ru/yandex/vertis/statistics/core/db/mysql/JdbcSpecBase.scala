package ru.yandex.vertis.statistics.db.mysql

import java.util.UUID
import com.typesafe.scalalogging.LazyLogging
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.scalatest.Suite
import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.statistics.db.mysql.JdbcSpecBase.effectiveConfig
import ru.yandex.vertis.statistics.core.db.DatabaseConfig
import ru.yandex.vertis.util.concurrent.Threads
import slick.jdbc.MySQLProfile.api._
import slick.dbio.DBIOAction
import slick.jdbc.JdbcBackend
import slick.util.AsyncExecutor

import java.util.Collections.singletonMap
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.io.Source
import scala.util.{Failure, Success, Try}

trait JdbcSpecBase extends LazyLogging {
  self: Suite =>

  // if set to `true` then generate a lot of debug logs
  private def profileSQL = false

  private def defaultProps: Map[String, String] =
    if (profileSQL) {
      DatabaseConfig.DefaultProps + ("TC_DAEMON" -> "true") + ("profileSQL" -> "true")
    } else {
      DatabaseConfig.DefaultProps + ("TC_DAEMON" -> "true")
    }

  def databaseConfig(name: String): DatabaseConfig =
    DatabaseConfig(
      url = "",
      username = JdbcSpecBase.DefaultUser,
      password = "",
      driverClassName = "",
      poolSize = 10,
      queueSize = 1000,
      minIdleConnections = 0,
      idleTimeout = None,
      properties = defaultProps,
      registerMBeans = false,
      name = name
    )

  final def prepareDatabase(schemaPath: String, name: String): Database = {
    JdbcSpecBase.Container
      .map(doPrepareDatabase(_, schemaPath, name)) match {
      case Success(db) =>
        db
      case Failure(e) =>
        cancel("The database is not available", e)
    }
  }

  private def doPrepareDatabase(c: MySQLContainer[_], schemaPath: String, name: String): Database = {
    val databaseName = nextDatabaseName(name)
    val baseConfig = databaseConfig(name)
    logger.info(s"Create database $databaseName")

    val rootDb = createDatabase(effectiveConfig(c, baseConfig, None))
    val start = System.currentTimeMillis()

    val actions = Seq(
      sqlu"CREATE DATABASE #$databaseName",
      sqlu"GRANT ALL PRIVILEGES ON #$databaseName.* TO '#${baseConfig.username}'@'%'",
      sqlu"FLUSH PRIVILEGES",
      sqlu"USE #$databaseName",
      getSchemaAction(schemaPath)
    )

    val wholeActions = DBIOAction.sequence(actions).transactionally
    Await.result(rootDb.run(wholeActions), 60.seconds)
    logger.info(s"Creating db took ${System.currentTimeMillis() - start} ms")

    rootDb.close()

    createDatabase(effectiveConfig(c, baseConfig, Some(databaseName)))
  }

  private def getSchemaAction(schemaPath: String) = {
    val schemaScript = Source.fromInputStream(getClass.getResourceAsStream(schemaPath)).mkString
    val statements = schemaScript.split(";")
    val effectiveStatements =
      statements.map(_.trim).filterNot(_.startsWith("--")).filter(_.nonEmpty).map(s => s"$s;")
    DBIOAction
      .sequence(
        effectiveStatements.toSeq.map { s =>
          sqlu"#$s"
        }
      )
      .map(_ => ())(Threads.SameThreadEc)
      .transactionally
  }

  private def createDatabase(config: DatabaseConfig) = {
    val ds = new HikariDataSource(convertToHikariConfig(config))
    val executor = AsyncExecutor(
      name = config.name,
      minThreads = config.poolSize,
      maxThreads = config.poolSize,
      queueSize = config.queueSize,
      maxConnections = config.poolSize,
      registerMbeans = config.registerMBeans
    )
    // force initialize lazy val
    executor.executionContext
    val db = JdbcBackend.Database.forDataSource(ds = ds, maxConnections = None, executor = executor)
    sys.addShutdownHook {
      db.close()
    }
    db
  }

  private def convertToHikariConfig(dbConfig: DatabaseConfig): HikariConfig = {
    val config = new HikariConfig()
    config.setPoolName(dbConfig.name)
    config.setDriverClassName(dbConfig.driverClassName)
    config.setJdbcUrl(dbConfig.url)
    config.setUsername(dbConfig.username)
    config.setPassword(dbConfig.password)
    dbConfig.idleTimeout.map(_.toMillis).foreach(config.setIdleTimeout)
    config.setMinimumIdle(dbConfig.minIdleConnections)
    config.setMaximumPoolSize(dbConfig.poolSize)
    dbConfig.properties.foreach {
      case (name, value) ⇒ config.addDataSourceProperty(name, value)
    }
    config.setRegisterMbeans(dbConfig.registerMBeans)
    config
  }

  def nextDatabaseName(name: String): String = {
    val id = UUID.randomUUID().toString.take(5)
    s"${name}_$id"
  }

}

object JdbcSpecBase {

  private def effectiveConfig(
    c: MySQLContainer[_],
    source: DatabaseConfig,
    databaseName: Option[String]
  ): DatabaseConfig = {
    def getUrl(databaseName: Option[String]): String =
      databaseName match {
        case Some(dbName) =>
          c.getJdbcUrl.replace(c.getDatabaseName, dbName)
        case None =>
          c.getJdbcUrl
      }
    source.copy(
      url = getUrl(databaseName),
      username = if (databaseName.isEmpty) RootUser else source.username,
      password = c.getPassword,
      driverClassName = c.getDriverClassName
    )
  }

  private val Port: Int = 3306

  private val RootUser: String = "root"

  private val DefaultUser: String = "test"

  private val DefaultPassword: String = "test"

  private lazy val Container = {
    Try {
      val c = new MySQLContainer("mysql:5.7")
      c.addExposedPort(Port)
      c.addEnv("MYSQL_DATABASE", "test")
      c.addEnv("MYSQL_USER", DefaultUser)
      c.addEnv("MYSQL_PASSWORD", DefaultPassword)
      c.addEnv("MYSQL_ROOT_PASSWORD", DefaultPassword)
      c.withTmpFs(singletonMap("/tmpfs", "rw"))
      c.setCommand(
        "mysqld",
        "--character-set-server=utf8",
        "--collation-server=utf8_general_ci",
        "--sql-mode=NO_ENGINE_SUBSTITUTION",
        "--default-time-zone=+3:00",
        "--max_allowed_packet=128M",
        "--max_connections=256",
        "--explicit_defaults_for_timestamp=1",
        "--datadir=/tmpfs"
      )
      c.setStartupAttempts(3)
      c.start()
      sys.addShutdownHook {
        c.close()
      }
      c
    }
  }

}
