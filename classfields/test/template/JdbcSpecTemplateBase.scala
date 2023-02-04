package ru.auto.salesman.test.template

import org.apache.commons.dbcp2.BasicDataSource

import java.util.UUID
import ru.auto.salesman.dao.impl.jdbc.database.Database
import ru.auto.salesman.dao.impl.jdbc.database.doobie.Transactor
import ru.auto.salesman.dao.impl.jdbc.database.doobie.impl.TransactorFactoryImpl
import scala.annotation.tailrec
import scala.io.Source
import scala.slick.jdbc.{JdbcBackend, StaticQuery}
import scala.util.control.NonFatal
import scala.util.{Failure, Try}

trait JdbcSpecTemplateBase {

  import JdbcSpecTemplateBase._

  def createTransactor(
      url: String,
      user: String,
      password: String,
      driver: String,
      databaseName: String,
      returnInstance: Boolean = false
  ): Transactor = {
    val ds =
      getConnection(url, user, password, driver, databaseName, returnInstance)

    TransactorFactoryImpl()
      .fromDataSource(ds)
      .unsafeRunToTry()
      .get
  }

  def createDatabase(
      tag: String,
      url: String,
      user: String,
      password: String,
      driver: String,
      schemaPath: String,
      addRandomPrefix: Boolean = true,
      returnInstance: Boolean = false
  ): Database = {
    val databaseName = getDatabaseName(
      tag,
      url,
      user,
      password,
      driver,
      schemaPath,
      addRandomPrefix
    )

    Database.forTests(
      url = getUrl(url, databaseName, returnInstance),
      user = user,
      password = password,
      driver = driver,
      databaseName = databaseName
    )
  }

  def nextDatabaseName(name: String): String = {
    val id = UUID.randomUUID().toString.take(8)
    s"${name}_$id"
  }

  private def getUrl(
      url: String,
      databaseName: DatabaseName,
      returnInstance: Boolean
  ): String =
    url + "/" + (if (returnInstance) "" else databaseName) + DbOptions

  private def getConnection(
      url: String,
      user: String,
      password: String,
      driver: String,
      databaseName: String,
      returnInstance: Boolean
  ): BasicDataSource = {
    val ds = new BasicDataSource
    ds.setUrl(getUrl(url, databaseName, returnInstance))
    ds.setUsername(user)
    ds.setPassword(password)
    ds.setDriverClassName(driver)
    ds
  }

  private def getDatabaseName(
      tag: String,
      url: String,
      user: String,
      password: String,
      driver: String,
      schemaPath: String,
      addRandomPrefix: Boolean
  ): DatabaseName = {
    val db: Database = Database.forTests(
      url = url + DbOptions,
      user = user,
      password = password,
      driver = driver,
      databaseName = "testInstance"
    )

    val databaseName = db.withTransaction { implicit session =>
      val databaseName = createDatabaseWithRetries(tag, addRandomPrefix)
      StaticQuery.updateNA(s"USE $databaseName").execute
      StaticQuery.updateNA("SET SQL_MODE=ALLOW_INVALID_DATES").execute

      /** Provides script for create schema */
      schemaPath.split(";").foreach { s =>
        def schemaScript =
          Source
            .fromInputStream(getClass.getResourceAsStream(s))("UTF-8")
            .mkString
        val statements = schemaScript.split(";")
        val effectiveStatements = statements
          .map(_.trim)
          .filterNot(_.startsWith("--"))
          .filter(_.nonEmpty)
          .map(s => s + ";")
        effectiveStatements.foreach { es =>
          StaticQuery.updateNA(es).execute
        }
      }
      databaseName
    }

    dropDatabaseOnShutdown(db, databaseName)
    databaseName
  }

  private def createDatabaseWithRetries(tag: String, addRandomPrefix: Boolean)(
      implicit session: JdbcBackend.Session
  ): DatabaseName =
    retry(times = 3)(justCreateDatabase(tag, addRandomPrefix)).get

  private def justCreateDatabase(tag: String, addRandomPrefix: Boolean)(
      implicit session: JdbcBackend.Session
  ): Try[DatabaseName] = {
    val databaseName = if (addRandomPrefix) nextDatabaseName(tag) else tag
    Try(StaticQuery.updateNA(s"CREATE DATABASE $databaseName").execute)
      .map(_ => databaseName)
  }

  private def dropDatabaseOnShutdown(
      database: Database,
      databaseName: String
  ): Unit =
    sys.addShutdownHook {
      database.withSession { implicit session =>
        StaticQuery.updateNA(s"DROP DATABASE $databaseName").execute
      }
    }

  @tailrec
  private def retry[T](times: Int)(action: => Try[T]): Try[T] = action match {
    case Failure(NonFatal(_)) if times > 0 =>
      retry(times - 1)(action)
    case other => other
  }
}

object JdbcSpecTemplateBase {

  private type DatabaseName = String

  val DbOptions =
    "?useUnicode=true&amp;characterEncoding=utf8&amp;autoReconnect=true&amp;useCursorFetch=true&amp;useCompression=true&rewriteBatchedStatements=true&useSSL=false&zeroDateTimeBehavior=convertToNull"

}
