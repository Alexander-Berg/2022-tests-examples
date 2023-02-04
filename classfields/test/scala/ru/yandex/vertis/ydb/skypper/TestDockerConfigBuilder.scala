package ru.yandex.vertis.ydb.skypper

import java.io.Closeable

import com.google.common.io.Closer
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.ydb.skypper.Using._

import scala.concurrent.ExecutionContext
import scala.sys.process._

/**
  * Created by andrey on 11/8/17.
  */
object TestDockerConfigBuilder {
  private val dockerImageName = "registry.yandex.net/yandex-docker-local-ydb:stable"
  private val endpoint = "localhost:2135"
  private val database = "/local"

  implicit private val trace: Traced = Traced.empty

  private val closer = Closer.create()

  def registerToClose(c: Closeable): Unit = {
    closer.register(c)
  }

  def stopAll(): Unit = {
    //reaper.stopAll()
    closer.close()
  }

  private def readSqlFile(name: String) = {
    using(scala.io.Source.fromURL(getClass.getResource(name))) { source =>
      source
        .getLines()
        .filter(s => s.trim.nonEmpty && !s.trim.startsWith("--"))
        .mkString
        .split(";")
        .map(line => {
          "--!syntax_v1\n" + line
        })
    }
  }

  private def prepareDb(schemaFilename: String)(implicit ec: ExecutionContext): YdbWrapper = {
    val ydb = YdbWrapper("test", endpoint, database, "")(ec)
    println(s"Using schema from $schemaFilename")
    val schemaFile = readSqlFile(schemaFilename)
    awaitConnection(ydb)
    ydb.rawExecute("ddl") { session =>
      schemaFile.foreach(sql => {
        val status = session.executeSchemeQuery(sql).join()
        if (!status.isSuccess) System.err.println(s"$status\nwhen executing SQL statement:\n$sql")
      })
    }
    ydb
  }

  private def awaitConnection(ydb: YdbWrapper): Unit = {
    if (!ydb.isConnected()) {
      println("awaiting connection...")
      Thread.sleep(5000)
      awaitConnection(ydb)
    } else {
      println("connected")
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

  private def startContainer(containerName: String): Unit = {
    s"docker start $containerName".!!
  }

  private def createAndStartContainer(containerName: String): Unit = {
    val cmd = "docker create " +
      "--hostname localhost " +
      "-p 2135:2135 " +
      s"--name $containerName " +
      s"$dockerImageName"

    cmd.!!

    s"docker start $containerName".!!
  }

  def checkStartOrCreate(containerName: String, schemaFilename: String)(implicit ec: ExecutionContext): YdbWrapper = {
    val (created, running) = containerStatus(containerName)
    if (!running) {
      if (created) startContainer(containerName)
      else createAndStartContainer(containerName)
    }

    println(s"$containerName jdbc url = $endpoint")

    prepareDb(schemaFilename)
  }
}
