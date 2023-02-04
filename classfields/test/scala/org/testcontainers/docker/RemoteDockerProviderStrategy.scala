package org.testcontainers.docker

import com.github.dockerjava.api.DockerClient
import org.testcontainers.dockerclient.{
  DockerClientProviderStrategy,
  TransportConfig,
}

import java.net.URI

class RemoteDockerProviderStrategy(
    description: String,
    realTransportConfig: TransportConfig,
    publicTransportConfig: TransportConfig,
) extends DockerClientProviderStrategy {

  def this(description: String, realDockerPath: URI, publicDockerPath: URI) = {
    this(
      description,
      TransportConfig.builder().dockerHost(realDockerPath).build(),
      TransportConfig.builder().dockerHost(publicDockerPath).build(),
    )
  }

  override def getDescription: String = description

  override def getTransportConfig: TransportConfig = publicTransportConfig

  override lazy val getDockerClient: DockerClient = DockerClientProviderStrategy
    .getClientForConfig(realTransportConfig)

}
