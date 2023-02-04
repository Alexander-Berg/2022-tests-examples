package ru.yandex.complaints.dao

import java.time.Duration

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import org.testcontainers.containers.MySQLContainer
import ru.yandex.complaints.dao.MySqlSpecTemplate._

import scala.io.Source
import scala.util.Random


/**
  * Utility for MySql specs
  *
  * @author alesavin
  */
trait MySqlSpecTemplate {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val schemaScript = "/initialize_db.sql"
  private def nextDatabaseName: String =  s"test_${Random.alphanumeric.take(5).mkString}"

  protected def schemaStatements: Seq[String] = {
    val data = Source.fromInputStream(getClass.getResourceAsStream(schemaScript)).mkString
    val statements = data.split(";")
    statements.map(_.trim).filterNot(_.startsWith("--")).filter(_.nonEmpty).map(s => s + ";")
  }

  lazy val mySql: MySql = {
    val RootDb = new MySql(serviceName, getConfig(RootUser, RootPassword))

    val databaseName = nextDatabaseName
    logger.info(s"Create database $databaseName")

    RootDb.shard.master.withTransaction {
      RootDb.shard.master.jdbc.update(s"CREATE DATABASE $databaseName")
      RootDb.shard.master.jdbc.update(s"GRANT ALL ON $databaseName.* TO $User")
      RootDb.shard.master.jdbc.update(s"USE $databaseName")
      schemaStatements.foreach(RootDb.shard.master.jdbc.update(_))
    }
    Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
      def run() {
        logger.info(s"Drop database $databaseName")
        try {
          RootDb.shard.master.jdbc.update(s"DROP DATABASE $databaseName")
        }
        catch {
          case e: Exception =>
            logger.error("Error while drop database", e)
        }
      }
    }, s"$databaseName-terminator"))

    new MySql(serviceName, getConfig(User, Password, Some(databaseName)))
  }
}

object MySqlSpecTemplate {

  private val serviceName = "testService"

  private val RootUser = "root"
  private val RootPassword = "test"

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

  private def getUrl(dbName: String): String =
    MySqlContainer.getJdbcUrl.replace(MySqlContainer.getDatabaseName, dbName)

  private def getUrl(dbName: Option[String]): String =
    dbName.map(getUrl).getOrElse(MySqlContainer.getJdbcUrl)

  private def getConfig(userName: String, password: String, dbName: Option[String] = None): Config = {
    ConfigFactory.parseString(
      s"""
         |   default-batch-size = 100
         |   master.url = "${getUrl(dbName)}?useUnicode=true&characterEncoding=utf8"
         |   slave.url = "${getUrl(dbName)}?useUnicode=true&characterEncoding=utf8"
         |   username = "$userName"
         |   password = "$password"
       """.stripMargin)
  }
}
