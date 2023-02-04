package ru.yandex.auto.vin.decoder.partners.audatex

import auto.carfax.common.utils.config.Environment
import auto.carfax.common.utils.http.TestHttpUtils
import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.cache.AudatexDealersCache
import ru.yandex.auto.vin.decoder.clients.PalmaClient
import ru.yandex.auto.vin.decoder.grpc.GrpcClientBuilder
import ru.yandex.auto.vin.decoder.hydra.HydraClientStub
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.auto.vin.decoder.tvm.{DefaultTvmConfig, DefaultTvmTicketsProvider}
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vertis.palma.services.proto_dictionary_service.ProtoDictionaryServiceGrpc

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Ignore
class AudatexRawFetchersIntTest extends AnyFunSuite {

  implicit private val m: TestOperationalSupport = TestOperationalSupport
  implicit private val trigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t = Traced.empty
  implicit val tracer: Tracer = NoopTracerFactory.create()

  private lazy val httpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("www.audatex.ru", 443, "https")
    new RemoteHttpService(name = "audatex", endpoint = endpoint, client = TestHttpUtils.DefaultHttpClient)
  }

  private lazy val rateLimiter = RateLimiter.create(10)
  private lazy val eventClient = new NoopPartnerEventManager()
  private lazy val hydraClient = new HydraClientStub(2)

  private lazy val tvmTicketsProvider = DefaultTvmTicketsProvider(
    DefaultTvmConfig(Environment.config.getConfig("auto-vin-decoder.tvm"))
  )

  private lazy val palmaClient = {
    val client = new GrpcClientBuilder(Feature("", _ => false))
      .build("auto-vin-decoder.palma", ProtoDictionaryServiceGrpc.blockingStub)
    new PalmaClient(client)
  }
  private lazy val audatexDealersCache = new AudatexDealersCache(palmaClient, tvmTicketsProvider)

  private lazy val audatexClient =
    new AudatexClient(
      httpService,
      eventClient,
      hydraClient,
      audatexDealersCache,
      Feature("max_clicks", _ => 20000),
      Feature("max_clicks", _ => 20000)
    )

  private lazy val audaHistoryFetcher =
    new AudatexAudaHistoryFetcher("RU434494", "2UukZijt", audatexClient, rateLimiter)

  private lazy val fraudCheckFetcher =
    new AudatexFraudCheckFetcher("RU434494", "2UukZijt", audatexClient, rateLimiter)

  test("get history with dealer login") {
    val trigger = PartnerRequestTrigger.Unknown // .copy(clientId = 38964L.some)
    val res = Await.result(audaHistoryFetcher.fetch(VinCode("XW8ZZZ61ZDG068605"))(t, trigger), 10.second)

    assert(res.optReports.nonEmpty)
  }

  test("get fraud check") {
    val trigger = PartnerRequestTrigger.Unknown
    val res = Await.result(fraudCheckFetcher.fetch(VinCode("WAUZZZ8U1HR059815"))(t, trigger), 10.second)

    assert(res.claims.nonEmpty)
  }
}
