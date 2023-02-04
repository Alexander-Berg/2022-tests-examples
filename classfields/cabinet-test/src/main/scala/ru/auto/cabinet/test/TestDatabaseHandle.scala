package ru.auto.cabinet.test

import org.slf4j.LoggerFactory
import ru.auto.cabinet.service.instr.DatabaseProxy
import ru.auto.cabinet.trace.Context
import slick.jdbc.MySQLProfile.api.{offsetDateTimeColumnType => _, _}
import slick.jdbc.{DataSourceJdbcDataSource, DriverDataSource, JdbcBackend}
import slick.util.AsyncExecutor

import java.io.FileNotFoundException
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

case class TestDatabaseHandle(
    databaseName: String,
    db: DatabaseProxy,
    dbServer: Database) {

  private val log = LoggerFactory.getLogger(this.getClass)

  def init(schemaScriptPaths: Seq[String]): Future[Seq[Int]] = {
    log.info(s"Creating database $databaseName")
    val create = sqlu"CREATE DATABASE #$databaseName"
    val use = sqlu"USE #$databaseName"
    val actions =
      Seq(create, use) ++ schemaScriptPaths.flatMap(getSchemaActions)
    dbServer.run(DBIO.sequence(actions).transactionally)
  }

  def drop: Future[Int] = {
    log.info(s"Dropping database $databaseName")
    try {
      db.close()
      dbServer.run(sqlu"DROP DATABASE #$databaseName")
    } catch {
      case e: Exception =>
        log.error("Error while drop database", e)
        Future.failed(e)
    }
  }

  private def getSchemaActions(path: String): Seq[DBIO[Int]] =
    loadFile(path)
      .split(";\n")
      .toSeq
      .map(_.trim)
      .filterNot(_.startsWith("--"))
      .filter(_.nonEmpty)
      .map(s => sqlu"#$s;")

  private def loadFile(path: String) = {
    Option(getClass.getResourceAsStream(path))
      .map(Source.fromInputStream(_)("UTF-8"))
      .getOrElse(throw new FileNotFoundException(path))
      .mkString
  }

}

/** Provides database base methods for tests runs
  */
object TestDatabaseHandle {

  def apply(
      tag: String,
      url: String,
      user: String,
      password: String): TestDatabaseHandle = {
    val databaseName = nextDatabaseName(tag)
    val driver = "com.mysql.jdbc.Driver"
    val dbServer =
      Database.forURL(url, user, password, driver = driver)

    val numThreads = 10
    val queueSize = 9000
    val maxConnections = numThreads * 5
    val registerMbeans = false
    val asyncExecutor =
      AsyncExecutor(
        databaseName,
        minThreads = numThreads,
        maxThreads = numThreads,
        queueSize = queueSize,
        maxConnections = maxConnections,
        registerMbeans = registerMbeans)

    val ds = new DriverDataSource(
      url = url + "/" + databaseName + "?useUnicode=true&amp;" +
        "characterEncoding=utf8&amp;autoReconnect=true&amp;useCursorFetch=true&amp;" +
        "useCompression=true&rewriteBatchedStatements=true&zeroDateTimeBehavior=convertToNull",
      user,
      password,
      null,
      driver)
    val jdbcDs = new DataSourceJdbcDataSource(ds, true, Some(10))
    val db =
      new DatabaseProxy {
        val db = new JdbcBackend.DatabaseDef(jdbcDs, asyncExecutor)
        override def run[R](a: DBIOAction[R, NoStream, Nothing])(implicit
            ec: ExecutionContext,
            rc: Context = Context.unknown,
            name: sourcecode.Name,
            file: sourcecode.File) = db.run(a)

        override def stream[T](a: DBIOAction[_, Streaming[T], Nothing]) =
          db.stream(a)

        override def close(): Unit = db.close()
      }

    new TestDatabaseHandle(databaseName, db, dbServer)

  }

  private def nextDatabaseName(tag: String) = {
    val id = UUID.randomUUID().toString.take(5)
    s"${tag}_$id"
  }
}
