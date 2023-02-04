package ru.yandex.vertis.telepony.util

import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

import com.typesafe.scalalogging.StrictLogging
import org.testcontainers.containers.MySQLContainer
import ru.yandex.vertis.telepony.dao.jdbc.JdbcSpecUtils.getSchemaAction
import ru.yandex.vertis.telepony.settings.MySqlConfig
import ru.yandex.vertis.telepony.util.db.DefaultDatabaseFactory
import ru.yandex.vertis.telepony.util.db.SlickDb.SqlWrite

import concurrent.duration._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{Await, Future}
import scala.util.Success

/**
  * @author neron
  */
object MySqlContainer extends StrictLogging {

  import Threads.lightWeightTasksEc

  def createDb(dbName: String, schemaScript: String, dropOnExit: Boolean = true): Future[Unit] = {
    val action = getSchemaAction(schemaScript)
    AdminDb.db.run(createDatabase(dbName).andThen(action).transactionally).andThen {
      case Success(_) if dropOnExit => dropActionHooks.add(dropDbAction(dbName))
    }
  }

  def getJdbcUrl: String = {
    Container.getJdbcUrl.replace("/".concat(Container.getDatabaseName), "")
  }

  private def dropDbAction(databaseName: String): SqlWrite[Int] = sqlu"DROP DATABASE #$databaseName"

  private def createDatabase(databaseName: String) =
    for {
      _ <- sqlu"CREATE DATABASE #$databaseName"
      _ <- sqlu"GRANT ALL PRIVILEGES ON #$databaseName.* TO #$User@'%'"
      _ <- sqlu"FLUSH PRIVILEGES"
      _ <- sqlu"USE #$databaseName"
    } yield ()

  val User: String = "telepony"
  val Password: String = "telepony"

  private lazy val AdminDb = {
    val config = MySqlConfig(
      url = getJdbcUrl,
      username = "root",
      password = Password,
      minConnections = 1,
      maxConnections = 1,
      idleTimeout = 1.minute,
      executorThreads = 1,
      executorQueue = 10
    )
    DefaultDatabaseFactory.buildDatabase("telepony-admin", config, readonly = false)
  }

  private val dropActionHooks = new CopyOnWriteArrayList[SqlWrite[Int]]()

  import scala.jdk.CollectionConverters._

  private def onExit(): Unit = {
    Await.result(AdminDb.db.run(DBIO.sequence(dropActionHooks.asScala.toList)), 10.seconds)
    Container.close()
  }

  private lazy val Container = {
    val mysqlVersion = sys.env.getOrElse("MYSQL_VERSION", "8.0")
    logger.info(s"Starting mysql container version $mysqlVersion")
    val c = new MySQLContainer(s"mysql:$mysqlVersion")
    c.withUsername(User)
    c.withPassword(Password)
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
      onExit()
    }
    c
  }

}
