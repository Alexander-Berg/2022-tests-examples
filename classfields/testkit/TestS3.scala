package common.zio.clients.s3.testkit

import common.zio.clients.s3.S3Client.{S3Auth, S3Client, S3Config}
import common.zio.clients.s3.S3ClientLive
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.localstack.LocalStackContainer.Service.S3
import org.testcontainers.utility.DockerImageName
import zio.{Has, ZLayer, ZManaged}

import scala.concurrent.duration._

object TestS3 {

  val dockerImageName: DockerImageName = DockerImageName.parse("localstack/localstack:0.11.2")

  val managedContainer: ZManaged[Any, Nothing, LocalStackContainer] =
    ZManaged.makeEffect {
      val container = new LocalStackContainer(dockerImageName).withServices(S3)
      container.start()
      container
    }(_.stop()).orDie

  val managedConfig: ZManaged[Any, Nothing, S3Config] = managedContainer.map { c =>
    S3Config(
      // собираем адрес руками, потому что getEndpointOverride собирает невалидный урл с ipv6 адресом
      url = s"http://${c.getHost}:${c.getFirstMappedPort}",
      signUrl = None,
      auth = S3Auth("key", "secret"),
      region = c.getRegion,
      connectionTimeout = 1.seconds,
      requestTimeout = 30.seconds,
      numRetries = 5,
      maxConnections = 10
    )
  }

  val live: ZLayer[Any, Nothing, S3Client] = managedConfig.toLayer >>> S3ClientLive.live

  val mocked: ZLayer[Any, Nothing, S3Client] = InMemoryS3.make.toLayer
}
