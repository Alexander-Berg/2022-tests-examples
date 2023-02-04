package ru.yandex.vertis.telepony.dao.jdbc

import org.joda.time.DateTime
import ru.yandex.vertis.telepony.dao.jdbc.api._
import ru.yandex.vertis.telepony.logging.SimpleLogging
import ru.yandex.vertis.telepony.settings.DualMysqlConfig
import ru.yandex.vertis.telepony.util.MySqlContainer
import ru.yandex.vertis.telepony.util.db.{DefaultDatabaseFactory, PlainDualDatabase}
import ru.yandex.vertis.telepony.util.random.IdUtil
import slick.dbio.Effect.Transactional

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Provides database base methods for tests runs
  *
  * @author dimas
  */
object JdbcSpecUtils extends SimpleLogging {

  def createDualDb(
      tag: String,
      mySqlConfig: DualMysqlConfig,
      schemaScript: String,
      dropOnExit: Boolean = true): PlainDualDatabase = {
    val databaseName = nextDatabaseName(tag)
    log.info(s"Create database $databaseName")
    val start = DateTime.now()
    Await.result(MySqlContainer.createDb(databaseName, schemaScript, dropOnExit), 60.seconds)
    val end = DateTime.now()
    val diff = end.getMillis - start.getMillis
    log.info(s"Creating db took $diff millis.")

    val dbUrl = s"${mySqlConfig.master.url}/$databaseName"

    DefaultDatabaseFactory.buildDualDatabase(
      databaseName,
      mySqlConfig.copy(
        master = mySqlConfig.master.copy(url = dbUrl),
        slave = mySqlConfig.slave.copy(url = dbUrl)
      )
    )
  }

  def getSchemaAction(schemaScript: String): DBIOAction[Unit, NoStream, Effect with Transactional] = {
    val statements = schemaScript.split(";")
    val effectiveStatements =
      statements.map(_.trim).filterNot(_.startsWith("--")).filter(_.nonEmpty).map(s => s"$s;")

    import scala.concurrent.ExecutionContext.Implicits.global
    DBIO
      .sequence(
        effectiveStatements.toSeq.map { s =>
          sqlu"#$s"
        }
      )
      .map(_ => ())
      .transactionally
  }

  private def nextDatabaseName(name: String) = {
    val id = IdUtil.generateId64().replaceAll("-", "_")
    s"${name}_$id"
  }

}
