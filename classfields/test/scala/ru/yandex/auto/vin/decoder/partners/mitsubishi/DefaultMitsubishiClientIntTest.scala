package ru.yandex.auto.vin.decoder.partners.mitsubishi

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.CommonVinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.config.Environment.config
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Ignore
class DefaultMitsubishiClientIntTest extends AnyFunSuite with MockitoSupport {

  implicit private val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  private val rateLimiter = RateLimiter.create(10)

  private lazy val proxy = new HttpHost("proxy-ext.test.vertis.yandex.net", 3128)

  private lazy val endpoint: HttpEndpoint = HttpEndpoint(
    "ws.mmcrus.com",
    90,
    "http"
  )

  private lazy val requestConfig = {
    RequestConfig
      .copy(RemoteHttpService.DefaultRequestConfig)
      .setProxy(proxy)
      .build()
  }

  private lazy val service = new RemoteHttpService(
    name = "mitsu",
    requestConfig = requestConfig,
    endpoint = endpoint
  )

  // https://yav.yandex-team.ru/secret/sec-01e1krhsemte36abb3gchf23ah
  private val partnerEventClient = new NoopPartnerEventManager
  implicit val t = Traced.empty

  private lazy val client = new DefaultMitsubishiClient(
    user = config.getString("auto-vin-decoder.mitsubishi.user"),
    password = config.getString("auto-vin-decoder.mitsubishi.password"),
    httpService = service,
    partnerEventClient = partnerEventClient,
    rateLimiter
  )

  test("int") {
    val vin = CommonVinCode("JMBSRCS3A5U010881")
    Await.result(client.fetch(vin), 10.seconds)
  }
}
