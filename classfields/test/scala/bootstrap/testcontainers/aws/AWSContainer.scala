package bootstrap.testcontainers.aws

import bootstrap.testcontainers.{Container, LogConsumer}
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  AwsCredentialsProvider,
  StaticCredentialsProvider,
}
import zio.*

import java.net.URI

case class AWSContainer private[aws] (container: LocalStackContainer)
    extends Container[LocalStackContainer] {

  def endpointOverride: UIO[URI] =
    ZIO.succeed(
      URI
        .create(s"http://${container.getHost}:${container.getMappedPort(4566)}"),
    )

  def defaultCredentialsProvider: UIO[AwsCredentialsProvider] =
    accessKey
      .zip(secretKey)
      .map { case (access, secret) =>
        StaticCredentialsProvider
          .create(AwsBasicCredentials.create(access, secret))
      }

  def accessKey: UIO[String] = ZIO.succeed(container.getAccessKey)

  def secretKey: UIO[String] = ZIO.succeed(container.getSecretKey)

  def region: UIO[String] = ZIO.succeed(container.getRegion)
}

case object AWSContainer {

  val Image: DockerImageName = DockerImageName
    .parse("localstack/localstack:0.11.2")

  def live(
    service: AwsUnit,
    services: AwsUnit*,
  ): RLayer[Scope, AWSContainer] = {
    val svcs = service +: services
    ZLayer.fromZIO {
      ZIO
        .fromAutoCloseable {
          ZIO.attempt {
            val container = new LocalStackContainer(Image)
              .withServices(svcs.map(_.service)*)
              .withLogConsumer(LogConsumer)
            container.setStartupAttempts(3)
            container.start()
            container
          }
        }
        .tap { container =>
          ZIO.foreachDiscard(svcs)(_.setup(container))
        }
        .map(AWSContainer(_))
        .tapError { e =>
          ZIO.debug(e.getStackTrace.mkString("\n"))
        }

    }
  }

  def endpointOverride: URIO[AWSContainer, URI] =
    ZIO.environmentWithZIO[AWSContainer](_.get.endpointOverride)

  def defaultCredentialsProvider: URIO[AWSContainer, AwsCredentialsProvider] =
    ZIO.environmentWithZIO[AWSContainer](_.get.defaultCredentialsProvider)

  def accessKey: URIO[AWSContainer, String] =
    ZIO.environmentWithZIO[AWSContainer](_.get.accessKey)

  def secretKey: URIO[AWSContainer, String] =
    ZIO.environmentWithZIO[AWSContainer](_.get.secretKey)

  def region: URIO[AWSContainer, String] =
    ZIO.environmentWithZIO[AWSContainer](_.get.region)

  sealed trait AwsUnit {
    def service: LocalStackContainer.Service

    def setup(c: LocalStackContainer): UIO[Unit]
  }

  case class S3(buckets: Seq[String]) extends AwsUnit {

    override def service: LocalStackContainer.Service =
      LocalStackContainer.Service.S3

    override def setup(c: LocalStackContainer): UIO[Unit] = {
      ZIO.foreachDiscard(buckets)(bucketName =>
        ZIO.succeed(
          c.execInContainer("awslocal", "s3", "mb", s"s3://$bucketName"),
        ),
      )
    }

  }

  object S3 {
    def apply(bucket: String, rest: String*): S3 = S3(bucket +: rest)
  }

}
