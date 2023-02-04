package ru.yandex.auto.vin.decoder.partners.nbki

import auto.carfax.common.clients.context.CommonHttpContext
import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import com.typesafe.config.ConfigFactory
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import org.scalatest.Ignore
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.partners.nbki.NbkiPledgesFetcher.NbkiFetcherConfig
import ru.yandex.auto.vin.decoder.partners.nbki.TestClients.{buildNbkiClient, buildYsignClient}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.tvm.{DefaultTvmConfig, DefaultTvmTicketsProvider, TvmConfig, TvmTicketsProvider}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.config.Environment.config
import ru.yandex.auto.vin.decoder.ysign.YsignManager.YsignConfig
import ru.yandex.auto.vin.decoder.ysign.{YsignClient, YsignManager}
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.ops.MetricsSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport

import java.util.concurrent.ForkJoinPool
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.MapHasAsJava

@Ignore
class NbkiPledgesFetcherIntTest extends AsyncFunSuite {

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t = Traced.empty
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(2))
  implicit val rateLimiter = RateLimiter.create(1.0)

  test("check pledges: testing") {
    val nbkiClient = buildNbkiClient(
      Map(
        "host" -> "collatauto.demo.nbki.ru",
        "port" -> 8080,
        "schema" -> "http"
      ),
      "/CollatAuto/collatauto"
    )

    val cert = YsignConfig(19270, "kostenko")

    val ysignClient = buildYsignClient("ysign-test.yandex-team.ru")
    val testConfig = NbkiFetcherConfig(
      user = "7001SS000005",
      memberCode = "7001SS000000",
      password = "wxuSsCZ3"
    )
    val fetcher = new NbkiPledgesFetcher(
      testConfig,
      nbkiClient,
      ysignManager = new YsignManager(cert, ysignClient),
      rateLimiter
    )

    val vin = VinCode("WAUZZZ8U3HR075806")
    val result = fetcher.fetch(vin).await
    assert(result.rawStatus == "200")
  }

  test("check pledges: prod") {
    val nbkiClient = buildNbkiClient(
      Map(
        "host" -> "squid-01-sas.test.vertis.yandex.net",
        "port" -> 4431,
        "schema" -> "http"
      ),
      "/collatauto"
    )

    val cert = YsignConfig(66606, "antkrotov")

    val ysignClient = buildYsignClient("ysign.yandex-team.ru")
    val prodConfig = NbkiFetcherConfig(user = "7001SS000002", memberCode = "7001SS000000", password = "kYc7pHhh")
    val manager = new NbkiPledgesFetcher(
      prodConfig,
      nbkiClient,
      ysignManager = new YsignManager(cert, ysignClient),
      rateLimiter
    )

    val vin = VinCode("WBA3Y510X0GZ92038")
    val result = manager.fetch(vin).await
    assert(result.rawStatus == "200")
  }
}

object TestClients extends CommonHttpContext {
  implicit override val m: MetricsSupport = TestOperationalSupport
  implicit override val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val tracer: Tracer = NoopTracerFactory.create()

  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager

  def buildYsignClient(host: String) = {
    val ysignRemoteService = new RemoteHttpService(
      "ysign",
      new HttpEndpoint(host, 80, "http")
    )

    lazy val tvmConfig: TvmConfig = DefaultTvmConfig(config.getConfig("auto-vin-decoder.tvm"))
    lazy val tvmTicketsProvider: TvmTicketsProvider = DefaultTvmTicketsProvider(tvmConfig)

    new YsignClient(ysignRemoteService, tvmTicketsProvider)
  }

  def buildNbkiClient(config: Map[String, Any], endpoint: String) = {
    val http = buildProxiedHttpService("nbki", disableSsl = true, config = Some(ConfigFactory.parseMap(config.asJava)))
    new NbkiClient(http, endpoint, partnerEventClient)
  }
}
