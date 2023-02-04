package vertis.yt

import java.time.Duration
import _root_.zio._
import common.zio.logging.SyncLogger
import org.testcontainers.containers.{GenericContainer, Network}
import ru.yandex.inside.yt.kosher.async.{CloseableYt => AsyncCloseableYt, Yt => AsyncYt}
import ru.yandex.inside.yt.kosher.cypress.YPath
import ru.yandex.inside.yt.kosher.impl.YtConfiguration
import ru.yandex.inside.yt.kosher.{CloseableYt, Yt}
import vertis.yt.config.YtConfig
import vertis.yt.zio.wrappers.YtZio
import vertis.zio.BaseEnv
import vertis.zio.logging.TestContainersLogging.toLogConsumer

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
trait YtTest {

  protected lazy val ytConf: YtConfiguration = YtTest.containerYtConf

  protected lazy val ytConfig: YtConfig = YtConfig(ytConf, YPath.cypressRoot())

  protected lazy val userName: String = ytConf.getUser

  protected lazy val ytClient: CloseableYt =
    Yt.builder(ytConf)
      .http()
      .build()

  protected lazy val ytClientAsync: AsyncCloseableYt =
    AsyncYt
      .builder(ytConf)
      .http()
      .build()

  def initContainer(): Unit = {
    YtTest.ytContainer.isRunning
    ()
  }

  lazy val ytZio: RManaged[BaseEnv, YtZio] = YtZio.make(ytConfig)
}

object YtTest {

  private val syncLogger = SyncLogger[YtTest.type]
  private val ytVersion = "r8224978"
  private val ytPort = 80 // do not change, configured in containers's script

  private lazy val containerYtConf: YtConfiguration = YtConfiguration
    .builder()
    .withApiUrl(s"http://${ytContainer.getHost}:${ytContainer.getMappedPort(ytPort)}")
    .withUser("unit_tester")
    .withToken("fake_token")
    // somehow wo a role heavy command's proxy falls into 'hostname:80'
    .withRole("the-one-who-knocks")
    // todo by default java client forces java porto layer and porto tag for all jobs
    .withJobSpecPatch(null)
    .withSpecPatch(null)
    .build()

  def containerApiPort: Int = ytContainer.getMappedPort(ytPort)

  lazy val ytContainer: GenericContainer[_] = {
    syncLogger.info("Starting yt container...")
    val container: GenericContainer[_] =
      new GenericContainer(s"registry.yandex.net/vertis/vertis-yt-local:$ytVersion")

    // todo: blocked on testcontainers-scala's support of testcontainers v.1.12.4
    // or you'd be using an old yt locally
//    container.withImagePullPolicy(PullPolicy.alwaysPull())
    container.withExposedPorts(ytPort)
    container.withStartupTimeout(Duration.ofSeconds(30))
    // tmpfs is cool but yql uploads huge mrjob file
//    container.withTmpFs(Map("/tmp" -> "rw").asJava)
    // note that yt_local does not support most of the operations (sort, merge, mr), requiring some heavy 'porto' layer
    // erase is the only one we know to be supported
    container.withCommand(
      "--enable-debug-logging " +
//        "--tmpfs-path /tmp " +
        // --fqdn 127.0.0.1 is required for parts on local yt installation to find each other inside the container
        "--fqdn 127.0.0.1 " +
        "--scheduler-count 1 " +
        "--node-count 2 " +
        """--proxy-config {address_resolver={enable_ipv4=%true;enable_ipv6=%false;};coordinator={public_fqdn="yt:80"}}"""
    )
    container.withLogConsumer(toLogConsumer(syncLogger))
    // to share with yql container
    container.withNetwork(Network.SHARED)
    container.withNetworkAliases("yt")
    container.withEnv("TZ", "Europe/Moscow")
    container.withEnv("YT_LOG_LEVEL", "Debug")
    container.start()
    container
  }
}
