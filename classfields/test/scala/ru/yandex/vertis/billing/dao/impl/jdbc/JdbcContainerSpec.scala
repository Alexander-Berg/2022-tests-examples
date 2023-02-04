package ru.yandex.vertis.billing.dao.impl.jdbc

import cats.effect.Blocker
import doobie.Transactor
import org.apache.commons.dbcp2.BasicDataSource
import org.slf4j.LoggerFactory
import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplateBase.{NamedDatabase, Scheme}
import ru.yandex.vertis.util.concurrent.Threads
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.{SQLActionBuilder, SetParameter}
import slick.jdbc.MySQLProfile.api._
import zio.Task
import zio.interop.catz._
import zio.blocking.Blocking

import java.util.{Collections, UUID}
import scala.annotation.nowarn
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

/**
 * @author ruslansd
 */
object JdbcContainerSpec {

  private val Driver: String = "com.mysql.jdbc.Driver"

  private val User: String = "test"

  private val Password: String = "test"

  protected def connectionProperties: String = DefaultConnectionProps

  private lazy val logger = LoggerFactory.getLogger(this.getClass)

  private lazy val adminDatabase = {
    val ds = {
      val ds = new BasicDataSource
      ds.setDriverClassName(Driver)
      ds.setUrl(Container.getJdbcUrl)
      ds.setUsername(RootUser)
      ds.setPassword(RootPassword)
      ds.setConnectionProperties(AdminDBConnectionProps)
      ds.setMaxTotal(10)
      ds.setMaxIdle(10)
      ds
    }
    Database.forDataSource(ds, maxConnections = Some(ds.getMaxTotal))
  }

  @nowarn("msg=discarded non-Unit value")
  def drop(name: String): Unit = {
    Await.result(
      adminDatabase.run {
        sqlu"""DROP DATABASE #$name"""
      },
      Duration.Inf
    )
  }

  def database(scheme: Scheme): NamedDatabase =
    database(scheme.namePrefix, Seq(scheme.schemePath))

  def database(name: String, schemas: Seq[String]): NamedDatabase = {
    val time = System.currentTimeMillis()
    val finalName = nextDatabaseName(name)
    initDatabase(finalName, schemas)

    val db = Database.forDataSource(
      {
        val ds = new BasicDataSource
        ds.setDriverClassName(Driver)
        ds.setUrl(getUrl(finalName))
        ds.setUsername(User)
        ds.setPassword(Password)
        ds.setConnectionProperties(connectionProperties)
        ds
      },
      maxConnections = None
    )
    val spent: Long = System.currentTimeMillis() - time
    logger.info(s"creating database took $spent millis")
    NamedDatabase(db, finalName)
  }

  def toQuery(statements: Seq[String]): DBIO[Int] = {
    DBIO
      .sequence(statements.map { st =>
        SQLActionBuilder(Seq(st), SetParameter.SetUnit).asUpdate
      })
      .map(_.sum)(Threads.SameThreadEc)
  }

  def readStatements(schemas: Seq[String]): Seq[String] = {
    schemas.flatMap { schemaPath =>
      Source
        .fromInputStream(getClass.getResourceAsStream(schemaPath), "UTF-8")
        .mkString
        .split(";")
        .map(_.trim)
        .filterNot(_.startsWith("--"))
        .filter(_.nonEmpty)
        .map(s => s + ";")
    }
  }

  private def initDatabase(databaseName: String, schemas: Seq[String]): Unit = {
    logger.info(s"Create database $databaseName for schema")

    val start = System.currentTimeMillis()
    Await.result(
      adminDatabase.run {
        val statements = Seq(
          s"CREATE DATABASE $databaseName;",
          s"GRANT ALL PRIVILEGES ON $databaseName.* TO $User@'%';",
          s"FLUSH PRIVILEGES;",
          s"USE $databaseName;"
        ) ++ readStatements(schemas)
        toQuery(statements)
      },
      Duration.Inf
    )

    logger.info(s"Time for creating schema took ${System.currentTimeMillis() - start}")
  }

  def getUrl(databaseName: String): String =
    Container.getJdbcUrl.replace(Container.getDatabaseName, databaseName)

  private def nextDatabaseName(name: String) = {
    val id = UUID.randomUUID().toString.take(5)
    s"${name}_$id"
  }

  private val AdminDBConnectionProps =
    "connectTimeout=5000;socketTimeout=30000;useUnicode=true;characterEncoding=utf8;autoReconnect=true;useCompression=true;allowMultiQueries=true;useSSL=false"

  // The property 'logger=...' allows to get pretty logs instead of exceptions in stderr
  // profileSQL=true;logger=com.mysql.jdbc.log.Slf4JLogger
  val DefaultConnectionProps =
    "connectTimeout=5000;socketTimeout=30000;useUnicode=true;characterEncoding=utf8;autoReconnect=true;useCursorFetch=true;useCompression=true;rewriteBatchedStatements=true;sessionVariables=group_concat_max_len=2048;allowMultiQueries=true;useSSL=false"

  private val port: Int = 3306

  private val RootUser: String = "root"

  private val RootPassword: String = "test"

  lazy val Container = {
    val c = new MySQLContainer("mysql:5.7.34")
    c.withPassword(Password)
    c.withUsername(User)
    c.withTmpFs(Collections.singletonMap("/tmpfs", "rw,size=768M"))
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

  /*
        Это хак, для того чтобы использовать базы созданные для тестов slick-a в тестах для doobie.
        И в тестах которые проверяют замену одного на другое.
   */
  def asTransactor(databaseName: String) = Transactor.fromDriverManager[Task](
    Container.getDriverClassName,
    getUrl(databaseName),
    Container.getUsername,
    Container.getPassword,
    Blocker.liftExecutionContext(Blocking.Service.live.blockingExecutor.asEC)
  )

}
