package ru.yandex.vertis.telepony.tools

import java.time.Duration
import java.util.Collections

import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals
import ru.yandex.vertis.telepony.journal.kafka.KafkaJournals.ConsumerGroup
import ru.yandex.vertis.telepony.model.proto

object KafkaConsumerApp extends Tool {

  val consumer = environment.CE.consumerSettings(ConsumerGroup("TeleponyKafkaConsumerApp")).createKafkaConsumer()
  consumer.subscribe(Collections.singleton(KafkaJournals.TeleponyCallChanges.name))

  val records = consumer.poll(Duration.ofSeconds(10))

  records.iterator().forEachRemaining { record =>
    val call = proto.TeleponyCall.parseFrom(record.value())
    println(call)
  }

}
