package ru.yandex.vertis.telepony.component.impl

import org.apache.curator.framework.CuratorFramework
import com.dimafeng.testcontainers.{GenericContainer => ScalaGenericContainer}
import org.testcontainers.containers.wait.strategy.Wait
import ru.yandex.vertis.telepony.component.CuratorComponent
import ru.yandex.vertis.telepony.factory.CuratorFactory

import java.time.Duration

trait CuratorComponentTest extends CuratorComponent {
    import CuratorComponentTest._

    private def connectString: String = s"${zkContainer.containerIpAddress}:${zkContainer.mappedPort(zkPort)}"

    override def curator: CuratorFramework = CuratorFactory.newClient(connectString, "testing", None, None)
}

object CuratorComponentTest {

    private val zkPort = 33234

    private lazy val zkContainer: ScalaGenericContainer = {
        val c =
            ScalaGenericContainer(
                "confluentinc/cp-zookeeper:latest",
                exposedPorts = Seq(zkPort),
                env = Map("ZOOKEEPER_CLIENT_PORT" -> zkPort.toString),
                waitStrategy = Wait
                    .defaultWaitStrategy()
                    .withStartupTimeout(Duration.ofSeconds(20))
            )
        c.start()
        c
    }
}
