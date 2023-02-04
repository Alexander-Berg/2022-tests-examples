package common.zio.kafka.testkit

import com.dimafeng.testcontainers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import zio.blocking.Blocking
import zio.kafka.admin.AdminClient.NewTopic
import zio.kafka.admin.{AdminClient, AdminClientSettings}
import zio._

object TestKafka {
  type TestKafka = Has[KafkaContainer]

  def makeLayer(
      confluentPlatformVersion: Option[String] = None): ZLayer[Any, Nothing, TestKafka] = {
    ZLayer
      .fromAcquireRelease {
        val image = DockerImageName
          .parse(KafkaContainer.defaultImage)
          .withTag(confluentPlatformVersion.getOrElse(KafkaContainer.defaultTag))
        ZIO
          .effectTotal(new KafkaContainer(image))
          .tap(c => ZIO.effect(c.start()).orDie)
      }(c => ZIO.effect(c.stop()).orDie)
  }

  val live: ZLayer[Any, Nothing, TestKafka] = makeLayer()

  val bootstrapServers: URIO[TestKafka, List[String]] =
    ZIO.access[TestKafka](_.get.bootstrapServers.split(",").toList)

  def createTopic(
      name: String,
      numPartitions: Int = 1,
      replicationFactor: Short = 1,
      configs: Map[String, String] = Map()): RIO[Blocking with TestKafka, Unit] = {
    for {
      servers <- bootstrapServers
      settings = AdminClientSettings(servers)
      topic = NewTopic(name, numPartitions, replicationFactor, configs)
      _ <- AdminClient.make(settings).use(admin => admin.createTopic(topic))
    } yield ()
  }
}
