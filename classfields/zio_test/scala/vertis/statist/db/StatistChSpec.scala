package vertis.statist.db

import common.db.config.DbConfig
import common.zio.logging.Logging
import common.zio.ops.prometheus.Prometheus
import org.testcontainers.containers.ClickHouseContainer
import vertis.zio.BaseEnv
import zio._
import zio.blocking.Blocking
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec}

import scala.io.Source

/**
 * @author Ratskevich Natalia reimai@yandex-team.ru
 */
abstract class StatistChSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    chSpec.provideCustomLayerShared {
      (Blocking.live ++ common.zio.clients.clickhouse.testkit.TestClickhouse.live >>> StatistChSpec.live) ++
        Prometheus.live ++
        Logging.live
    }

  protected def chSpec: ZSpec[BaseEnv with Has[Database], Any]
}

object StatistChSpec {

  val live: URLayer[Blocking with Has[ClickHouseContainer], Has[Database]] = {
    ZLayer.fromServiceM[ClickHouseContainer, Blocking, Nothing, Database] { container =>
      val dbConfig = DbConfig(
        driver = "ru.yandex.clickhouse.ClickHouseDriver",
        url = container.getJdbcUrl,
        user = container.getUsername,
        password = container.getPassword
      )
      ZIO
        .environment[Blocking]
        .flatMap { blocking =>
          val db = DatabaseFactory.buildDatabase("test", dbConfig, blocking.get)
          UIO(db).tap(initDb(_).orDie)
        }
    }
  }

  private def initDb(db: Database): Task[Unit] = {
    val scripts = getSchemaAction("/schema.sql")
    db.withTxSession { implicit session =>
      scripts.foreach(session.execute(_))
    }
  }

  private def getSchemaAction(file: String): Seq[String] = {
    val schemaScript = Source.fromInputStream(getClass.getResourceAsStream(file)).mkString
    val cleaned = schemaScript
      .split("\n")
      .filterNot(_.startsWith("--"))
      .filterNot(_.startsWith("#"))
      .filter(_.nonEmpty)
      .mkString
    val statements = cleaned.split(";")
    statements
      .map(_.trim)
      .filterNot(_.startsWith("--"))
      .filterNot(_.startsWith("#"))
      .filter(_.nonEmpty)
      .toSeq
  }
}
