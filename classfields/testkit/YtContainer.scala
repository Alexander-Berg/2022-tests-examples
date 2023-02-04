package common.yt.testkit

import com.dimafeng.testcontainers.SingleContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.containers.{GenericContainer => OTCGenericContainer}
import org.testcontainers.utility.DockerImageName
import zio.ZManaged

import scala.language.existentials

/** Based on https://github.yandex-team.ru/yt/docker
  *
  * See also
  *  https://a.yandex-team.ru/arc/trunk/arcadia/market/mbi/mbi-log-processor/src/large-test/java/ru/yandex/market/mbi/logprocessor/YtContainer.java?rev=7355157
  *  https://a.yandex-team.ru/arc/trunk/arcadia/market/mbi/mbi-log-processor/src/large-test/resources/docker/docker-compose.yml?rev=7355157
  */
class YtContainer(imageName: String) extends SingleContainer[OTCGenericContainer[_]] {

  type OTCContainer = OTCGenericContainer[T] forSome {
    type T <: OTCGenericContainer[T]
  }

  override val container: OTCContainer = new OTCGenericContainer(DockerImageName.parse(imageName))

  container.withExposedPorts(80, 8002)

  container.withCommand(
    """--proxy-config "{address_resolver={enable_ipv4=%true;enable_ipv6=%false;};coordinator={public_fqdn=\"localhost:8000\"}}" --rpc-proxy-count 1 --rpc-proxy-port 8002"""
  )
  container.waitingFor(Wait.forLogMessage(".*Local YT started.*", 1))

  def httpPort: Int = container.getMappedPort(80)
  def rpcPort: Int = container.getMappedPort(8002)

  def httpApi: String = container.getHost + ":" + httpPort
}

object YtContainer {
  val StableImage = "registry.yandex.net/yt/yt:stable"

  def stable = new YtContainer(StableImage)

  val managed: ZManaged[Any, Throwable, YtContainer] = ZManaged.makeEffect(stable)(_.stop())
}
