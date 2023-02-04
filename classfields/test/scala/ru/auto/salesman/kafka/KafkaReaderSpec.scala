package ru.auto.salesman.kafka

import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.{
  ConsumerRecord,
  MockConsumer,
  OffsetResetStrategy
}
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.TimeoutException
import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.environment.runtime
import ru.auto.salesman.kafka.consumer.KafkaConsumerConfig
import ru.auto.salesman.kafka.consumer.impl._
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.HasRequestContext
import zio.blocking.{effectBlocking, Blocking}
import zio.duration._
import zio.stream.ZStream
import zio.test.environment.{TestClock, TestEnvironment}
import zio.{Ref, Schedule}

import scala.collection.JavaConverters._

class KafkaReaderSpec extends BaseSpec {

  System.setProperty("config.resource", "application.conf")

  private val now = DateTime.now()

  private val kafkaConfig = KafkaConsumerConfig(
    ConfigFactory.load().getConfig("kafka"),
    runtime
  )

  private val partition = new TopicPartition(kafkaConfig.topic, 0)

  private val consumer = new MockConsumer[String, Long](
    OffsetResetStrategy.EARLIEST
  )

  "KafkaReader" should {

    "consume and handle messages from topic successfully" in {
      consumer.schedulePollTask { () =>
        consumer.rebalance(List(partition).asJava)
        consumer.addRecord(generateConsumerRecords.head)
      }

      generateConsumerRecords.tail.foreach { record =>
        consumer.schedulePollTask { () =>
          consumer.addRecord(record)
          consumer.addEndOffsets(
            Map(partition -> java.lang.Long.valueOf(record.offset())).asJava
          )
        }
      }

      consumer.updateBeginningOffsets(
        Map(partition -> java.lang.Long.valueOf(0)).asJava
      )

      val kafkaReader = new KafkaReaderImpl[String, Long](
        LoggedConsumerImpl(
          ConsumerImpl(consumer),
          kafkaConfig.topic,
          Ref.make[DateTime](now).success.value
        ),
        10.seconds,
        kafkaConfig.topic
      )

      (for {
        ref <- Ref.make[Long](0L)
        _ <- kafkaReader.subscribeAndStart(ref.set)
        _ <-
          ZStream
            .repeatEffect(ref.get)
            .takeUntil(_ == 10L)
            .runDrain
        _ <-
          effectBlocking {
            consumer.wakeup()
            consumer.close()
          }
      } yield ()).success.value
    }

    "retry polling endlessly in case of errors" in {
      consumer.schedulePollTask { () =>
        consumer.rebalance(List(partition).asJava)
        consumer.addRecord(generateConsumerRecords.head)
      }

      (1 to 5).foreach { _ =>
        consumer.schedulePollTask { () =>
          consumer.setPollException(
            new TimeoutException("Some timeout occured")
          )
        }
      }

      generateConsumerRecords.tail.foreach { record =>
        consumer.schedulePollTask { () =>
          consumer.addRecord(record)
          consumer.addEndOffsets(
            Map(partition -> java.lang.Long.valueOf(record.offset())).asJava
          )
        }
      }

      consumer.updateBeginningOffsets(
        Map(partition -> java.lang.Long.valueOf(0)).asJava
      )

      val kafkaReader = new KafkaReaderImpl[String, Long](
        LoggedConsumerImpl(
          ConsumerImpl(consumer),
          kafkaConfig.topic,
          Ref.make[DateTime](now).success.value
        ),
        10.seconds,
        kafkaConfig.topic
      )

      (for {
        _ <-
          TestClock
            .adjust(10.hours)
            .repeat(Schedule.forever)
            .forkDaemon
        ref <- Ref.make[List[Long]](List.empty)
        _ <- kafkaReader.subscribeAndStart { value =>
          ref.get
            .map(rs => value :: rs)
            .flatMap(ref.set)
        }
        _ <-
          ZStream
            .repeatEffect(ref.get)
            .takeUntil(_ == (0 to 10).toList.reverse)
            .runDrain
        _ <-
          effectBlocking {
            consumer.wakeup()
            consumer.close()
          }
      } yield ())
        .provideSomeLayer[HasRequestContext with Blocking](
          zio.ZEnv.live >>> TestEnvironment.live
        )
        .success
        .value
    }

    "retry polling in case of processing error" in {
      consumer.schedulePollTask { () =>
        consumer.rebalance(List(partition).asJava)
      }

      generateConsumerRecords.foreach { record =>
        consumer.schedulePollTask { () =>
          consumer.addRecord(record)
          consumer.addEndOffsets(
            Map(partition -> java.lang.Long.valueOf(record.offset())).asJava
          )
        }
      }

      consumer.updateBeginningOffsets(
        Map(partition -> java.lang.Long.valueOf(0)).asJava
      )

      val kafkaReader = new KafkaReaderImpl[String, Long](
        LoggedConsumerImpl(
          ConsumerImpl(consumer),
          kafkaConfig.topic,
          Ref.make[DateTime](now).success.value
        ),
        10.seconds,
        kafkaConfig.topic
      )

      (for {
        ref <- Ref.make(0L)
        f = mock[Long => Task[Unit]]
        _ =
          (f.apply _)
            .expects(*)
            .throwingZ(new Exception("Processing error"))
            .repeat(10)
        _ =
          (f.apply _)
            .expects(*)
            .onCall((value: Long) => ref.set(value))
            .atLeastOnce()
        _ <- kafkaReader.subscribeAndStart(f)
        _ <-
          ZStream
            .repeatEffect(ref.get)
            .takeUntil(_ == 10)
            .runDrain
        _ <-
          effectBlocking {
            consumer.wakeup()
            consumer.close()
          }
      } yield ()).success.value
    }

  }

  private def generateConsumerRecords: List[ConsumerRecord[String, Long]] =
    (0L to 10L)
      .map(i => new ConsumerRecord(kafkaConfig.topic, 0, i, "", i))
      .toList

}
