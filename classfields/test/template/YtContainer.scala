package ru.auto.salesman.test.template

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

object YtContainer {
  val imageName = "registry.yandex.net/yt/yt:stable"

  private val ytPort = 80

  lazy val ytContainer: GenericContainer[_] = {
    val container: GenericContainer[_] =
      new GenericContainer(DockerImageName.parse(imageName))

    container.withExposedPorts(ytPort)
    container.withCommand(
      s"""--proxy-config {address_resolver={enable_ipv4=%true;enable_ipv6=%false;};coordinator={public_fqdn="localhost:$ytPort"}}"""
    )
    container.withEnv("TZ", "Europe/Moscow")
    container.waitingFor(Wait.forLogMessage(".*Local YT started.*", 1))
    container.start()
    container
  }

  def httpApi: String =
    s"${ytContainer.getContainerIpAddress}:${ytContainer.getMappedPort(ytPort)}"

}
