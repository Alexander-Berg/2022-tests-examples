package ru.yandex.vertis.telepony.tools

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.Materializer
import com.google.protobuf.ByteString
import org.apache.kafka.clients.producer.{ProducerConfig, ProducerRecord}
import org.joda.time.DateTime
import ru.yandex.vertis.telepony.component.impl.KafkaSupportComponentImpl.IntSerializer
import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals.{RawCallKafkaJournal, Telepony}
import ru.yandex.vertis.telepony.journal.serde.KafkaSerde
import ru.yandex.vertis.telepony.journal.serializer.RawCallSerDe
import ru.yandex.vertis.telepony.kafka.proto
import ru.yandex.vertis.telepony.kafka.proto.{KafkaMessage, OriginMessage}
import ru.yandex.vertis.telepony.model.RawCall.Origins
import ru.yandex.vertis.telepony.model.{CallResults, Operators, Phone, RawCall, RefinedSource, Source, TypedDomains}
import ru.yandex.vertis.telepony.server.env.ConfigHelper

import scala.util.Random
import scala.concurrent.duration._

/**
  * For testing purposes.
  * For generate raw calls.
  * @author neron
  */
object RawCallEmitterTool extends App {

  // where raw calls should be written
  val kafkaBootstrapServers = "neron-01-sas.dev.vertis.yandex.net:9092"

  val domain = TypedDomains.autoru_def

  implicit val actorSystem = ActorSystem("test", ConfigHelper.load(Seq("server.conf")))
  implicit val materializer = Materializer(actorSystem)

  val producerSettings: ProducerSettings[Integer, proto.KafkaMessage] =
    ProducerSettings(actorSystem, IntSerializer, KafkaSerde)
      .withBootstrapServers(kafkaBootstrapServers)
      .withProperty(ProducerConfig.ACKS_CONFIG, "all")

  val producer = producerSettings.createKafkaProducer()

  rawCalls.foreach { rawCall =>
    val value = rawCall

    val origin = OriginMessage
      .newBuilder()
      .setType(RawCallKafkaJournal.journalType)
      .setOccuredOn(System.currentTimeMillis())
      .setWhereFrom("whereFrom")

    origin.setName(domain.toString)

    val builder = KafkaMessage
      .newBuilder()
      .setOrigin(origin)
      .setPayload(ByteString.copyFrom(RawCallSerDe.serialize(value)))

    val record = new ProducerRecord(Telepony.name, null.asInstanceOf[Integer], builder.build())

    val result = producer.send(record).get()

    println(result)
  }

  // raw call provider
  def rawCalls: Iterable[RawCall] = {
    val proxyNumber = Phone("+79852587318")
    val target = Phone("+79819998877")

    val sourcePhones = Seq(Source("9817757577"))

    val rawCalls = sourcePhones.map { source =>
      RawCall(
        externalId = s"fake-gen-${Random.nextInt()}",
        source = Some(RefinedSource.from(source)),
        proxy = proxyNumber,
        target = Some(target),
        startTime = DateTime.now(),
        duration = 20.second,
        talkDuration = 0.second,
        recUrl = None,
        callResult = CallResults.NoAnswer,
        origin = Origins.Online,
        operator = Operators.Mts
      )
    }

    rawCalls
  }

}
