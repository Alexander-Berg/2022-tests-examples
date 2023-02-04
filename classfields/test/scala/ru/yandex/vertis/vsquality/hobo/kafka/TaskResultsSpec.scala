package ru.yandex.vertis.vsquality.hobo.kafka

import akka.kafka.ProducerSettings
import io.github.embeddedkafka.EmbeddedKafka
import org.apache.kafka.common.serialization.StringSerializer
import ru.yandex.vertis.vsquality.hobo.kafka.taskresults.{TaskResultSerializerDeserializer, TaskResultsProducer}
import ru.yandex.vertis.vsquality.hobo.model.{KafkaResponse, Task}
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators.{NotificationInfoGen, TaskGen}
import ru.yandex.vertis.vsquality.hobo.model.generators.Producer._
import ru.yandex.vertis.vsquality.utils.test_utils.KafkaSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

class TaskResultsSpec extends KafkaSpecBase {

  implicit val taskResultsSerDeser = new TaskResultSerializerDeserializer

  lazy val settings: ProducerSettings[String, Task] =
    ProducerSettings(actorSystem, new StringSerializer, taskResultsSerDeser)
      .withBootstrapServers(connectionString)

  lazy val taskResultsProducer = new TaskResultsProducer(settings) {
    override def topic: String = kafkaTopic
  }

  "TaskResultsProducer" should {
    "send to kafka correctly" in {

      val notificationInfo = NotificationInfoGen.next.copy(response = Some(KafkaResponse))
      val task: Task = TaskGen.next.copy(notificationInfo = notificationInfo)

      taskResultsProducer.append(task)

      val consumedMsg = EmbeddedKafka.consumeNumberMessagesFrom[Task](kafkaTopic, 1)
      consumedMsg shouldBe Seq(task)
    }
  }
}
