package ru.yandex.auto.vin.decoder.partners.bmw

import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
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
class BmwClientIntTest extends AnyFunSuite {

  val service = new RemoteHttpService(
    "bmw",
    new HttpEndpoint("api.bmw.ru", 443, "https"),
    client = new ApacheHttpClient({
      val client = HttpAsyncClientBuilder
        .create()
        .setSSLContext(HttpClient.TrustAll)
        .build()
      client.start()
      client
    })
  )

  implicit val t = Traced.empty

  private val partnerEventClient = new NoopPartnerEventManager

  private val rateLimiter = RateLimiter.create(10)

  val client = new BmwClient(service, "AutoruTest", "dx4bAG", partnerEventClient, rateLimiter)

  implicit val partnerRequestTrigger: PartnerRequestTrigger =
    PartnerRequestTrigger.Unknown

  test("vehicle") {
    val vin = VinCode("X4XKS694600K29362")
    val res = client.fetch(vin).await

    assert(res.identifier === vin)
  }

}
