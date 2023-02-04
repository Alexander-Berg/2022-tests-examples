package ru.auto.cabinet.util

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.scalatest.flatspec.{AnyFlatSpec => FlatSpec}
import org.scalatest.matchers.should.Matchers
import ru.auto.cabinet.kafka.{KafkaReader, MeteredKafkaReader}
import ru.auto.cabinet.metrics.BackendMetrics
import ru.yandex.vertis.ops.prometheus.{
  CompositeCollector,
  OperationalAwareRegistry
}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class DummyReader extends KafkaReader[String] {

  override def subscribe: Future[Unit] = Future.unit

  override def unsubscribe: Future[Unit] = Future.unit

  override def startReading(process: String => Future[Unit]): Future[Unit] =
    Future.unit

  override def shutdown(): Unit = ()

  override protected def processBatch(
      batch: Iterable[ConsumerRecord[String, String]],
      process: String => Future[Unit]): Future[Offsets] =
    Future
      .traverse(batch.map(_.value()).toList)(process)
      .map(_ => lastOffsets(batch))

  override def commit(offsets: Offsets): Future[Unit] =
    Future.unit

  def produceMessage(messages: String*): Future[Unit] = {
    val records = messages.zipWithIndex.map { case (m, i) =>
      new ConsumerRecord[String, String]("testTopic", 0, i, m, m)
    }
    processBatch(records, _ => Future.unit).flatMap(commit)
  }
}

class KafkaMetricsSpec extends FlatSpec with Matchers {

  val registry = new OperationalAwareRegistry(
    new CompositeCollector,
    "testTopic",
    "local",
    "localhost",
    "")
  val backendMetrics = BackendMetrics()

  BackendMetrics.register(registry, backendMetrics)

  val meteredReader = new DummyReader with MeteredKafkaReader[String] {

    override def metrics: BackendMetrics.KafkaReaderMetrics =
      backendMetrics.kafkaReaderMetrics
    implicit override def ec: ExecutionContext = ExecutionContext.global

    override def topic: String = "test"
  }

  "metered reader" should "write all metrics" in {
    meteredReader.produceMessage("hello", "kitty").onComplete {
      case Success(_) =>
        if (registry
            .asCollectorRegistry()
            .getSampleValue(
              "kafka_consumer_offset",
              Array("topic", "partition", "env", "dc", "instance"),
              Array(
                "testTopic",
                "testTopic-0",
                "testTopic",
                "local",
                "localhost")) == null)
          fail("metrics insertion failed")
      case Failure(_) => fail("metrics writing failed")
    }
  }

}
