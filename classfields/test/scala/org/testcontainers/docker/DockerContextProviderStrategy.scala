package org.testcontainers.docker

import com.github.dockerjava.api.DockerClient
import io.circe.generic.JsonCodec
import io.circe.parser.decode
import org.testcontainers.docker.DockerContextProviderStrategy.Context
import org.slf4j.LoggerFactory
import org.testcontainers.dockerclient.{
  DockerClientProviderStrategy,
  TransportConfig,
}

import java.net.URI
import scala.sys.process.*

class DockerContextProviderStrategy extends DockerClientProviderStrategy {

  private val log = LoggerFactory
    .getLogger(classOf[DockerContextProviderStrategy])

  override def getDescription: String = "DockerClient based on `docker context`"

  private val dockerExecutable =
    List("docker", "/usr/bin/docker", "/opt/homebrew/bin/docker")
      .find(exec => s"which $exec".!< == 0)
      .get

  private lazy val dockerHost: Option[URI] = {
    try {
      val output = s"$dockerExecutable context inspect".!!
      log.debug(s"Current docker context: $output")
      decode[List[Context]](output) match {
        case Left(error) =>
          throw new RuntimeException(s"failed to decode $output: $error")
        case Right(value) =>
          val res =
            value.map(_.Endpoints.docker.Host).map(new URI(_)).headOption
          res.foreach(host =>
            log.info(s"Extracted docker host ($host) from docker context"),
          )
          res
      }
    } catch {
      case e: Throwable =>
        log.warn(s"Failed to parse docker context", e)
        None
    }
  }

  private lazy val realStrategy: Option[DockerClientProviderStrategy] = {
    dockerHost.map { host =>
      if (host.getScheme == "ssh") {
        new SshProviderStrategy(host)
      } else if (host.getScheme == "unix" && host.getPath.contains(".colima")) {
        new RemoteDockerProviderStrategy(
          "colima",
          host,
          new URI("unix:///var/run/docker.sock"),
        )
      } else {
        new RemoteDockerProviderStrategy("", host, host)
      }
    }
  }

  override def isApplicable: Boolean  = dockerHost.nonEmpty
  override def isPersistable: Boolean = false

  override def getTransportConfig: TransportConfig =
    realStrategy.get.getTransportConfig

  override def getDockerClient: DockerClient = realStrategy.get.getDockerClient

  override def getDockerHostIpAddress: String =
    realStrategy.get.getDockerHostIpAddress

  override def getPriority: Int = 90
}

object DockerContextProviderStrategy {

  @JsonCodec
  case class Context(Endpoints: Endpoints)

  @JsonCodec
  case class Endpoints(docker: Docker)

  @JsonCodec
  case class Docker(Host: String)

}
