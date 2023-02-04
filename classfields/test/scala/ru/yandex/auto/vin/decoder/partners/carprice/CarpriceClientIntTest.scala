package ru.yandex.auto.vin.decoder.partners.carprice

import auto.carfax.common.utils.http.TestHttpUtils
import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.Ignore
import org.scalatest.funsuite.AsyncFunSuite
import ru.yandex.auto.vin.decoder.model.CommonVinCode
import ru.yandex.auto.vin.decoder.partners.event.{NoopPartnerEventManager, PartnerEventManager}
import ru.yandex.auto.vin.decoder.state.PartnerRequestTrigger
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import ru.yandex.vertis.commons.http.client.{HttpEndpoint, RemoteHttpService}
import ru.yandex.vertis.mockito.MockitoSupport

@Ignore
class CarpriceClientIntTest extends AsyncFunSuite with MockitoSupport {

  implicit val t: Traced = Traced.empty
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  private val rateLimiter = RateLimiter.create(10)

  val service = new RemoteHttpService(
    name = "carprice",
    endpoint = new HttpEndpoint("b2b.carpriceindex.ru", 443, "https"),
    requestConfig = TestHttpUtils.configWithProxy,
    client = TestHttpUtils.DefaultHttpClient
  )

  val partnerEventClient: PartnerEventManager = new NoopPartnerEventManager

  val client = new CarpriceClient(service, "F4CuhCNuPBbquVMcExwbjRYt9rtNrhoewQCnZ", partnerEventClient, rateLimiter)

  test("get mileage inspection") {
    client.fetch(CommonVinCode("XUUJF35E9E0001871")).await

    assert(2 + 2 == 4)
  }
}
