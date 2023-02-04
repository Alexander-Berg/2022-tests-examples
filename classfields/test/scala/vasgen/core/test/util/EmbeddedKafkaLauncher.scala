package vasgen.core.test.util

import java.net.Socket
import scala.util.Try

object EmbeddedKafkaLauncher extends Logging {

  val live: ZLayer[Any, Nothing, Service] =
    (
      for {
        ref <- Ref.make[Option[EmbeddedK]](None)
      } yield new Launcher(config, ref)
    ).toLayer
  val producerLayer: ZLayer[Any, Throwable, Producer] =
    Producer
      .make(
        ProducerSettingsTO(
          bootstrapServers = bootstrapServers,
          closeTimeout = 1.seconds,
        ).toProducerSettings,
      )
      .mapError(VasgenFailure)
      .toLayer
  private val config = EmbeddedKafkaConfig(customBrokerProperties =
    Map(
      "auto.create.topics.enable"        -> "true",
      "group.min.session.timeout.ms"     -> "500",
      "group.initial.rebalance.delay.ms" -> "0",
      "zookeeper.connection.timeout.ms"  -> "30000",
    ),
  )

  def consumerProviderLayer[S <: Setup[_] : Tag](
    implicit
    setup: S,
  ) = {
    (ZLayer.requires[Clock] ++ ZLayer.requires[Any] ++
      ZLayer.requires[Tracing] ++ ZLayer.requires[Metrics] ++
      Terrain.fromSource(
        ConfigSource.fromMap {
          Map(
            s"${setup}_KAFKA_BOOTSTRAP_SERVERS" ->
              bootstrapServers.mkString(","),
            s"${setup}_KAFKA_GROUP_ID"  -> "0",
            s"${setup}_KAFKA_CLIENT_ID" -> "embedded",
          )
        },
      )) >>> ConsumerFactory.live[S]
  }

  private def bootstrapServers: List[String] =
    List(s"localhost:${config.kafkaPort}")

  trait Service {
    def start(): IO[VasgenStatus, Unit]
    def stop(): UIO[Unit]
    def isListeningKafka: UIO[Boolean]
    def isListeningZk: UIO[Boolean]
  }

  class Launcher(config: EmbeddedKafkaConfig, ref: Ref[Option[EmbeddedK]])
      extends Service {

    override def start(): IO[VasgenStatus, Unit] =
      for {
        ko <- ref.get
        _ <-
          ko match {
            case Some(k) =>
              ZIO.fail(
                VasgenFailure(
                  new IllegalStateException(
                    s"Embedded kafka already started at ${k.config.kafkaPort}",
                  ),
                ),
              )
            case None =>
              log.info(s"Starting embedded kafka") *>
                ref.set(Some(EmbeddedKafka.start()(config)))

          }
      } yield ()

    override def stop(): UIO[Unit] =
      for {
        ko <- ref.get
        _ <-
          ko match {
            case Some(k) =>
              log.info(s"Stopping embedded kafka").as(k.stop(true)) *>
                ref.set(None)
            case None =>
              log.warn(s"Embedded kafka already stopped")
          }
      } yield ()

    override def isListeningKafka: UIO[Boolean] = isListening(config.kafkaPort)

    override def isListeningZk: UIO[Boolean] = isListening(config.zooKeeperPort)

    private def isListening(port: Int): UIO[Boolean] =
      ZIO.succeed {
        Try(new Socket("localhost", port))
          .map { s =>
            Try(s.close()).recover { case cause: Throwable =>
              logger.error(s"Error during socket close $cause")
            }
            true
          }
          .getOrElse(false)
      }

  }

}
