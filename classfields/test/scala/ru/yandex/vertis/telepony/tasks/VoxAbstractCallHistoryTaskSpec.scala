package ru.yandex.vertis.telepony.tasks

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.scalatest.Ignore
import ru.yandex.vertis.application.runtime.VertisRuntime
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.PrometheusRegistry
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.generator.Generator._
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.journal.WriteJournal
import ru.yandex.vertis.telepony.model.CallbackOrder._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.model.vox.GetCallHistoryItem
import ru.yandex.vertis.telepony.operational.Operational
import ru.yandex.vertis.telepony.service.impl.vox.{VoxClient, VoxClientImpl}
import ru.yandex.vertis.telepony.service.{CallbackOrderService, DateTimeStorage}
import ru.yandex.vertis.telepony.tasks.vox.VoxCallbackHistoryTask
import ru.yandex.vertis.telepony.util.Threads
import ru.yandex.vertis.telepony.util.http.client.{HttpClientBuilder, PipelineBuilder}

import scala.concurrent.Future

class VoxAbstractCallHistoryTaskSpec {

  implicit val ac = ActorSystem("test", ConfigFactory.empty())
  implicit val mat = ActorMaterializer()
  implicit val ec = Threads.lightWeightTasksEc
  implicit val prometheusRegistry = Operational.default(VertisRuntime).prometheusRegistry

  protected val domain = TypedDomains.autoru_def

  protected val client: VoxClient = {
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
