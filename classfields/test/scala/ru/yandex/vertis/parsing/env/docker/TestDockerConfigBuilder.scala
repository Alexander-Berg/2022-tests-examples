package ru.yandex.vertis.parsing.env.docker

import java.io.Closeable
import java.sql.DriverManager

import com.google.common.io.Closer
import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.parsing.env.EnvProvider
import ru.yandex.vertis.parsing.util.TimeWatcher
import ru.yandex.vertis.parsing.util.logging.Logging

import scala.sys.process._

/**
  * Created by andrey on 11/8/17.
  */
object TestDockerConfigBuilder extends Logging {
  val dockerImageName = "percona:5.7.15"

  private val closer = Closer.create()

  def registerToClose(c: Closeable): Unit = {
    closer.register(c)
  }

  def stopAll() {
    //reaper.stopAll()
    closer.close()
  }

  private def readSqlFile(name: String) = {
    scala.io.Source
      .fromURL(getClass.getResource(name))
      .getLines
      .filter(s => s.trim.nonEmpty && !s.trim.startsWith("--"))
      .mkString
      .split(";")
  }

  private def prepareDb(jdbcUrl: String, containerName: String): Unit = {
    val connection = DriverManager.getConnection(jdbcUrl, "root", "sqlsql")
    def executeSql(sql: String): Unit = {
      val ps = connection.prepareStatement(sql)
      try {
        ps.execute()
      } finally {
        ps.close()
      }
    }

    try {
      executeSql(s"create database if not exists `$containerName`")
      val schemaFilename = "/schema_base.sql"
      log.info(s"Using schema from $schemaFilename")
      val autoruSchemaFile = readSqlFile(schemaFilename)
      executeSql("SET FOREIGN_KEY_CHECKS=0;")
      autoruSchemaFile.foreach(sql => {
        //log.info(sql)
        executeSql(sql)
      })
      executeSql("SET FOREIGN_KEY_CHECKS=1;")
    } finally {
      connection.close()
    }
  }

  private def containerStatus(containerName: String): (Boolean, Boolean) = {
    val result = "docker ps -a".!!
    val lines = result.split("\n")
    val columns = lines.head
    val columnOffsets = Map(
      "NAMES" -> columns.indexOf("NAMES"),
      "STATUS" -> columns.indexOf("STATUS")
    )
    val containersMap = lines
      .drop(1)
      .filterNot(_.trim.isEmpty)
      .map(line => {
        val name = line.drop(columnOffsets("NAMES")).trim
        val status = line.slice(columnOffsets("STATUS"), columnOffsets("NAMES")).trim
        (name, status)
      })
      .toMap

    val created = containersMap.contains(containerName)
    val running = created && containersMap(containerName).startsWith("Up")

    (created, running)
  }

  private def containerUrl(containerName: String): String = {
    val mapped = s"docker port $containerName 3306".!!.split("\n", 2).head
      .replace("0.0.0.0", "127.0.0.1")
    s"jdbc:mysql://$mapped/$containerName?useSSL=false"
  }

  private def waitConnection(jdbcUrl: String): Unit = {
    Class.forName("com.mysql.jdbc.Driver")
    var connected = false
    val start = System.currentTimeMillis()
    while (!connected) {
      if (System.currentTimeMillis() - start > 60000) {
        sys.error("Failed to connect to mysql")
      }
      try {
        log.info(s"Trying to connect to $jdbcUrl")
        val connection = DriverManager.getConnection(jdbcUrl, "root", "sqlsql")
        connection.close()
        connected = true
      } catch {
        case _: Exception =>
          log.info("Failed to connect. Retrying...")
          Thread.sleep(1000)
      }
    }
  }

  private def startContainer(containerName: String): String = {
    s"docker start $containerName".!!
    val jdbcUrl = containerUrl(containerName)
    waitConnection(jdbcUrl)
    jdbcUrl
  }

  private def createAndStartContainer(containerName: String): String = {
    val cmd = "docker create " +
      "-p 3306 " +
      "--tmpfs /tmpfs:size=1000M " +
      s"-e MYSQL_DATABASE=$containerName " +
      "-e MYSQL_USER=vos " +
      "-e MYSQL_PASSWORD=sqlsql " +
      "-e MYSQL_ROOT_PASSWORD=sqlsql " +
      s"--name $containerName " +
      s"$dockerImageName --datadir=/tmpfs --character-set-server=utf8mb4 --sql-mode=NO_ENGINE_SUBSTITUTION"

    cmd.!!

    s"docker start $containerName".!!
    val jdbcUrl = containerUrl(containerName)
    waitConnection(jdbcUrl)
    jdbcUrl
  }

  private def checkStartOrCreate(containerName: String): String = {
    val (created, running) = containerStatus(containerName)
    val jdbcUrl = if (running) {
      containerUrl(containerName)
    } else if (created) {
      startContainer(containerName)
    } else createAndStartContainer(containerName)

    log.info(s"$containerName jdbc url = $jdbcUrl")

    prepareDb(jdbcUrl, containerName)

    jdbcUrl
  }

  //scalastyle:off method.length
  def createConfig(containerName: String, node: String): String = {
    val sqlTime = TimeWatcher.withNanos()
    val jdbcUrlParsing = checkStartOrCreate(containerName)
    log.info(s"schema creation: ${sqlTime.toMillis} ms.")

    s"""$node {
        |  driverClass = "com.mysql.jdbc.Driver"
        |
        |   master.url = "$jdbcUrlParsing"
        |   slave.url = "$jdbcUrlParsing"
        |
        |  username = "root"
        |  password = "sqlsql"
        |}""".stripMargin
  }

  def createEnvProvider(containerName: String, node: String): EnvProvider = {
    val config = createConfig(containerName, node)

    new EnvProvider {
      override def environmentType: Environments.Value = Environments.Development

      override val props: Config = {
        def load(resource: String): Config = {
          ConfigFactory.parseResources(resource)
        }
        ConfigFactory.load(
          ConfigFactory
            .parseString(config)
            .withFallback(load("test.local.conf"))
            .withFallback(load("src/test/resources/test.conf"))
            .withFallback(load("properties.testing.conf"))
            .withFallback(load("properties.development.conf"))
            .withFallback(load("properties.conf"))
            .withFallback(load("application.conf"))
        )
      }

      override def dataCenter: String = "undefined"
    }
  }
}
