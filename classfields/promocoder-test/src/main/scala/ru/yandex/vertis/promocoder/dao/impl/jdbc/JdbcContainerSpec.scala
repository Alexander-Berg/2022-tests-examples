package ru.yandex.vertis.promocoder.dao.impl.jdbc

import java.util.{Collections, UUID}

import com.github.dockerjava.api.model.AccessMode
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import javax.sql.DataSource
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory
import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.promocoder.dao.impl.jdbc.JdbcContainerSpec.AdminDb
import slick.dbio.DBIOAction
import slick.jdbc.JdbcBackend
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api.{actionBasedSQLInterpolation, jdbcActionExtensionMethods}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.io.Source

/** Provides database base methods for tests runs
  *
  * @author alex-kovalenko
  */
trait JdbcContainerSpec extends BeforeAndAfterAll with ScalaFutures {
  this: Suite =>

  private val log = LoggerFactory.getLogger(this.getClass)

  private var databaseName: String = ""

  def createDatabase(
      tag: String,
      driver: String,
      user: String,
      password: String,
      schemaPath: String,
      connectionProperties: String = JdbcContainerSpec.DefaultConnectionProperties,
      drop: Boolean): JdbcBackend.Database = {
    databaseName = nextDatabaseName(tag)
    log.info(s"Create database $databaseName")
    AdminDb
    val start = System.currentTimeMillis()

    val create = sqlu"CREATE DATABASE #$databaseName"
    val use = sqlu"USE #$databaseName"
    val grants = sqlu"""GRANT ALL PRIVILEGES ON #$databaseName.* TO #$user@'%'"""
    val flush = sqlu"FLUSH PRIVILEGES"
    val action = getSchemaAction(schemaPath)
    val wholeActions = DBIOAction.sequence(Seq(create, grants, flush, use, action)).transactionally
    Await.result(AdminDb.run(wholeActions), 60.seconds)
    log.info(s"Schema creating took ${System.currentTimeMillis() - start} ms")
    val ds =
      JdbcContainerSpec.getDatasource(driver, getUrl(databaseName), user, password, connectionProperties, "unit-test")
    val db = getDatabase(ds)

    sys.addShutdownHook {
      dropAction(databaseName): Unit
    }

    db
  }

  private def dropAction(dbName: String) = {
    val drop = sqlu"DROP DATABASE #$dbName"
    Await.ready(AdminDb.run(drop), 5.seconds)
  }

  private def getUrl(databaseName: String): String =
    JdbcContainerSpec.Container.getJdbcUrl
      .replace(JdbcContainerSpec.Container.getDatabaseName, databaseName)

  private def getSchemaAction(schemaPath: String) = {
    val schemaScript = Source.fromInputStream(getClass.getResourceAsStream(schemaPath)).mkString
    val statements = schemaScript.split(";")
    val effectiveStatements =
      statements.map(_.trim).filterNot(_.startsWith("--")).filter(_.nonEmpty).map(s => s"$s;")
    import scala.concurrent.ExecutionContext.Implicits.global
    DBIOAction
      .sequence(
        effectiveStatements.toSeq.map { s =>
          sqlu"#$s"
        }
      )
      .map(_ => ())
      .transactionally
  }

  private def getDatabase(ds: DataSource) =
    JdbcBackend.Database.forDataSource(ds, None)

  def nextDatabaseName(name: String): String = {
    val id = UUID.randomUUID().toString.take(5)
    s"${name}_$id"
  }

}

object JdbcContainerSpec {

  // scalastyle:off
  val DefaultConnectionProperties =
    "connectTimeout=5000;socketTimeout=30000;useUnicode=true;characterEncoding=utf8;autoReconnect=true;useCursorFetch=true;useCompression=true;rewriteBatchedStatements=true;useSSL=false"

  val AdminDBConnectionProps =
    "connectTimeout=5000;socketTimeout=30000;useUnicode=true;characterEncoding=utf8;autoReconnect=true;useCompression=true;allowMultiQueries=true;useSSL=false"

  private val RootUser: String = "root"
  private val RootPassword: String = "test"

  private val Driver = "com.mysql.jdbc.Driver"

  private lazy val AdminDb = {
    val ds = getDatasource(
      Driver,
      Container.getJdbcUrl,
      RootUser,
      RootPassword,
      AdminDBConnectionProps,
      "admin"
    )
    Database.forDataSource(ds, None)
  }

  private def getDatasource(
      driver: String,
      url: String,
      user: String,
      password: String,
      connectionProperties: String,
      name: String): DataSource = {
    getHikariDS(driver, url, user, password, connectionProperties, name)
  }

  private def getHikariDS(
      driver: String,
      url: String,
      user: String,
      password: String,
      connectionProperties: String,
      name: String) = {
    val config = new HikariConfig()
    config.setDriverClassName(driver)
    config.setJdbcUrl(url)
    config.setUsername(user)
    config.setPassword(password)
    config.setMaximumPoolSize(1)
    config.setPoolName(s"db-$name")
    connectionProperties.split(";").foreach { props =>
      val kv = props.split("=", 2)
      config.addDataSourceProperty(kv(0), kv(1))
    }

    new HikariDataSource(config)
  }

  private lazy val Container = {
    val c = new MySQLContainer("mysql:5.7.18")
    c.addEnv("MYSQL_DATABASE", "promocoder_unit_test_db")
    c.addEnv("MYSQL_USER", RootUser)
    c.addEnv("MYSQL_PASSWORD", RootPassword)
    c.addEnv("MYSQL_ROOT_PASSWORD", RootPassword)
    c.withTmpFs(Collections.singletonMap("/tmpfs", "rw,size=512M"))
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
