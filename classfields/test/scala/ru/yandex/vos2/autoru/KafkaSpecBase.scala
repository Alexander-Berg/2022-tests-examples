package ru.yandex.vos2.autoru

import akka.actor.ActorSystem
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.scalatest.{BeforeAndAfterAll, Suite}
import ru.yandex.vos2.autoru.KafkaSpecBase._

import scala.concurrent.duration._

trait KafkaSpecBase extends Suite with BeforeAndAfterAll with EmbeddedKafka {

  implicit protected val actorSystem: ActorSystem = ActorSystem("akka-service")

  private val kafkaPort: Int = EmbeddedKafkaConfig.defaultConfig.kafkaPort

  protected val connectionString: String = s"127.0.0.1:$kafkaPort"

  protected val topicName: String

  override protected def beforeAll(): Unit = {
    EmbeddedKafka.start()
    EmbeddedKafka.createCustomTopic(topicName)
    Thread.sleep(InitializationDelay.toMillis)
  }

  override protected def afterAll(): Unit = EmbeddedKafka.stop()
}

object KafkaSpecBase {
  protected val InitializationDelay: FiniteDuration = 1.second
}
