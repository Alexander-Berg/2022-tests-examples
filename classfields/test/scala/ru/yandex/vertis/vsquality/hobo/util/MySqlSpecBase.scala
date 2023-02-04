package ru.yandex.vertis.vsquality.hobo.util

import com.dimafeng.testcontainers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import ru.yandex.vertis.vsquality.hobo.database._
import ru.yandex.vertis.vsquality.hobo.util.CommonUtil.RichFuture
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase.{myContainer, mySqlDatabase}
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api.{Database => _, _}

import scala.annotation.nowarn
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.io.{Codec, Source}

/**
 * @author potseluev
 */
trait MySqlSpecBase {

  val database: Database = mySqlDatabase

  def getContainer: MySQLContainer = myContainer;

}

object MySqlSpecBase {
  lazy val myContainer: MySQLContainer = getContainer()

  lazy val mySqlDatabase: Database = {
    val config = getDbConfig("root", "test", Some(DatabaseName))
    val db = DatabaseFactory.getDatabase(config)
    db.run(getSchemaAction(getSchema), DbMode.Write).get()
    db
  }
  private val MaxReadConnections: Int = 2
  private val RootUser = "garry-root"
  private val MaxWriteConnections: Int = 2
  private val RootPassword = "garry-root-password"
  private val DatabaseName: String = s"QWERTY"

  private def getContainer(): MySQLContainer = {
    val container = MySQLContainer(
      mysqlImageVersion = DockerImageName.parse("mysql:5.7.18"),
      databaseName = DatabaseName,
      username = RootUser,
      password = RootPassword
    )
    container.start()
    container
  }

  protected def schemaScripts: Seq[String] =
    Seq(
      "hobo.sql",
      "hobo_analytics.sql"
    )

  private def getSchema: String =
    schemaScripts
      .map { fileName =>
        Source.fromResource(s"vs-quality/hobo/scripts/$fileName")(Codec.UTF8).mkString
      }
      .mkString("\n")

  @nowarn
  private def getDbConfig(user: String, password: String, database: Option[String] = None): DbConfig =
    DbConfig(
      domain = "hobo-test",
      driver = myContainer.driverClassName,
      writeUrl = myContainer.jdbcUrl,
      readUrl = myContainer.jdbcUrl,
      user = "garry-root",
      password = "garry-root-password",
      idleTimeout = 80.seconds,
      maxReadConnections = MaxReadConnections,
      maxWriteConnections = MaxWriteConnections,
      slowQueryThreshold = None,
      createTestUsers = false,
      socketTimeout = 60.seconds,
      writeValidationQuery = None,
      readValidationQuery = None,
      allowPublicKeyRetrieval = true
    )

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

}
