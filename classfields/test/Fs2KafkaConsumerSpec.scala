package ru.yandex.vertis.vsquality.utils.kafka_utils

import cats.Monad
import fs2.kafka.{AutoOffsetReset, ConsumerRecord, ConsumerSettings}
import ru.yandex.vertis.vsquality.utils.kafka_utils.ConsumerSpecBase.{Key, Value}

import scala.concurrent.duration._

/**
  * @author potseluev
  */
class Fs2KafkaConsumerSpec extends ConsumerSpecBase {

  override protected def createConsumer(
      topic: String,
      consumerGroupId: String,
      filter: ConsumerRecord[Key, Value] => Boolean,
      process: ConsumerRecord[Key, Value] => F[Unit]): Consumer[F, Key, Value] = {
    val action =
      ConsumerAction(
        filter.andThen(Monad[F].pure),
        process
      )
    val consumerSettings: ConsumerSettings[F, Key, Value] =
      ConsumerSettingsFactory[F, Key, Value](this.connectionString)
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
    new Fs2KafkaConsumer[F, Key, Value](
      topic,
      consumerGroupId,
      consumerSettings,
      action,
      commitOffsetsStep = 1,
      retryDelayDuration = Duration.Zero
    )
  }
}
