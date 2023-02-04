package vertis.zio.zk.test

import com.dimafeng.testcontainers.{GenericContainer => ScalaGenericContainer}
import common.zio.app.AppInfo
import common.zio.logging.SyncLogger
import common.zookeeper.Zookeeper
import org.apache.curator.framework.CuratorFramework
import org.testcontainers.containers.wait.strategy.Wait
import vertis.zio.logging.TestContainersLogging.toLogConsumer
import vertis.zio.test.ZioSpecBase
import vertis.zio.test.ZioSpecBase.TestBody
import vertis.zio.zk.test.ZkTest.{zkContainer, zkPort}
import zio.TaskManaged

import java.time.Duration

/** @author kusaeva
  */
trait ZkTest extends ZioSpecBase {

  val namespace: String = "namespace"

  protected def connectString: String = s"${zkContainer.containerIpAddress}:${zkContainer.mappedPort(zkPort)}"

  protected lazy val zkClient: TaskManaged[CuratorFramework] = {
    val config = Zookeeper.Config(connectString, namespace, None)
    Zookeeper.makeService(AppInfo(), config).flatMap(_.zkClientForGroup.toManaged_)
  }

  protected def zkTest(body: CuratorFramework => TestBody): Unit =
    ioTest {
      zkClient.use(body)
    }
}

object ZkTest {
  private val syncLogger = SyncLogger[ZkTest.type]

  /* same port as in docker-compose.yml */
  private val zkPort = 33234

  lazy val zkContainer: ScalaGenericContainer = {
    val c =
      ScalaGenericContainer(
        "confluentinc/cp-zookeeper:latest",
        exposedPorts = Seq(zkPort),
        env = Map("ZOOKEEPER_CLIENT_PORT" -> zkPort.toString),
        waitStrategy = Wait
          .defaultWaitStrategy()
          .withStartupTimeout(Duration.ofSeconds(20))
      )
    c.container.withLogConsumer(toLogConsumer(syncLogger))
    c.start()
    c
  }
}
