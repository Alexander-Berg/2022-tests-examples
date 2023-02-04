package ru.yandex.vertis.zio_baker.zio.kafka

import io.github.embeddedkafka.{EmbeddedK, EmbeddedKafka, EmbeddedKafkaConfig}
import ru.yandex.vertis.zio_baker.zio.kafka.EmbeddedKafkaSpec.{KafkaConfigOnFoundPort, Localhost}
import zio.test.{Spec, TestFailure, ZSpec}
import zio.{Task, UIO, ZIO, ZManaged}

trait EmbeddedKafkaSpec {

  def bootstrap(port: Int) = s"$Localhost:$port"

  def withKafka[R](suite: EmbeddedKafkaConfig => ZSpec[R, Any]): ZSpec[R, Any] =
    withKafka(KafkaConfigOnFoundPort)(suite)

  def withKafka[R](
      config: EmbeddedKafkaConfig
    )(suite: EmbeddedKafkaConfig => ZSpec[R, Any]): ZSpec[R, Any] = Spec.managed {
    ZManaged.make(kafkaStart(config))(kafkaStop).mapBoth(TestFailure.fail, srv => suite(srv.config))
  }

  private def kafkaStart(config: EmbeddedKafkaConfig): Task[EmbeddedK] = ZIO.effect {
    EmbeddedKafka.start()(config)
  }

  private def kafkaStop(srv: EmbeddedK): UIO[Unit] = ZIO.succeed {
    EmbeddedKafka.stop(srv)
  }

}

object EmbeddedKafkaSpec {
  private val KafkaConfigOnFoundPort: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = 0, zooKeeperPort = 0)
  private val Localhost: String = "127.0.0.1"
}
