package ru.yandex.vos2.reviews.utils

import java.io.Closeable
import java.sql.DriverManager

import com.google.common.io.Closer
import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vos2.util.TimeWatcher
import ru.yandex.vos2.util.environment.EnvProvider
import ru.yandex.vos2.util.log.Logging

import scala.sys.process._


/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 05/10/2017.
  */
object DockerReviewCoreComponentsBuilder extends Logging {
  val containerName = "vos2_reviews"

  val dbMapping = Map(
    containerName -> List("vos2_reviews")
  )

  val dockerImageName = "percona:5.7.15"

  private val closer = Closer.create()

  private val addressRegex = "([\\d.]+):([\\d]*):::([\\d]*)".r

  def registerToClose(c: Closeable): Unit = {
    closer.register(c)
  }

  def stopAll() {
    closer.close()
  }

  private def containerStatus(containerName: String): (Boolean, Boolean) = {
    val result = "docker ps -a".!!
    val lines = result.split("\n")
    val columns = lines.head
    val columnOffsets = Map(
      "NAMES" -> columns.indexOf("NAMES"),
      "STATUS" -> columns.indexOf("STATUS")
    )
    val containersMap = lines.drop(1).filterNot(_.trim.isEmpty).map(line => {
      val name = line.drop(columnOffsets("NAMES")).trim
      val status = line.slice(columnOffsets("STATUS"), columnOffsets("NAMES")).trim
      (name, status)
    }).toMap

    val created = containersMap.contains(containerName)
    val running = created && containersMap(containerName).startsWith("Up")

    (created, running)
  }

  private def containerUrl(containerName: String): String = {
    val mapped = s"docker port $containerName 3306".!!.replace("\n", "")
    mapped match {
      case addressRegex(host, _, port) =>
        s"jdbc:mysql://$host:$port"
      case _ =>
        s"jdbc:mysql://$mapped"
    }
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
        case _: Exception ⇒
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

  private def createAndStartContainer(containerName: String, dbNames: List[String]): String = {
    val cmd = "docker create " +
      "-p 3306 " +
      "--tmpfs /tmpfs:size=1000M " +
      s"-e MYSQL_DATABASE=${dbNames.head} " +
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

  private def checkStartOrCreate(containerName: String, dbNames: List[String]): String = {
    val (created, running) = containerStatus(containerName)
    val jdbcUrl = if (running) {
      containerUrl(containerName)
    } else if (created) {
      startContainer(containerName)
    } else createAndStartContainer(containerName, dbNames)

    log.info(s"$containerName jdbc url = $jdbcUrl")

    jdbcUrl
  }

  def createConfig: String = {
    val sqlTime = TimeWatcher.withNanos()
    val jdbcUrlVos2Reviews = checkStartOrCreate(containerName, dbMapping(containerName))
    log.info(s"schema creation: ${sqlTime.toMillis} ms.")

    s"""vos2.reviews {
       |
       |  mysql {
       |    vos {
       |    shards = [
       |      {
       |        master.url =  "$jdbcUrlVos2Reviews/vos2_reviews"
       |        slave.url =  "$jdbcUrlVos2Reviews/vos2_reviews"
       |        username = "root"
       |        password = "sqlsql"
       |      }
       |    ]
       |    default-batch-size = 100
       |    }
       |  }
       |
       |  comments {
       |      shards = [
       |        {
       |          master.url = "$jdbcUrlVos2Reviews/comments"
       |          slave.url = "$jdbcUrlVos2Reviews/comments"
       |          username = "auto"
       |          password = "KiX1euph"
       |        }
       |      ]
       |      default-batch-size = 100
       |    }
       |
       |}""".stripMargin
  }
}
