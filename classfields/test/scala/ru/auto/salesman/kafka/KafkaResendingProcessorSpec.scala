package ru.auto.salesman.kafka

import cakesolutions.kafka.KafkaProducer
import com.google.protobuf.Message
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import ru.auto.salesman.Task
import ru.auto.salesman.kafka.producer.KafkaProducerConfig
import ru.auto.salesman.kafka.producer.impl.ProducerImpl
import ru.auto.salesman.test.BaseSpec
import zio.UIO
import zio.duration._
import zio.stream.ZStream

class KafkaResendingProcessorSpec extends BaseSpec {

  System.setProperty("config.resource", "application.conf")

  val config = KafkaProducerConfig(
    ConfigFactory.load().getConfig("kafka.dead-letter")
  )

  "KafkaResendingProcessor" should {

    "successfully apply passed function" in {
      val producer = new MockProducer(
        true,
        new StringSerializer(),
        new ByteArraySerializer()
      )

      val f = mock[Message => Task[Unit]]

      (f.apply _)
        .expects(*)
        .returningZ(())
        .once()

      val processor = KafkaResendingProcessor[Message](
        f,
        ProducerImpl[Message](KafkaProducer(producer)),
        config.topic
      )

      processor(mock[Message]).success.value
      producer.close()
    }

    "send message in specific topic if unable to apply passed function" in {
      val producer = new MockProducer(
        true,
        new StringSerializer(),
        new ByteArraySerializer()
      )

      val f = mock[Message => Task[Unit]]

      (f.apply _)
        .expects(*)
        .throwingZ(new Exception("Logic error"))

      val processor = KafkaResendingProcessor[Message](
        f,
        ProducerImpl[Message](KafkaProducer(producer)),
        config.topic
      )

      val message = mock[Message]

      (message.toByteArray _)
        .expects()
        .returning("Message".getBytes)

      producer.history().size().shouldBe(0)
      processor(message).success.value
      producer.history().size().shouldBe(1)
      producer.close()
    }

    "retry sending finite amount of time and eventually send message in specific topic" in {
      val producer = new MockProducer(
        false,
        new StringSerializer(),
        new ByteArraySerializer()
      )

      val f = mock[Message => Task[Unit]]

      (f.apply _)
        .expects(*)
        .throwingZ(new Exception("Logic error"))

      val processor = new KafkaResendingProcessor[Message](
        f,
        ProducerImpl[Message](KafkaProducer(producer)),
        config.topic,
        100.millis,
        1.second
      )

      val message = mock[Message]

      (message.toByteArray _)
        .expects()
        .returning("Message".getBytes)

      val err = new RuntimeException("Producer error")
      val completeWithErr =
        ZStream
          .repeatEffect(UIO(producer.errorNext(err)))
          .takeUntil(_ == true)

      val completeWithSuccess =
        ZStream
          .repeatEffect(UIO(producer.completeNext()))
          .takeUntil(_ == true)

      (completeWithErr ++ completeWithSuccess).runDrain.forkDaemon.success.value

      processor(message).success.value
      producer.close()
    }

    "retry sending finite amount of time and fail if unable to send" in {
      val producer = new MockProducer(
        false,
        new StringSerializer(),
        new ByteArraySerializer()
      )

      val f = mock[Message => Task[Unit]]

      (f.apply _)
        .expects(*)
        .throwingZ(new Exception("Logic error"))

      val processor = new KafkaResendingProcessor[Message](
        f,
        ProducerImpl[Message](KafkaProducer(producer)),
        config.topic,
        100.millis,
        1.second
      )

      val message = mock[Message]

      (message.toByteArray _)
        .expects()
        .returning("Message".getBytes)

      val err = new RuntimeException("Producer error")
      val completeWithErr = ZStream.repeatEffect(UIO(producer.errorNext(err)))

      completeWithErr.runDrain.forkDaemon.success.value

      processor(message).failure
      producer.close()
    }

  }

}
