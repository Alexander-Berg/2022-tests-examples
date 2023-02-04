package org.testcontainers.docker

import com.github.dockerjava.api.DockerClient
import org.slf4j.LoggerFactory
import org.testcontainers.dockerclient.{
  DockerClientProviderStrategy,
  TransportConfig,
}
import org.testcontainers.utility.ResourceReaper

import java.net.URI
import java.nio.file.{Files, Path}
import scala.sys.process.*

class SshProviderStrategy(uri: URI) extends DockerClientProviderStrategy {

  // fake config for testcontainers (so DockerClientFactory.getRemoteDockerUnixSocketPath works correctly)
  override lazy val getTransportConfig: TransportConfig = {
    TransportConfig
      .builder()
      .dockerHost(URI.create(s"unix:///var/run/docker.sock"))
      .build()
  }

  override lazy val getDockerClient: DockerClient = {
    DockerClientProviderStrategy.getClientForConfig(realTransportConfig)
  }

  private lazy val tempUnixSocket = Path
    .of("/tmp", "tc.ssh-" + System.nanoTime() + ".sock")

  private lazy val command = Seq(
    "ssh",
    "-nNT",
    "-L",
    s"$tempUnixSocket:/var/run/docker.sock",
    uri.getAuthority,
  )

  // real transport config used to build DockerClient
  private lazy val realTransportConfig: TransportConfig = {
    log.info(s"Starting SSH session with $uri, socket = $tempUnixSocket")
    val process = command
      .run(BasicIO.standard(connectInput = false).daemonized())
    log.info(s"SSH session started")
    sys.addShutdownHook {
      log.info("Cleaning up containers")
      ResourceReaper.instance().performCleanup()
      log.info("Stopping ssh session")
      process.destroy()
      process.exitValue()
      Files.deleteIfExists(tempUnixSocket)

      log.info(s"Stopped ssh session with $uri")
    }
    TransportConfig
      .builder()
      .dockerHost(URI.create(s"unix://$tempUnixSocket"))
      .build()
  }

  private val log = LoggerFactory.getLogger(classOf[SshProviderStrategy])

  override def getDescription: String =
    "Unix socket on remote machine forwarded with ssh"

  override def isApplicable: Boolean = true

  override def isPersistable: Boolean = false

  override def getDockerHostIpAddress: String = uri.getHost

}
