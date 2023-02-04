package ru.yandex.vertis.telepony.yt

import org.testcontainers.containers.{GenericContainer, Network}
import ru.yandex.vertis.zio.logging.SyncLogging
import ru.yandex.vertis.zio.logging.SyncLogging.toLogConsumer

import java.time.Duration

/**
  * @author tolmach
  */
object YtTest extends SyncLogging {

  private val ytVersion = "stable"
  private val ytPort = 80 //do not change, configured in containers's script

  lazy val ytContainer: GenericContainer[_] = {
    syncLogger.info("Starting yt container...")
    val container: GenericContainer[_] =
      new GenericContainer(s"registry.yandex.net/yt/yt:$ytVersion")

    // todo: blocked on testcontainers-scala's support of testcontainers v.1.12.4
    //or you'd be using an old yt locally
    //    container.withImagePullPolicy(PullPolicy.alwaysPull())
    container.withExposedPorts(ytPort)
    container.withStartupTimeout(Duration.ofSeconds(30))
    //tmpfs is cool but yql uploads huge mrjob file
    //    container.withTmpFs(Map("/tmp" -> "rw").asJava)
    //note that yt_local does not support most of the operations (sort, merge, mr), requiring some heavy 'porto' layer
    //erase is the only one we know to be supported
    container.withCommand(
      "--enable-debug-logging " +
        //        "--tmpfs-path /tmp " +
        //--fqdn 127.0.0.1 is required for parts on local yt installation to find each other inside the container
        "--fqdn 127.0.0.1 " +
        "--scheduler-count 1 " +
        "--node-count 2 " +
        """--proxy-config {address_resolver={enable_ipv4=%true;enable_ipv6=%false;};coordinator={public_fqdn="yt:80"}}"""
    )
    container.withLogConsumer(toLogConsumer(syncLogger))
    //to share with yql container
    container.withNetwork(Network.SHARED)
    container.withNetworkAliases("yt")
    container.withEnv("TZ", "Europe/Moscow")
    container.start()
    container
  }

}
