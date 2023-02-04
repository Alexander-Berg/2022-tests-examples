package ru.yandex.vertis.vsquality.utils.kafka_utils

import cats.effect.Sync
import fs2.kafka.{AutoOffsetReset, ConsumerRecord, ConsumerSettings}
import io.github.embeddedkafka.EmbeddedKafka
import org.scalacheck.Arbitrary
import ru.yandex.vertis.vsquality.utils.cats_utils.Executable._
import ru.yandex.vertis.vsquality.utils.test_utils.KafkaSpecBase

import scala.collection.mutable

/**
  * @author potseluev
  */
trait ConsumerSpecBase extends KafkaSpecBase {

  import ConsumerSpecBase._
  import KafkaSpecBase._
  import Arbitraries._

  implicit private val stringArb: Arbitrary[String] = alphaNumStr

  protected def createConsumer(
      topic: String,
      consumerGroupId: String = generate[String](),
      filter: ConsumerRecord[Key, Value] => Boolean,
      process: ConsumerRecord[Key, Value] => F[Unit]
    ): Consumer[F, Key, Value]

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "filter correctly",
        data = 1 to 4,
        isSuccess = _ => true,
        filter = _ % 2 == 0,
        expectedData = Seq(2, 4)
      ),
      TestCase(
        description = "eventually consume all records even in case of failure",
        data = 1 to 4,
        isSuccess = _ => generate[Boolean](),
        filter = _ => true,
        expectedData = 1 to 4
      )
    )

  "Consumer" should {
    testCases.foreach { case TestCase(description, data, isSuccess, accept, expectedData) =>
      description in {
        val resultBuffer = new mutable.TreeSet[Value]
        val topic = generate[String]()
        val process: ConsumerRecord[Key, Value] => F[Unit] =
          record =>
            if (isSuccess(record.value)) {
              Sync[F].delay(resultBuffer += record.value)
            } else {
              Sync[F].raiseError(new RuntimeException(s"failed to consume value ${record.value}"))
            }
        val consumer =
          createConsumer(
            process = process,
            topic = topic,
            filter = record => accept(record.value)
          )
        data.foreach(EmbeddedKafka.publishToKafka(topic, "", _))
        consumer.run.startAsync
        var timeAcc = 0
        val sleepTime = 300
        while (resultBuffer.size != expectedData.size && timeAcc < sleepTime * 100) {
          timeAcc += sleepTime
          Thread.sleep(sleepTime)
        }
        resultBuffer.toSeq shouldBe expectedData
      }
    }
  }

}

object ConsumerSpecBase {
  type Key = String
  type Value = Int

  case class TestCase(
      description: String,
      data: Seq[Value],
      isSuccess: Value => Boolean,
      filter: Value => Boolean,
      expectedData: Seq[Value])

}
