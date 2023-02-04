package bootstrap.testcontainers.zookeeper

import bootstrap.testcontainers.LogConsumer
import org.testcontainers.containers.GenericContainer

class ZKTestContainer
    extends GenericContainer[ZKTestContainer](ZKContainer.Image) {
  addExposedPorts(2181, 2888, 3888, 8080)
  withLogConsumer(LogConsumer)

  def getConnectString: String = s"$getHost:2181"

}

object ZKTestContainer {}
