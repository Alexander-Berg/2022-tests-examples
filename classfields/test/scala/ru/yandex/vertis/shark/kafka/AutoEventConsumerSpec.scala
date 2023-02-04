package ru.yandex.vertis.shark.kafka

import auto.events.model.{Event => ProtoAutoEvent, EventType => ProtoAutoEventType}
import io.github.embeddedkafka.EmbeddedKafka
import org.apache.kafka.common.serialization.{Deserializer, Serializer}
import ru.yandex.vertis.shark.kafka.consumer.AutoEventConsumer
import ru.yandex.vertis.shark.kafka.producer.UserEventProducer
import ru.yandex.vertis.shark.proto.model.{UserEvent => ProtoUserEvent}
import ru.yandex.vertis.test_utils.assertions.Assertions.DiffSupport
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoFormatInstances._
import ru.yandex.vertis.zio_baker.scalapb_utils.ProtoSyntax.MessageWrites
import ru.yandex.vertis.zio_baker.zio.kafka.Testable.Default
import ru.yandex.vertis.zio_baker.zio.kafka.config.{ConsumerConfig, ProducerConfig}
import ru.yandex.vertis.zio_baker.zio.kafka.serde.{ScalaProtobufDeserializer, ScalaProtobufSerializer}
import ru.yandex.vertis.zio_baker.zio.kafka.{EmbeddedKafkaSpec, KafkaProducer, Testable}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test._

import java.time.Instant

object AutoEventConsumerSpec extends DefaultRunnableSpec with EmbeddedKafkaSpec with DiffSupport {

  private val consumerTopic: String = "broker-auto-events"
  private val consumerGroupId: String = "shark-auto-event"
  private val producerTopic: String = "shark-user-event"

  private val produceMessages = {
    val instant = Some(Instant.now.toProtoMessage)
    Seq(
      ProtoAutoEvent(
        eventId = "event-id-1",
        timestamp = instant,
        userId = "111",
        offerId = "offer-id-1",
        eventType = ProtoAutoEventType.CARD_VIEW
      ),
      ProtoAutoEvent(
        eventId = "event-id-2",
        timestamp = instant,
        userId = "222",
        offerId = "offer-id-2",
        eventType = ProtoAutoEventType.SEARCH
      ),
      ProtoAutoEvent(
        eventId = "event-id-3",
        timestamp = instant,
        userId = "333",
        offerId = "offer-id-3",
        eventType = ProtoAutoEventType.PHONE_CALL
      )
    )
  }

  private val resultDelay: Duration = 10.seconds

  type Testable = AutoEventConsumer with Default.Single[String, ProtoAutoEvent]
  type TestLayer = RLayer[Blocking, Blocking with Clock with KafkaUserEventProducer with Has[Testable]]

  private def create(
      consumerConfig: ConsumerConfig): ZIO[Blocking with Clock with KafkaUserEventProducer, Nothing, Testable] = {
    for {
      clockService <- ZIO.service[Clock.Service]
      blockingService <- ZIO.service[Blocking.Service]
      producer <- ZIO.service[KafkaProducer.Service[String, ProtoUserEvent]]
      ref <- Ref.make(Default.Context(Seq.empty[ProtoAutoEvent]))
    } yield new AutoEventConsumer(consumerConfig, clockService, blockingService, producer)
      with Testable.Default.Single[String, ProtoAutoEvent] {
      override protected def parallel: Int = 1
      override def context: Ref[Default.Context[Seq[ProtoAutoEvent]]] = ref
      override def complete: UIO[Option[Default.Context[Seq[ProtoAutoEvent]]]] = for {
        ctx <- context.get
      } yield Option.when(ctx.items.size >= produceMessages.size)(ctx)
    }
  }

  private def testLayer(port: Int): TestLayer = {
    val consumerLayer = {
      val config = ConsumerConfig(consumerTopic, consumerGroupId, bootstrap(port))
      create(config).toLayer
    }
    val producerLayer = {
      val config = ProducerConfig(producerTopic, bootstrap(port))
      Blocking.any ++ ZLayer.succeed(config) >>> UserEventProducer.live
    }
    Blocking.any ++ Clock.live ++ producerLayer >+> consumerLayer
  }

  implicit val autoEventSerializer: Serializer[ProtoAutoEvent] = new ScalaProtobufSerializer[ProtoAutoEvent]
  implicit val userEventDeserializer: Deserializer[ProtoUserEvent] = new ScalaProtobufDeserializer[ProtoUserEvent]

  override def spec: ZSpec[TestEnvironment, Any] =
    withKafka(implicit cfg =>
      suite(s"AutoEventConsumer")(
        testM("read broker events and write as shark user event") {
          val expected = Seq("111", "222", "333", "auto_111", "auto_333")

          (for {
            consumer <- ZIO.service[Testable]
            _ = EmbeddedKafka.createCustomTopic(consumerTopic)
            _ = produceMessages.foreach(EmbeddedKafka.publishToKafka(consumerTopic, _))
            consumerResult <- consumer.runAwait(resultDelay)
            producerResult = EmbeddedKafka.consumeNumberMessagesFrom(producerTopic, 2)
          } yield {
            val consumerIds = consumerResult.toSeq.flatMap(_.items).map(_.userId)
            val producerIds = producerResult.map(_.userId)
            val res = consumerIds ++ producerIds

            assert(consumerResult)(isSome) &&
            assert(res)(noDiff(expected))
          }).provideLayer(testLayer(cfg.kafkaPort))
        }
      )
    )
}
