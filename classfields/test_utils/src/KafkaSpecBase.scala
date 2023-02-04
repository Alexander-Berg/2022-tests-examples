package ru.yandex.vertis.vsquality.utils.test_utils

import akka.actor.ActorSystem
import fs2.kafka.Headers
import io.github.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._

class KafkaSpecBase extends SpecBase with EmbeddedKafka {

  implicit protected val actorSystem: ActorSystem = ActorSystem("akka-service")

  implicit protected var kafkaConfig: EmbeddedKafkaConfig = _

  protected var connectionString: String = _

  protected val kafkaTopic: String = "kafka-topic"

  override def beforeAll(): Unit = {
    val actualConfig = EmbeddedKafka.start()(EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0)).config
    connectionString = s"127.0.0.1:${actualConfig.kafkaPort}"
    kafkaConfig = EmbeddedKafkaConfig(zooKeeperPort = actualConfig.zooKeeperPort, kafkaPort = actualConfig.kafkaPort)
    EmbeddedKafka
      .createCustomTopic(kafkaTopic)
      .getOrElse(fail(s"Could not create topic $kafkaTopic"))
  }

  override def afterAll(): Unit = {
    EmbeddedKafka.stop()
  }

}

object KafkaSpecBase {

  implicit def fromFs2Serializer[C[_]: Awaitable, T](
      implicit fs2Serializer: fs2.kafka.Serializer[C, T]): Serializer[T] = (topic: String, data: T) =>
    fs2Serializer.serialize(topic, Headers.empty, data).await

  implicit def fromFs2Deserializer[C[_]: Awaitable, T](
      implicit fs2Deserializer: fs2.kafka.Deserializer[C, T]): Deserializer[T] =
    (topic: String, data: Array[Byte]) => fs2Deserializer.deserialize(topic, Headers.empty, data).await
}
