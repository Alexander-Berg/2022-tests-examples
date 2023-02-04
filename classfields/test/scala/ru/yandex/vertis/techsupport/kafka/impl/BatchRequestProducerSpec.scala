package ru.yandex.vertis.vsquality.techsupport.kafka.impl

import cats.effect.IO
import io.github.embeddedkafka.EmbeddedKafka
import ru.yandex.vertis.vsquality.techsupport.config.KafkaConfig
import ru.yandex.vertis.vsquality.techsupport.model.BatchRequest
import ru.yandex.vertis.vsquality.utils.test_utils.KafkaSpecBase
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
import ru.yandex.vertis.vsquality.techsupport.conversion.proto.ProtoFormatInstances._
import KafkaSerDe._

class BatchRequestProducerSpec extends KafkaSpecBase {

  private lazy val config: KafkaConfig = KafkaConfig(connectionString, kafkaTopic, "all")
  private lazy val producer = new BatchRequestKafkaProducer[IO](config)

  "RequestKafkaProducer" should {
    "append" in {
      val expectedMsg = generate[BatchRequest]()
      producer.append(expectedMsg).await
      val consumedMsg = EmbeddedKafka.consumeNumberMessagesFrom[BatchRequest](kafkaTopic, 1)
      consumedMsg shouldBe Seq(expectedMsg)
    }
  }
}
