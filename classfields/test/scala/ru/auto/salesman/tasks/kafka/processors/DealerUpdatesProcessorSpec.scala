package ru.auto.salesman.tasks.kafka.processors

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import ru.auto.cabinet.DealerAutoru.Dealer
import ru.auto.salesman.environment.runtime
import ru.auto.salesman.kafka.consumer.KafkaConsumerConfig
import ru.auto.salesman.kafka.producer.KafkaProducerConfig
import ru.auto.salesman.kafka.producer.impl.{LoggedProducerImpl, ProducerImpl}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.docker.Kafka
import ru.auto.salesman.util.HasRequestContext
import ru.yandex.vertis.ops.test.TestOperationalSupport
import zio._
import zio.blocking.Blocking
import zio.duration._
import zio.stream.ZStream
import zio.test.environment.{TestClock, TestEnvironment}

class DealerUpdatesProcessorSpec extends BaseSpec {

  System.setProperty("config.resource", "test.conf")

  trait configuration extends Kafka {

    val config = ConfigFactory
      .load()
      .getConfig("kafka")
      .withValue(
        "bootstrap.servers",
        ConfigValueFactory.fromAnyRef(getBootstrapServers)
      )

    val consumerConfig = KafkaConsumerConfig(
      config,
      runtime
    )

    val producerConfig = KafkaProducerConfig(
      connectionString = getBootstrapServers,
      topic = consumerConfig.topic
    )

    val producer = LoggedProducerImpl(
      ProducerImpl[Dealer](producerConfig)
    )

    val dealer = Dealer.newBuilder().setId(100).build()

    createTopic(consumerConfig.topic, 1, 1)
  }

  "DealerUpdatesProcessor" should {

    "successfully process dealers from topic" in new configuration {
      Ref
        .make[Option[Dealer]](None)
        .flatMap { ref =>
          for {
            _ <- producer.send(producerConfig.topic, dealer)
            _ <- DealerUpdatesProcessor.run(
              (dealer: Dealer) => ref.set(Some(dealer)),
              config,
              runtime
            )(TestOperationalSupport)
            _ <- ZStream
              .repeatEffect(ref.get)
              .takeUntil(_.contains(dealer))
              .runDrain
            _ = container.stop
          } yield ()
        }
        .success
        .value
    }

    "successfully read several times from topic" in new configuration {
      Ref
        .make[Int](0)
        .flatMap { ref =>
          for {
            _ <- producer.send(producerConfig.topic, dealer)
            _ <- DealerUpdatesProcessor.run(
              (dealer: Dealer) =>
                for {
                  curr <- ref.get
                  _ <- ref.set(curr + 1)
                  _ <- producer.send(producerConfig.topic, dealer)
                } yield (),
              config,
              runtime
            )(TestOperationalSupport)
            _ <- ZStream
              .repeatEffect(ref.get)
              .takeUntil(_ == 5)
              .runDrain
            _ = container.stop
          } yield ()
        }
        .success
        .value
    }

    "eventually process dealers from topic" in new configuration {
      Ref
        .make[Option[Dealer]](None)
        .flatMap { ref =>
          val processor = mock[DealerUpdatesProcessor]

          (processor.process _)
            .expects(*)
            .onCall { (_: Dealer) =>
              Task.fail(new Exception("Processing exception"))
            }
            .repeat(10)

          (processor.process _)
            .expects(*)
            .onCall((dealer: Dealer) => ref.set(Some(dealer)))
            .once()

          for {
            _ <- producer.send(producerConfig.topic, dealer)
            _ <-
              TestClock
                .adjust(10.hours)
                .repeat(Schedule.forever)
                .forkDaemon
            _ <- DealerUpdatesProcessor.run(
              processor,
              config,
              runtime
            )(TestOperationalSupport)
            _ <- ZStream
              .repeatEffect(ref.get)
              .takeUntil(_.contains(dealer))
              .runDrain
            _ = container.stop
          } yield ()
        }
        .provideSomeLayer[HasRequestContext with Blocking](
          zio.ZEnv.live >>> TestEnvironment.live
        )
        .success
        .value
    }

    "successfully read several times even in case of failure" in new configuration {
      Ref
        .make[Option[Dealer]](None)
        .flatMap { ref =>
          val processor = mock[DealerUpdatesProcessor]

          (processor.process _)
            .expects(*)
            .onCall { (dealer: Dealer) =>
              if (dealer == this.dealer)
                Task.fail(new Exception("Processing exception"))
              else
                producer.send(producerConfig.topic, this.dealer).unit
            }
            .repeat(30)

          (processor.process _)
            .expects(*)
            .onCall((dealer: Dealer) =>
              producer.send(producerConfig.topic, this.dealer).unit
            )
            .once()

          (processor.process _)
            .expects(*)
            .onCall((dealer: Dealer) => ref.set(Some(dealer)))
            .once()

          for {
            _ <- producer.send(producerConfig.topic, Dealer.getDefaultInstance)
            _ <-
              TestClock
                .adjust(10.hours)
                .repeat(Schedule.forever)
                .forkDaemon
            _ <- DealerUpdatesProcessor.run(
              processor,
              config,
              runtime
            )(TestOperationalSupport)
            _ <- ZStream
              .repeatEffect(ref.get)
              .takeUntil(_.contains(dealer))
              .runDrain
            _ = container.stop
          } yield ()
        }
        .provideSomeLayer[HasRequestContext with Blocking](
          zio.ZEnv.live >>> TestEnvironment.live
        )
        .success
        .value
    }

  }

}
