package ru.yandex.realty.seller.tasks.`export`

import org.scalatest.concurrent.ScalaFutures
import org.slf4j.Logger
import org.testcontainers.containers.{GenericContainer, Network}
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.YtConfiguration
import ru.yandex.realty.application.ng.yt.{YtClientSupplier, YtConfig, YtConfigSupplier}
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.pos.TestOperationalComponents
import ru.yandex.realty.seller.tasks.`export`.YtTestClientSupplier.logConsumer

import java.util.function.Consumer
import java.time.Duration

/**
  * Inspired by verticals-backend YtTest
  */
trait YtTestClientSupplier
  extends YtClientSupplier
  with YtConfigSupplier
  with TestOperationalComponents
  with ScalaFutures
  with Logging {

  private val ytVersion = "r8224978"
  private val ytPort = 80

  lazy val ytContainer: GenericContainer[_] = {
    val container: GenericContainer[_] =
      new GenericContainer(s"registry.yandex.net/vertis/vertis-yt-local:$ytVersion")
    container.withExposedPorts(ytPort)
    container.withStartupTimeout(Duration.ofSeconds(30))
    container.withCommand(
      "--enable-debug-logging " +
        "--fqdn 127.0.0.1 " +
        "--scheduler-count 1 " +
        "--node-count 3 " +
        """--proxy-config {address_resolver={enable_ipv4=%true;enable_ipv6=%false;};coordinator={public_fqdn="yt:80"}}"""
    )
    container.withLogConsumer(logConsumer(log))

    // to share with yql container
    container.withNetwork(Network.SHARED)
    container.withNetworkAliases("yt")
    container.withEnv("TZ", "Europe/Moscow")
    container.withEnv("YT_LOG_LEVEL", "Debug")
    container.start()
    container
  }

  override lazy val ytConfig: YtConfig = YtConfig(
    host = s"${ytContainer.getContainerIpAddress}:${ytContainer.getMappedPort(ytPort)}",
    token = "",
    basePath = YPath.cypressRoot()
  )

  lazy val testYt = {
    val ytConf = YtConfiguration
      .builder()
      .withApiUrl(s"http://${ytContainer.getContainerIpAddress}:${ytContainer.getMappedPort(ytPort)}")
      .withUser("unit_tester")
      .withToken("fake_token")
      .withRole("the-one-who-knocks")
      .withJobSpecPatch(null)
      .withSpecPatch(null)
      .build()

    Yt.builder(ytConf)
      .http()
      .build()
  }
}

object YtTestClientSupplier {

  def logConsumer(log: Logger): Consumer[OutputFrame] =
    (t: OutputFrame) =>
      t.getType match {
        case OutputType.STDOUT => log.info(t.getUtf8String)
        case OutputType.STDERR => log.error(t.getUtf8String)
        case OutputType.END =>
      }
}
