package bootstrap.testcontainers.kafka

import bootstrap.testcontainers.{Container, LogConsumer}
import org.testcontainers.containers
import org.testcontainers.utility.DockerImageName
import zio.*

case class KafkaContainer private[kafka] (container: containers.KafkaContainer)
    extends Container[containers.KafkaContainer] {

  def bootstrapServers: UIO[List[String]] =
    ZIO.from(List(container.getBootstrapServers)).orDie

}

case object KafkaContainer {

  val Image: DockerImageName = DockerImageName.parse("confluentinc/cp-kafka")

  val live: RLayer[Scope, KafkaContainer] = ZLayer.fromZIO {
    ZIO
      .acquireRelease(
        ZIO
          .attempt(
            new containers.KafkaContainer(Image).withLogConsumer(LogConsumer),
          )
          .tap(c => ZIO.attempt(c.start())),
      )(c => ZIO.attempt(c.stop()).orDie)
      .map(KafkaContainer(_))
  }

  def bootstrapServers: URIO[KafkaContainer, List[String]] =
    ZIO.environmentWithZIO[KafkaContainer](_.get.bootstrapServers)

}
