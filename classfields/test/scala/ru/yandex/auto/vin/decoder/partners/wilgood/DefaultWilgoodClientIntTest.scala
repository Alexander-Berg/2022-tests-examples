package ru.yandex.auto.vin.decoder.partners.wilgood

import auto.carfax.common.utils.http.TestHttpUtils
import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.Ignore
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.event.NoopPartnerEventManager
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

@Ignore
class DefaultWilgoodClientIntTest extends AsyncFunSuite with MockitoSupport {

  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown
  implicit val t = Traced.empty

  val service = new RemoteHttpService(
    name = "wilgood",
    endpoint = new HttpEndpoint("exch02.wilgood.ru", 80, "http"),
    client = TestHttpUtils.DefaultHttpClient
  )

  private val rateLimiter = RateLimiter.create(10)
  private val partnerEventClient = new NoopPartnerEventManager
  private val client = new DefaultWilgoodClient(service, partnerEventClient, rateLimiter)

  test("get info") {
    val vin = VinCode("JMZBK12F601785337")
    client.fetch(vin).map { response =>
      println(response.raw)
      assert(response.identifier == vin)
    }
  }
}
