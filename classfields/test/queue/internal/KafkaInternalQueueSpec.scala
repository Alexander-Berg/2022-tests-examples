package amogus.logic.queue.internal

import amogus.logic.queue.internal.kafka.KafkaInternalQueue.KafkaQueueAmoRecord
import amogus.logic.queue.internal.kafka.{KafkaInternalQueueReader, KafkaInternalQueueWriter}
import common.zio.kafka.testkit.TestKafka
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import ru.yandex.vertis.amogus.amo_request.AmoRequest
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.consumer.Consumer.{AutoOffsetStrategy, OffsetRetrieval}
import zio.kafka.consumer.{Consumer, ConsumerSettings}
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.magic._
import zio.test.Assertion.{equalTo, isNone, isUnit}
import zio.test.environment.TestEnvironment
import zio.test.{assert, DefaultRunnableSpec, ZSpec}
import zio.{Has, Schedule, ZLayer}

object KafkaInternalQueueSpec extends DefaultRunnableSpec {
  private val topic = "queue-topic"

  private val defaultTimeout = 30.seconds

  private val kafkaProducer = (for {
    producerSettings <- TestKafka.bootstrapServers.map { servers =>
      ProducerSettings(servers)
        .withCloseTimeout(defaultTimeout)
        .withProperties(Map.empty[String, AnyRef])
    }.toManaged_

    producer <- Producer.make(producerSettings).orDie
  } yield producer).toLayer

  private val kafkaConsumer = (for {
    consumerSetting <- TestKafka.bootstrapServers.map { servers =>
      ConsumerSettings(servers)
        .withCloseTimeout(defaultTimeout)
        .withGroupId("test")
        .withOffsetRetrieval(OffsetRetrieval.Auto(AutoOffsetStrategy.Earliest))
    }.toManaged_

    consumer <- Consumer.make(consumerSetting).orDie
  } yield consumer).toLayer

  private type Env = Has[InternalQueueReader[KafkaQueueAmoRecord]]
    with Has[InternalQueueWriter[AmoRequest]]
    with Clock
    with Blocking
    with Has[Consumer]

  private val kafkaEnv = ZLayer
    .fromSomeMagic[TestEnvironment, Env](
      TestKafka.live,
      kafkaProducer,
      kafkaConsumer,
      ZLayer.succeed(topic),
      KafkaInternalQueueReader.layer,
      KafkaInternalQueueWriter.layer
    )

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("KafkaInternalQueue")(
      testM("successfully works with proper data") {
        val testRequest = AmoRequest.defaultInstance
        val topicPartition = new TopicPartition(topic, 0)

        for {
          additionResult <- KafkaInternalQueueWriter(_.addToQueue(testRequest))
          offsetBeforeReading <- Consumer.committed(Set(topicPartition)).map(_.get(topicPartition).flatten)

          streamResult <- KafkaInternalQueueReader(_.getQueueAsStream.take(1).runCollect)

          _ <- streamResult.head.offset.commitOrRetry(Schedule.spaced(5.second)) // now it's happening in the limiter
          offsetAfterReading <- Consumer.committed(Set(topicPartition)).map(_.get(topicPartition).flatten)

          firstRequest = streamResult.head.record.value()
        } yield assert(additionResult)(isUnit) &&
          assert(firstRequest)(equalTo(testRequest)) &&
          assert(offsetBeforeReading)(isNone) &&
          assert(offsetAfterReading)(equalTo(Some(new OffsetAndMetadata(1))))
      }
    ).provideLayer(kafkaEnv).provideCustomLayerShared(Clock.live)
  }
}
