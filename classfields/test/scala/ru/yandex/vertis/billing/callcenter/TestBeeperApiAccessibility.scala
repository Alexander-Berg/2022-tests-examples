package ru.yandex.vertis.billing.callcenter

import com.codahale.metrics.MetricRegistry
import org.joda.time.DateTime
import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.common.monitoring.{CompoundHealthCheckRegistry, HealthChecks}
import ru.yandex.vertis.billing.callcenter.client.impl.BeeperCallCenterWithCampaignsClient
import ru.yandex.vertis.billing.environment.BillingConfig.runtime
import ru.yandex.vertis.billing.model_core.callcenter.CallCenterIds
import ru.yandex.vertis.billing.service.logging.LoggedSttpBackend
import ru.yandex.vertis.billing.settings.CallCenterWithCampaignsSettings
import ru.yandex.vertis.billing.util.{DateTimeInterval, DateTimeUtils}
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.codahale.CodahaleRegistry
import ru.yandex.vertis.ops.prometheus.{CompositeCollector, OperationalAwareRegistry, PrometheusRegistry}
import ru.yandex.vertis.util.ahc.{AsyncHttpClientBuilder, HttpSettings, ProxySettings}
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.monad._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

@Ignore
class TestBeeperApiAccessibility extends AnyWordSpec with Matchers {

  val campaignIds = Seq("5154")
  val baseUrl = "http://autorucalls.beeper.ru"

  val campaignIdToUrl = campaignIds.map { id =>
    id -> baseUrl
  }.toMap

  val clientSettings = HttpSettings(
    1.minute,
    1.minute,
    3,
    16,
    followRedirect = true,
    proxy = Some(ProxySettings("localhost", 3000))
  )

  val settings = CallCenterWithCampaignsSettings(
    CallCenterIds.Beeper,
    campaignIdToUrl
  )

  implicit val helperExecutor = ExecutionContext.global
  val asyncClient = AsyncHttpClientBuilder.createClient(clientSettings)

  implicit val clientFutureBackend = {
    new LoggedSttpBackend[Future, Any](AsyncHttpClientFutureBackend.usingClient(asyncClient))
  }
  implicit val monadError = new FutureMonad()

  val ops: OperationalSupport = new OperationalSupport {
    implicit override val codahaleRegistry: CodahaleRegistry = new MetricRegistry
    override val healthChecks: CompoundHealthCheckRegistry = HealthChecks.compoundRegistry()

    implicit override val prometheusRegistry: PrometheusRegistry =
      new OperationalAwareRegistry(
        new CompositeCollector,
        runtime.environment.toString,
        runtime.localDataCenter,
        runtime.hostname,
        ""
      )
  }

  val client = new BeeperCallCenterWithCampaignsClient[Future](settings)

  "CallCenterWithCampaignsClient" should {
    "get calls" in {
      val from = DateTime.parse("2020-01-29T18:34.000").withZone(DateTimeUtils.TimeZone)
      val to = DateTime.parse("2020-01-29T19:00.000").withZone(DateTimeUtils.TimeZone)
      val interval = DateTimeInterval(from, to)
      val calls = Await.result(client.calls(interval), 1.minute)
      calls.foreach(println)
    }
  }

}
