package ru.yandex.vertis.telepony.util

import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

object KafkaTestContainer {

  def getBootstrapServers: String = Container.getBootstrapServers

  // see https://docs.confluent.io/current/installation/versions-interoperability.html#cp-and-apache-ak-compatibility
  private lazy val Container = {
    val imageName = DockerImageName.parse("confluentinc/cp-kafka")
    val c = new KafkaContainer(imageName)
    c.start()
    sys.addShutdownHook {
      c.close()
    }
    c
  }

}
