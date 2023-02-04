package ru.yandex.vertis.telepony.client.autoru

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.testkit.TestKit
import org.scalatest.Ignore
import ru.yandex.vertis.telepony.http.HttpClientImpl
import ru.yandex.vertis.telepony.service.AutoRuSearcherClient
import ru.yandex.vertis.telepony.service.impl.autoru.AutoRuSearcherClientImpl
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.http.client.PipelineBuilder

@Ignore
class AutoRuSearcherClientImplIntSpec extends TestKit(ActorSystem("VoxClientImplSpec")) with AutoRuSearcherClientSpec {

  implicit val am = Materializer(system)
  implicit val ec = Threads.lightWeightTasksEc

  private val httpClient =
    new HttpClientImpl("default-spec-client", PipelineBuilder.buildSendReceive(proxy = None, maxConnections = 2), None)

  override lazy val autoRuSearcherClient: AutoRuSearcherClient =
    new AutoRuSearcherClientImpl(
      httpClient = httpClient,
      host = "auto-searcher-01-sas.test.vertis.yandex.net",
      port = 34389
    )

}
