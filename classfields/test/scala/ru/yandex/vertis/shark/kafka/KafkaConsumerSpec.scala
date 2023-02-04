package ru.yandex.vertis.shark.kafka

import io.github.embeddedkafka.EmbeddedKafka
import ru.yandex.vertis.test_utils.assertions.Assertions.DiffSupport
import ru.yandex.vertis.zio_baker.zio.kafka.Testable.Default
import ru.yandex.vertis.zio_baker.zio.kafka.config.ConsumerConfig
import ru.yandex.vertis.zio_baker.zio.kafka.impl.DefaultKafkaConsumer
import ru.yandex.vertis.zio_baker.zio.kafka.{EmbeddedKafkaSpec, KafkaConsumer, Testable}
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.duration._
import zio.kafka.serde.Deserializer
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object KafkaConsumerSpec extends DefaultRunnableSpec with EmbeddedKafkaSpec with DiffSupport {

  class ExampleKafkaConsumer(val config: ConsumerConfig, val clock: Clock.Service, val blocking: Blocking.Service)
    extends DefaultKafkaConsumer[String, String] {
    override protected def keyDeserializer: Deserializer[Any, String] = Deserializer.string
    override protected def valueDeserializer: Deserializer[Any, String] = Deserializer.string
    override def process(items: Seq[String]): Task[Unit] = Task.unit
  }

  type SingleTestable = KafkaConsumer.Service[String, String] with Testable.Default.Single[String, String]
  type SingleTestLayer = URLayer[Blocking, Blocking with Clock with Has[SingleTestable]]

  type GroupedTestable = KafkaConsumer.Service[String, String] with Testable.Default.Grouped[String, String, Int]
  type GroupedTestLayer = URLayer[Blocking, Blocking with Clock with Has[GroupedTestable]]

  private val singleTopic: String = "single-test-topic"
  private val singleGroupId: String = "single-test-group"
  private val groupedTopic: String = "grouped-test-topic"
  private val groupedGroupId: String = "grouped-test-group"

  private val produceMessages = Seq(
    "1-test-message-1",
    "1-test-message-2",
    "2-test-message-3",
    "2-test-message-4",
    "3-test-message-5"
  )

  private val expectedGroupedResult = Map(
    1 -> Seq("1-test-message-1", "1-test-message-2"),
    2 -> Seq("2-test-message-3", "2-test-message-4"),
    3 -> Seq("3-test-message-5")
  )

  private val resultDelay: Duration = 10.seconds

  private def createSingle(
      consumerConfig: ConsumerConfig): ZIO[Clock with Blocking, Nothing, SingleTestable] = {
    for {
      clockService <- ZIO.service[Clock.Service]
      blockingService <- ZIO.service[Blocking.Service]
      ref <- Ref.make(Default.Context(Seq.empty[String]))
    } yield new ExampleKafkaConsumer(consumerConfig, clockService, blockingService)
      with Testable.Default.Single[String, String] {
      override def process(item: String): Task[Unit] = Task.unit
      override def context: Ref[Default.Context[Seq[String]]] = ref
      override def complete: UIO[Option[Default.Context[Seq[String]]]] = for {
        ctx <- context.get
      } yield Option.when(ctx.items.size >= produceMessages.size)(ctx)
    }
  }

  private def createGrouped(
      consumerConfig: ConsumerConfig): ZIO[Clock with Blocking, Nothing, GroupedTestable] = {
    for {
      clockService <- ZIO.service[Clock.Service]
      blockingService <- ZIO.service[Blocking.Service]
      ref <- Ref.make(Default.Context(Map.empty[Int, Seq[String]]))
    } yield new ExampleKafkaConsumer(consumerConfig, clockService, blockingService)
      with Testable.Default.Grouped[String, String, Int] {
      override protected def process(item: String): Task[Unit] = Task.unit
      override protected def groupKey(item: String): Int = item.take(1).toInt
      override def context: Ref[Default.Context[Map[Int, Seq[String]]]] = ref
      override def complete: UIO[Option[Default.Context[Map[Int, Seq[String]]]]] = for {
        ctx <- context.get
      } yield Option.when(ctx.items.map(_._2.size).sum >= produceMessages.size)(ctx)
    }
  }

  private def singleTestLayer(port: Int): SingleTestLayer = {
    val config = ConsumerConfig(singleTopic, singleGroupId, bootstrap(port))
    val consumerLayer = createSingle(config).toLayer
    Blocking.any ++ Clock.live >+> consumerLayer
  }

  private def groupedTestLayer(port: Int): GroupedTestLayer = {
    val config = ConsumerConfig(groupedTopic, groupedGroupId, bootstrap(port))
    val consumerLayer = createGrouped(config).toLayer
    Blocking.any ++ Clock.live >+> consumerLayer
  }

  override def spec: ZSpec[TestEnvironment, Any] =
    withKafka(implicit cfg =>
      suite(s"KafkaConsumer")(
        testM("single") {
          val res = for {
            consumer <- ZIO.service[SingleTestable]
            _ = EmbeddedKafka.createCustomTopic(singleTopic)
            _ = produceMessages.foreach(EmbeddedKafka.publishStringMessageToKafka(singleTopic, _))
            res <- consumer.runAwait(resultDelay)
          } yield res.toSeq.flatMap(_.items)
          assertM(res)(noDiff(produceMessages)).provideLayer(singleTestLayer(cfg.kafkaPort))
        },
        testM("grouped") {
          val res = for {
            consumer <- ZIO.service[GroupedTestable]
            _ = EmbeddedKafka.createCustomTopic(groupedTopic)
            _ = produceMessages.foreach(EmbeddedKafka.publishStringMessageToKafka(groupedTopic, _))
            res <- consumer.runAwait(resultDelay)
          } yield res.map(_.items).getOrElse(Map.empty)
          assertM(res)(noDiff(expectedGroupedResult)).provideLayer(groupedTestLayer(cfg.kafkaPort))
        }
      )
    )
}
