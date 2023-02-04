package vertis.logbroker.client.test

import com.github.dockerjava.api.model.{ExposedPort, HostConfig, PortBinding, Ports}

import java.time
import java.util.{Optional, UUID}
import common.zio.logging.SyncLogger
import org.testcontainers.containers.wait.strategy.{Wait, WaitAllStrategy}
import org.testcontainers.containers.{GenericContainer, ToxiproxyContainer}
import org.testcontainers.images.PullPolicy
import org.testcontainers.utility.DockerImageName
import vertis.zio.logging.TestContainersLogging
import vertis.logbroker.client.model.LbErrors
import vertis.logbroker.client.test.LbServer.syncLogger
import zio.{Task, UIO}

import scala.jdk.CollectionConverters._
import scala.util.Try

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
class LbServer private (lbContainer: GenericContainer[_], toxiproxy: ToxiproxyContainer, lbPort: Int)
  extends AutoCloseable
  with LbErrors {

  private val lbContainerIp = lbContainer.getContainerInfo.getNetworkSettings.getNetworks
    .values()
    .asScala
    .headOption
    .map(_.getIpAddress)
    .getOrElse(throw new IllegalStateException("No network available to extract the internal IP from!"))

  def newTransportFactory(): Task[ToxicProxyTransportFactory] = UIO {
    new ToxicProxyTransportFactory(
      toxiproxy.getProxy(lbContainerIp, lbPort, Optional.of(UUID.randomUUID().toString))
    )
  }

  override def close(): Unit = {
    syncLogger.info("Stopping lb server")
    Try(toxiproxy.close()).failed.foreach(e => syncLogger.warn(s"Error stopping toxiproxy. $e"))
    Try(lbContainer.stop()).failed.foreach(e => syncLogger.warn(s"Error stopping lb container. $e"))
  }
}

object LbServer {

  private val lbVersion = "latest"
  private val syncLogger = SyncLogger[LbServer]

  private def availablePort: Int = {
    val socket = new java.net.ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }

  def create(topics: Set[String]): LbServer = {
    val lbPort = availablePort // minimize collision chance
    val container: GenericContainer[_] =
      new GenericContainer(s"registry.yandex.net/vertis/yandex-docker-local-lbk:$lbVersion")
        .withCreateContainerCmdModifier { cmd =>
          // fixed port mapping and localhost hostname is for ydb discovery service
          val _ = cmd
            .withHostName("localhost")
            .withHostConfig(
              new HostConfig()
                .withPortBindings(new PortBinding(Ports.Binding.bindPort(lbPort), new ExposedPort(lbPort)))
            )
        }
    container.withImagePullPolicy(PullPolicy.alwaysPull())
    container.withExposedPorts(lbPort)
    container.withStartupTimeout(time.Duration.ofSeconds(60))
    val topicsString =
      (topics + "test") // recipe checks `test` topic, suppress error logs https://a.yandex-team.ru/arc_vcs/kikimr/public/tools/lbk_recipe/__main__.py?rev=r9090790#L101
        .map(_.stripPrefix("/"))
        .mkString(",")
    container.withEnv("LOGBROKER_CREATE_TOPICS", topicsString)
    container.withEnv("GRPC_PORT", lbPort.toString)
    container.withEnv("YDB_DEFAULT_LOG_LEVEL", "INFO")
    container.withEnv("YDB_USE_IN_MEMORY_PDISKS", "true")
    container.withLogConsumer(TestContainersLogging.toLogConsumer(syncLogger))
    container.waitingFor(
      new WaitAllStrategy()
        .withStrategy(Wait.forHealthcheck())
        .withStrategy(Wait.forLogMessage(".*PQ initializing complete.*", 1))
    )
    container.start()

    syncLogger.info("Starting toxiproxy container")
    val imageName = DockerImageName.parse("shopify/toxiproxy")
    val toxiproxy = new ToxiproxyContainer(imageName)
      .withStartupTimeout(time.Duration.ofSeconds(30))
      .withLogConsumer(TestContainersLogging.toLogConsumer(syncLogger))
    toxiproxy.start()

    syncLogger.info(s"Running lb server with ${container.getMappedPort(lbPort)} port")

    val lbServer = new LbServer(container, toxiproxy, lbPort)

    lbServer
  }
}
