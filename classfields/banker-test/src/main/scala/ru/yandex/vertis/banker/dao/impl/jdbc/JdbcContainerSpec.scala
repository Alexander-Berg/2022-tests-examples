package ru.yandex.vertis.banker.dao.impl.jdbc

import java.util.{Collections, UUID}

import org.apache.commons.dbcp2.BasicDataSource
import org.slf4j.{Logger, LoggerFactory}
import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplateBase.{NamedDatabase, Scheme}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.MySQLProfile.api._
import slick.sql.SqlAction

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt
import scala.io.Source

/**
  * @author ruslansd
  */
object JdbcContainerSpec extends AsyncSpecBase {

  def createDatabase(scheme: Scheme): NamedDatabase = {
    val logger = LoggerFactory.getLogger(this.getClass)
    val time = System.currentTimeMillis()

    val databaseName = createDatabase(scheme.namePrefix, scheme.schemePath, logger)
    val ds = new BasicDataSource {
      setDriverClassName(Driver)
      setUrl(JdbcContainerSpec.getUrl(databaseName))
      setUsername(RootUser)
      setPassword(RootPassword)
      setConnectionProperties(DefaultConnectionProps)
    }
    val db = Database.forDataSource(ds, None)

    val schemaStart = System.currentTimeMillis()
    Await.result(db.run(readStatements(scheme.schemePath).transactionally), 60.seconds)
    logger.info(s"Schema creation took ${System.currentTimeMillis() - schemaStart}")

    val spent: Long = System.currentTimeMillis() - time
    JdbcContainerSpec.totalTime += spent
    logger.info(s"ddl took ${JdbcContainerSpec.totalTime} millis for now")
    NamedDatabase(db, databaseName)
  }

  def drop(name: String): Future[Unit] =
    AdminDb.run(sqlu"""DROP DATABASE #$name""").map(_ => ())

  private def createDatabase(tag: String, schemaPath: String, logger: Logger): String = {
    val databaseName = nextDatabaseName(tag)
    AdminDb
    logger.info(s"Create database $databaseName")
    val start = System.currentTimeMillis()
    Await.result(AdminDb.run(createDatabase(databaseName).transactionally), 60.seconds)
    logger.info(s"Db $databaseName creation took ${System.currentTimeMillis() - start}")
    databaseName
  }

  private def createDatabase(databaseName: String) =
    for {
      _ <- sqlu"CREATE DATABASE #$databaseName"
      _ <- sqlu"GRANT ALL PRIVILEGES ON #$databaseName.* TO #$RootUser@'%'"
      _ <- sqlu"FLUSH PRIVILEGES"
      _ <- sqlu"USE #$databaseName"
    } yield ()

  def readStatements(schemaPath: String): SqlAction[Int, NoStream, Effect] = {
    val schemaScript =
      Source.fromInputStream(getClass.getResourceAsStream(schemaPath)).mkString

    val schemaStatements = schemaScript
      .split(";")
      .map(_.trim)
      .filterNot(_.startsWith("--"))
      .filter(_.nonEmpty)
      .map(_ + ";")
      .mkString("\n")
    sqlu"#$schemaStatements"
  }

  @volatile
  var totalTime = 0L
  // scalastyle:off

  // The property 'logger=...' allows to get pretty logs instead of exceptions in stderr
  // profileSQL=true;logger=com.mysql.jdbc.log.Slf4JLogger

  private def getUrl(databaseName: String): String =
    Container.getJdbcUrl.replace(Container.getDatabaseName, databaseName)

  private def nextDatabaseName(name: String) = {
    val id = UUID.randomUUID().toString.take(5)
    s"${name}_$id"
  }

  val AdminDBConnectionProps =
    "connectTimeout=5000;socketTimeout=30000;useUnicode=true;characterEncoding=utf8;autoReconnect=true;useCompression=true;allowMultiQueries=true;useSSL=false"

  val DefaultConnectionProps =
    "connectTimeout=5000;socketTimeout=30000;useUnicode=true;characterEncoding=utf8;autoReconnect=true;useCursorFetch=true;useCompression=true;rewriteBatchedStatements=true;allowMultiQueries=true;useSSL=false"

  private val RootUser: String = "root"
  private val RootPassword: String = "test"

  private val Driver = "com.mysql.jdbc.Driver"

  private lazy val AdminDb = {
    val ds = new BasicDataSource {
      setDriverClassName(Driver)
      setUrl(Container.getJdbcUrl)
      setUsername(RootUser)
      setPassword(RootPassword)
      setConnectionProperties(AdminDBConnectionProps)
    }
    Database.forDataSource(ds, None)
  }

  private lazy val Container = {
    val c = new MySQLContainer("mysql:5.7.34")
    c.addEnv("MYSQL_DATABASE", "banker_unit_test_db")
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
      "--innodb-print-all-deadlocks=ON",
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
