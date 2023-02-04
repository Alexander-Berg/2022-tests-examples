package ru.auto.salesman.test.docker

import org.apache.kafka.clients.admin._
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

import java.util.Properties
import scala.collection.JavaConverters._

trait Kafka {

  val container: KafkaContainer = {
    val imageName =
      DockerImageName
        .parse("registry.yandex.net/vertis/cp-kafka")
        .asCompatibleSubstituteFor("confluentinc/cp-kafka")
    val container = new KafkaContainer(imageName)
    container.start()
    container
  }

  val admin: AdminClient = {
    val config = new Properties()
    config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers)
    AdminClient.create(config)
  }

  def getBootstrapServers: String = container.getBootstrapServers

  def createTopic(
      name: String,
      numPartitions: Int,
      replicationFactor: Short
  ): Unit = {
    val topic = new NewTopic(name, numPartitions, replicationFactor)
    admin
      .createTopics(List(topic).asJava)
      .all()
      .get()
  }

}

object Kafka extends Kafka
