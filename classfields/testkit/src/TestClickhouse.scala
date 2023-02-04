package common.zio.clients.clickhouse.testkit

import common.testcontainers.zookeeper.ZookeeperContainer
import common.zio.files.ZFiles
import org.testcontainers.containers.ClickHouseContainer
import org.testcontainers.utility.{DockerImageName, MountableFile}
import zio.{Has, IO, TaskManaged, ULayer, UManaged, ZIO, ZManaged}

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import scala.io.Source

object TestClickhouse {
  private val NAME = "yandex/clickhouse-server"
  private val TAG = "21.8"
  private val zkPort = 2181

  val managedContainer: UManaged[ClickHouseContainer] = {
    val container = for {
      zkContainer <- ZManaged.succeed(ZookeeperContainer.createZkContainer(zkPort))
      _ <- ZManaged.makeEffect_(zkContainer.start())(zkContainer.stop())
      clickhouseContainer <- buildContainer(zkContainer.internalIpAddress, zkPort)
      _ <- ZManaged.makeEffect_(clickhouseContainer.start())(clickhouseContainer.stop())
    } yield clickhouseContainer

    container.orDie
  }

  val live: ULayer[Has[ClickHouseContainer]] =
    managedContainer.toLayer

  private def buildContainer(zkHost: String, zkPort: Int): TaskManaged[ClickHouseContainer] =
    for {
      config <- prepareConfig(zkHost, zkPort)
      container <- ZManaged.succeed(setupContainer(config))
    } yield container

  private def setupContainer(config: File): ClickHouseContainer = {
    val clickHouseContainer = new ClickHouseContainer(DockerImageName.parse(NAME).withTag(TAG)) {
      override def getDriverClassName: String = "ru.yandex.clickhouse.ClickHouseDriver"
    }
    clickHouseContainer.withCopyFileToContainer(
      MountableFile.forHostPath(config.getPath),
      "/etc/clickhouse-server/config.d/config.xml"
    )
    clickHouseContainer
  }

  private def prepareConfig(zkHost: String, zkPort: Int): TaskManaged[File] =
    makeConfigFromTemplate(zkHost, zkPort)
      .use(config => ZIO.succeed(writeToTempFile(config)))
      .toManaged_
      .flatten

  private def makeConfigFromTemplate(zkHost: String, zkPort: Int): TaskManaged[String] = {
    for {
      template <- ZManaged.fromAutoCloseable(IO(Source.fromResource("template_config.xml")))
      config = template
        .getLines()
        .mkString
        .replace("{ZK_HOST}", zkHost)
        .replace("{ZK_PORT}", zkPort.toString)
    } yield config
  }

  private def writeToTempFile(config: String): TaskManaged[File] = {
    for {
      configFile <- ZFiles.makeTempFile(
        "clickhouse_config",
        "xml",
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--"))
      )
      _ <- ZManaged.effect(Files.writeString(configFile.toPath, config))
    } yield configFile
  }

}
