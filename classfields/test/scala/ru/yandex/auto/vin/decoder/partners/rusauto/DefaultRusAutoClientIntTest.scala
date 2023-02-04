package ru.yandex.auto.vin.decoder.partners.rusauto

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.apache.http.HttpHost
import org.apache.http.client.config.RequestConfig
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}

import scala.concurrent.Await
import scala.concurrent.duration._

@Ignore
class DefaultRusAutoClientIntTest extends AnyFunSuite {

  private lazy val client = buildClient()
  implicit lazy val trigger = PartnerRequestTrigger.Unknown
  implicit lazy val t = Traced.empty
  private lazy val rateLimiter = RateLimiter.create(10)

  test("found response") {
    pending
    val vin = VinCode("KPACA1EKS9P052260")
    val res = Await.result(client.fetch(vin), 3.seconds)

    assert(res.identifier === "KPACA1EKS9P052260")
    assert(res.rawStatus === "200")
    assert(res.response.head.status === true)
    assert(res.response.head.carData.get.orders.length === 3)
  }

  test("not found") {
    pending
    val vin = VinCode("KPACA1EKS9P052261")
    val res = Await.result(client.fetch(vin), 3.seconds)

    assert(res.identifier === "KPACA1EKS9P052261")
    assert(res.rawStatus === "200")
    assert(res.response.head.status === false)
    assert(res.response.head.carData.isEmpty === true)
  }

  private def buildClient() = {
    val endpoint: HttpEndpoint = HttpEndpoint(
      "188.64.129.21",
      80,
      "http"
    )

    val proxy = new HttpHost("infra-proxy.test.vertis.yandex.net", 3128)

    val requestConfig: RequestConfig = RequestConfig
      .copy(RemoteHttpService.DefaultRequestConfig)
      .setProxy(proxy)
      .build()

    val service = new RemoteHttpService(
      "rus_auto",
      requestConfig = requestConfig,
      endpoint = endpoint
    )

    new DefaultRusAutoClient(service, new NoopPartnerEventManager, rateLimiter)
  }

}
