package ru.yandex.vertis.moderation.util

import java.time.Duration
import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.moderation.util.MySqlSpecBase._
import ru.yandex.vertis.moderation.util.mysql._
import ru.yandex.vertis.ops.test.TestOperationalSupport
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration.DurationInt
import scala.io.{Codec, Source}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author potseluev
  */
trait MySqlSpecBase {

  private val DatabaseName: String = s"test_${Random.alphanumeric.take(5).mkString}"

  private val CreateDatabaseSql =
    DBIO.seq(
      sqlu"CREATE DATABASE #$DatabaseName;",
      sqlu"GRANT ALL ON #$DatabaseName.* TO #$User;"
    )

  protected def schemaScripts: Seq[String] =
    Seq(
      "/mysql/schema.sql"
    )

  lazy val database: MySqlDb = {
    RootDb.run(CreateDatabaseSql, MySqlModes.Write).get()
    val config = getDbConfig(User, Password, Some(DatabaseName))
    val db = createDatabase(config)
    db.run(getSchemaAction(getSchema), MySqlModes.Write).get()
    db
  }

  private def getSchemaAction(schemaScript: String): DBIO[Unit] = {
    val statements = schemaScript.split(";")
    val effectiveStatements =
      statements
        .map(_.trim)
        .filterNot(_.startsWith("--"))
        .filterNot(_.startsWith("#"))
        .filter(_.nonEmpty)
        .map(statement => sqlu"#$statement;")
        .toSeq

    DBIO.sequence(effectiveStatements).map(_ => ()).transactionally
  }

  private def getSchema: String =
    schemaScripts
      .map { fileName =>
        val stream = getClass.getResourceAsStream(fileName)
        Source.fromInputStream(stream)(Codec.UTF8).mkString
      }
      .mkString("\n")
}

object MySqlSpecBase {

  private val RootUser = "root"
  private val RootPassword = "test"

  private val MaxConnections: Int = 2

  private val MySqlVersion = "5.7.18"

  private lazy val MySqlContainer: MySQLContainer[Nothing] = {
    val container: MySQLContainer[Nothing] =
      new MySQLContainer(s"${MySQLContainer.NAME}:$MySqlVersion")
        .withStartupTimeout(Duration.ofMinutes(3))
    container.start()
    container
  }

  private val User: String = MySqlContainer.getUsername
  private val Password: String = MySqlContainer.getPassword

  private def getDbConfig(user: String, password: String, database: Option[String] = None): MySqlConfig =
    MySqlConfig(
      read =
        MySqlConfig0(
          poolName = "moderation-test",
          driver = MySqlContainer.getDriverClassName,
          url = getUrl(database),
          user = user,
          password = password,
          idleTimeout = 80.seconds,
          maxConnections = MaxConnections,
          leakDetectionThreshold = 5.minutes,
          readOnly = true
        ),
      write =
        MySqlConfig0(
          poolName = "moderation-test",
          driver = MySqlContainer.getDriverClassName,
          url = getUrl(database),
          user = user,
          password = password,
          idleTimeout = 80.seconds,
          maxConnections = MaxConnections,
          leakDetectionThreshold = 5.minutes,
          readOnly = false
        )
    )

  private def getUrl(dbName: String): String = MySqlContainer.getJdbcUrl.replace(MySqlContainer.getDatabaseName, dbName)

  private def getUrl(dbName: Option[String]): String = dbName.map(getUrl).getOrElse(MySqlContainer.getJdbcUrl)

  private lazy val RootDb: MySqlDb = {
    val config = getDbConfig(RootUser, RootPassword)
    val db = createDatabase(config)
    db.run(
      sqlu"""
            SET GLOBAL sql_mode = 'NO_ENGINE_SUBSTITUTION',
                       explicit_defaults_for_timestamp = 1
        """,
      MySqlModes.Write
    ).get()
    db
  }

  private def createDatabase(config: MySqlConfig) = {
    val dsFactory = new HikariDataSourceFactory(Some(TestOperationalSupport.prometheusRegistry))
    new MySqlDbFactoryImpl(dsFactory).createDatabase(config)
  }

  implicit class RichMySqlDb(val db: MySqlDb) extends AnyVal {

    def runAsRoot[T](action: DBIO[T]): Unit = {
      RootDb.run(action, MySqlModes.Write).get()
    }
  }

}
