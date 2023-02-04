package ru.yandex.vertis.feedprocessor.util

import java.util.UUID

import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Suite}
import org.springframework.jdbc.CannotGetJdbcConnectionException
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.dao.utils.{Database, DatabaseConfig, DbCluster}

/**
  * DB support for tests
  */
trait DatabaseSpec extends BeforeAndAfterAll with DummyOpsSupport with TestApplication with Logging {
  this: Suite =>

  private val uniqueName = {
    val hash = UUID.randomUUID().toString.take(5)
    s"feedprocessor_unit_test_$hash"
  }

  @volatile private var databases: List[(String, DbCluster)] = Nil

  implicit lazy val tasksDb: DbCluster = {
    val db = createDatabase(uniqueName)
    recreateDb(db)
    // устанавливаем mode как в MDB
    db.master.jdbc.execute("SET sql_mode = 'ONLY_FULL_GROUP_BY';")
    db
  }

  protected def recreateDb(db: DbCluster) = {
    val drop = readSqlFile("/sql/schema_drop.sql")
    val create = readSqlFile("/sql/schema_base.sql")
    drop.foreach { sql =>
      db.master.jdbc.execute(sql)
    }
    create.foreach { sql =>
      db.master.jdbc.execute(sql)
    }
  }

  override protected def afterAll(): Unit = {
    databases.foreach(v => clearDatabase(v._1, v._2))
    super.afterAll()
  }

  private def clearDatabase(dbName: String, db: DbCluster) = {
    try {
      db.master.jdbc.update(s"DROP DATABASE IF EXISTS $dbName")
    } catch {
      case ex: CannotGetJdbcConnectionException => // silently ignore already closed connection
    }
    db.master.close()
  }

  private def createDatabase(databaseName: String): DbCluster = {
    val url = MySqlContainer.container.getJdbcUrl
    val urlWithDatabase = s"$url/$databaseName"
    val configStr =
      s"""
        username = "${MySqlContainer.container.getUsername}"
        password = "${MySqlContainer.container.getPassword}"
        driverClass = "${MySqlContainer.container.getDriverClassName}"
      """
    val config = ConfigFactory.parseString(configStr)
    val dbConfig = DatabaseConfig.fromConfig(url, config, false, databaseName)
    val db0 = new Database(dbConfig)
    try {
      db0.jdbc.update(s"CREATE DATABASE $databaseName")
    } finally {
      db0.close()
    }
    val db = new Database(dbConfig.copy(url = urlWithDatabase))
    val cluster = DbCluster(db, None)
    sys.addShutdownHook(clearDatabase(databaseName, cluster))
    databases = (databaseName, cluster) :: databases
    cluster
  }

  private def readSqlFile(name: String) = {
    scala.io.Source
      .fromURL(getClass.getResource(name), "UTF-8")
      .getLines()
      .filter(s => s.trim.nonEmpty && !s.trim.startsWith("--"))
      .map(s => s.split("--").head)
      .mkString("\n")
      .split(";")
  }
}
