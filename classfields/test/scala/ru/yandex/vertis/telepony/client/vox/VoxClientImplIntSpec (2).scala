package ru.yandex.vertis.telepony.client.vox
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.service.impl.vox.{VoxClient, VoxClientImpl}
import ru.yandex.vertis.telepony.util.http.client.{HttpClientBuilder, PipelineBuilder}

/**
  * @author neron
  */
@Ignore
class VoxClientImplIntSpec extends TestKit(ActorSystem("VoxClientImplSpec")) with VoxClientSpec {

  implicit val am = Materializer(system)

  override lazy val client: VoxClient = VoxClientImplIntSpec.createClient()

}

object VoxClientImplIntSpec {

  def createClient()(implicit am: Materializer): VoxClient = {
    import am.executionContext

    val sendReceive = PipelineBuilder.buildSendReceive(proxy = None, maxConnections = 2)
    val httpClient = HttpClientBuilder.fromSendReceive("vox-client-spec", sendReceive)

    new VoxClientImpl(
      httpClient = httpClient,
      accountId = "2148380",
      apiKey = "???", // see in yav.yandex-team.ru
      host = "api.voximplant.com",
      port = 443,
      basePath = "/platform_api",
      applicationName = "test.yavert-test.voximplant.com"
    )
  }

}
