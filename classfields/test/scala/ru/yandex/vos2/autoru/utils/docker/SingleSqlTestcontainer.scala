package ru.yandex.vos2.autoru.utils.docker

import com.dimafeng.testcontainers.SingleContainer
import org.testcontainers.containers.wait.strategy.Wait
import ru.yandex.vos2.autoru.utils.docker.SingleSqlTestcontainer.ContainerConfig
import org.testcontainers.containers.{GenericContainer => OTCGenericContainer}

import scala.jdk.CollectionConverters._
import scala.language.existentials

class SingleSqlTestcontainer(config: ContainerConfig) extends SingleContainer[OTCGenericContainer[_]] {

  type OTCContainer = OTCGenericContainer[T] forSome {
    type T <: OTCGenericContainer[T]
  }

  override val container: OTCContainer = new OTCGenericContainer(config.imageName)

  container
    .withExposedPorts(config.exposedPort)
    .withEnv(config.env.asJava)
    .withTmpFs(config.tmpFs.asJava)

  container.withCommand(config.commands: _*)

  container.setWaitStrategy(Wait.forListeningPort())

  def jdbcUrl: String =
    "jdbc:mysql://" + container.getContainerIpAddress + ":" + container.getMappedPort(config.exposedPort)

  def jdbcUrlUseSSLFalse: String =
    "jdbc:mysql://" + container.getContainerIpAddress + ":" + container.getMappedPort(config.exposedPort) + "?useSSL=false"

}

object SingleSqlTestcontainer {

  case class ContainerConfig(imageName: String,
                             exposedPort: Int,
                             env: Map[String, String],
                             commands: Seq[String],
                             tmpFs: Map[String, String])

}
