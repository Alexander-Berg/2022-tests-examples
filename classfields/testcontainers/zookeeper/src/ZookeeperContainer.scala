package common.testcontainers.zookeeper

import com.dimafeng.testcontainers.GenericContainer
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import scala.jdk.CollectionConverters._
import java.time.Duration

/** @author kusaeva
  */
class ZookeeperContainer(port: Int, underlying: GenericContainer) extends GenericContainer(underlying) {

  container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(getClass)))

  def internalIpAddress: String = underlyingUnsafeContainer.getContainerInfo.getNetworkSettings.getNetworks
    .values()
    .asScala
    .headOption
    .map(_.getIpAddress)
    .getOrElse(throw new IllegalStateException("No network available to extract the internal IP from!"))

  def connectString: String = s"$containerIpAddress:${mappedPort(port)}"
}

object ZookeeperContainer {

  case class Def(port: Int) extends GenericContainer.Def[ZookeeperContainer](createZkContainer(port))

  def createZkContainer(port: Int): ZookeeperContainer = {
    new ZookeeperContainer(
      port,
      GenericContainer(
        "confluentinc/cp-zookeeper:latest",
        exposedPorts = Seq(port),
        env = Map("ZOOKEEPER_CLIENT_PORT" -> port.toString),
        waitStrategy = Wait
          .defaultWaitStrategy()
          .withStartupTimeout(Duration.ofSeconds(20))
      )
    )
  }
}
