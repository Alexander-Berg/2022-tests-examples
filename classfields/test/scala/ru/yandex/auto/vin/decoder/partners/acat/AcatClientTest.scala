package ru.yandex.auto.vin.decoder.partners.acat

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{ApacheHttpClient, HttpClient, HttpEndpoint, RemoteHttpService}

import scala.concurrent.ExecutionContext.Implicits.global

@Ignore
class AcatClientTest extends AnyFunSuite {

  val requestConfig = {
    val default = RequestConfig.copy(RemoteHttpService.DefaultRequestConfig)
    val proxy = new HttpHost("proxy-ext.test.vertis.yandex.net", 3128)
    default.setProxy(proxy).build()
  }

  implicit val t = Traced.empty

  private val rateLimiter = RateLimiter.create(10)

  val service = new RemoteHttpService(
    name = "acat",
    requestConfig = requestConfig,
    endpoint = new HttpEndpoint("acat.online", 443, "https"),
    client = new ApacheHttpClient({
      val client = HttpAsyncClientBuilder
        .create()
        .setSSLContext(HttpClient.TrustAll)
        .build()
      client.start()
      client
    })
  )

  private val partnerEventClient = new NoopPartnerEventManager

  val token = "test-token"
  val client = new AcatClient(service, token, partnerEventClient, rateLimiter)

  implicit val partnerRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger.Unknown

  test("vehicle found by vin") {
    val vin = VinCode("JSAFJB33V00122511")
    val res = client.fetch(vin).await

    assert(res.identifier === vin)
    assert(res.optData.nonEmpty)
  }

  test("not found vehicle by vin") {
    val vin = VinCode("U5YFF24227L420768")
    val res = client.fetch(vin).await

    assert(res.identifier === vin)
    assert(res.optData.isEmpty)
  }
}
