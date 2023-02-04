package ru.yandex.vertis.clustering.kafka

import java.time.temporal.ChronoUnit
import java.time.{Instant, ZonedDateTime}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.config.KafkaTopicConfig
import ru.yandex.vertis.clustering.kafka.Consumer.{Offsets, OffsetsByTimestamp}
import ru.yandex.vertis.clustering.kafka.impl.KafkaConsumerBase
import ru.yandex.vertis.clustering.kafka.impl.KafkaConsumerBase.KafkaConsumerConfig
import ru.yandex.vertis.clustering.utils.DateTimeUtils
import ru.yandex.vertis.events.Event
import ru.yandex.vertis.kafka.util.CommittableConsumerRecord

import scala.concurrent.ExecutionContextExecutor
import scala.language.reflectiveCalls
import scala.util.Try

/**
  * Spec for [SourceFactory.stream]
  *
  * @author devreggs
  */
@Ignore
@RunWith(classOf[JUnitRunner])
class VertisEventsConsumerSpec extends BaseSpec {

  implicit val actorSystem: ActorSystem =
    ActorSystem("vertis-events-graph-test")
  implicit val materializer: ActorMaterializer =
    ActorMaterializer()
  implicit val ec: ExecutionContextExecutor =
    actorSystem.dispatcher

  private val vertisEventsConfig = new KafkaTopicConfig {
    override def connectionString: String =
      "kafka-01-man.test.vertis.yandex.net:9092," +
        "kafka-01-myt.test.vertis.yandex.net:9092," +
        "kafka-01-sas.test.vertis.yandex.net:9092"

    override def topic: String = "vertis-event-realty"
  }

  private val vertisEventsConsumerConfig = KafkaConsumerConfig(
    vertisEventsConfig,
    "user-clustering-testing",
    new org.apache.kafka.common.serialization.StringDeserializer,
    EventDeserializers.VertisEventsDeserializer)

  private def zdt(r: Event) = {
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(r.getTimestamp.getSeconds), DateTimeUtils.DefaultZoneId)
  }

  private val consumer = new KafkaConsumerBase[String, Event](vertisEventsConsumerConfig) {

    val name = "testing-consumer"

    var messages: List[Event] = Nil

    override def offsetsBeforeStarts: Option[Offsets] =
      Some(OffsetsByTimestamp(DateTimeUtils.now.minus(2, ChronoUnit.DAYS).toInstant))

    def consume(k: String, v: Event): Try[Unit] = Try {
      if (messages.size < 40) {
        messages ::= v
      }
      if (messages.size == 20) throw new IllegalArgumentException("Should restart")
    }

    override def consume(data: Iterable[CommittableConsumerRecord[String, Event]]): Try[Unit] = Try {
      data.foreach { rec =>
        consume(rec.key, rec.value).get
      }
    }

  }

  consumer.run

  while (consumer.messages.size < 40) ()

  "SourceFactory.create" should {
    "correctly commit offset 2 days ago" in {

      consumer.messages.map(zdt).foreach { zdt =>
        scala.math.abs(ChronoUnit.MINUTES
          .between(zdt, DateTimeUtils.now.minus(2, ChronoUnit.DAYS))) should be < 2L
      }
    }

    "correctly restart failed stream" in {
      consumer.messages.size shouldBe 40
    }
  }
}
