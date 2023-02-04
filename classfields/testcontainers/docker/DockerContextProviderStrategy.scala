package common.testcontainers.docker

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{DeserializationFeature, PropertyNamingStrategies}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.dockerjava.api.DockerClient
import common.testcontainers.docker.DockerContextProviderStrategy.Context
import org.slf4j.LoggerFactory
import org.testcontainers.dockerclient.{DockerClientProviderStrategy, TransportConfig}

import java.net.URI
import scala.sys.process._

class DockerContextProviderStrategy extends DockerClientProviderStrategy {
  private val log = LoggerFactory.getLogger(classOf[DockerContextProviderStrategy])

  override def getDescription: String = "DockerClient based on `docker context`"

  private lazy val dockerHost: Option[URI] = {
    try {
      val output = "docker context inspect".!!
      log.debug(s"Current docker context: $output")
      val json = JsonMapper
        .builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .addModule(DefaultScalaModule)
        .build()
      val parsed = json.readValue(json.createParser(output), new TypeReference[List[Context]] {})
      val res = parsed.map(_.Endpoints.docker.Host).map(new URI(_)).headOption
      res.foreach(host => log.info(s"Extracted docker host ($host) from docker context"))
      res
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
        new RemoteDockerProviderStrategy("colima", host, new URI("unix:///var/run/docker.sock"))
      } else {
        new RemoteDockerProviderStrategy("", host, host)
      }
    }
  }

  override def isApplicable: Boolean = dockerHost.nonEmpty
  override def isPersistable: Boolean = false

  override def getTransportConfig: TransportConfig = realStrategy.get.getTransportConfig

  override def getDockerClient: DockerClient = realStrategy.get.getDockerClient

  override def getDockerHostIpAddress: String = realStrategy.get.getDockerHostIpAddress

  override def getPriority: Int = 90
}

object DockerContextProviderStrategy {
  case class Context(Endpoints: Endpoints)
  case class Endpoints(docker: Docker)
  case class Docker(Host: String)

}
