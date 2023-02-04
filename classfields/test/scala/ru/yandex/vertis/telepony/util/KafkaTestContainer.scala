package ru.yandex.vertis.telepony.util

import org.testcontainers.containers.KafkaContainer

object KafkaTestContainer {

  def getBootstrapServers: String = Container.getBootstrapServers

  // see https://docs.confluent.io/current/installation/versions-interoperability.html#cp-and-apache-ak-compatibility
  private lazy val Container = {
    val c = new KafkaContainer("5.3.1") // corresponds to Apache Kafka 2.3.x
    c.start()
    sys.addShutdownHook {
      c.close()
    }
    c
  }

}
