package ru.yandex.vertis.scoring.dao

import java.time.Duration
import java.util.function.Consumer

import com.dimafeng.testcontainers.{Container, ForAllTestContainer, GenericContainer}
import com.typesafe.scalalogging.StrictLogging
import org.testcontainers.containers.output.OutputFrame
import org.testcontainers.containers.output.OutputFrame.OutputType
import org.testcontainers.containers.{Network, GenericContainer => JavaGenericContainer}
import ru.yandex.vertis.quality.test_utils.SpecBase

//TODO VSMODERATION-5342
//Container startup fails by timeout, we need to understand why
trait YtSpecBase extends SpecBase /*with ForAllTestContainer */ with StrictLogging {
  protected val apiPort = 32395

  protected val ytVersion = "stable"
  protected val ytPort = 80 //do not change, configured in containers's script

  lazy val ytContainer: JavaGenericContainer[_] = {
    val container: JavaGenericContainer[_] = new JavaGenericContainer(s"registry.yandex.net/yt/yt:$ytVersion")

    // todo: blocked on testcontainers-scala's support of testcontainers v.1.12.4
    //or you'd be using an old yt locally
    //    javaContainer.withImagePullPolicy(PullPolicy.alwaysPull())
    container.withExposedPorts(ytPort)
    container.withStartupTimeout(Duration.ofMinutes(2))
    //tmpfs is cool but yql uploads huge mrjob file
    //    javaContainer.withTmpFs(Map("/tmp" -> "rw").asJava)
    //note that yt_local does not support most of the operations (sort, merge, mr), requiring some heavy 'porto' layer
    //erase is the only one we know to be supported
    container.withCommand(
      "--enable-debug-logging " +
        //        "--tmpfs-path /tmp " +
        //--fqdn 127.0.0.1 is required for parts on local yt installation to find each other inside the javaContainer
        "--fqdn 127.0.0.1 " +
        "--scheduler-count 1 " +
        "--node-count 2 " +
        """--proxy-config {address_resolver={enable_ipv4=%true;enable_ipv6=%false;};coordinator={public_fqdn="yt:80"}}"""
    )
    container.withLogConsumer(toLogConsumer)
    //to share with yql javaContainer
    container.withNetwork(Network.SHARED)
    container.withNetworkAliases("yt")
    container.withEnv("TZ", "Europe/Moscow")
    container.start()
    container
  }

  lazy val yqlContainer: JavaGenericContainer[_] = {
    val javaContainer: JavaGenericContainer[_] = new JavaGenericContainer("registry.yandex.net/vertis/etc/yql:dev")

    javaContainer.withExposedPorts(apiPort)
    javaContainer.withStartupTimeout(Duration.ofMinutes(2))

    //too noisy, enable for debug issues only
    //it is easier to connect to javaContainer directly
    //    javaContainer.withLogConsumer(toLogConsumer(logger))

    //to share with yql javaContainer
    javaContainer.withNetwork(Network.SHARED)

    //that's actually doesn't do much because it should be already started in val devinition
    javaContainer.dependsOn(ytContainer)

    javaContainer
  }

//  override lazy val container: Container =
//    new GenericContainer(yqlContainer)

  def toLogConsumer: Consumer[OutputFrame] =
    (t: OutputFrame) =>
      t.getType match {
        case OutputType.STDOUT => logger.info(t.getUtf8String)
        case OutputType.STDERR => logger.error(t.getUtf8String)
        case OutputType.END    => //what is that?
      }
}
